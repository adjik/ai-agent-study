package com.example.demos.stage02.lc4j;

import com.example.demos.stage02.api.ToolChatRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stage/02")
public class Stage02Lc4jController {
    private final OrderAssistant orderAssistant;

    public Stage02Lc4jController(OrderAssistant orderAssistant) {
        this.orderAssistant = orderAssistant;
    }

    @PostMapping("/chat-lc4j")
    public Map<String, String> chatLc4j(@RequestBody ToolChatRequest body) {
        String question = body.question() == null ? "" : body.question().trim();
        return Map.of("answer", orderAssistant.chat(question));
    }
}
