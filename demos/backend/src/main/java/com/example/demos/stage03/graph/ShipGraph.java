package com.example.demos.stage03.graph;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.example.demos.stage03.graph.node.QueryInventoryNode;
import com.example.demos.stage03.graph.node.QueryOrderNode;
import com.example.demos.stage03.graph.node.WriteCopyNode;
import com.example.demos.stage03.loop.FixtureToolDispatcher;
import com.example.demos.stage03.loop.StepRecord;
import com.example.demos.stage03.plan.PlanningModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 第 9 天 · 固定工作流。写法照 Graph Core 快速开始：
 * 每个节点是一个 {@code NodeAction} 实现类（都在 {@code node} 子包里），这里只负责把它们连起来。
 * <p>
 * <b>图长这样</b>（方括号是节点，箭头是边）：
 * <pre>
 * START → [queryOrder] → [queryInventory] →（条件边 decideBranch）
 *                                             ├─ canShip      → [canShipCopy]    → END
 *                                             └─ cannotShip  → [cannotShipCopy] → END
 * </pre>
 * 前两条是 {@code addEdge} 写的固定边，只有最后那条条件边是运行时判断的，判断逻辑在 {@link #decideBranch}。
 * <p>
 * <b>和前面几天的差别只有一处：下一跳是谁定的。</b>
 * 第 2～8 天由模型在 {@code tool_calls} 里点；这里由边写死。
 * 模型只活在最后那个文案节点里，负责把事实写成句子，想再查表也没有工具可点。
 * <p>
 * 状态是 {@link OverAllState}，本质是一个在节点之间传递的 Map。节点返回的 Map 会被合进去，
 * 合并方式由 {@link KeyStrategy} 决定：{@code nodes} / {@code steps} 追加，其余覆盖。
 */
@Component
public class ShipGraph {

    private static final Logger log = LoggerFactory.getLogger(ShipGraph.class);

    // ---- 状态里的 key。节点写入和条件边读取都用这几个名字 ----
    /** 用户原句。查订单节点从这里抽单号，文案节点不用它。 */
    private static final String KEY_QUESTION = "question";
    /** 本次实际查的单号，由 queryOrder 节点从问句抽出后写入。查不到单号时是兜底值。 */
    private static final String KEY_ORDER_ID = "orderId";
    /** 本次判断用的可用数量，来自请求参数。没有这个 key 时库存节点读夹具。 */
    private static final String KEY_OVERRIDE = "availableOverride";
    /** 订单整行 JSON，由 queryOrder 节点写入。 */
    private static final String KEY_ORDER_JSON = "orderJson";
    /** 订单状态值（PAID / CANCELLED），由 queryOrder 节点抽出，条件边直接判断它。 */
    private static final String KEY_ORDER_STATUS = "orderStatus";
    /** 本次判断实际用的数量，由 queryInventory 节点写入。可能是 override。 */
    private static final String KEY_AVAILABLE = "available";
    /** 夹具里的真实数量，由 queryInventory 节点写入。override 不影响它。 */
    private static final String KEY_FIXTURE_AVAILABLE = "fixtureAvailable";
    /** 条件边的结果名，由文案节点写入，响应里原样返回。 */
    private static final String KEY_BRANCH = "branch";
    /** 模型写出的那句话，由文案节点写入。 */
    private static final String KEY_ANSWER = "answer";
    /** 走过的节点名列表，追加策略。它就是页面上的执行轨迹，也是天然的审计日志。 */
    private static final String KEY_NODES = "nodes";
    /** 查表轨迹，追加策略。和手写循环的 steps 同一个类型，方便页面对照。 */
    private static final String KEY_STEPS = "steps";

    // ---- 节点名。addNode、addEdge 和条件边的映射表都用同一份字符串 ----
    /** 节点一：查订单。入口节点。 */
    private static final String NODE_QUERY_ORDER = "queryOrder";
    /** 节点二：查库存。固定边从 queryOrder 连过来。 */
    private static final String NODE_QUERY_INVENTORY = "queryInventory";
    /** 节点三：写「可以发」的文案。条件边的一个分支。 */
    private static final String NODE_CAN_SHIP = "canShipCopy";
    /** 节点四：写「不能发」的文案。条件边的另一个分支。 */
    private static final String NODE_CANNOT_SHIP = "cannotShipCopy";
    /** 条件边返回值之一。必须和 addConditionalEdges 映射表里的 key 一字不差。 */
    private static final String BRANCH_CAN_SHIP = "canShip";
    /** 条件边返回值之二。 */
    private static final String BRANCH_CANNOT_SHIP = "cannotShip";

    private final FixtureToolDispatcher dispatcher;
    private final PlanningModel planningModel;

    public ShipGraph(FixtureToolDispatcher dispatcher, PlanningModel planningModel) {
        this.dispatcher = dispatcher;
        this.planningModel = planningModel;
    }

    /**
     * 走一遍图。
     * <p>
     * 输入只有问句和一个可选的覆盖值。问句决定<b>查哪张单</b>（订单节点从里面抽单号），
     * 但不决定<b>走哪条边</b>——边仍然写死，所以只问状态时库存也照样会被查一次。
     * 订单查不到时库存节点跳过查询，条件边看到 available=0 走不可发分支。
     * 输出的 nodes / steps / branch / answer 全部从最终状态里取，不从节点里另存一份。
     *
     * @param override 本次判断用的可用数量。null 表示读夹具
     */
    public GraphResponse run(String question, Integer override) {
        // 初始状态。override 没传时干脆不放这个 key，库存节点那边 orElse(null) 自然会用夹具
        Map<String, Object> input = new HashMap<>();
        input.put(KEY_QUESTION, question);
        if (override != null) {
            input.put(KEY_OVERRIDE, override);
        }

        log.info("[graph] 开始 question={} override={}", question, override);
        OverAllState state = invoke(buildSafely(), input);

        // 四个结果都从最终状态里读。nodes 的顺序就是执行顺序
        List<String> nodes = listValue(state, KEY_NODES);
        List<StepRecord> steps = listValue(state, KEY_STEPS);
        String branch = state.value(KEY_BRANCH).map(String::valueOf).orElse("");
        String answer = state.value(KEY_ANSWER).map(String::valueOf).orElse("");
        // 夹具真实值由库存节点顺手写进状态；override 只影响 available，不影响这一项
        int fixtureAvailable = state.value(KEY_FIXTURE_AVAILABLE).map(v -> (Integer) v).orElse(0);
        String orderId = state.value(KEY_ORDER_ID).map(String::valueOf).orElse("");

        log.info("[graph] 结束 orderId={} nodes={} branch={} fixtureAvailable={}",
                orderId, nodes, branch, fixtureAvailable);
        return new GraphResponse(orderId, nodes, branch, answer, fixtureAvailable, steps);
    }

    /**
     * 建图。顺序：声明状态策略 → 加节点 → 连边 → 编译。
     * <p>
     * 节点是普通的 {@code NodeAction} 实现类，这里用 {@code node_async(...)} 包一层交给图。
     * {@code addNode} / {@code addEdge} / {@code compile} 都会抛受检的
     * {@link GraphStateException}，和官方示例一样在这里声明出去，由调用方转成运行时异常。
     */
    private CompiledGraph buildGraph() throws GraphStateException {
        // ---- 第一步：声明每个 key 的合并方式 ----
        // 追加的用 AppendStrategy，只留最后一个值的用 ReplaceStrategy
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            // 这几个都是「各写一次」，后写的覆盖先写的
            strategies.put(KEY_QUESTION, new ReplaceStrategy());
            strategies.put(KEY_ORDER_ID, new ReplaceStrategy());
            strategies.put(KEY_OVERRIDE, new ReplaceStrategy());
            strategies.put(KEY_ORDER_JSON, new ReplaceStrategy());
            strategies.put(KEY_ORDER_STATUS, new ReplaceStrategy());
            strategies.put(KEY_AVAILABLE, new ReplaceStrategy());
            strategies.put(KEY_FIXTURE_AVAILABLE, new ReplaceStrategy());
            strategies.put(KEY_BRANCH, new ReplaceStrategy());
            strategies.put(KEY_ANSWER, new ReplaceStrategy());
            // 走过的节点和查表轨迹要越积越多，所以追加。
            // 这两个若错用 ReplaceStrategy，nodes 就只剩最后一个节点名，看不出走过哪几步
            strategies.put(KEY_NODES, new AppendStrategy());
            strategies.put(KEY_STEPS, new AppendStrategy());
            return strategies;
        };

        // ---- 第二步：把四个 NodeAction 包成图的节点 ----
        // 前两个查表，后两个写文案。文案节点共用 WriteCopyNode，只是措辞不同
        var queryOrder = node_async(new QueryOrderNode(dispatcher));
        var queryInventory = node_async(new QueryInventoryNode(dispatcher));
        var canShipCopy = node_async(new WriteCopyNode(
                planningModel, NODE_CAN_SHIP, BRANCH_CAN_SHIP,
                "你是电商助手。根据给定事实写一句话，说明这张单可以发货。不要编造其他订单。"));
        var cannotShipCopy = node_async(new WriteCopyNode(
                planningModel, NODE_CANNOT_SHIP, BRANCH_CANNOT_SHIP,
                "你是电商助手。根据给定事实写一句话，说明这张单暂时不能发货，并说明原因。不要编造其他订单。"));

        StateGraph workflow = new StateGraph(keyStrategyFactory)
                .addNode(NODE_QUERY_ORDER, queryOrder)
                .addNode(NODE_QUERY_INVENTORY, queryInventory)
                .addNode(NODE_CAN_SHIP, canShipCopy)
                .addNode(NODE_CANNOT_SHIP, cannotShipCopy);

        // ---- 第三步：连边 ----
        // 固定边：入口查订单，查完必然查库存。这两条不看问句，也不看模型。
        // 所以用户只问「什么状态」时，库存也照样会被查一次——这是工作流的定价
        workflow.addEdge(START, NODE_QUERY_ORDER);
        workflow.addEdge(NODE_QUERY_ORDER, NODE_QUERY_INVENTORY);

        // 条件边：从库存节点出来后，由 Java 读状态决定去哪。判断逻辑在 decideBranch 里。
        // 映射表的 key 必须和 decideBranch 的返回值一字不差，否则运行时报错
        workflow.addConditionalEdges(NODE_QUERY_INVENTORY, edge_async(this::decideBranch), Map.of(
                BRANCH_CAN_SHIP, NODE_CAN_SHIP,
                BRANCH_CANNOT_SHIP, NODE_CANNOT_SHIP));

        // 两个文案节点都到终点。它们只套句子，不再查表
        workflow.addEdge(NODE_CAN_SHIP, END);
        workflow.addEdge(NODE_CANNOT_SHIP, END);

        // ---- 第四步：编译 ----
        // recursionLimit 是死循环保护。今天这张图最长走 3 步，到不了上限；写上是为了和图有环时的写法一致
        return workflow.compile(CompileConfig.builder().recursionLimit(10).build());
    }

    /** 把受检异常收成运行时异常：图有问题应该在第一次请求时就暴露成 500，而不是编译期传染到上层。 */
    private CompiledGraph buildSafely() {
        try {
            return buildGraph();
        } catch (GraphStateException e) {
            throw new IllegalStateException("固定工作流建图失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读状态里的 List。{@code value(key, Class)} 在类型擦除下对 {@code List<StepRecord>}
     * 拿不到干净的泛型，所以这里自己做一次转换；取不到就给空 List。
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> listValue(OverAllState state, String key) {
        Object raw = state.data().get(key);
        if (raw instanceof List<?> list) {
            return (List<T>) list;
        }
        return List.of();
    }

    /**
     * 条件边。返回的是字符串，框架拿它去查上面那张映射表，
     * 所以返回值和 {@code Map.of(...)} 的 key 必须对得上，拼错会运行时报错。
     * <p>
     * 判断规则写死在这里，所以库存为 0 时<b>根本走不到</b> canShipCopy 那个节点——
     * 不是「模型通常不会这么说」，是图里没有那条路径。
     */
    private String decideBranch(OverAllState state) {
        String status = state.value(KEY_ORDER_STATUS).map(String::valueOf).orElse("");
        int available = state.value(KEY_AVAILABLE).map(v -> (Integer) v).orElse(0);
        // 两个条件都要满足：已支付，且库存大于 0
        boolean paid = "PAID".equals(status);
        String branch = paid && available > 0 ? BRANCH_CAN_SHIP : BRANCH_CANNOT_SHIP;
        log.info("[graph] 条件边 status={} available={} → {}", status, available, branch);
        return branch;
    }

    /**
     * 跑一次图。每次都给一个新的 {@code threadId}，这一条不能省。
     * <p>
     * 图是单例，{@code nodes} / {@code steps} 又是追加策略。框架按 threadId 区分运行，
     * 如果不给（或所有请求共用一个），上一次的节点名和轨迹会原样带进这一次，
     * 页面上就会看到 nodes 一次比一次长。这和 HTTP 会话要一个 sessionId 是同一回事。
     */
    private OverAllState invoke(CompiledGraph graph, Map<String, Object> input) {
        RunnableConfig config = RunnableConfig.builder()
                .threadId(UUID.randomUUID().toString())
                .build();
        try {
            Optional<OverAllState> result = graph.invoke(input, config);
            return result.orElseThrow(() -> new IllegalStateException("图没有返回状态"));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("固定工作流执行失败: " + e.getMessage(), e);
        }
    }
}
