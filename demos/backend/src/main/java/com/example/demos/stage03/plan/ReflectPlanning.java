package com.example.demos.stage03.plan;

import org.springframework.stereotype.Component;

/**
 * 做完再评：先走边想边做拿到草稿，再发一次不带工具的检查，只改句子，不再查表。
 * <p>
 * 第二次调用看到的库存来自草稿的上下文，不是新查的一次。所以 steps 与纯 react 相同，
 * 多出来的只有 {@code draft} 和 {@code review} 两个字段。
 */
@Component
public class ReflectPlanning {

    private static final String REVIEW_SYSTEM = """
            你是审稿人。下面是一句给客户的发货答复草稿。
            检查三点：不超过 40 个字；必须包含订单号 ORD-1001；必须明确说能发或不能发。
            先写一行「过长」或「通过」，再给出改写后的句子。只输出这两部分，不要解释其他内容。
            """;

    private final ReactPlanning react;
    private final PlanningModel planningModel;

    public ReflectPlanning(ReactPlanning react, PlanningModel planningModel) {
        this.react = react;
        this.planningModel = planningModel;
    }

    /**
     * 两次调用：第一次就是第 4 天那个会查表的循环（草稿），第二次不带工具、只改句子。
     * 所以这里没有新增任何查表代码，steps 直接沿用草稿那几轮，页面上的库存仍是同一次查出来的。
     */
    public PlanningResponse run(String question, int maxSteps) {
        // 第一次：边想边做，拿草稿和那几轮查表
        PlanningResponse draft = react.run(question, maxSteps);

        // 第二次：只把草稿正文交给审稿提示，steps 和 fixture 不进去，免得模型把观察也改了
        String review = planningModel.complete(REVIEW_SYSTEM, "草稿：" + draft.answer());

        // 从审查结果里取出改写后的句子
        String answer = rewrite(review);
        return PlanningResponse.reflect(answer, draft.answer(), review, draft.steps(), draft.fixture());
    }

    /**
     * 审查约定是「一行结论 + 改写句子」。从后往前找第一条不是结论标记的行：
     * 模型若多写了原因说明，最后一行才是真正的改写；一行都没给时退回审查原文。
     */
    private static String rewrite(String review) {
        String[] lines = review.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            boolean isVerdict = line.isEmpty() || line.startsWith("过长") || line.startsWith("通过");
            if (!isVerdict) {
                return line;
            }
        }
        return review.trim();
    }
}
