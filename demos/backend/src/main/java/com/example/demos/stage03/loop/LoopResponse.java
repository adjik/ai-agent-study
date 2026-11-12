package com.example.demos.stage03.loop;

import java.util.List;

/**
 * POST /api/stage/03/loop 的响应。阶段 2 的 /chat 只有 answer 和 toolsCalled；
 * 这里多了每一轮的 steps，用来在页面上指出「哪一轮点菜、哪一轮是观察、哪一轮才是句子」。
 *
 * @param answer     给人看的句子，来自最后一轮 message.content。
 *                   步数用尽时模型还没写句子，这里是固定文案「已达步数上限」，不要把它当成业务结论
 * @param stopReason 循环为什么停。{@code final}：模型不再点菜。{@code max_steps}：for 跑满后响应里仍有 tool_calls。
 *                   不要和单轮的 finishReason（stop / tool_calls）混用
 * @param steps      按时间排列。问状态时期望 2 条，问发货时期望至少 3 条，最后一条没有工具名
 */
public record LoopResponse(String answer, String stopReason, List<StepRecord> steps) {}
