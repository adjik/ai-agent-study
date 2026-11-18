package com.example.demos.stage03.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.demos.stage03.loop.FixtureToolDispatcher;
import com.example.demos.stage03.loop.StepRecord;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点二：查库存。由 {@code queryOrder} 的固定边连过来，<b>不管用户问了什么都会执行</b>。
 * <p>
 * <b>干什么</b>：从状态里的 {@code orderJson} 取 sku 和 warehouse，查这个坐标的可用数量。
 * 取不到（订单没查到、字段缺失）就跳过查询，直接写 0 和一条 {@code skipped} 轨迹——
 * 不拿空 sku 去查表，那只会换来一个没信息量的 {@code invalid_argument}。
 * <p>
 * <b>从状态里读</b>：{@code orderJson}、{@code availableOverride}。有 override 就用它代替夹具数量，用来演练「库存为 0」。
 * <br>
 * <b>往状态里写</b>：
 * <ul>
 *   <li>{@code available}：本次判断实际用的数字，可能是 override；订单没查到时为 0</li>
 *   <li>{@code fixtureAvailable}：夹具里的真实数字，始终来自 Store，override 不影响它</li>
 *   <li>{@code steps} / {@code nodes}：轨迹和节点名，追加</li>
 * </ul>
 * 两个数字都写进状态，是为了让「状态可改、夹具不可改」在页面上看得见。
 */
public class QueryInventoryNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(QueryInventoryNode.class);

    /** 订单查不到时的观察。它是一个明确的「没查」，不是一次失败的查询。 */
    private static final String SKIPPED_OBSERVATION = "{\"skipped\":\"order_not_found\"}";

    private final FixtureToolDispatcher dispatcher;

    public QueryInventoryNode(FixtureToolDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String orderJson = state.value("orderJson").map(String::valueOf).orElse("");
        String sku = FixtureJson.text(orderJson, "sku");
        String warehouse = FixtureJson.text(orderJson, "warehouse");

        // 没拿到坐标就跳过查表。写 0 让条件边判成不可发，比误放行安全
        if (sku.isEmpty() || warehouse.isEmpty()) {
            log.info("[graph] 节点 queryInventory 跳过：orderJson 里没有 sku/warehouse，orderJson={}", orderJson);
            StepRecord step = new StepRecord(
                    2, "graph", "getInventory", "", SKIPPED_OBSERVATION, 0L);
            return Map.of(
                    "available", 0,
                    "fixtureAvailable", 0,
                    "steps", List.of(step),
                    "nodes", List.of("queryInventory"));
        }

        // 查表。和订单节点走同一个 dispatcher，sku/warehouse 格式校验也在这里面
        String arguments = "{\"sku\":\"" + sku + "\",\"warehouse\":\"" + warehouse + "\"}";
        FixtureToolDispatcher.Observation observation = dispatcher.dispatch("getInventory", arguments);

        // 先拿到夹具真实值，再决定本次用哪个数字
        int fixtureQty = readAvailable(observation.json());
        // 没传 override 时状态里没有这个 key，orElse(null) 得到 null，于是用夹具数量
        Integer override = state.value("availableOverride").map(v -> (Integer) v).orElse(null);
        int available = override != null ? override : fixtureQty;
        log.info("[graph] 节点 queryInventory {}/{} 夹具={} override={} 本次判断用={}",
                sku, warehouse, fixtureQty, override, available);

        // 轮次写 2：在订单节点之后
        StepRecord step = new StepRecord(
                2, "graph", "getInventory", arguments, observation.json(), observation.latencyMs());

        return Map.of(
                "available", available,
                "fixtureAvailable", fixtureQty,
                "steps", List.of(step),
                "nodes", List.of("queryInventory"));
    }

    /** 从库存 JSON 里读 available。读不到按 0 处理，让条件边判成不可发，比误放行安全。 */
    static int readAvailable(String inventoryJson) {
        int index = inventoryJson.indexOf("\"available\":");
        if (index < 0) {
            return 0;
        }
        int start = index + "\"available\":".length();
        int end = start;
        while (end < inventoryJson.length() && Character.isDigit(inventoryJson.charAt(end))) {
            end++;
        }
        return end == start ? 0 : Integer.parseInt(inventoryJson.substring(start, end));
    }
}
