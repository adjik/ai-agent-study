package com.example.demos.stage03.plan;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第 8 天 · POST /api/stage/03/plan。同一句问句，换 {@code strategy} 就换「下一步由谁决定」：
 * <ul>
 *   <li>{@code react}：模型每轮自己选，响应里没有 plan。</li>
 *   <li>{@code plan}：先写清单再查表，响应先有 plan、后有 observation。</li>
 *   <li>{@code reflect}：先出草稿再检查，响应多 draft 与 review。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/stage/03")
public class PlanningController {

    private final ReactPlanning reactPlanning;
    private final PlanThenExecute planThenExecute;
    private final ReflectPlanning reflectPlanning;

    public PlanningController(
            ReactPlanning reactPlanning, PlanThenExecute planThenExecute, ReflectPlanning reflectPlanning) {
        this.reactPlanning = reactPlanning;
        this.planThenExecute = planThenExecute;
        this.reflectPlanning = reflectPlanning;
    }

    @PostMapping("/plan")
    public PlanningResponse plan(@RequestBody PlanningRequest body) {
        if (body == null || !StringUtils.hasText(body.question())) {
            throw new IllegalArgumentException("question 不能为空");
        }
        /*
         * maxSteps 只影响 react 与 reflect：plan 策略没有「模型反复点菜」的循环，
         * 轮次由清单长度决定。这里仍统一校验，页面上的输入框才不用随策略变灰。
         */
        int maxSteps = body.maxSteps() == null ? 4 : body.maxSteps();
        if (maxSteps < 1 || maxSteps > 8) {
            throw new IllegalArgumentException("maxSteps 取值 1 到 8，默认 4");
        }
        String question = body.question().trim();
        /* 不认识的值直接报 400，不默认成 react，免得打错字时以为在看边想边做的结果。 */
        return switch (body.strategy() == null ? "react" : body.strategy().trim()) {
            case "react" -> reactPlanning.run(question, maxSteps);
            case "plan" -> planThenExecute.run(question);
            case "reflect" -> reflectPlanning.run(question, maxSteps);
            default -> throw new IllegalArgumentException("strategy 取值 react / plan / reflect");
        };
    }
}
