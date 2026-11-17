package com.example.demos.stage03.lc4j;

import com.example.demos.stage03.springai.MemoryMessageView;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 第 7 天 · 同一句问题换 LangChain4j 走。
 * <p>
 * {@link AiServices} 内部自己跑工具循环，这里不写 while。查表的仍是阶段 2 的 Store，变的只是框架。
 * 记忆分两层：{@link MessageWindowChatMemory} 决定留最近几条，
 * {@link InMemoryChatMemoryStore} 决定存在哪——本阶段在堆里，进程一停就没了。
 * 窗口每次按请求的 {@code maxMessages} 新建，所以同一个 id 改成 2 之后，下一句的提示里就只剩 2 条。
 */
@Service
public class Lc4jAgentService {

    private final Stage03Lc4jTools tools;
    private final OpenAiChatModel chatModel;
    private final InMemoryChatMemoryStore memoryStore = new InMemoryChatMemoryStore();

    public Lc4jAgentService(
            Stage03Lc4jTools tools,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${spring.ai.openai.chat.model:deepseek-chat}") String model) {
        this.tools = tools;
        /*
         * LangChain4j 有自己的 ChatModel 接口，和 Spring AI 的 ChatModel 不是同一个类型，
         * 不能注入 Spring 那个 Bean。这里用它自己的 OpenAiChatModel 指向同一个 DeepSeek 地址，
         * 于是两页查的是同一份 Store、同一个模型，差别只在框架。
         * temperature 设 0，同一句问句尽量走出相同的工具顺序，方便和 Spring 页对照。
         */
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.0)
                .build();
    }

    /**
     * @param conversationId 记忆 key。空则生成新的，页面把返回值记下来供下一句使用
     * @param maxMessages    这一次窗口留几条。同一个 id 改成 2，下一次读出的提示就只剩 2 条
     */
    public Lc4jAgentResponse run(String question, String conversationId, int maxMessages) {
        String id = StringUtils.hasText(conversationId) ? conversationId.trim() : UUID.randomUUID().toString();
        /*
         * 每次调用都新建窗口，但底层 store 是同一个。窗口条数跟着这次请求走，
         * 写入时就把更早的消息从 store 里裁掉，下一次读到的自然也是裁过的。
         * 用 provider 而不是单个 memory：窗口要按 id 建，而 AiServices 只在你给了 provider 时才把
         * 方法上的 @MemoryId 传进来。
         */
        ChatMemoryProvider provider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(memoryStore)
                .build();
        /*
         * 工具循环在 build() 出来的代理里，这个类没有 while。
         * 但也正因为循环在里面，「哪一轮点了哪把工具」不会出现在响应里——
         * 想看见轮次，要回第 4 天自己写循环的那一页。
         */
        Stage03Assistant assistant = AiServices.builder(Stage03Assistant.class)
                .chatModel(chatModel)
                .tools(tools)
                .chatMemoryProvider(provider)
                .build();

        String answer = assistant.chat(id, question);
        /* 用同一个 provider 再取一次，拿到的是刚才写入并裁过的窗口。 */
        ChatMemory memory = provider.get(id);
        return new Lc4jAgentResponse(answer, id, views(memory.messages()));
    }

    /** 把 LangChain4j 的消息压成页面能看的几行。系统消息由接口注解带进去，也算一条，方便对窗口条数。 */
    private static List<MemoryMessageView> views(List<ChatMessage> messages) {
        List<MemoryMessageView> views = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message instanceof UserMessage user) {
                views.add(new MemoryMessageView("user", user.singleText()));
            } else if (message instanceof AiMessage assistant) {
                views.add(new MemoryMessageView("assistant", aiText(assistant)));
            } else if (message instanceof ToolExecutionResultMessage tool) {
                views.add(new MemoryMessageView("tool", tool.toolName() + " " + tool.text()));
            } else if (message instanceof SystemMessage system) {
                views.add(new MemoryMessageView("system", system.text()));
            }
        }
        return views;
    }

    /**
     * 带工具调用的模型消息，正文常常是空的，只剩一份点菜清单。
     * 页面上若只显示空字符串，就看不出「这一条是 assistant 要求查表」，
     * 所以把函数名拼在后面，和 Spring 页 memoryMessages 的写法保持一致。
     */
    private static String aiText(AiMessage assistant) {
        String text = assistant.text() == null ? "" : assistant.text();
        if (!assistant.hasToolExecutionRequests()) {
            return text;
        }

        /* 一轮可能点不止一把，把名字用逗号连起来，和响应里的点菜清单同序。 */
        List<String> names = new ArrayList<>();
        for (var request : assistant.toolExecutionRequests()) {
            names.add(request.name());
        }
        String joined = String.join(", ", names);
        return text.isBlank() ? "tool_calls: " + joined : text + " tool_calls: " + joined;
    }
}
