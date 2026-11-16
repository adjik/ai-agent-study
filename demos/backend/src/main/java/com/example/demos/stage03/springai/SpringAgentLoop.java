package com.example.demos.stage03.springai;

import com.example.demos.stage02.api.InventoryRecord;
import com.example.demos.stage02.api.OrderRecord;
import com.example.demos.stage02.store.InventoryStore;
import com.example.demos.stage02.store.OrderStore;
import com.example.demos.stage02.support.ToolCallLog;
import com.example.demos.stage02.tool.InventoryQueryTools;
import com.example.demos.stage02.tool.OrderQueryTools;
import com.example.demos.stage03.loop.FixtureSummary;
import com.example.demos.stage03.loop.StepRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 第 4 天的手动循环，加上第 5 天的窗口记忆和第 6 天的内存仓库。
 * <p>
 * 循环是这个 Service 的方法。聊天接口和巡检都调用 {@link #run}，巡检不读 HTTP 入参。
 * {@link ChatMemory} 决定留哪些消息，{@link ChatMemoryRepository} 决定存在哪。
 * 这里的仓库是 {@link InMemoryChatMemoryRepository}，数据在堆里，进程一停 key 就没了。
 * 以后换成 Redis，换的是仓库实现，{@code conversationId} 的用法不变。
 */
@Service
public class SpringAgentLoop {

    /** 巡检固定问这句，不从输入框读。 */
    static final String PATROL_QUESTION = "ORD-1001 能不能发货？先查订单再查对应库存。";

    private static final String SYSTEM = """
            你是电商助手。订单和库存只存在于工具返回里，禁止编造状态或数量。
            getOrder 只查订单，不查库存。getInventory 只查库存，不查订单。一次只调用一个工具。
            用户只问订单状态时，调用一次 getOrder，看到观察后直接写回答，不要再查库存。
            用户问能不能发货时，必须先 getOrder；下一轮再用订单观察里的 sku 和 warehouse 调用 getInventory；两段观察都在消息里之后，才写能不能发。
            用户说「刚才那张单」时，只能使用记忆里出现过的单号。记忆里没有单号就说不知道是哪张单，禁止改用夹具里的 ORD-1001。
            观察里的 error 是 invalid_argument 或 not_found 时，按这个错误说明，禁止说成已支付或 PAID。
            """;

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final OrderStore orderStore;
    private final InventoryStore inventoryStore;
    private final OpenAiChatOptions options;
    /** 进程内仓库。所有 conversationId 共用这一份，窗口策略每次按请求的 maxMessages 包一层。 */
    private final ChatMemoryRepository memoryRepository = new InMemoryChatMemoryRepository();

    public SpringAgentLoop(
            ChatModel chatModel,
            ToolCallingManager toolCallingManager,
            OrderQueryTools orderQueryTools,
            InventoryQueryTools inventoryQueryTools,
            OrderStore orderStore,
            InventoryStore inventoryStore) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.orderStore = orderStore;
        this.inventoryStore = inventoryStore;
        ToolCallback[] callbacks = ToolCallbacks.from(orderQueryTools, inventoryQueryTools);
        String model = chatModel.getDefaultOptions() == null ? null : chatModel.getDefaultOptions().getModel();
        this.options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .parallelToolCalls(false)
                .toolCallbacks(callbacks)
                .build();
    }

    /**
     * 定时器以后只调这个方法。问句和会话 id 都写在这里，响应里 {@code trigger=schedule}。
     */
    public SpringAgentResponse patrol() {
        SpringAgentResponse result = run(PATROL_QUESTION, "patrol", 4, 20, false);
        return new SpringAgentResponse(
                result.answer(),
                result.stopReason(),
                result.steps(),
                result.fixture(),
                result.conversationId(),
                result.memoryMessages(),
                result.memoryRepository(),
                "schedule");
    }

    /**
     * @param rememberToolTrace 关闭时只把用户句和最终回答写入记忆。打开时，循环里的点菜消息和工具结果也写入同一个 id
     */
    public SpringAgentResponse run(
            String question, String conversationId, int maxSteps, int maxMessages, boolean rememberToolTrace) {
        ToolCallLog.clear();
        String id = StringUtils.hasText(conversationId) ? conversationId.trim() : UUID.randomUUID().toString();
        /*
         * 窗口条数跟着这次请求走，仓库不变。下一次把 maxMessages 改成 2，写入时会把更早的消息从这份仓库里裁掉。
         */
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(memoryRepository)
                .maxMessages(maxMessages)
                .build();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM));
        messages.addAll(memory.get(id));
        UserMessage user = new UserMessage(question);
        messages.add(user);
        List<StepRecord> steps = new ArrayList<>();
        boolean userStored = false;

        for (int round = 1; round <= maxSteps; round++) {
            Prompt prompt = new Prompt(messages, options);
            ChatResponse response = chatModel.call(prompt);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new IllegalStateException("ChatModel 没有返回 choices");
            }
            AssistantMessage assistant = response.getResult().getOutput();
            if (!response.hasToolCalls()) {
                String answer = assistant.getText() == null ? "" : assistant.getText();
                String finishReason = response.getResult().getMetadata().getFinishReason();
                steps.add(new StepRecord(
                        round,
                        StringUtils.hasText(finishReason) ? finishReason : "stop",
                        null,
                        null,
                        null,
                        null));
                remember(memory, id, user, assistant, rememberToolTrace, userStored);
                return finish(answer, "final", steps, id, memory);
            }

            long started = System.nanoTime();
            ToolExecutionResult executed = toolCallingManager.executeToolCalls(prompt, response);
            long latencyMs = (System.nanoTime() - started) / 1_000_000L;
            messages = new ArrayList<>(executed.conversationHistory());
            ToolResponseMessage toolMessage = lastToolMessage(executed);
            recordToolSteps(round, assistant, toolMessage, latencyMs, steps);
            if (rememberToolTrace) {
                if (!userStored) {
                    memory.add(id, user);
                    userStored = true;
                }
                memory.add(id, List.of(assistant, toolMessage));
            }
        }
        AssistantMessage limited = new AssistantMessage("已达步数上限");
        remember(memory, id, user, limited, rememberToolTrace, userStored);
        return finish("已达步数上限", "max_steps", steps, id, memory);
    }

    /**
     * 不记工具来回时，一次写入用户句和最终回答。记工具来回时，点菜和观察已在循环里写入，这里只补最终回答。
     */
    private static void remember(
            ChatMemory memory,
            String conversationId,
            UserMessage user,
            AssistantMessage assistant,
            boolean rememberToolTrace,
            boolean userStored) {
        if (!rememberToolTrace) {
            memory.add(conversationId, List.of(user, assistant));
            return;
        }
        if (!userStored) {
            memory.add(conversationId, user);
        }
        memory.add(conversationId, assistant);
    }

    private SpringAgentResponse finish(
            String answer, String stopReason, List<StepRecord> steps, String conversationId, ChatMemory memory) {
        return new SpringAgentResponse(
                answer,
                stopReason,
                List.copyOf(steps),
                fixture(),
                conversationId,
                views(memory.get(conversationId)),
                memoryRepository.getClass().getName(),
                null);
    }

    private static List<MemoryMessageView> views(List<Message> messages) {
        List<MemoryMessageView> views = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof UserMessage user) {
                views.add(new MemoryMessageView("user", user.getText()));
            } else if (message instanceof AssistantMessage assistant) {
                views.add(new MemoryMessageView("assistant", assistantText(assistant)));
            } else if (message instanceof ToolResponseMessage tool) {
                for (ToolResponseMessage.ToolResponse response : tool.getResponses()) {
                    views.add(new MemoryMessageView(
                            "tool",
                            response.name() + " " + (response.responseData() == null ? "" : response.responseData())));
                }
            }
        }
        return views;
    }

    private static String assistantText(AssistantMessage assistant) {
        String text = assistant.getText() == null ? "" : assistant.getText();
        if (!assistant.hasToolCalls()) {
            return text;
        }
        String names = assistant.getToolCalls().stream().map(AssistantMessage.ToolCall::name).reduce((a, b) -> a + ", " + b).orElse("");
        return text.isBlank() ? "tool_calls: " + names : text + " tool_calls: " + names;
    }

    private static void recordToolSteps(
            int round,
            AssistantMessage assistant,
            ToolResponseMessage toolMessage,
            long latencyMs,
            List<StepRecord> steps) {
        for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
            String observation = "";
            for (ToolResponseMessage.ToolResponse response : toolMessage.getResponses()) {
                if (call.id().equals(response.id())) {
                    observation = response.responseData() == null ? "" : response.responseData();
                    break;
                }
            }
            steps.add(new StepRecord(round, "tool_calls", call.name(), call.arguments(), observation, latencyMs));
        }
    }

    private static ToolResponseMessage lastToolMessage(ToolExecutionResult executed) {
        List<Message> history = executed.conversationHistory();
        Message last = history.get(history.size() - 1);
        if (last instanceof ToolResponseMessage toolMessage) {
            return toolMessage;
        }
        throw new IllegalStateException("executeToolCalls 没有把工具结果接进会话");
    }

    private FixtureSummary fixture() {
        List<OrderRecord> orders = orderStore.snapshot().values().stream()
                .sorted(Comparator.comparing(OrderRecord::orderId))
                .toList();
        List<InventoryRecord> inventory = inventoryStore.snapshot().values().stream()
                .sorted(Comparator.comparing(InventoryRecord::sku).thenComparing(InventoryRecord::warehouse))
                .toList();
        return new FixtureSummary(orders, inventory);
    }
}
