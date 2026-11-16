package com.example.demos.stage03.springai;

import com.example.demos.stage03.loop.LoopResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第 4 天 · Spring AI 循环入口。POST /api/stage/03/spring-ai。
 * <p>
 * 响应形状和手写循环的 {@link LoopResponse} 相同：answer、stopReason、steps、fixture。
 * 页面用来对照的是谁在发 HTTP，不是另一套订单表。
 */
@RestController
@RequestMapping("/api/stage/03")
public class SpringAgentController {

    private final SpringAgentLoop agentLoop;

    public SpringAgentController(SpringAgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    @PostMapping("/spring-ai")
    public LoopResponse springAi(@RequestBody SpringAgentRequest body) {
        if (body == null || !StringUtils.hasText(body.question())) {
            throw new IllegalArgumentException("question 不能为空");
        }
        int maxSteps = body.maxSteps() == null ? 4 : body.maxSteps();
        if (maxSteps < 1 || maxSteps > 8) {
            throw new IllegalArgumentException("maxSteps 取值 1 到 8，默认 4");
        }
        return agentLoop.run(body.question().trim(), maxSteps);
    }
}
