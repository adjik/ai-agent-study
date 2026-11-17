package com.example.demos.stage03.plan;

/**
 * 事先写下的一步：调哪把工具、带什么参数。它出现在任何观察之前，
 * 所以参数里可能没有 sku、warehouse 这种只有查过表才知道的值。
 *
 * @param tool      工具名，例如 getOrder、getInventory
 * @param arguments 模型写下的参数，字符串形式的 JSON
 */
public record PlanStep(String tool, String arguments) {}
