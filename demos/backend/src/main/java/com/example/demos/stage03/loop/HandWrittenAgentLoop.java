package com.example.demos.stage03.loop;

import com.example.demos.common.config.DeepSeekConfig;
import com.example.demos.stage02.store.InventoryStore;
import com.example.demos.stage02.store.OrderStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 第 2 天 · 手写 Agent Loop。只用 JDK {@link HttpClient} 和 Jackson 调 DeepSeek 的
 * {@code /chat/completions}，这个类里不出现 {@code ChatClient}。阶段 2 的 {@code ToolCallingAdvisor}
 * 把同一段循环藏在 {@code call()} 里面，页面只能看到最终句子；这里把每一轮留在 {@link StepRecord} 里。
 * <p>
 * 模型权重里没有 ORD-1001。表在本进程的 {@link OrderStore} / {@link InventoryStore}。
 * 所以第一轮模型只能「点菜」（返回 {@code tool_calls}），Java 查完表，把 JSON 以 {@code role=tool}
 * 追加进 {@code messages}，再把整包历史 POST 回去。下一轮模型才第一次看见 {@code PAID}。
 * <p>
 * 「ORD-1001 能不能发货」在 {@code maxSteps=4} 时通常是三轮：
 * <ol>
 *   <li>请求里只有 system + 用户问句 + 工具菜单。响应 {@code finish_reason=tool_calls}，
 *       {@code getOrder("ORD-1001")}。Java 写入观察，其中有 {@code status=PAID}、{@code sku=SKU-88}、{@code warehouse=WH-A}。</li>
 *   <li>请求带上上一轮的点菜消息和那条观察。模型这时才点 {@code getInventory("SKU-88","WH-A")}。
 *       观察里 {@code available=10}。第二把的参数来自第一把的观察，不是问句里写死的。</li>
 *   <li>两段观察都在消息里。响应不再带 {@code tool_calls}，{@code content} 才是给人看的句子。
 *       {@code stopReason=final}。</li>
 * </ol>
 * 只问「现在什么状态」时，第 2 轮就该停，steps 长度为 2，中间不要出现 {@code getInventory}。
 * <p>
 * 步数到达 {@code maxSteps} 仍在点菜时不再发下一次 HTTP，{@code stopReason=max_steps}，
 * 回答固定为「已达步数上限」。已执行过的工具步骤仍然留在 {@code steps} 里。
 */
@Component
public class HandWrittenAgentLoop {

    private static final Logger log = LoggerFactory.getLogger(HandWrittenAgentLoop.class);

    /**
     * 每次请求都会带上的系统消息。它不代替工具菜单：菜单告诉模型「有哪两个函数、参数长什么样」，
     * 这段文字告诉模型「什么时候该停、什么时候必须再点一把」。
     * <p>
     * 两把工具的职责保持互斥，和阶段 2 的 {@code @Tool} 一致：getOrder 的说明里写明不返回库存，
     * getInventory 的说明里写明不返回订单状态。如果发货场景不在说明里要求「看到订单后再查库存」，
     * 模型经常在第一轮拿到 PAID 就写句子，页面上就只有一条 getOrder，库存 10 不会出现。
     * 「一次只调用一个工具」是为了让第二把的 sku / warehouse 来自第一把的观察，而不是同一轮并行猜出来。
     */
    private static final String SYSTEM = """
            你是电商助手。订单和库存只存在于工具返回里，禁止编造状态或数量。
            getOrder 只查订单，不查库存。getInventory 只查库存，不查订单。一次只调用一个工具。
            用户只问订单状态时，调用一次 getOrder，看到观察后直接写回答，不要再查库存。
            用户问能不能发货时，必须先 getOrder；下一轮再用订单观察里的 sku 和 warehouse 调用 getInventory；两段观察都在消息里之后，才写能不能发。
            """;

