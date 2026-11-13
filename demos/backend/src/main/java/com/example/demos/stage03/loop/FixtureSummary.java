package com.example.demos.stage03.loop;

import com.example.demos.stage02.api.InventoryRecord;
import com.example.demos.stage02.api.OrderRecord;
import java.util.List;

/**
 * 这一次响应时内存里的订单和库存。只读快照，循环不会改它。
 * 页面用来对照观察：ORD-1001 是 PAID，SKU-88 在 WH-A 的 available 是 10。
 *
 * @param orders    按 orderId 排序
 * @param inventory 按 sku、warehouse 排序
 */
public record FixtureSummary(List<OrderRecord> orders, List<InventoryRecord> inventory) {}
