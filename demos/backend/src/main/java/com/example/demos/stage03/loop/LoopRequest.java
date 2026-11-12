package com.example.demos.stage03.loop;

/**
 * POST /api/stage/03/loop 的请求体。本接口无状态：两次点击之间不共享 messages。
 *
 * @param question 用户原句，例如「ORD-1001 现在什么状态？」。模型不记得上一问，每次只带这一句
 * @param maxSteps 循环上限，算的是请求模型的次数，不是工具次数。null 时 Controller 用 4。
 *                 发货要 3 次请求（两次点菜加一次写句子），设成 2 会在写出答案前停住
 */
public record LoopRequest(String question, Integer maxSteps) {}