    /** 阶段 2 的订单夹具。ORD-1001 的 status 是 PAID，同时带出 sku=SKU-88、warehouse=WH-A。 */
    private final OrderStore orderStore;
    /** 阶段 2 的库存夹具。SKU-88 在 WH-A 的 available 是 10。 */
    private final InventoryStore inventoryStore;
    /**
     * 手写 HTTP 用的 Jackson 2。Spring Boot 4 自动配置的是 tools.jackson（Jackson 3），
     * 容器里没有 {@code com.fasterxml.jackson.databind.ObjectMapper} 这个 Bean，所以自己 new，
     * 不从构造器注入。
     */
    private final ObjectMapper objectMapper;
    /** 连接超时 20 秒。单次读响应的超时写在每次 {@link HttpRequest} 上，与 spring.ai 的 180 秒对齐。 */
    private final HttpClient httpClient;
    /** 来自环境变量 DEEPSEEK_API_KEY，经 {@link DeepSeekConfig} 读入。不写进源码、不进 Git。 */
    private final String apiKey;
    /** 例如 {@code https://api.deepseek.com/chat/completions}。由 base-url 拼出来，见 {@link #chatCompletionsUrl}。 */
    private final String endpoint;
    /** application.properties 里的 spring.ai.openai.chat.model，当前是 deepseek-v4-flash。 */
    private final String model;
    /** 每次请求都带同一份 tools。模型只能点这里登记过的名字。 */
    private final ArrayNode toolMenu;

    public HandWrittenAgentLoop(
            OrderStore orderStore,
            InventoryStore inventoryStore,
            DeepSeekConfig deepSeekConfig) {
        this.orderStore = orderStore;
        this.inventoryStore = inventoryStore;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.apiKey = deepSeekConfig.getApiKey();
        this.endpoint = chatCompletionsUrl(deepSeekConfig.getBaseUrl());
        this.model = deepSeekConfig.getModel();
        this.toolMenu = toolMenu();
    }

