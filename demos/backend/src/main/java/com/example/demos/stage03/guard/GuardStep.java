package com.example.demos.stage03.guard;

/**
 * 一次运行里的一步。比第 2 天的 {@code StepRecord} 多两个字段，
 * 因为今天要能看出「模型点了菜，但这一步没有真的执行」。
 *
 * <h2>四种轨迹长什么样</h2>
 * <pre>
 * 查询        round=1 toolName=getOrder       executed=true   observation={"status":"PAID",...}
 * 菜单外      round=1 toolName=updateStock    executed=false  observation=null   note=菜单里没有...
 * 退款挂起    round=2 toolName=refundOrder    executed=false  observation=null   note=写操作等用户确认
 * 收口        round=3 toolName=null           executed=false  observation=null   note=模型不再点菜
 * </pre>
 * 只有第一种会碰 Store。后三种都是「模型提议了，Java 没做」——这正是今天的主题。
 *
 * @param round        第几次请求模型，从 1 开始。它是请求次数，不是 steps 下标
 * @param finishReason 点菜轮写 {@code tool_calls}；收口轮抄厂商的 {@code finish_reason}，常见 {@code stop}。
 *                     注意它是「这一轮为什么结束」，不是「整次运行为什么结束」，后者看响应里的 stopReason
 * @param toolName     模型点的函数名。收口轮为 null，页面显示成「不再点菜」
 * @param arguments    模型给出的参数原文，字符串形式的 JSON。收口轮为 null
 * @param observation  Java 执行后的 JSON。查询命中时是记录本身；没执行时是 null。
 *                     这是事实来源：PAID、available、not_found 都在这里，不在 answer 的措辞里
 * @param executed     这一步是否真的执行了。这是今天最重要的字段：
 *                     <ul>
 *                       <li>true —— 查询工具，真的读了 Store</li>
 *                       <li>false —— 菜单里没有的工具、等确认的退款、收口轮</li>
 *                     </ul>
 *                     页面靠它区分「模型说了要做」和「Java 真的做了」
 * @param note         为什么没执行、或这一轮为什么停。给人看的短句，不参与任何判断逻辑
 */
public record GuardStep(
        int round,
        String finishReason,
        String toolName,
        String arguments,
        String observation,
        boolean executed,
        String note) {}
