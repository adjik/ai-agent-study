package com.example.demos.stage02.tool;

import com.example.demos.stage02.store.InventoryStore;
import com.example.demos.stage02.support.ToolCallLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class InventoryQueryTools {
    private static final Logger log = LoggerFactory.getLogger(InventoryQueryTools.class);

    private final InventoryStore inventoryStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryQueryTools(InventoryStore inventoryStore) {
        this.inventoryStore = inventoryStore;
    }

    @Tool(description = "按 SKU 和仓库查询可用库存。sku 形如 SKU-88，warehouse 形如 WH-A。只能查库存数量，不能查订单状态。查不到返回 not_found，不要猜测。")
    public String getInventory(
            @ToolParam(description = "SKU，例如 SKU-88") String sku,
            @ToolParam(description = "仓库编码，例如 WH-A") String warehouse) {
        boolean skuOk = sku != null && sku.matches("SKU-\\d+");
        boolean warehouseOk = warehouse != null && warehouse.matches("WH-[A-Z]+");
        if (!skuOk || !warehouseOk) {
            String payload = toJson(Map.of(
                    "error", "invalid_argument",
                    "sku", sku == null ? "" : sku,
                    "warehouse", warehouse == null ? "" : warehouse));
            log.info("[tool] getInventory sku={} warehouse={} result=invalid_argument elapsedMs=0", sku, warehouse);
            ToolCallLog.record(
                    "getInventory",
                    "sku=" + sku + ",warehouse=" + warehouse,
                    false,
                    0);
            return payload;
        }
        long startedAt = System.nanoTime();
        var found = inventoryStore.find(sku, warehouse);
        String payload = found.map(this::toJson)
                .orElseGet(() -> toJson(Map.of("error", "not_found", "sku", sku, "warehouse", warehouse)));
        String result = found.map(row -> String.valueOf(row.available())).orElse("not_found");
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        log.info("[tool] getInventory sku={} warehouse={} result={} elapsedMs={}",
                sku, warehouse, result, elapsedMs);
        ToolCallLog.record("getInventory", "sku=" + sku + ",warehouse=" + warehouse, true, elapsedMs);
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
