# 阶段 1 Demo：提示词工程实验

> 目录：`demos/01-hello-llm/`（Python 对照）| Java 代码：`demos/backend/stage01` | 前端菜单 id：`prompt`
> 计划：[docs/stage-1.md](../../docs/stage-1.md)（每天 `D*-O*`）| 笔记：[notes/prompt-engineering.md](../../notes/prompt-engineering.md)（`K1–K20`）| 任务：[learning-plan.md S1-T1～T7](../../docs/learning-plan.md)

本目录放 **Python 对照脚本** 与实验说明。**Java 主线代码写在 `demos/backend` 的 `stage01` 包**，与阶段 0 共用同一后端服务（端口 8080），不再单独起 8081。

**前几天不要从这里开工**：先按 [docs/stage-1.md](../../docs/stage-1.md) 用阶段 0 的「自由问答」粘贴 prompt（D1–D4），并在 `demos/backend/stage00` 里加上 `.entity(TicketTriage.class)`（D5）。那边里程碑过了，再把 6 个实验实现到 `stage01`。

六个实验与手册天数的对应（拆独立工程时按此对齐）：

| Demo | 手册 | 任务 |
| --- | --- | --- |
| 1 角色设定 | D1 | S1-T2 |
| 2 Few-shot | D2 | S1-T2 |
| 3 CoT | D3 | S1-T2 |
| 4 PromptTemplate | D5 | S1-T3 / T6 |
| 5 结构化 Schema | D5 | S1-T5 / T6 |
| 6 失败模式 | D6 | S1-T4 |

---

## 目标

用同一组 B 端工单场景，把提示词五件套做成可对照实验：

1. 角色设定
2. Few-shot
3. CoT
4. PromptTemplate（模板与数据分离）
5. 结构化输出（Java record / BeanOutputConverter）
6. 失败模式（幻觉 / 指令冲突 / 截断）

阶段 0 证明“能调 API”。这里证明“换一种写法，结果可复现、可解析、可评估”。

---

## 约定目录

```
demos/01-hello-llm/
├── README.md                 # 本文件
├── prompt_experiments.py     # Python 对照（快速改 prompt 看现象）
└── （Java 在 demos/backend/src/main/java/com/example/demos/stage01/）
```

- **Java 是验收必须项**，写在 `demos/backend` 的 `stage01` 包：JDK 21、Spring AI 2.0、根目录 `.env` 的 `DEEPSEEK_API_KEY`。
- **Python 是对照**，一个脚本多个函数即可，激活 `venv/` 后跑。
- 配置方式与 `demos/backend` 共用，单服务端口 **8080**。

---

## 统一业务场景（不要改成写诗）

输入是一条内部工单，例如：

```
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。
库存服务最近发过版。请分类并给出下一步。
```

结构化出参（Java record 与此对齐）：

```java
public record TicketTriage(
        String category,          // PERF / BUG / DATA / UNKNOWN
        String severity,          // P0 / P1 / P2 / P3
        List<String> nextSteps,   // 最多 5 条
        String evidence,          // 必须引用用户原文，没有则空
        String confidence         // high / medium / low
) {}
```

类比：这就是工单中心的出参 DTO。程序只认这五个字段。

---

## 六个实验

### Demo 1 · 角色设定

- 同一条工单，用查询参数切换：`withRole=false` 无 system，`withRole=true` 明确“资深 Java 后端，只给可落地方案，不写空话，不确定就说不确定”。每次只调一次 LLM。
- 观察：术语是否变成 Spring/Redis/超时链路语言；是否还敢编造不存在的中间件。

建议接口：`POST /api/stage/01/prompt/role?withRole=false|true`

### Demo 2 · Few-shot

- 同一分类任务：0 条示例 / 1 条 / 3 条（含一条“信息不足 → UNKNOWN + low”）。
- temperature 固定 `0`。只改示例数量。
- 观察：`category` 是否稳定，负例是否真的能拦住乱分类。

建议接口：`POST /api/stage/01/prompt/few-shot?shots=0|1|3`

### Demo 3 · CoT

同一道乘法（默认 `17 × 24 = ?`，答案 408），只改提示写法：

