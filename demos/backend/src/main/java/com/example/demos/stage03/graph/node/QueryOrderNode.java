package com.example.demos.stage03.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.demos.stage03.loop.FixtureToolDispatcher;
import com.example.demos.stage03.loop.StepRecord;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点一：查订单。整张图的第一个节点，{@code START} 直接连到它。
 * <p>
 * <b>干什么</b>：从问句里抽订单号，拿去 {@link FixtureToolDispatcher} 查内存表，把整行订单 JSON 写进状态。
 * 抽不到单号时兜底用 {@code ORD-1001}，保证演示页不填单号也能跑通。
 * <p>
 * <b>从状态里读</b>：{@code question}。
 * <br>
 * <b>往状态里写</b>：
 * <ul>
 *   <li>{@code orderId}：本次实际查的单号，供日志和页面排查</li>
 *   <li>{@code orderJson}：整行 JSON；查不到时是 {@code {"error":"not_found",...}}，后面的库存节点据此跳过查询</li>
 *   <li>{@code orderStatus}：单独抽出状态值（PAID / CANCELLED），条件边直接拿它判断</li>
 *   <li>{@code steps}：一条查表记录，追加进轨迹</li>
 *   <li>{@code nodes}：节点名，追加进「走过的节点」列表</li>
 * </ul>
 * <p>
 * <b>它不知道下一步是谁</b>。查完返回 Map 就结束，边在 {@code ShipGraph} 里用 {@code addEdge} 连。
 * 这是图和普通方法调用最大的差别：节点之间不直接传参，也不互相调用，只通过状态交换数据。
 * <p>
 * 注意抽单号只影响「查哪张单」，不影响「走哪条边」——边仍然是写死的，
 * 问句驱动的是数据，不是流程。
 */
public class QueryOrderNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(QueryOrderNode.class);

    /** 问句里没有合法单号时的兜底，让「只问状态」这类演示句也能跑。 */
    static final String DEFAULT_ORDER_ID = "ORD-1001";

    /** 和 dispatcher 的格式校验同一条规则，避免抽出一个注定 invalid_argument 的值。 */
    private static final Pattern ORDER_ID = Pattern.compile("ORD-\\d+");

    /** 查表实现与第 2 天手写循环共用，所以 not_found / invalid_argument 的判定完全一致。 */
    private final FixtureToolDispatcher dispatcher;

    public QueryOrderNode(FixtureToolDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * 节点主体。框架会把这个方法包成异步动作，在轮到本节点时调用一次。
     *
     * @param state 当前全局状态。它同时包含用户输入和前几个节点写过的东西
     * @return 要合并回状态的键值对。只放本次新增的，不要重复写整个列表
     */
    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 读状态的固定写法：value(key) 拿到 Optional，缺 key 时用 orElse 兜底，不会抛异常
        String question = state.value("question").map(String::valueOf).orElse("");
        String orderId = extractOrderId(question);
        log.info("[graph] 节点 queryOrder 开始 question={} orderId={}", question, orderId);

        // 组装工具参数。查表逻辑走 dispatcher，这里不直接碰 OrderStore
        String arguments = "{\"orderId\":\"" + orderId + "\"}";
        FixtureToolDispatcher.Observation observation = dispatcher.dispatch("getOrder", arguments);
        log.info("[graph] 节点 queryOrder 观察={}", observation.json());

        // 一条查表轨迹。轮次写 1，因为它固定是图中第一个查表的节点
        StepRecord step = new StepRecord(
                1, "graph", "getOrder", arguments, observation.json(), observation.latencyMs());

        // 返回的 Map 会被框架合进状态。nodes / steps 是追加策略，这里只给新的那一条
        return Map.of(
                "orderId", orderId,
                "orderJson", observation.json(),
                "orderStatus", FixtureJson.text(observation.json(), "status"),
                "steps", List.of(step),
                "nodes", List.of("queryOrder"));
    }

    /**
     * 从问句里抽第一个合法单号。抽不到用 {@link #DEFAULT_ORDER_ID}。
     * 只认 {@code ORD-} 加数字，大小写不同的写法当作没提，免得把 ord-1001 查成 not_found。
     */
    static String extractOrderId(String question) {
        Matcher matcher = ORDER_ID.matcher(question);
        return matcher.find() ? matcher.group() : DEFAULT_ORDER_ID;
    }
}
