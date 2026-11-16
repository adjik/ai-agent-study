package com.example.demos.stage03.springai;

import com.example.demos.stage02.api.InventoryRecord;
import com.example.demos.stage02.api.OrderRecord;
import com.example.demos.stage02.store.InventoryStore;
import com.example.demos.stage02.store.OrderStore;
import com.example.demos.stage02.support.ToolCallLog;
import com.example.demos.stage02.tool.InventoryQueryTools;
import com.example.demos.stage02.tool.OrderQueryTools;
import com.example.demos.stage03.loop.FixtureSummary;
import com.example.demos.stage03.loop.LoopResponse;
import com.example.demos.stage03.loop.StepRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 第 4 天 · 把 Spring AI 藏在 {@code ChatClient.call()} 里的循环交还给这段 {@code for}。
 * <p>
 * Spring AI 2.0 的自动循环不在 {@link ChatModel#call} 里，而在 {@code ChatClient} 默认挂上的
 * {@code ToolCallingAdvisor}。这个类不注入 {@code ChatClient}，只调用 {@code ChatModel}，
 * 所以顾问不会自己把 while 跑完。工具执行交给 {@link ToolCallingManager#executeToolCalls}，
 * 它只按模型点的菜单调用阶段 2 的 {@code @Tool} 方法，不决定要不要再问一轮。
 * <p>
 * 查的仍是 {@link OrderQueryTools} / {@link InventoryQueryTools}，和手写循环读同一份 Store。
 * steps 的字段与第 2、3 天相同，页面才能并排对照。
 */
@Component
public class SpringAgentLoop {

    /**
     * 和手写循环同一套约束：两把工具互斥，发货必须先订单再库存，一次只点一把。
     * 说明写在这里，是因为 {@code @Tool} 的 description 不会单独告诉模型「发货要连查」。
     */
    private static final String SYSTEM = """
            你是电商助手。订单和库存只存在于工具返回里，禁止编造状态或数量。
            getOrder 只查订单，不查库存。getInventory 只查库存，不查订单。一次只调用一个工具。
            用户只问订单状态时，调用一次 getOrder，看到观察后直接写回答，不要再查库存。
            用户问能不能发货时，必须先 getOrder；下一轮再用订单观察里的 sku 和 warehouse 调用 getInventory；两段观察都在消息里之后，才写能不能发。
            观察里的 error 是 invalid_argument 或 not_found 时，按这个错误说明，禁止说成已支付或 PAID。
            """;

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final OrderStore orderStore;
    private final InventoryStore inventoryStore;
    private final OpenAiChatOptions options;

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
        /*
         * 工具菜单来自阶段 2 的 @Tool 方法，不在这里再写一份 JSON Schema。
         * parallelToolCalls=false：一轮只点一把，第二把的 sku / warehouse 才能来自第一把的观察。
         * 没有 ChatClient，也就没有 ToolCallingAdvisor 的自动 while。这就是 2.0 里
         * 「internalToolExecutionEnabled(false)」对应的做法。
         */
        ToolCallback[] callbacks = ToolCallbacks.from(orderQueryTools, inventoryQueryTools);
        /*
         * 自建的 OpenAiChatOptions 会整份替换 ChatModel 的默认项。不写 model 时，
         * 官方 SDK 会落成 gpt-5-mini，DeepSeek 直接 400。模型名从已装配的默认项抄过来。
         */
        String model = chatModel.getDefaultOptions() == null ? null : chatModel.getDefaultOptions().getModel();
        this.options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .parallelToolCalls(false)
                .toolCallbacks(callbacks)
                .build();
    }

    public LoopResponse run(String question, int maxSteps) {
        // 阶段 2 的 @Tool 会往 ToolCallLog 里记一行。先清掉，避免和上一次 /chat 的审计串在同一线程。
        ToolCallLog.clear();
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM));
        messages.add(new UserMessage(question));
        List<StepRecord> steps = new ArrayList<>();

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
                return new LoopResponse(answer, "final", List.copyOf(steps), fixture());
            }

            /*
             * executeToolCalls 只执行这一轮点的菜，并把助手消息和 role=tool 的观察接进 conversationHistory。
             * 它不会再调用模型。下一轮用这份历史重新 new Prompt，循环才继续。
             */
            long started = System.nanoTime();
            ToolExecutionResult executed = toolCallingManager.executeToolCalls(prompt, response);
            long latencyMs = (System.nanoTime() - started) / 1_000_000L;
            messages = new ArrayList<>(executed.conversationHistory());
            recordToolSteps(round, assistant, lastToolMessage(executed), latencyMs, steps);
        }
        return new LoopResponse("已达步数上限", "max_steps", List.copyOf(steps), fixture());
    }

    /**
     * 从这一轮的点菜和工具结果里取出名字、参数、观察。
     * 历史里还有更早的轮次，所以不用整段 conversationHistory 重扫，只对当前 assistant 的 toolCalls。
     */
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
