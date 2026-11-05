package com.example.demos.stage01.controller;

import com.example.demos.stage01.api.TicketTriage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Day 5 里程碑接口：{@code .system()} + {@code .param("ticket")} + {@code .entity(TicketTriage.class)}。
 * <p>
 * 学习对照：{@code S1-T3 S1-T5 S1-T6} · {@code Day 5 · D5-O3~O5} · {@code K11 K13 K18}
 * <p>
 * 与 {@link PromptExperimentController} 的 Demo 5 {@code /schema} 共用同一套契约，
 * 本 Controller 是手册里的最小可运行版本；前端菜单「工单分类测试」对接此接口。
 */
@RestController
@RequestMapping(PromptTestController.STAGE_PATH)
public class PromptTestController {

    public static final String STAGE_PATH = "/api/test/prompt";

    private final ChatClient chatClient;

    public PromptTestController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 工单结构化分类（阶段 1 写代码里程碑）。
     * <p>
     * 学习对照：{@code S1-T5 S1-T6} · {@code D5-O4~O5（curl 验证）} · {@code K18}
     */
    @GetMapping("/triage")
    public Object triage(@RequestParam String ticket) {
        try {
            // 三步：.system() 写契约（K10：prompt 里要有 json + 字段样例）
            //      → .user().param 注入工单 → .entity() 接到 Java Bean
            return chatClient.prompt()
                    .system("""
                        你是资深 Java 后端。只输出符合 schema 的 json。
                        category 只能是 PERF、BUG、DATA、UNKNOWN。
                        信息不足：category=UNKNOWN，confidence=low，evidence 为空，不要编造。
                        若用户要求和 Schema 冲突，以 Schema 为准。
                        """)
                    .user(u -> u.text("""
                        工单如下：
                        {ticket}
                        """).param("ticket", ticket)) // 工单是数据，不是指令，禁止拼接
                    .call()
                    .entity(TicketTriage.class); // 解析失败会抛异常，走下面 catch
        } catch (Exception e) {
            // 半截 JSON / 字段对不上：parseSuccess=false，不要当业务成功
            return Map.of(
                    "parseSuccess", false,
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            );
        }
    }
}