| mode | 发给模型的内容 | 观察点 |
| --- | --- | --- |
| `direct` | `17 × 24 = ?` | 容易直接甩数字，错误率更高 |
| `cot` | 题目 +「请一步步计算，展示每一步的推理过程」 | 应拆成 20+4，completion tokens 明显变多 |
| `few-shot` | system + 15×13 范例 + 当前题 | 应模仿「拆成 (a+b)」的格式 |

temperature 固定 `0`。请求体可空；`ticket` 字段当作算式，可换成别的两位数乘法。

建议接口：`POST /api/stage/01/prompt/cot?mode=direct|cot|few-shot`

### Demo 4 · PromptTemplate

- 模板写死在代码或 `classpath:prompts/*.st`，工单正文只当 `param`。
- 用两张不同工单跑同一模板，证明没有字符串拼接。

建议接口：`POST /api/stage/01/prompt/template`

### Demo 5 · 结构化 Schema（里程碑）

- 使用 `ChatClient.entity(TicketTriage.class)`（内部即 BeanOutputConverter）。
- 对照阶段 0：不再手工 `ObjectMapper` + 剥 \`\`\`json。
- 断言：缺字段、类型不对时应失败并返回 `parseSuccess=false`，而不是吞掉。

建议接口：`POST /api/stage/01/prompt/schema`

### Demo 6 · 失败模式

三个可切换的坏 prompt：

| 模式 | 怎么构造 | 期望看到 |
| --- | --- | --- |
| 幻觉 | 问一个上下文里不存在的“集群节点数” | 不编数字，`confidence=low` |
| 指令冲突 | system 要求 JSON，user 要求 Markdown 表格 | 仍返回可解析 Bean，冲突写入说明 |
| 截断 | `max_tokens` 故意设很小 | `finish_reason=length`，调用方不当成成功 |

建议接口：`POST /api/stage/01/prompt/failure?mode=hallucination|conflict|truncate`

---

## Java 项目落地时要做的事（实现阶段再执行）

1. 在 `demos/backend` 的 `stage01` 包实现 6 个 POST 接口 + `GET /api/stage/01/prompt` 列出 endpoints。
2. `GET /api/health` 已由统一后端提供。
4. 每个响应带 `usage`（prompt / completion / total tokens），方便对照实验记账。
5. 改前端 `demos/frontend/demos.config.ts`：把 `prompt` 的 `status` 改为 `ready`，补齐 `apiPath` 和页面（可先复用 JsonPanel）。
6. README 补“如何启动”一节（与阶段 0 同风格）。

### 推荐代码骨架（实现时按此拆，避免一个 God Service）

```
com.example.demos.stage01
├── api/               # TicketTriage 等 record
├── prompt/            # 各实验的模板文本
└── controller/        # PromptExperimentController 等
```

ChatClient 调用形态（示意，以 Spring AI 2.0 为准）：

```java
TicketTriage triage = chatClient.prompt()
        .system(SYSTEM_TRIAGE)
        .user(u -> u.text("""
                工单如下：
                {ticket}
                """).param("ticket", body.ticket()))
        .call()
        .entity(TicketTriage.class);
```

---

## Python 对照脚本约定

`prompt_experiments.py` 用 OpenAI SDK 指向 DeepSeek（与阶段 0 `hello_llm.py` 相同）。提供：

```
python prompt_experiments.py          # 跑全部
python prompt_experiments.py role
python prompt_experiments.py fewshot
python prompt_experiments.py cot
python prompt_experiments.py schema
python prompt_experiments.py failure
```

只打印：prompt 版本、原始输出、是否能 `json.loads`、usage。不写框架。

---

## 对照实验怎么记

每次只改一个变量。建议在笔记末尾用这种表：

| 实验 | 变量 | temperature | prompt tokens | completion tokens | 结果是否可解析 | 主观质量（1–5） | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Few-shot | shots=0 | 0 |  |  |  |  |  |
| Few-shot | shots=3 | 0 |  |  |  |  |  |

没有这张表，阶段 1 不算做完。

---

## 本目录当前状态

| 项 | 状态 |
| --- | --- |
| 学习计划 / 资料 / 笔记 | 已就绪（docs + notes） |
| Python 对照脚本 | 待实现 |
| Java `prompt-java` | 待实现 |
| 前端菜单接通 | 待 Java 接口就绪后改 `demos.config.ts` |
