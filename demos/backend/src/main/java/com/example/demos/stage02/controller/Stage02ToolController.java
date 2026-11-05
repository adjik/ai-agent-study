package com.example.demos.stage02.controller;

import com.example.demos.stage02.api.ToolChatRequest;
import com.example.demos.stage02.store.InventoryStore;
import com.example.demos.stage02.store.OrderStore;
import com.example.demos.stage02.support.ToolCallLog;
import com.example.demos.stage02.tool.InventoryQueryTools;
import com.example.demos.stage02.tool.OrderQueryTools;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阶段 2 Function Calling。D4：响应带 {@code toolsCalled}；查无此单走 not_found。
 * <p>
 * S2-T4 · docs/stage-2.md D4 · K10 K11
 */
@RestController
@RequestMapping(Stage02ToolController.STAGE_PATH)
public class Stage02ToolController {
    public static final String STAGE_PATH = "/api/stage/02";

    private final ChatClient chatClient;
    private final OrderQueryTools orderQueryTools;
    private final OrderStore orderStore;
    private final InventoryQueryTools inventoryQueryTools;
    private final InventoryStore inventoryStore;

    public Stage02ToolController(
            ChatClient chatClient,
            OrderQueryTools orderQueryTools,
            OrderStore orderStore,
            InventoryQueryTools inventoryQueryTools,
            InventoryStore inventoryStore) {
        this.chatClient = chatClient;
        this.orderQueryTools = orderQueryTools;
        this.orderStore = orderStore;
        this.inventoryQueryTools = inventoryQueryTools;
        this.inventoryStore = inventoryStore;
    }

    @GetMapping("/tools")
    public Map<String, Object> tools() {
        return Map.of("tools", java.util.List.of("getOrder", "getInventory"));
    }

    @GetMapping("/store")
    public Map<String, Object> store() {
        return Map.of("orders", orderStore.snapshot(), "inventory", inventoryStore.snapshot());
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ToolChatRequest body) {
        String question = body.question() == null ? "" : body.question().trim();
        ToolCallLog.clear();
        String answer = chatClient.prompt()
                .system("""
                        你是电商售后助手。只能通过工具查询订单或库存。
                        查订单用 getOrder，查库存用 getInventory，不要混用。
                        没有查到就说没有，禁止编造状态、金额、库存，也没有修改数据的工具。
                        """)
                .user(question)
                .tools(orderQueryTools, inventoryQueryTools)
                .options(OpenAiChatOptions.builder().temperature(0.0))
                .call()
                .content();
        return Map.of(
                "answer", answer == null ? "" : answer,
                "toolsCalled", ToolCallLog.snapshot());
    }
}
