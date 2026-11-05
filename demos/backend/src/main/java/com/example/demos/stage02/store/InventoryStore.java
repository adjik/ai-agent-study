package com.example.demos.stage02.store;

import com.example.demos.stage02.api.InventoryRecord;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InventoryStore {
    private final Map<String, InventoryRecord> rows = new ConcurrentHashMap<>();

    public InventoryStore() {
        put(new InventoryRecord("SKU-88", "WH-A", 10));
        put(new InventoryRecord("SKU-99", "WH-A", 0));
    }

    private static String key(String sku, String warehouse) {
        return sku + "|" + warehouse;
    }

    private void put(InventoryRecord row) {
        rows.put(key(row.sku(), row.warehouse()), row);
    }

    public Optional<InventoryRecord> find(String sku, String warehouse) {
        return Optional.ofNullable(rows.get(key(sku, warehouse)));
    }

    public Map<String, InventoryRecord> snapshot() {
        return Map.copyOf(rows);
    }
}
