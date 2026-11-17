package com.example.demos.stage03.loop;

import com.example.demos.stage02.api.InventoryRecord;
import com.example.demos.stage02.api.OrderRecord;
import com.example.demos.stage02.store.InventoryStore;
import com.example.demos.stage02.store.OrderStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 查表这一段，从第 2 天的手写循环里单独拿出来。
 * <p>
 * 谁来决定调用哪把工具，是循环或规划的事；「名字加参数进来、JSON 观察出去」这一段不变。
 * 手写循环、先计划再执行、做完再评都用这一份，页面上的 <code>not_found</code> 和
 * <code>invalid_argument</code> 才是同一套规则。
 */
@Component
public class FixtureToolDispatcher {

    private static final Logger log = LoggerFactory.getLogger(FixtureToolDispatcher.class);

    /** 合法单号。不匹配的值是格式错误，不能拿去对 OrderStore。 */
    private static final Pattern ORDER_ID = Pattern.compile("ORD-\\d+");

    private final OrderStore orderStore;
    private final InventoryStore inventoryStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FixtureToolDispatcher(OrderStore orderStore, InventoryStore inventoryStore) {
        this.orderStore = orderStore;
        this.inventoryStore = inventoryStore;
    }

    /**
     * 一次工具执行的结果。json 既进轨迹，也作为 role=tool 的 content 发回模型。
     * latencyMs 只包查表和校验，不含模型 HTTP。
     */
    public record Observation(String json, long latencyMs) {}

    /**
     * 三种观察要能在页面上分开：命中是记录本身；{@code not_found} 是格式合法但表里没有；
     * {@code invalid_argument} 是进表之前被挡住。查表异常只打日志，不把堆栈交给模型。
     */
    public Observation dispatch(String name, String arguments) {
        long started = System.nanoTime();
        String observation;
        try {
            JsonNode args = parseArguments(arguments);
            /* 名字不认识时给 unknown_tool，不抛异常：这也是一次观察，模型可以在下一轮改口。 */
            observation = switch (name) {
                case "getOrder" -> orderObservation(textOrEmpty(args.get("orderId")));
                case "getInventory" -> inventoryObservation(
                        textOrEmpty(args.get("sku")), textOrEmpty(args.get("warehouse")));
                default -> toJson(Map.of("error", "unknown_tool", "name", name));
            };
        } catch (RuntimeException ex) {
            /* 堆栈只进服务端日志。观察里放异常原文，等于把内部实现交给模型和浏览器。 */
            log.error("[dispatch] failed name={} args={}", name, arguments, ex);
            observation = toJson(Map.of("error", "tool_failed", "name", name));
        }
        long latencyMs = (System.nanoTime() - started) / 1_000_000L;
        return new Observation(observation, latencyMs);
    }

    /**
     * 整行订单序列化，不能只返回 "PAID"。发货的下一轮需要这条 JSON 里的 sku 和 warehouse。
     * 格式校验在 findById 之前：空串、ABC、ord-1001 都是 invalid_argument，不进 Store。
     * 只有整串匹配 {@code ORD-} 加数字才查表；表里没有则是 not_found，例如 ORD-9999。
     */
    private String orderObservation(String orderId) {
        if (!ORDER_ID.matcher(orderId).matches()) {
            log.info("[dispatch] getOrder orderId={} result=invalid_argument", orderId);
            return toJson(Map.of("error", "invalid_argument", "orderId", orderId));
        }

        var found = orderStore.findById(orderId);
        if (found.isEmpty()) {
            log.info("[dispatch] getOrder orderId={} result=not_found", orderId);
            return toJson(Map.of("error", "not_found", "orderId", orderId));
        }

        var order = found.get();
        log.info("[dispatch] getOrder orderId={} result={}", orderId, order.status());
        return toJson(order);
    }

    /**
     * 库存先看 sku、warehouse 是否有字，空的是 invalid_argument，不去拼 Map 的 key。
     * 两个都有字才 find。找不到是 not_found，和订单的「格式对但没有这行」同一含义。
     */
    private String inventoryObservation(String sku, String warehouse) {
        if (!StringUtils.hasText(sku) || !StringUtils.hasText(warehouse)) {
            log.info("[dispatch] getInventory sku={} warehouse={} result=invalid_argument", sku, warehouse);
            return toJson(Map.of("error", "invalid_argument", "sku", sku, "warehouse", warehouse));
        }

        var found = inventoryStore.find(sku, warehouse);
        if (found.isEmpty()) {
            log.info("[dispatch] getInventory sku={} warehouse={} result=not_found", sku, warehouse);
            return toJson(Map.of("error", "not_found", "sku", sku, "warehouse", warehouse));
        }

        var row = found.get();
        log.info("[dispatch] getInventory sku={} warehouse={} result={}", sku, warehouse, row.available());
        return toJson(row);
    }

    /** 页面响应附带的夹具快照。只读，排序只为页面稳定。 */
    public FixtureSummary fixture() {
        List<OrderRecord> orders = orderStore.snapshot().values().stream()
                .sorted(Comparator.comparing(OrderRecord::orderId))
                .toList();
        List<InventoryRecord> inventory = inventoryStore.snapshot().values().stream()
                .sorted(Comparator.comparing(InventoryRecord::sku).thenComparing(InventoryRecord::warehouse))
                .toList();
        return new FixtureSummary(orders, inventory);
    }

    /**
     * 参数字符串解析成对象。解析失败返回空对象，随后查表走 invalid_argument / not_found，
     * 不把 Jackson 异常抛出循环——坏参数是一次观察，循环还可以继续。
     */
    private JsonNode parseArguments(String arguments) {
        try {
            JsonNode node = objectMapper.readTree(arguments);
            return node.isObject() ? node : objectMapper.createObjectNode();
        } catch (JsonProcessingException ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }

    private static String textOrEmpty(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText("");
    }
}
