package com.example.demos.stage03.graph;

/**
 * POST /api/stage/03/graph 的请求体。
 * <p>
 * 这一页没有 strategy，也没有 maxSteps：图上没有「模型决定要不要继续」这一环，
 * 走几个节点由边写死，所以请求只需要问句和一个可选的库存覆盖值。
 *
 * @param question          用户这一句。订单节点从里面抽 ORD-数字 作为要查的单号（抽不到兜底 ORD-1001）；
 *                          走哪条边仍由写死的边决定，问句只改数据不改流程
 * @param availableOverride 本次判断用的可用数量，null 表示读夹具。
 *                          填 0 可以在不改库存的前提下演练「走不可发货分支」
 */
public record GraphRequest(String question, Integer availableOverride) {}
