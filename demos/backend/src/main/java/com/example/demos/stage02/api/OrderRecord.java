package com.example.demos.stage02.api;

public record OrderRecord(
        String orderId, String userId, String sku, int qty, String status, String warehouse) {}
