package com.example.demos.stage03.loop;

/**
 * 返回给页面的一条轨迹，对应一次「模型决定」或一次「Java 查表」。
 * <p>
 * 点菜的记录五个字段都有值，例如只问状态的第 1 步：
 * {@code round=1, finishReason=tool_calls, toolName=getOrder,
 * arguments={"orderId":"ORD-1001"}, observation={...,"status":"PAID",...}}。
 * 收口的记录只有 round 和 finishReason，后三个为 null，页面显示「不再点菜」。
 * 只问状态时这条是第 2 步；问发货时通常是第 3 步。
 * <p>
 * 同一轮若点了多把工具（并行没关掉时），会有多条 round 相同的记录，每条对一次 tool_call。
 * 今天请求里关了并行，正常情况下一轮只有一条点菜记录。
 *
 * @param round        第几次 POST 模型，从 1 开始。它是请求次数，不是 steps 数组下标
 * @param finishReason 点菜记录固定写 {@code tool_calls}。收口记录抄厂商的 {@code finish_reason}，常见 {@code stop}
 * @param toolName     模型点的函数名。收口轮为 null
 * @param arguments    模型给出的参数原文，字符串形式的 JSON。收口轮为 null
 * @param observation  Java 查表得到的 JSON。这是事实来源：PAID、not_found、invalid_argument 都在这里，不在 answer 的措辞里。
 *                     {@code not_found} 表示单号合法但表里没有；{@code invalid_argument} 表示格式在进表之前被挡住
 * @param latencyMs    这一次 dispatch 的耗时，单位毫秒。只包查表和校验，不含模型 HTTP。收口轮没有查表，为 null
 */
public record StepRecord(
        int round,
        String finishReason,
        String toolName,
        String arguments,
        String observation,
        Long latencyMs) {}
