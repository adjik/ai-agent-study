package com.example.demos.stage03.plan;

import com.example.demos.stage03.loop.FixtureSummary;
import com.example.demos.stage03.loop.FixtureToolDispatcher;
import com.example.demos.stage03.loop.StepRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 先计划再执行：第一次模型调用里没有工具菜单，只让它写 JSON 步骤清单；
 * 清单回来之后 Java 才按顺序查表；最后一次调用拿观察写句子。
 * <p>
 * 和 react 的差别就在顺序上：这里 {@code plan} 在任何观察之前就已经存在，
 * 所以清单里的参数只能来自问句和菜单，不能来自查表结果。
 */
@Component
public class PlanThenExecute {

    private static final Logger log = LoggerFactory.getLogger(PlanThenExecute.class);

    private static final String PLAN_SYSTEM = """
            你是电商助手的规划器。现在还不能查表，只能写一张待办清单。
            可用工具只有两个：
            - getOrder：参数 orderId，形如 ORD-1001。
            - getInventory：参数 sku（形如 SKU-88）与 warehouse（形如 WH-A）。
            用户问订单状态时写一步 getOrder。用户问能不能发货时，先 getOrder，再 getInventory。
            某个参数只有查过订单才知道时，值写 null，执行时再补。
            只输出 JSON 数组，不要解释，例如：
            [{"tool":"getOrder","arguments":{"orderId":"ORD-1001"}},{"tool":"getInventory","arguments":{"sku":null,"warehouse":null}}]
            """;

    private static final String ANSWER_SYSTEM = """
            你是电商助手。下面有问句和已经查到的观察。只根据观察写一到两句结论，不要再要求查询。
            观察里的 error 是 invalid_argument 或 not_found 时，按这个错误说明，禁止说成已支付或 PAID。
            """;

    private final PlanningModel planningModel;
    private final FixtureToolDispatcher dispatcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlanThenExecute(PlanningModel planningModel, FixtureToolDispatcher dispatcher) {
        this.planningModel = planningModel;
        this.dispatcher = dispatcher;
    }

    /**
     * 三步，顺序不能换：
     * <ol>
     *   <li>写清单。此时没有工具菜单，模型看不到 PAID 和 10，清单只能根据问句猜。</li>
     *   <li>Java 按清单查表。这一步与模型无关，steps 里的观察是查出来的，不是模型写的。</li>
     *   <li>把观察拼成一段文字，让模型写结论。这次同样不挂工具，避免它又想再查一遍。</li>
     * </ol>
     */
    public PlanningResponse run(String question) {
        // 第一步：只要一张清单，此时还没有任何观察
        List<PlanStep> plan = parsePlan(planningModel.complete(PLAN_SYSTEM, question));
        if (plan.isEmpty()) {
            /*
             * 模型偶尔不按 JSON 输出。退化成只查订单，页面至少留下一条轨迹；
             * 比起整页 500，更能看出「规划这一步本身失败了」。
             */
            plan = List.of(new PlanStep("getOrder", "{}"));
        }

        // 第二步：Java 照着清单查表
        List<StepRecord> steps = new ArrayList<>();
        List<String> observationLines = new ArrayList<>();
        /* 订单观察里的 sku 和 warehouse，给下一步库存补参数用。清单里这两项是 null。 */
        String knownSku = null;
        String knownWarehouse = null;

        for (int i = 0; i < plan.size(); i++) {
            PlanStep item = plan.get(i);

            /* 库存这一步的参数写清单时还不知道，用上一步的订单观察补上。 */
            String arguments = item.arguments();
            if ("getInventory".equals(item.tool()) && knownSku != null) {
                arguments = toJson(Map.of("sku", knownSku, "warehouse", knownWarehouse));
            }

            FixtureToolDispatcher.Observation observation = dispatcher.dispatch(item.tool(), arguments);
            /*
             * finishReason 写 "plan" 而不是 "tool_calls"：这一次请求里模型没有点菜，
             * 是 Java 照着清单在查。两者在页面上是不同的东西，不能共用一个字符串。
             */
            steps.add(new StepRecord(
                    i + 1, "plan", item.tool(), arguments, observation.json(), observation.latencyMs()));
            observationLines.add((i + 1) + ". " + item.tool() + " " + observation.json());

            /* 订单这一步把 sku/warehouse 记下来，下一步库存要用。查不到时 JSON 里没有这两个键。 */
            if ("getOrder".equals(item.tool())) {
                JsonNode order = readJson(observation.json());
                if (order != null && order.hasNonNull("sku") && order.hasNonNull("warehouse")) {
                    knownSku = order.path("sku").asText();
                    knownWarehouse = order.path("warehouse").asText();
                }
            }
        }

        // 第三步：把观察整段交给模型写结论，不挂工具
        String answer = planningModel.complete(
                ANSWER_SYSTEM, "问句：" + question + "\n观察：\n" + String.join("\n", observationLines));
        FixtureSummary fixture = dispatcher.fixture();
        log.info("[plan] steps={} plan={}", steps.size(), plan);
        return PlanningResponse.plan(answer, steps, plan, fixture);
    }

    /**
     * 把模型写的清单解析成步骤。先去掉 markdown 代码围栏，再取第一对方括号之间的内容——
     * 模型常在 JSON 前后加一句解释。解析不出数组时返回空清单，由调用方退化处理。
     */
    private List<PlanStep> parsePlan(String raw) {
        String json = raw.replace("```json", "").replace("```", "").trim();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end <= start) {
            log.warn("[plan] 没有解析到 JSON 数组: {}", raw);
            return List.of();
        }

        JsonNode array = readJson(json.substring(start, end + 1));
        if (array == null || !array.isArray()) {
            return List.of();
        }

        List<PlanStep> plan = new ArrayList<>();
        for (JsonNode node : array) {
            String tool = node.path("tool").asText("");
            if (tool.isBlank()) {
                continue;
            }
            JsonNode args = node.get("arguments");
            plan.add(new PlanStep(tool, args == null || args.isNull() ? "{}" : args.toString()));
        }
        return plan;
    }

    /** 解析 JSON；不是 JSON 时打一条日志并返回 null，调用方跳过即可。 */
    private JsonNode readJson(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (JsonProcessingException ex) {
            log.warn("[plan] 不是合法 JSON，已跳过: {}", text);
            return null;
        }
    }

    /** 补库存参数时用它拼 JSON，避免手写引号拼错。失败时返回固定错误 JSON。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }
}
