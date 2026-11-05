package com.example.demos.stage00.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
public class Stage00DemoService {

    private static final Logger log = LoggerFactory.getLogger(Stage00DemoService.class);

    private final ChatClient chatClient;

    public Stage00DemoService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public static class HelloResult {

        private final String question;
        private final String answer;
        private final long promptTokens;
        private final long completionTokens;
        private final long totalTokens;

        public HelloResult(String question, String answer, long promptTokens, long completionTokens, long totalTokens) {
            this.question = question;
            this.answer = answer;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }

        public long getPromptTokens() {
            return promptTokens;
        }

        public long getCompletionTokens() {
            return completionTokens;
        }

        public long getTotalTokens() {
            return totalTokens;
        }
    }

    public HelloResult demoHello() {
        log.info("=== Demo 1：最简单的问答 ===");

        ChatResponse response = chatClient.prompt()
                .user("用一句话介绍什么是 AI Agent")
                .call()
                .chatResponse();

        String answer = response.getResult().getOutput().getText();
        var usage = response.getMetadata().getUsage();

        return new HelloResult(
                "用一句话介绍什么是 AI Agent",
                answer,
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens()
        );
    }

    public static class ConversationResult {

        private final String round1Question;
        private final String round1Answer;
        private final String round2Question;
        private final String round2Answer;
        private final long totalTokens;

        public ConversationResult(String round1Question, String round1Answer,
                                  String round2Question, String round2Answer,
                                  long totalTokens) {
            this.round1Question = round1Question;
            this.round1Answer = round1Answer;
            this.round2Question = round2Question;
            this.round2Answer = round2Answer;
            this.totalTokens = totalTokens;
        }

        public String getRound1Question() {
            return round1Question;
        }

        public String getRound1Answer() {
            return round1Answer;
        }

        public String getRound2Question() {
            return round2Question;
        }

        public String getRound2Answer() {
            return round2Answer;
        }

        public long getTotalTokens() {
            return totalTokens;
        }
    }

    public ConversationResult demoConversation() {
        log.info("=== Demo 2：系统提示词 + 多轮对话 ===");

        String round1 = chatClient.prompt()
                .system(s -> s.text("""
                        你是一个资深 Java 后端工程师，擅长 Spring Boot 微服务架构。
                        回答问题时要：
                        1. 结合工程经验给出实用建议
                        2. 如果涉及代码，给出关键示例而非伪代码
                        3. 用简洁但不失准确的语言
                        """))
                .user("Redis 缓存穿透、击穿、雪崩有什么区别？")
                .call()
                .content();

        String round2 = chatClient.prompt()
                .system(s -> s.text("""
                        你是一个资深 Java 后端工程师，擅长 Spring Boot 微服务架构。
                        """))
                .user("Redis 缓存穿透、击穿、雪崩有什么区别？")
                .user("帮我写一段防止缓存穿透的代码（Java + Redisson）")
                .call()
                .content();

        return new ConversationResult(
                "Redis 缓存穿透、击穿、雪崩有什么区别？",
                round1,
                "帮我写一段防止缓存穿透的代码（Java + Redisson）",
                round2,
                -1L
        );
    }

    public static class StructureResult {

        private final String rawJson;
        private final Map<String, Object> parsed;
        private final boolean parseSuccess;
        private final String parseError;

        public StructureResult(String rawJson, Map<String, Object> parsed,
                               boolean parseSuccess, String parseError) {
            this.rawJson = rawJson;
            this.parsed = parsed;
            this.parseSuccess = parseSuccess;
            this.parseError = parseError;
        }

        public String getRawJson() {
            return rawJson;
        }

        public Map<String, Object> getParsed() {
            return parsed;
        }

        public boolean isParseSuccess() {
            return parseSuccess;
        }

        public String getParseError() {
            return parseError;
        }
    }

    public StructureResult demoStructuredOutput() {
        log.info("=== Demo 3：结构化输出（JSON 格式）===");

        String raw = chatClient.prompt()
                .system(s -> s.text("你是一个数据分析助手。始终以 JSON 格式输出，不要输出其他内容。"))
                .user("""
                        分析以下微服务架构问题：
                        一个订单服务调用库存服务超时，请分析可能的原因。

                        输出格式（JSON）：
                        {"问题": "订单服务调用库存服务超时", "可能原因": ["原因1", "原因2", ...], "排查建议": ["建议1", "建议2", ...], "预防措施": ["措施1", "措施2", ...]}
                        """)
                .options(org.springframework.ai.openai.OpenAiChatOptions.builder().temperature(0.0))
                .call()
                .content();

        String cleaned = raw;
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf("\n") + 1;
            int end = cleaned.lastIndexOf("```");
            cleaned = cleaned.substring(start, end > 0 ? end : cleaned.length());
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = new ObjectMapper().readValue(cleaned, Map.class);
            return new StructureResult(raw, parsed, true, null);
        } catch (Exception e) {
            return new StructureResult(raw, null, false, e.getMessage());
        }
    }

    public Flux<String> demoStreaming() {
        log.info("=== Demo 4：流式输出（Streaming）===");

        return chatClient.prompt()
                .user("用 5 句话介绍 Spring Boot 的核心特性")
                .stream()
                .content();
    }
}
