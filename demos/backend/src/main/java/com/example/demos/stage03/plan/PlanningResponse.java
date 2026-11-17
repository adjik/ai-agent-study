package com.example.demos.stage03.plan;

import com.example.demos.stage03.loop.FixtureSummary;
import com.example.demos.stage03.loop.StepRecord;
import java.util.List;

/**
 * 规划对照的响应。三种策略共用这个形状，靠哪个字段有值来区分：
 *
 * @param strategy   {@code react} / {@code plan} / {@code reflect}，原样回显请求里的值
 * @param answer     最终给人看的句子
 * @param stopReason {@code final} 或 {@code max_steps}，含义与手写循环一致
 * @param steps      查表轨迹。react 是边想边做跑出来的；plan 是照清单执行；reflect 仍是草稿那几次查表
 * @param plan       只有 {@code plan} 策略有值：查表之前就写好的步骤清单。react 与 reflect 为 null
 * @param draft      只有 {@code reflect} 策略有值：未经检查的初稿
 * @param review     只有 {@code reflect} 策略有值：检查意见。可能写过长的原因，也可能是「通过」
 * @param fixture    当前内存夹具。三种策略查的是同一份
 */
public record PlanningResponse(
        String strategy,
        String answer,
        String stopReason,
        List<StepRecord> steps,
        List<PlanStep> plan,
        String draft,
        String review,
        FixtureSummary fixture) {

    /** react：没有事先的 plan，也没有草稿和检查。 */
    public static PlanningResponse react(String answer, String stopReason, List<StepRecord> steps, FixtureSummary fixture) {
        return new PlanningResponse("react", answer, stopReason, steps, null, null, null, fixture);
    }

    /** plan：先有清单，再有观察，最后才有句子。 */
    public static PlanningResponse plan(
            String answer, List<StepRecord> steps, List<PlanStep> plan, FixtureSummary fixture) {
        return new PlanningResponse("plan", answer, "final", steps, plan, null, null, fixture);
    }

    /** reflect：草稿与检查都留下，steps 仍只是草稿那几轮查表。 */
    public static PlanningResponse reflect(
            String answer, String draft, String review, List<StepRecord> steps, FixtureSummary fixture) {
        return new PlanningResponse("reflect", answer, "final", steps, null, draft, review, fixture);
    }
}
