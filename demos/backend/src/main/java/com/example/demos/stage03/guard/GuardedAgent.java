package com.example.demos.stage03.guard;

import com.example.demos.stage03.loop.FixtureToolDispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 第 10 天 · 停止与边界。复用第 4 天的手动循环骨架，加了三道闸。
 *
 * <h2>三道闸分别拦什么</h2>
 * <ol>
 *   <li><b>步数上限</b>：{@code maxSteps} 到头就停，不再发下一次模型请求，{@code stopReason=max_steps}。
 *       防的是模型反复点同一把工具、或者一直不写结论。</li>
 *   <li><b>菜单边界</b>：模型点了菜单里没有的工具（比如「改库存」），不执行，只记一条
 *       {@code executed=false}。防的是「模型编了一个不存在的函数，程序却照做」。</li>
 *   <li><b>人工确认</b>：退款是写操作，第一次点菜只挂起、不执行，{@code stopReason=waiting_human}。
 *       防的是「一句话就把钱退了」。</li>
 * </ol>
 * 三者合起来的要点：<b>能做什么由菜单定，做不做由 Java 定</b>。模型只有「提议」的权力。
 *
 * <h2>和前面几天的差别</h2>
 * 第 4 天的循环把每次点菜交给 {@code ToolCallingManager} 自动执行。今天不行——
 * 自动执行会把退款直接跑掉，确认这一步就无从谈起。所以这里自己判断每一个 {@code tool_call}：
 * 查询照做，退款挂起，菜单外的拒绝。
 *
 * <h2>一次运行的全过程</h2>
 * <pre>
 * run()                                     confirm()
 *  ├ round 1: 模型点 getOrder   → 执行查表
 *  ├ round 2: 模型点 refundOrder → 挂起，返回 runId ──→ 人点「确认」
 *  └ （不会往下走了）                                  └ 改退款标记，返回 final
 * </pre>
 * 两次 HTTP 请求之间，待执行的退款调用存在 {@link RefundStore} 的暂停表里，
 * 用 {@code runId} 串起来。这就是「人工介入」在代码里的样子：一个可恢复的暂停点。
 */
@Service
public class GuardedAgent {

    private static final Logger log = LoggerFactory.getLogger(GuardedAgent.class);

    /**
     * 系统提示只描述「有哪些工具」和「该点哪个」。真正的边界在 Java 里，不靠提示词约束模型——
     * 提示词可以被模型忽略，代码里的 {@code if} 不会。
     * <p>
     * 退款那句必须写「直接调用工具」：如果不写，模型常常在文字里反问「要不要退」而不去点菜，
     * 于是跑不到今天的挂起分支。挂起之后由 Java 返回 waiting_human，那才是今天要看的东西。
     * <p>
     * 「菜单里没有的工具不能调用」这句是给模型的礼貌提醒，真正的拦截在 {@link #run} 里，
     * 模型即使不听话也改不了任何数据。
     */
    private static final String SYSTEM = """
            你是电商售后助手。订单和库存只存在于工具返回里，禁止编造。
            getOrder 只查订单。getInventory 只查库存。refundOrder 是写操作。
            用户问能不能发货时，先 getOrder，再用订单里的 sku 和 warehouse 调用 getInventory。
            用户要求退款时，直接调用 refundOrder，不要在文字里反问；确认由系统负责，调用后自然会等用户确认。
            菜单里没有的工具不能调用，也不要假装已经改过数据。
            """;

    /**
     * 确认之后返回的固定句子。
     * 这里不调模型：确认这一步要的是「确定执行了」，而不是「让模型再描述一遍」。
     */
    private static final String CONFIRMED_ANSWER = "已按确认执行退款。";

    /** 挂起时给用户的提示语。同样不调模型，避免它把「还没退」说成「已退」。 */
    private static final String WAITING_HUMAN_ANSWER = "这次退款需要你确认后才会执行。";

    /** 步数用尽时的固定句子。此时模型还想点菜，业务结论并不存在。 */
    private static final String MAX_STEPS_ANSWER = "已达步数上限，模型仍在点菜。";

