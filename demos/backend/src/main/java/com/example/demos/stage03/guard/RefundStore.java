package com.example.demos.stage03.guard;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 写操作那一侧的两份内存数据：订单的退款标记，以及暂停中的退款调用。
 *
 * <h2>为什么单独开一个 Store</h2>
 * 阶段 2 的 {@code OrderStore} 里，订单是不可变的 record，没有「改状态」的方法——
 * 这是故意的：查询用的夹具不该被写。今天要能看见「确认前没退、确认后才退」，
 * 所以单独放一张小表，用 orderId 作 key，值为是否已退款。
 * <p>
 * 这样做还有个好处：{@code OrderStore} 全程只读，页面上「订单还是 PAID、库存还是 10」
 * 就能当作「查询没被污染」的证据。
 *
 * <h2>两张表分别是什么</h2>
 * <table border="1">
 *   <caption>字段</caption>
 *   <tr><th>表</th><th>key</th><th>value</th><th>谁写</th></tr>
 *   <tr><td>{@code refunded}</td><td>orderId</td><td>是否已退款</td><td>只有 {@link #confirm}</td></tr>
 *   <tr><td>{@code paused}</td><td>runId</td><td>待执行的退款</td><td>{@link #pause} 写，{@link #confirm} 取走</td></tr>
 * </table>
 *
 * <h2>和生产环境的差距</h2>
 * 两张表都在堆里，进程一停就没了。生产上暂停表通常落 Redis 或数据库，
 * 因为「等用户确认」可能跨越几分钟、甚至跨服务重启。这一点和第 6 天的内存记忆仓库是同一类取舍。
 */
@Component
public class RefundStore {

    private static final Logger log = LoggerFactory.getLogger(RefundStore.class);

    /** 菜单里的三个工具名。多处引用同一份字符串，抽出来避免条件分支里拼错。 */
    public static final String TOOL_GET_ORDER = "getOrder";
    public static final String TOOL_GET_INVENTORY = "getInventory";
    public static final String TOOL_REFUND = "refundOrder";

    /** 菜单里全部工具名。模型点了不在这个集合里的名字，就是「菜单里没有」。 */
    public static final List<String> MENU = List.of(TOOL_GET_ORDER, TOOL_GET_INVENTORY, TOOL_REFUND);

    /**
     * orderId → 是否已退款。
     * <p>
     * 用 {@link ConcurrentHashMap} 而不是 HashMap：挂起和确认是两次独立 HTTP 请求，
     * 可能落在两个 Tomcat 线程上，甚至用户连点两次确认。并发容器保证不会读到半更新的状态。
     * <p>
     * 换成真实项目，这里就是一条 UPDATE 语句，而不是一个 Map。
     */
    private final Map<String, Boolean> refunded = new ConcurrentHashMap<>();

    /**
     * runId → 被拦下的那次退款调用。
     * <p>
     * 这张表就是「暂停点」在代码里的样子：{@code run()} 往里放，{@code confirm()} 把它取走。
     * 两次 HTTP 请求之间，整个运行的状态就靠这一条记录维系。
     * <p>
     * 换到生产环境，这张表通常落在 Redis 或数据库里，因为进程重启不能丢掉待确认的操作。
     * 本阶段放内存，重启即失效，和第 6 天的内存记忆仓库是同一类取舍。
     */
    private final Map<String, PendingRefund> paused = new ConcurrentHashMap<>();

    /**
     * 一次等着确认的退款。
     * <p>
     * 只存「要做什么」，不存模型说过的话——确认接口拿到它就能直接执行，不需要再问模型一遍。
     *
     * @param runId   这次运行的 id，同时也是暂停表的主键
     * @param orderId 要退的单号，从模型的参数里解析出来
     * @param round   第几轮模型点菜时被拦下的。确认后回填到轨迹里，两次轨迹的 round 能对上
     */
    public record PendingRefund(String runId, String orderId, int round) {}

    /** 没登记过的单号按「未退」处理，不抛异常。用 Boolean.TRUE.equals 避免自动拆箱 NPE。 */
    public boolean isRefunded(String orderId) {
        return Boolean.TRUE.equals(refunded.get(orderId));
    }

    /**
     * 把一次退款调用挂起，等确认接口来取。<b>此时不改退款标记</b>——
     * 这正是「确认前没有执行」的证据：响应里 refunded 仍是 false。
     */
    public void pause(PendingRefund pending) {
        paused.put(pending.runId(), pending);
        log.info("[guard] 退款调用已挂起 runId={} orderId={} round={}",
                pending.runId(), pending.orderId(), pending.round());
    }

    /**
     * 确认并执行。取出后从暂存里删掉，所以同一个 runId 只能确认一次——
     * 第二次点确认走「取不到」这条路，不会重复退款。
     * <p>
     * <b>这是整条链路上唯一改退款标记的地方。</b>
     * 查询节点不碰它，挂起时不碰它，模型更碰不到它。
     *
     * @return 找到并执行了就是这次退款；runId 错或已确认过返回 null
     */
    public PendingRefund confirm(String runId) {
        /* remove 是原子的：并发点两次确认时，只有一个线程能拿到非 null */
        PendingRefund pending = paused.remove(runId);
        if (pending == null) {
            log.warn("[guard] 确认失败：runId 不存在或已被确认过 runId={}", runId);
            return null;
        }
        refunded.put(pending.orderId(), true);
        log.info("[guard] 确认执行退款 orderId={} runId={}", pending.orderId(), runId);
        return pending;
    }

    /** 当前挂起中的数量。排查用：正常流程走完后它应该回到 0，没有残留。 */
    public int pausedCount() {
        return paused.size();
    }
}
