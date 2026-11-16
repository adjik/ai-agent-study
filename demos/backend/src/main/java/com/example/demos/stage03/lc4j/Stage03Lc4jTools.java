package com.example.demos.stage03.lc4j;

import com.example.demos.stage02.store.InventoryStore;
import com.example.demos.stage02.store.OrderStore;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 第 7 天 · LangChain4j 侧的工具。注解来自 {@code dev.langchain4j.agent.tool}，
 * 与阶段 2 的 {@code org.springframework.ai.tool.annotation.Tool} 不是同一个类型，所以单独一个类。
 * 方法内部仍查阶段 2 的 Store，换框架不换夹具。
 */
@Component
public class Stage03Lc4jTools {

    private final OrderStore orderStore;
    private final InventoryStore inventoryStore;

    public Stage03Lc4jTools(OrderStore orderStore, InventoryStore inventoryStore) {
        this.orderStore = orderStore;
        this.inventoryStore = inventoryStore;
    }

    @Tool("""
            按订单号查询订单。orderId 必须形如 ORD-数字，例如 ORD-1001。
            只能查订单状态和对应 SKU，不能查库存数量。查不到返回 not_found，不要猜测。
            """)
    public String getOrder(@P("订单号，例如 ORD-1001") String orderId) {
        if (orderId == null || !orderId.matches("ORD-\\d+")) {
            return "{\"error\":\"invalid_argument\",\"orderId\":\"" + nullToEmpty(orderId) + "\"}";
        }
        return orderStore.findById(orderId)
                .map(row -> "{\"orderId\":\"" + row.orderId() + "\",\"userId\":\"" + row.userId()
                        + "\",\"sku\":\"" + row.sku() + "\",\"qty\":" + row.qty()
                        + ",\"status\":\"" + row.status() + "\",\"warehouse\":\"" + row.warehouse() + "\"}")
                .orElse("{\"error\":\"not_found\",\"orderId\":\"" + orderId + "\"}");
    }

    @Tool("""
            按 SKU 和仓库查询可用库存。sku 形如 SKU-88，warehouse 形如 WH-A。
            只能查库存数量，不能查订单状态。查不到返回 not_found，不要猜测。
            """)
    public String getInventory(@P("SKU，例如 SKU-88") String sku,
                               @P("仓库，例如 WH-A") String warehouse) {
        boolean skuOk = sku != null && sku.matches("SKU-\\d+");
        boolean warehouseOk = warehouse != null && warehouse.matches("WH-[A-Z]+");
        if (!skuOk || !warehouseOk) {
            return "{\"error\":\"invalid_argument\",\"sku\":\"" + nullToEmpty(sku)
                    + "\",\"warehouse\":\"" + nullToEmpty(warehouse) + "\"}";
        }
        return inventoryStore.find(sku, warehouse)
                .map(row -> "{\"sku\":\"" + row.sku() + "\",\"warehouse\":\"" + row.warehouse()
                        + "\",\"available\":" + row.available() + "}")
                .orElse("{\"error\":\"not_found\",\"sku\":\"" + sku + "\",\"warehouse\":\"" + warehouse + "\"}");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
