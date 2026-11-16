package com.example.demos.stage03.springai;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第 6 天 · 聊天和巡检都打到 {@link SpringAgentLoop}。
 * 巡检路径不读请求体，用来模拟以后的 {@code @Scheduled}。
 */
@RestController
@RequestMapping("/api/stage/03")
public class SpringAgentController {

    private final SpringAgentLoop agentLoop;

    public SpringAgentController(SpringAgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    @PostMapping("/spring-ai")
    public SpringAgentResponse springAi(@RequestBody SpringAgentRequest body) {
        if (body == null || !StringUtils.hasText(body.question())) {
            throw new IllegalArgumentException("question 不能为空");
        }
        int maxSteps = body.maxSteps() == null ? 4 : body.maxSteps();
        if (maxSteps < 1 || maxSteps > 8) {
            throw new IllegalArgumentException("maxSteps 取值 1 到 8，默认 4");
        }
        int maxMessages = body.maxMessages() == null ? 20 : body.maxMessages();
        if (maxMessages < 1 || maxMessages > 40) {
            throw new IllegalArgumentException("maxMessages 取值 1 到 40，默认 20");
        }
        boolean rememberToolTrace = Boolean.TRUE.equals(body.rememberToolTrace());
        return agentLoop.run(body.question().trim(), body.conversationId(), maxSteps, maxMessages, rememberToolTrace);
    }

    @PostMapping("/spring-ai/patrol")
    public SpringAgentResponse patrol() {
        return agentLoop.patrol();
    }
}
