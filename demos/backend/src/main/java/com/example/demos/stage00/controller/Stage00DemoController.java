package com.example.demos.stage00.controller;

import com.example.demos.stage00.service.Stage00DemoService;
import com.example.demos.stage00.service.Stage00DemoService.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({
        Stage00DemoController.STAGE_PATH,
        Stage00DemoController.LEGACY_PATH
})
public class Stage00DemoController {

    public static final String STAGE_PATH = "/api/stage/00/demo";
    public static final String LEGACY_PATH = "/api/demo";

    public static final String PATH_ALL = STAGE_PATH + "/all";
    public static final String PATH_HELLO = STAGE_PATH + "/hello";
    public static final String PATH_CONVERSATION = STAGE_PATH + "/conversation";
    public static final String PATH_STRUCTURED = STAGE_PATH + "/structured";
    public static final String PATH_STREAMING = STAGE_PATH + "/streaming";

    private static final Logger log = LoggerFactory.getLogger(Stage00DemoController.class);

    private final Stage00DemoService demoService;

    public Stage00DemoController(Stage00DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping
    public Map<String, Object> index() {
        return Map.of(
                "stage", "00",
                "base", STAGE_PATH,
                "legacyBase", LEGACY_PATH,
                "endpoints", Map.of(
                        "all", "GET " + PATH_ALL,
                        "hello", "GET " + PATH_HELLO,
                        "conversation", "GET " + PATH_CONVERSATION,
                        "structured", "GET " + PATH_STRUCTURED,
                        "streaming", "GET " + PATH_STREAMING
                )
        );
    }

    @GetMapping("/all")
    public Map<String, Object> runAll() {
        log.info("🚀 运行全部 4 个 LLM Demo");

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            HelloResult r1 = demoService.demoHello();
            result.put("demo1_hello", Map.of(
                    "question", r1.getQuestion(),
                    "answer", r1.getAnswer(),
                    "tokens", Map.of(
                            "prompt", r1.getPromptTokens(),
                            "completion", r1.getCompletionTokens(),
                            "total", r1.getTotalTokens()
                    )
            ));
        } catch (Exception e) {
            result.put("demo1_hello", Map.of("error", e.getMessage()));
        }

        try {
            ConversationResult r2 = demoService.demoConversation();
            result.put("demo2_conversation", Map.of(
                    "round1", Map.of("question", r2.getRound1Question(), "answer", r2.getRound1Answer()),
                    "round2", Map.of("question", r2.getRound2Question(), "answer", r2.getRound2Answer())
            ));
        } catch (Exception e) {
            result.put("demo2_conversation", Map.of("error", e.getMessage()));
        }

        try {
            StructureResult r3 = demoService.demoStructuredOutput();
            result.put("demo3_structured", Map.of(
                    "rawJson", r3.getRawJson(),
                    "parsed", r3.getParsed() != null ? r3.getParsed() : "parse_failed",
                    "parseSuccess", r3.isParseSuccess()
            ));
        } catch (Exception e) {
            result.put("demo3_structured", Map.of("error", e.getMessage()));
        }

        result.put("demo4_streaming", "请访问 GET " + PATH_STREAMING + " 查看流式输出");

        return result;
    }

    @GetMapping("/hello")
    public Map<String, Object> demoHello() {
        HelloResult r = demoService.demoHello();
        return Map.of(
                "question", r.getQuestion(),
                "answer", r.getAnswer(),
                "promptTokens", r.getPromptTokens(),
                "completionTokens", r.getCompletionTokens(),
                "totalTokens", r.getTotalTokens()
        );
    }

    @GetMapping("/conversation")
    public Map<String, Object> demoConversation() {
        ConversationResult r = demoService.demoConversation();
        return Map.of(
                "round1", Map.of("question", r.getRound1Question(), "answer", r.getRound1Answer()),
                "round2", Map.of("question", r.getRound2Question(), "answer", r.getRound2Answer())
        );
    }

    @GetMapping("/structured")
    public Map<String, Object> demoStructured() {
        StructureResult r = demoService.demoStructuredOutput();
        return Map.of(
                "rawJson", r.getRawJson(),
                "parsed", r.getParsed(),
                "parseSuccess", r.isParseSuccess(),
                "parseError", r.getParseError() != null ? r.getParseError() : "none"
        );
    }

    @GetMapping(value = "/streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> demoStreaming() {
        return demoService.demoStreaming();
    }
}
