package com.example.demos.stage03.lc4j;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第 7 天 · POST /api/stage/03/langchain4j。循环握在 {@code AiServices} 手里，
 * 这里只收问句和会话参数，再把回答与记忆窗口交回去。
 */
@RestController
@RequestMapping("/api/stage/03")
public class Lc4jAgentController {

    private final Lc4jAgentService agentService;

    public Lc4jAgentController(Lc4jAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/langchain4j")
    public Lc4jAgentResponse chat(@RequestBody Lc4jAgentRequest body) {
        if (body == null || !StringUtils.hasText(body.question())) {
            throw new IllegalArgumentException("question 不能为空");
        }
        int maxMessages = body.maxMessages() == null ? 20 : body.maxMessages();
        if (maxMessages < 1 || maxMessages > 40) {
            throw new IllegalArgumentException("maxMessages 取值 1 到 40，默认 20");
        }
        return agentService.run(body.question().trim(), body.conversationId(), maxMessages);
    }
}
