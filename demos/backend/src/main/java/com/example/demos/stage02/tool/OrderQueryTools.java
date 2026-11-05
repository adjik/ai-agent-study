package com.example.demos.stage02.tool;

import com.example.demos.stage02.store.OrderStore;
import com.example.demos.stage02.support.ToolCallLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * D1–D5 · S2-T2 / S2-T4 / S2-T6 · stage-2.md。查无此单返回 not_found；单号不是 {@code ORD-数字} 时返回 invalid_argument，不进 Store。
 */
@Component
public class OrderQueryTools {
    private static final Logger log = LoggerFactory.getLogger(OrderQueryTools.class);

    private final OrderStore orderStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderQueryTools(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    @Tool(description = "按订单号查询订单。orderId 必须形如 ORD-数字，例如 ORD-1001。只能查订单，不能查库存数量。查不到返回 not_found，不要猜测。")
    public String getOrder(@ToolParam(description = "订单号，例如 ORD-1001") String orderId) {
        if (orderId == null || !orderId.matches("ORD-\\d+")) {
            String payload = toJson(Map.of(
                    "error", "invalid_argument",
                    "orderId", orderId == null ? "" : orderId));
            log.info("[tool] getOrder orderId={} result=invalid_argument elapsedMs=0", orderId);
            ToolCallLog.record("getOrder", "orderId=" + orderId, false, 0);
            return payload;
        }
        long startedAt = System.nanoTime();
        var found = orderStore.findById(orderId);
        String payload = found.map(this::toJson)
                .orElseGet(() -> toJson(Map.of("error", "not_found", "orderId", orderId)));
        String result = found.map(order -> order.status()).orElse("not_found");
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        log.info("[tool] getOrder orderId={} result={} elapsedMs={}", orderId, result, elapsedMs);
        ToolCallLog.record("getOrder", "orderId=" + orderId, true, elapsedMs);
        return payload;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }
}