    /** Spring AI 的模型入口。这里只调一次 {@code call}，循环由本类自己控制。 */
    private final ChatModel chatModel;
    /** 查询类工具的实际执行者，与第 2 天手写循环、第 8 天规划共用同一份判定规则。 */
    private final FixtureToolDispatcher dispatcher;
    /** 退款标记 + 暂停表。只有 {@link #confirm} 会改退款标记。 */
    private final RefundStore refundStore;
    /** 每次请求复用的选项，里面只有工具菜单，没有「自动执行」开关。 */
    private final OpenAiChatOptions options;

    public GuardedAgent(
            ChatModel chatModel,
            GuardTools guardTools,
            FixtureToolDispatcher dispatcher,
            RefundStore refundStore) {
        this.chatModel = chatModel;
        this.dispatcher = dispatcher;
        this.refundStore = refundStore;
        /*
         * 菜单就由这个注解类决定：三个 @Tool 方法 → 三个可点项。
         * 因为没有「改库存」的方法，模型就点不出来——这是「菜单边界」的第一道保险。
         * GuardTools 的方法体是占位，永远不会被调用，真正执行全在本类里。
         */
        ToolCallback[] callbacks = ToolCallbacks.from(guardTools);
        String model = chatModel.getDefaultOptions() == null ? null : chatModel.getDefaultOptions().getModel();
        this.options = OpenAiChatOptions.builder()
                .model(model)
                /* 温度 0：同一句问句尽量走出相同的工具顺序，方便对照页面 */
                .temperature(0.0)
                /* 禁止一轮里同时点两把：否则退款和查询混在同一轮，挂起时不好说清停在哪一步 */
                .parallelToolCalls(false)
                .toolCallbacks(callbacks)
                .build();
    }

