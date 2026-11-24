package com.example.demos.stage03.guard;

/**
 * POST /api/stage/03/guard 的请求体。
 * <p>
 * 没有 conversationId，也没有 strategy：
 * 每次点击都是一次独立运行，不接上一次的历史（会话记忆是第 5–7 天的事）。
 *
 * @param question 用户这一句。三种停法靠不同问句触发：
 *                 <ul>
 *                   <li>「能不能发货」——用来演示步数上限，把 maxSteps 调小就停</li>
 *                   <li>「把库存改成 999」——菜单里没有这个方法，演示菜单边界</li>
 *                   <li>「退款」——写操作，演示人工确认</li>
 *                 </ul>
 * @param maxSteps 循环上限，null 时用 4。算的是请求模型的次数，不是工具次数。
 *                 发货要 3 轮（查订单、查库存、写句子），设成 2 会在写出结论前停住
 */
public record GuardRequest(String question, Integer maxSteps) {}
