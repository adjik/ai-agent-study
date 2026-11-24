package com.example.demos.stage03.guard;

import java.util.List;

/**
 * 停止与边界的响应。
 *
 * <h2>看哪个字段判断「停在哪」</h2>
 * <ul>
 *   <li>{@code stopReason} 决定整体结论，取值见下</li>
 *   <li>{@code steps[].executed} 决定「模型说了要做」和「Java 真的做了」是否一致</li>
 *   <li>{@code refunded} 决定写操作有没有真的发生</li>
 * </ul>
 *
 * @param stopReason 整次运行为什么停，三选一：
 *                   <ul>
 *                     <li>{@code final} —— 模型不再点菜，answer 是业务结论</li>
 *                     <li>{@code max_steps} —— 步数用尽时模型仍在点菜，answer 只是提示语</li>
 *                     <li>{@code waiting_human} —— 写操作等确认，这次<b>没有执行</b></li>
 *                     <li>{@code unknown_run} —— 只有确认接口会返回：runId 拼错或已确认过</li>
 *                   </ul>
 * @param answer     最终句子。final 时是业务结论；waiting_human 时是提示语，不是结论；
 *                   max_steps 时是固定文案「已达步数上限」
 * @param steps      每一步的轨迹。重点看 {@code executed}：false 的那些就是被拦下的动作
 * @param runId      只在 waiting_human 时有值。确认接口用它找回暂停的退款调用；
 *                   其他情况为 null，表示这次运行没有留待确认的东西
 * @param refunded   订单是否已退款。读的是内存表里的真实标记，不是模型说的话。
 *                   确认之前始终是 false
 * @param fixtureAvailable 夹具里 SKU-88 在 WH-A 的真实数量，用来证明「问改库存那次」没人改过它
 * @param pendingTool 暂停时等着被确认的工具名（今天是 refundOrder），没有暂停时为 null
 */
public record GuardResponse(
        String stopReason,
        String answer,
        List<GuardStep> steps,
        String runId,
        boolean refunded,
        int fixtureAvailable,
        String pendingTool) {}