    /**
     * 跑完一次提问。调用方是 {@link LoopController}，入参已经校验过。
     * <p>
     * 控制流对应手册伪代码，有两处为了协议和页面多写了一点：
     * <ul>
     *   <li>助手点菜消息在 for 外追加一次。伪代码写在 for 里面，多把工具时会把同一条 assistant 重复塞进历史。</li>
     *   <li>不再点菜的那一轮也 {@code steps.add}。伪代码在这里直接 return，页面就看不到「第 2 步不再点菜」。</li>
     * </ul>
     */
    public LoopResponse run(String question, int maxSteps) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("未配置 DEEPSEEK_API_KEY");
        }
        /*
         * messages 是这次提问的全部上下文，相当于一次 RPC 的请求体，不是服务端会话。
         * 进程不替 DeepSeek 记历史：第 2 轮能说出 PAID，只因为第 1 轮的 tool 消息还在这个 List 里，
         * 并且被 httpPost 原样再发出去。本接口不做跨请求记忆，那是第 5 天 ChatMemory 的事。
         *
         * 初始只有两句：
         *   {role:system, content:SYSTEM}
         *   {role:user,   content:question}
         * 此时模型看不见 OrderStore。它若直接写「已支付」，那就是编的；正常情况会返回 tool_calls。
         */
        List<ObjectNode> messages = new ArrayList<>();
        messages.add(textMessage("system", SYSTEM));
        messages.add(textMessage("user", question));
        List<StepRecord> steps = new ArrayList<>();

        for (int round = 1; round <= maxSteps; round++) {
            JsonNode response = httpPost(messages);
            /*
             * DeepSeek / OpenAI 兼容响应的形状：
             * {
             *   "choices": [{
             *     "finish_reason": "tool_calls" | "stop" | "length",
             *     "message": {
             *       "role": "assistant",
             *       "content": null 或给人看的句子,
             *       "tool_calls": [{ "id", "type":"function", "function":{ "name", "arguments" } }]
             *     }
             *   }]
             * }
             * finish_reason=tool_calls 表示这一轮的 content 还不是答案，模型在等你执行。
             * 判断以 tool_calls 数组是否非空为准，不单独相信 finish_reason 字符串。
             */
            JsonNode choice = response.path("choices").path(0);
            JsonNode message = choice.path("message");
            String finishReason = choice.path("finish_reason").asText("");
            JsonNode toolCalls = message.path("tool_calls");
            if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                String answer = textOrEmpty(message.get("content"));
                /*
                 * 收口也占一条轨迹，工具三个字段留 null。页面把空工具名显示成「不再点菜」。
                 * 只问状态时，这条是第 2 步，finishReason 通常是 stop，stopReason 用 final 而不是 stop：
                 * final 是循环自己的结束原因，stop 是厂商这一轮的结束原因，两个字段不要混用。
                 */
                steps.add(new StepRecord(round, finishReason.isBlank() ? "stop" : finishReason, null, null, null));
                log.info("[loop] round={} finishReason={} tools=0 stop=final", round, finishReason);
                return new LoopResponse(answer, "final", List.copyOf(steps));
            }

            /*
             * 协议要求的消息顺序（一轮点了 N 把工具时）：
             *   ...已有历史,
             *   {role:assistant, tool_calls:[call1..callN]},   // 只此一条
             *   {role:tool, tool_call_id:call1.id, content:观察1},
             *   {role:tool, tool_call_id:call2.id, content:观察2}
             * 点菜消息若写进下面的 for，第二条工具会再追加一遍 assistant，下一次请求对不上 tool_call_id。
             * parallel_tool_calls=false 时期望 N=1，这里仍按协议写成「先一条 assistant，再逐条 tool」。
             */
            messages.add(assistantMessage(message));
            for (JsonNode call : toolCalls) {
                String toolName = call.path("function").path("name").asText("");
                String arguments = argumentsText(call.path("function"));
                /*
                 * dispatch 只读内存表，不写。观察是字符串 JSON，既放进 steps 给页面，
                 * 也放进 role=tool 给下一轮模型。模型从这段 JSON 里读 sku、warehouse，
                 * 才会在下一轮点 getInventory，而不是在第一轮就猜仓库。
                 */
                String observation = dispatch(toolName, arguments);
                steps.add(new StepRecord(round, "tool_calls", toolName, arguments, observation));
                messages.add(toolMessage(call.path("id").asText(""), observation));
                log.info("[loop] round={} finishReason=tool_calls tool={} args={}", round, toolName, arguments);
            }
            // 本轮工具已经执行完，for 回到下一轮，用变长之后的 messages 再 POST 一次。
        }
        /*
         * 循环条件耗尽仍收到 tool_calls：Java 不再发下一次请求，避免模型反复点同一把工具。
         * 发货至少三轮（查订单、查库存、写句子）。maxSteps=2 时通常停在这里，回答还不是业务句子。
         * 已经执行过的查询仍在 steps 里，页面能看到查到哪一步。
         */
        return new LoopResponse("已达步数上限", "max_steps", List.copyOf(steps));
    }

    /**
     * 按模型点的名字查表。返回值永远是一段 JSON 字符串，成功时是夹具记录，失败时是带 {@code error} 的对象。
     * 第 3 天会在进表之前增加单号格式校验（{@code invalid_argument}）；今天格式不对也只是查不到，返回 {@code not_found}。
     * 菜单外的名字不访问 Store，避免模型编一个「改库存」就把内存写掉——今天的菜单里本来也没有写方法。
     */
    private String dispatch(String name, String arguments) {
        JsonNode args = parseArguments(arguments);
        return switch (name) {
            case "getOrder" -> orderObservation(textOrEmpty(args.get("orderId")));
            case "getInventory" -> inventoryObservation(
                    textOrEmpty(args.get("sku")), textOrEmpty(args.get("warehouse")));
            default -> toJson(Map.of("error", "unknown_tool", "name", name));
        };
    }

    /**
     * 整行订单序列化，不能只返回 "PAID"。发货的下一轮需要这条 JSON 里的 sku 和 warehouse
     * 去点 getInventory。orderId 为空时不去 {@code ConcurrentHashMap.get(null)}，那个调用会 NPE。
     */
    private String orderObservation(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            return toJson(Map.of("error", "not_found", "orderId", ""));
        }
        return orderStore.findById(orderId)
                .map(this::toJson)
                .orElseGet(() -> toJson(Map.of("error", "not_found", "orderId", orderId)));
    }

    /**
     * 库存主键是 sku + warehouse。页面要在观察里看到 {@code available:10}。
     * 任一参数为空都当成未命中，不拿空串去拼 Map 的 key。
     */
    private String inventoryObservation(String sku, String warehouse) {
        if (!StringUtils.hasText(sku) || !StringUtils.hasText(warehouse)) {
            return toJson(Map.of(
                    "error", "not_found",
                    "sku", sku,
                    "warehouse", warehouse));
        }
        return inventoryStore.find(sku, warehouse)
                .map(this::toJson)
                .orElseGet(() -> toJson(Map.of("error", "not_found", "sku", sku, "warehouse", warehouse)));
    }

    /**
     * 一次 Chat Completions。请求体四个和循环有关的字段：
     * <ul>
     *   <li>{@code model}：与阶段 0、2 同一配置，避免手写循环和 ChatClient 打到不同模型。</li>
     *   <li>{@code temperature=0}：同一问句尽量走出相同的工具顺序，方便对照页面。</li>
     *   <li>{@code parallel_tool_calls=false}：禁止一轮里同时点 getOrder 和 getInventory。
     *       并行时第二把的参数还没看到第一把的观察，发货问句就不再是「先查订单再查库存」。</li>
     *   <li>{@code tools}：函数菜单。不传这个字段时，模型没有函数可点，只能凭权重编订单状态。</li>
     * </ul>
     * messages 每次都是截至当前轮的全量列表，不是只发新增的那一条。
     */
    private JsonNode httpPost(List<ObjectNode> messages) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("parallel_tool_calls", false);
        body.set("messages", objectMapper.valueToTree(messages));
        body.set("tools", toolMenu);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(180))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            /*
             * 非 2xx、body 里的 error、没有 choices，都变成 IllegalStateException。
             * ApiExceptionHandler 会把它收成 500 JSON，页面的 el-alert 显示 message。
             * 响应体截断后再放进异常，避免把整段 HTML 或超长报错灌进前端。
             */
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("DeepSeek HTTP " + response.statusCode() + " " + abbreviate(response.body()));
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.hasNonNull("error")) {
                throw new IllegalStateException("DeepSeek 返回错误: " + root.path("error").toString());
            }
            if (!root.path("choices").isArray() || root.path("choices").isEmpty()) {
                throw new IllegalStateException("DeepSeek 响应没有 choices: " + abbreviate(response.body()));
            }
            return root;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("调用 DeepSeek 失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 把本轮模型消息抄回历史，供下一次 POST 使用。只保留协议要的字段，不把 usage 等响应元数据塞进去。
     * <p>
     * content 在点菜轮经常是 null，要显式写出 {@code "content": null}，有的实现省略这个字段会报格式错误。
     * tool_calls 必须 deepCopy，里面的 {@code id} 和后面 {@link #toolMessage} 的 {@code tool_call_id} 是一对。
     * 思考模型还会多一个 {@code reasoning_content}。下一轮请求如果丢掉它，DeepSeek 可能直接 400，
     * 所以出现时原样带上；普通对话模型没有这个字段，就不写。
     */
    private ObjectNode assistantMessage(JsonNode message) {
        ObjectNode copy = objectMapper.createObjectNode();
        copy.put("role", "assistant");
        JsonNode content = message.get("content");
        if (content == null || content.isNull()) {
            copy.putNull("content");
        } else {
            copy.set("content", content.deepCopy());
        }
        copy.set("tool_calls", message.path("tool_calls").deepCopy());
        if (message.hasNonNull("reasoning_content")) {
            copy.set("reasoning_content", message.get("reasoning_content").deepCopy());
        }
        return copy;
    }

    /**
     * 一条观察。{@code content} 是查表 JSON，不是自然语言。
     * 没有 {@code tool_call_id}，厂商无法把它安到上一轮的某一次点菜上，请求会被拒绝。
     * 这就是 ReAct 里的 Observation：来自 Java，不是模型在 content 里自己写的「我查到了已支付」。
     */
    private ObjectNode toolMessage(String toolCallId, String observation) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("content", observation);
        return message;
    }

    private ObjectNode textMessage(String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 工具菜单，形状与 DeepSeek 文档里的 tools 一致，每次请求都原样附上：
     * <pre>
     * { "type": "function",
     *   "function": {
     *     "name": "getOrder",
     *     "description": "...不能查库存...",
     *     "parameters": { "type": "object", "properties": { "orderId": {"type":"string"} }, "required": ["orderId"] }
     *   }
     * }
     * </pre>
     * description 是模型决定点不点、点哪一把的依据，required 约束参数名。
     * 这里不调用阶段 2 的 {@code @Tool} 方法：那些方法会写入 {@code ToolCallLog}，手写循环的轨迹以 steps 为准，
     * 两套审计叠在一次点击里会对不上。查的仍是同一份 Store。
     */
    private ArrayNode toolMenu() {
        ArrayNode tools = objectMapper.createArrayNode();
        tools.add(functionTool(
                "getOrder",
                "按订单号查询订单。只能查订单状态、SKU 和仓库，不能查库存数量。orderId 形如 ORD-1001。"
                        + "用户问能不能发货时，本工具的观察里还没有库存，下一轮必须再调用 getInventory。",
                Map.of("orderId", "订单号，例如 ORD-1001"),
                List.of("orderId")));
        tools.add(functionTool(
                "getInventory",
                "按 SKU 和仓库查询可用库存。只能查库存数量，不能查订单状态。sku 形如 SKU-88，warehouse 形如 WH-A。"
                        + "判断能不能发货时，以本工具返回的 available 为准。",
                Map.of("sku", "SKU，例如 SKU-88", "warehouse", "仓库编码，例如 WH-A"),
                List.of("sku", "warehouse")));
        return tools;
    }

    /** 拼一个 function tool。properties 的 value 是给模型看的参数说明，不是示例值本身。 */
    private ObjectNode functionTool(String name, String description, Map<String, String> properties, List<String> required) {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        ObjectNode props = parameters.putObject("properties");
        properties.forEach((key, desc) -> props.putObject(key).put("type", "string").put("description", desc));
        ArrayNode requiredNode = parameters.putArray("required");
        required.forEach(requiredNode::add);

        ObjectNode function = objectMapper.createObjectNode();
        function.put("name", name);
        function.put("description", description);
        function.set("parameters", parameters);

        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");
        tool.set("function", function);
        return tool;
    }

    /**
     * {@code function.arguments} 在协议里是字符串，内容却是 JSON，例如 {@code "{\"orderId\":\"ORD-1001\"}}。
     * 轨迹的 arguments 字段保留这份原文，页面上能看到模型实际传了什么。
     * 少数响应会把 arguments 直接做成对象，这时 toString 收成同样的 JSON 文本。
     */
    private String argumentsText(JsonNode function) {
        JsonNode arguments = function.get("arguments");
        if (arguments == null || arguments.isNull()) {
            return "{}";
        }
        if (arguments.isTextual()) {
            return arguments.asText();
        }
        return arguments.toString();
    }

    /**
     * 把参数字符串解析成对象再取 orderId / sku / warehouse。
     * 解析失败时返回空对象，随后的查表走 not_found，不把 Jackson 异常抛出循环——
     * 坏参数是一次观察，循环还可以把这段错误发回模型。
     */
    private JsonNode parseArguments(String arguments) {
        try {
            JsonNode node = objectMapper.readTree(arguments);
            return node.isObject() ? node : objectMapper.createObjectNode();
        } catch (JsonProcessingException ex) {
            return objectMapper.createObjectNode();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }

    private static String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    /**
     * application.properties 的 base-url 是 {@code https://api.deepseek.com}，没有路径。
     * Spring AI 自己会补 {@code /v1/chat/completions}；这里手写，补的是 DeepSeek 文档中的
     * {@code /chat/completions}。两条路径厂商都接受。已经带了这个后缀时不再拼第二次。
     */
    private static String chatCompletionsUrl(String baseUrl) {
        String root = baseUrl == null ? "" : baseUrl.trim();
        if (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        if (root.endsWith("/chat/completions")) {
            return root;
        }
        return root + "/chat/completions";
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        String oneLine = body.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 400 ? oneLine : oneLine.substring(0, 400);
    }
}
