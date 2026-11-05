package com.example.demos.stage02.store;

import com.example.demos.stage02.api.OrderRecord;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OrderStore {
    private final Map<String, OrderRecord> orders = new ConcurrentHashMap<>();

    public OrderStore() {
        put(new OrderRecord("ORD-1001", "U001", "SKU-88", 2, "PAID", "WH-A"));
        put(new OrderRecord("ORD-1002", "U002", "SKU-88", 1, "CANCELLED", "WH-A"));
        put(new OrderRecord("ORD-1003", "U001", "SKU-99", 5, "PAID", "WH-A"));
    }

    private void put(OrderRecord row) {
        orders.put(row.orderId(), row);
    }

    public Optional<OrderRecord> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public Map<String, OrderRecord> snapshot() {
        return Map.copyOf(orders);
    }
}
