package com.example.demos.stage02.lc4j;

import com.example.demos.stage02.store.InventoryStore;
import com.example.demos.stage02.store.OrderStore;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * D6 · LangChain4j 工具。只委托已有 Store，不复制夹具。格式校验与 Spring 侧 D5 相同，非法参数不进 Store。
 */
@Component
public class Lc4jOrderTools {
    private final OrderStore orderStore;
    private final InventoryStore inventoryStore;

    public Lc4jOrderTools(OrderStore orderStore, InventoryStore inventoryStore) {
        this.orderStore = orderStore;
        this.inventoryStore = inventoryStore;
    }

    @Tool("按订单号查询订单。orderId 形如 ORD-数字。查不到 not_found。")
    public String getOrder(@P("订单号，例如 ORD-1001") String orderId) {
        if (orderId == null || !orderId.matches("ORD-\\d+")) {
            return "{\"error\":\"invalid_argument\",\"orderId\":\"" + (orderId == null ? "" : orderId) + "\"}";
        }
        return orderStore.findById(orderId)
                .map(r -> "{\"orderId\":\"" + r.orderId() + "\",\"status\":\"" + r.status() + "\"}")
                .orElse("{\"error\":\"not_found\",\"orderId\":\"" + orderId + "\"}");
    }

    @Tool("按 SKU 和仓库查可用库存。不能查订单。")
    public String getInventory(@P("SKU，例如 SKU-88") String sku,
                               @P("仓库，例如 WH-A") String warehouse) {
        boolean skuOk = sku != null && sku.matches("SKU-\\d+");
        boolean warehouseOk = warehouse != null && warehouse.matches("WH-[A-Z]+");
        if (!skuOk || !warehouseOk) {
            return "{\"error\":\"invalid_argument\",\"sku\":\"" + (sku == null ? "" : sku)
                    + "\",\"warehouse\":\"" + (warehouse == null ? "" : warehouse) + "\"}";
        }
        return inventoryStore.find(sku, warehouse)
                .map(r -> "{\"sku\":\"" + r.sku() + "\",\"available\":" + r.available() + "}")
                .orElse("{\"error\":\"not_found\"}");
    }
}