    /**
     * 跑一次。三种停止原因都可能从这里返回：
     * {@code final}（模型不再点菜）、{@code max_steps}（步数用尽）、{@code waiting_human}（写操作等确认）。
     *
     * @param question 用户原句
     * @param maxSteps 请求模型的次数上限。问发货至少要 3 轮，设成 2 就会停在 max_steps
     */
    public GuardResponse run(String question, int maxSteps) {
        /* runId 是这次运行的身份证。退款挂起后，确认接口靠它找回待执行的调用 */
        String runId = UUID.randomUUID().toString();

        /*
         * messages 是这次请求的全部上下文。和手写循环一样，模型不记历史，
         * 每一轮都是把截至当前的全量消息再发一次。
         * 初始只有两句：system（工具说明）+ user（问句）。此时模型还看不见任何订单数据。
         */
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM));
        messages.add(new UserMessage(question));

        /* 轨迹。每执行一步、每拒绝一步、每挂起一步，都往里加一条，页面靠它区分「做了」和「没做」 */
        List<GuardStep> steps = new ArrayList<>();

        log.info("[guard] 开始 runId={} question={} maxSteps={}", runId, question, maxSteps);

        /* ---- 闸一：步数上限。for 的上界就是上限，跑满自然退出 ---- */
        for (int round = 1; round <= maxSteps; round++) {
            ChatResponse response = chatModel.call(new Prompt(messages, options));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new IllegalStateException("ChatModel 没有返回 choices");
            }
            AssistantMessage assistant = response.getResult().getOutput();

            /*
             * 停法一：模型不再点菜。这一轮 content 就是给人看的句子，循环正常收口。
             * 收口也占一条轨迹，toolName 留 null，页面显示成「不再点菜」。
             */
            if (!response.hasToolCalls()) {
                String answer = assistant.getText() == null ? "" : assistant.getText();
                String finishReason = response.getResult().getMetadata().getFinishReason();
                steps.add(new GuardStep(round, blankToStop(finishReason), null, null, null, false, "模型不再点菜"));
                log.info("[guard] 结束 stopReason=final round={}", round);
                return response("final", answer, steps, null, question, null);
            }

            /*
             * 点菜消息必须先放进历史。后面每一把工具的结果都要通过 tool_call_id 挂到它上面，
             * 少了这一条，下一次请求里的 tool 消息就无处安放，厂商会直接拒绝请求。
             */
            messages.add(assistant);

            /* 一轮可能点不止一把工具。这里关了并行，正常情况下一轮只有一把 */
            for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {

                /* ---- 闸二：菜单里没有这个方法 ---- */
                if (!RefundStore.MENU.contains(call.name())) {
                    String note = "菜单里没有 " + call.name() + "，没有执行";
                    /* executed=false 是这一条记录的重点：模型点了，Java 没做 */
                    steps.add(new GuardStep(round, "tool_calls", call.name(), call.arguments(),
                            null, false, note));
                    /*
                     * 仍然回一条工具消息。如果不回，模型会以为工具结果还在路上，
                     * 下一轮可能继续点同一个不存在的函数。回一条错误观察，它通常会改口。
                     */
                    messages.add(toolResponse(call,
                            "{\"error\":\"tool_not_in_menu\",\"name\":\"" + call.name() + "\"}"));
                    log.info("[guard] 拒绝执行：{}", note);
                    continue;
                }

                /* ---- 闸三：写操作先挂起等确认 ---- */
                if (RefundStore.TOOL_REFUND.equals(call.name())) {
                    String orderId = readOrderId(call.arguments());
                    /*
                     * 关键：这里只挂起，不执行。待执行的调用存进暂停表，等确认接口来取。
                     * 退款标记此时仍是 false——所以响应里 refunded 也是 false。
                     */
                    refundStore.pause(new RefundStore.PendingRefund(runId, orderId, round));
                    steps.add(new GuardStep(round, "tool_calls", call.name(), call.arguments(),
                            null, false, "写操作等用户确认，未执行"));
                    log.info("[guard] 挂起等确认 runId={} orderId={}", runId, orderId);
                    /*
                     * 立刻返回，不再往下走循环。这就是「暂停点」：整次运行被切成两次 HTTP 请求，
                     * 中间那一次点击由人来做。
                     */
                    return response("waiting_human", WAITING_HUMAN_ANSWER,
                            steps, runId, question, call.name());
                }

                /* ---- 查询类：真的执行。只有这一类会碰 Store ---- */
                FixtureToolDispatcher.Observation observation =
                        dispatcher.dispatch(call.name(), call.arguments());
                steps.add(new GuardStep(round, "tool_calls", call.name(), call.arguments(),
                        observation.json(), true, "已执行查询"));
                messages.add(toolResponse(call, observation.json()));
                log.info("[guard] 执行查询 {} 观察={}", call.name(), observation.json());
            }
            /* 本轮工具都处理完了，回到循环顶部，带着新的工具结果再问模型一次 */
        }

        /*
         * 循环跑满仍在点菜：不再发下一次请求，避免模型无限点下去。
         * 这是「上限」真正起作用的地方——不是返回一句提示语就算了，是<b>不再执行</b>。
         */
        log.info("[guard] 结束 stopReason=max_steps steps={}", steps.size());
        return response("max_steps", MAX_STEPS_ANSWER, steps, null, question, null);
    }

    /**
     * 确认之后接上那次运行。只有这条路径会改退款标记。
     * <p>
     * 注意这里<b>不调模型</b>：确认是执行既定的写操作，不是让模型重新决定。
     *
     * @return runId 不存在（拼错或已确认过）时返回 {@code stopReason=unknown_run}
     */
    public GuardResponse confirm(String runId) {
        /* confirm 内部会在同一把锁外完成「取出 + 删除 + 改标记」，所以同一个 runId 只能成功一次 */
        RefundStore.PendingRefund pending = refundStore.confirm(runId);
        if (pending == null) {
            log.warn("[guard] 确认失败 runId={}", runId);
            /*
             * 找不到也要如实报告当前的退款标记，不能固定写 false。
             * 否则「已经退过、重复点确认」这种场景，页面会显示成「从未退款」，误导排查。
             */
            return new GuardResponse("unknown_run", "没有找到这个 runId，可能已经确认过。",
                    List.of(), null, refundStore.isRefunded("ORD-1001"), fixtureAvailable(), null);
        }

        /* 补一条轨迹，说明这一次退款是「确认之后才执行的」，和挂起时那条 executed=false 正好成对 */
        List<GuardStep> steps = List.of(new GuardStep(pending.round(), "tool_calls",
                RefundStore.TOOL_REFUND, "{\"orderId\":\"" + pending.orderId() + "\"}",
                "{\"orderId\":\"" + pending.orderId() + "\",\"refunded\":true}", true, "用户已确认，执行退款"));
        log.info("[guard] 确认完成 runId={} orderId={}", runId, pending.orderId());
        return new GuardResponse("final", CONFIRMED_ANSWER, steps, null, true, fixtureAvailable(), null);
    }

    /**
     * 组装响应。
     * <p>
     * {@code refunded} 从内存表读，而不是由调用方传进来——这样「确认前 false、确认后 true」
     * 永远反映同一处数据，不会出现「响应说退了、表里没退」的不一致。
     *
     * @param runId       只在 waiting_human 时有值
     * @param pendingTool 暂停时等着被确认的工具名
     */
    private GuardResponse response(
            String stopReason, String answer, List<GuardStep> steps,
            String runId, String question, String pendingTool) {
        String orderId = readOrderId(question);
        boolean refunded = refundStore.isRefunded(orderId);
        /* List.copyOf：交给响应之后就不能再被本类改动，避免调用方看到半截数据 */
        return new GuardResponse(stopReason, answer, List.copyOf(steps), runId, refunded,
                fixtureAvailable(), pendingTool);
    }

    /**
     * 夹具里 SKU-88 在 WH-A 的数量。
     * 今天<b>没有任何代码能改它</b>——菜单里没有写库存的方法，{@code fixtureAvailable} 应该永远是 10。
     * 页面用它来证明「问改库存那次，数据确实没变」。
     */
    private int fixtureAvailable() {
        return dispatcher.fixture().inventory().stream()
                .filter(row -> "SKU-88".equals(row.sku()) && "WH-A".equals(row.warehouse()))
                .map(row -> row.available())
                .findFirst()
                .orElse(0);
    }

    /**
     * 从参数 JSON 里取 orderId。取不到就给 ORD-1001，
     * 让响应里的退款标记总有一个单号可查，不会因为参数缺失就丢掉整个判断。
     * <p>
     * 这里手工截字符串而不是上 ObjectMapper：参数形状固定只有 {@code {"orderId":"..."}} 一个字段，
     * 引一个解析器只为取一个值不划算。真实项目当然该用 ObjectMapper。
     */
    private static String readOrderId(String text) {
        if (!StringUtils.hasText(text)) {
            return "ORD-1001";
        }
        int index = text.indexOf("\"orderId\":\"");
        if (index < 0) {
            return "ORD-1001";
        }
        int start = index + "\"orderId\":\"".length();
        int end = text.indexOf('"', start);
        return end < 0 ? "ORD-1001" : text.substring(start, end);
    }

    /**
     * 一条工具结果消息。<b>没有 tool_call_id，厂商无法把它安到上一轮的点菜上</b>，请求会被拒绝。
     * 所以这里必须带上 {@code call.id()}，和点菜消息里的 id 是同一对。
     */
    private static ToolResponseMessage toolResponse(AssistantMessage.ToolCall call, String data) {
        return ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse(
                        call.id(), call.name(), data)))
                .build();
    }

    /**
     * 收口轮的 finishReason。厂商偶尔给空串，补成 stop；
     * 注意 stop 是「这一轮为什么结束」，不是「整次运行为什么结束」，后者看 stopReason。
     */
    private static String blankToStop(String finishReason) {
        return StringUtils.hasText(finishReason) ? finishReason : "stop";
    }
}
