package com.example.demos.stage03.plan;

import com.example.demos.stage03.springai.SpringAgentLoop;
import com.example.demos.stage03.springai.SpringAgentResponse;
import org.springframework.stereotype.Component;

/**
 * 边想边做：下一步在看到上一次观察之后才决定，响应的 steps 随时间变长，
 * 没有任何事先写好的 plan 字段。这里直接复用第 4 天的手动循环，不另做一套查单。
 * <p>
 * 传空的 conversationId，让每次点击都是全新会话，避免第 5 天的记忆把上一句带进来——
 * 这一页比较的是「下一步由谁决定」，不是会话接续。
 */
@Component
public class ReactPlanning {

    private final SpringAgentLoop agentLoop;

    public ReactPlanning(SpringAgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    /**
     * @param maxSteps 请求模型的次数上限。发货通常三轮就够：查订单、查库存、写句子
     */
    public PlanningResponse run(String question, int maxSteps) {
        SpringAgentResponse result = agentLoop.run(question, null, maxSteps, 20, false);
        /*
         * 只取回答、结束原因、轨迹和夹具。会话 id 与仓库名不往规划响应里放：
         * 这一页比较的是「下一步由谁决定」，多两个记忆字段只会让页面更难读。
         */
        return PlanningResponse.react(result.answer(), result.stopReason(), result.steps(), result.fixture());
    }
}
