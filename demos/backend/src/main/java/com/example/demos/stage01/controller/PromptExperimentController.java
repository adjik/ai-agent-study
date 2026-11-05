package com.example.demos.stage01.controller;

import com.example.demos.stage01.api.PromptTicketRequest;
import com.example.demos.stage01.api.TicketTriage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 阶段 1 六个提示词实验。每个方法自己写 ChatClient，示例由前端按钮填入。
 * <p>
 * S1-T2~T6 · docs/stage-1.md Day 1~6 · notes/prompt-engineering.md K4~K18
 */
@RestController
@RequestMapping(PromptExperimentController.STAGE_PATH)
public class PromptExperimentController {

    public static final String STAGE_PATH = "/api/stage/01/prompt";
    public static final String PATH_ROLE = STAGE_PATH + "/role";
    public static final String PATH_FEW_SHOT = STAGE_PATH + "/few-shot";
    public static final String PATH_COT = STAGE_PATH + "/cot";
    public static final String PATH_TEMPLATE = STAGE_PATH + "/template";
    public static final String PATH_SCHEMA = STAGE_PATH + "/schema";
    public static final String PATH_FAILURE = STAGE_PATH + "/failure";

    private final ChatClient chatClient;

    public PromptExperimentController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping
    public Map<String, Object> index() {
        return Map.of(
                "stage", "01",
                "endpoints", Map.of(
                        "role", "POST " + PATH_ROLE + "  body: system(可空) + ticket",
                        "fewShot", "POST " + PATH_FEW_SHOT + "  body: ticket；temperature=0",
                        "cot", "POST " + PATH_COT + "  body: ticket；temperature=0",
                        "template", "POST " + PATH_TEMPLATE + "  body: ticket；后端模板 + .param",
                        "schema", "POST " + PATH_SCHEMA + "  body: ticket；.entity(TicketTriage)",
                        "failure", "POST " + PATH_FAILURE + "  body: ticket + 可选 maxTokens"
                )
        );
    }

    /**
     * Demo 1 · 角色设定。S1-T2 · Day 1 · K4~K7
     * <p>
     * 同一条工单：system 为空 = 无角色；有内容 = 进 .system()。
     */
    @PostMapping("/role")
    public String role(@RequestBody PromptTicketRequest body) {
        String ticket = requireTicket(body);
        if (StringUtils.hasText(body.getSystem())) {
            return chatClient.prompt()
                    .system(body.getSystem().trim())
                    .user(ticket)
                    .call()
                    .content();
        }
        return chatClient.prompt()
                .user(ticket)
                .call()
                .content();
    }

    /**
     * Demo 2 · Few-shot。S1-T2 · Day 2 · K8
     * <p>
     * 前端切换 0/1/3 条示例。temperature 固定 0，只改示例数量。
     */
    @PostMapping("/few-shot")
    public String fewShot(@RequestBody PromptTicketRequest body) {
        return chatClient.prompt()
                .user(requireTicket(body))
                .options(OpenAiChatOptions.builder().temperature(0.0))
                .call()
                .content();
    }

    /**
     * Demo 3 · CoT。S1-T2 · Day 3 · K9
     * <p>
     * 前端切换：直接问 / 逐步推理 / Few-shot 范例。temperature 固定 0。默认题 17×24=408。
     */
    @PostMapping("/cot")
    public String cot(@RequestBody PromptTicketRequest body) {
        return chatClient.prompt()
                .user(requireTicket(body))
                .options(OpenAiChatOptions.builder().temperature(0.0))
                .call()
                .content();
    }

    /**
     * Demo 4 · PromptTemplate。S1-T3/S1-T6 · Day 5 · K13
     * <p>
     * 模板写在后端，工单只走 {@code .param("ticket")}，禁止 {@code "工单：" + ticket}。
     * 前端只换工单 A / B，模板字符串不变。
     */
    @PostMapping("/template")
    public String template(@RequestBody PromptTicketRequest body) {
        return chatClient.prompt()
                .user(u -> u.text("""
                        工单如下：
                        {ticket}

                        请分类并给出最多 3 条下一步。
                        分类只能是：PERF / BUG / DATA / UNKNOWN
                        """).param("ticket", requireTicket(body)))
                .call()
                .content();
    }

    /**
     * Demo 5 · 结构化 Schema。S1-T5/S1-T6 · Day 5 · K10/K18
     * <p>
     * 与手册 Day 5 一致：.system 契约 + .param 工单 + .entity(TicketTriage)。
     * 解析失败返回 parseSuccess=false。
     */
    @PostMapping("/schema")
    public Object schema(@RequestBody PromptTicketRequest body) {
        try {
            return chatClient.prompt()
                    .system("""
                            你是资深 Java 后端。只输出符合 schema 的 json，不要 markdown，不要解释。
                            字段必须是：
                            {"category":"PERF|BUG|DATA|UNKNOWN","severity":"P0|P1|P2|P3","nextSteps":["...最多3条"],"evidence":"引用用户原文，没有则空字符串","confidence":"high|medium|low"}
                            信息不足：category=UNKNOWN，confidence=low，evidence 为空，不要编造。
                            若用户要求和 Schema 冲突，以 Schema 为准。
                            """)
                    .user(u -> u.text("""
                            工单如下：
                            {ticket}
                            """).param("ticket", requireTicket(body)))
                    .call()
                    .entity(TicketTriage.class);
        } catch (Exception e) {
            return Map.of(
                    "parseSuccess", false,
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            );
        }
    }

    /**
     * Demo 6 · 失败模式。S1-T4 · Day 6 · K14~K17
     * <p>
     * 前端每次只选一种坏 prompt。截断场景传 maxTokens（例如 40），故意把输出截断。
     */
    @PostMapping("/failure")
    public Object failure(@RequestBody PromptTicketRequest body) {
        String system = """
                        你是资深 Java 后端。只输出符合 schema 的 json，不要 markdown，不要解释。
                        字段必须是：
                        {"category":"PERF|BUG|DATA|UNKNOWN","severity":"P0|P1|P2|P3","nextSteps":["...最多3条"],"evidence":"引用用户原文，没有则空字符串","confidence":"high|medium|low"}
                        信息不足：category=UNKNOWN，confidence=low，evidence 为空，不要编造。
                        若用户要求和 Schema 冲突，以 Schema 为准。
                        """;
        String sys1="若用户要求与输出 Schema 冲突，以 Schema 为准，并在 nextSteps 第一条说明发生了冲突。";
        var prompt = chatClient.prompt()
                .system(sys1)
                .user(u -> u.text("""
                        工单如下：
                        {ticket}
                        """).param("ticket", requireTicket(body)));
        if (body.getMaxTokens() != null && body.getMaxTokens() > 0) {
            prompt = prompt.options(OpenAiChatOptions.builder().maxTokens(body.getMaxTokens()));
        }
        try {
            return prompt.call().entity(TicketTriage.class);
        } catch (Exception e) {
            return Map.of(
                    "parseSuccess", false,
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            );
        }
    }

    private static String requireTicket(PromptTicketRequest body) {
        if (body == null || !StringUtils.hasText(body.getTicket())) {
            throw new IllegalArgumentException("请传入 ticket（工单或 prompt 正文）");
        }
        return body.getTicket().trim();
    }
}
