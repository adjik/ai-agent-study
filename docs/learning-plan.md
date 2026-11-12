# AI Agent 分阶段学习计划（Java 主线版）

> 适用人群：资深 Java Web 后端工程师（Spring Boot / Dubbo / MySQL / Redis 生态），零 AI 基础 | 环境：macOS | 更新日期：2026-09-10
> 配套资料见 [resources.md](resources.md)，每个阶段的产出落地在 `demos/` 对应目录。
> **怎么判定一项任务完成**：阶段 1、阶段 2 看「阶段文档三层约定」。阶段 3 要掌握的知识点在本文件，每天的博客、代码和页面步骤在 [stage-3.md](stage-3.md)。阶段 4 起再按三层约定改造。

---

## 总体设计思路

AI Agent 开发 = **LLM 能力理解（基础） + 工具调用与编排（核心） + 工程化落地（差异化）**

对你最有利的一点：**生产级 Agent 的难点不在"调 API"，而在状态管理、并发控制、可观测性、评估和与现有系统的集成——这些恰好是你的主场**（分布式、缓存、微服务治理经验可以直接迁移）。学习时要有意识地建立类比：

| AI Agent 概念         | 你已有的工程经验（类比）             |
| --------------------- | ------------------------------------ |
| Agent 状态 / 会话记忆 | 分布式会话状态、Redis 缓存           |
| RAG 检索              | 缓存 + 数据库索引 + 搜索引擎倒排索引 |
| 工具（Tool）          | 微服务 API 接口（RPC/HTTP）          |
| MCP 协议              | Dubbo / OpenAPI 这类标准化服务协议   |
| 多 Agent 编排         | 微服务治理、服务编排                 |
| Agent 评估            | 测试体系、SLA 保障                   |
| 提示词 / 上下文       | 需求文档 / 接口文档                  |
| Token 成本 / 延迟优化 | 性能优化、成本治理                   |

**路线选择**：**主学 Java 生态**（Spring AI + LangChain4j + Spring AI Alibaba，在你的 Spring Boot 技术栈上直接构建 Agent），同时**了解 Python 生态**（LangGraph、CrewAI 等，用于学习行业方法论、阅读社区教程时无障碍）。两条腿走路，但 Java 为主。

### Java Agent 技术栈全景图

```
┌─────────────────────────────────────────────────────────┐
│                   多 Agent 编排层                         │
│  Spring AI Alibaba (Supervisor/ReAct)  │  LangGraph(Python参考)│
├─────────────────────────────────────────────────────────┤
│                   工具与协议层                            │
│  MCP Java SDK  │  Spring AI @McpTool  │  Function Calling  │
├─────────────────────────────────────────────────────────┤
│                   知识检索层 (RAG)                        │
│  LangChain4j  │  Spring AI VectorStore  │  pgvector/Redis  │
├─────────────────────────────────────────────────────────┤
│                   模型调用层                              │
│  Spring AI (ChatClient)  │  LangChain4j  │  OpenAI SDK兼容 │
├─────────────────────────────────────────────────────────┤
│                   基础设施层                              │
│  Spring Boot 4.x  │  JDK 21  │  MySQL/Redis  │  Maven/Gradle │
└─────────────────────────────────────────────────────────┘
```

---

## 阶段文档三层约定（阶段 1、2，以及阶段 4 起）

阶段 3 不另拆任务号、操作号、知识号。要掌握的点在下面「阶段 3」一节；每天读什么、写什么代码、页面上会看到什么，在 [stage-3.md](stage-3.md)。

每一阶段拆成三份文档，**编号一一对应**。不要只打总计划的勾、也不要只跑代码不记知识。

| 层             | 文件                    | 编号                | 写什么                              | 怎样算完成                                                |
| -------------- | ----------------------- | ------------------- | ----------------------------------- | --------------------------------------------------------- |
| **任务** | 本文件对应阶段          | `S{阶段}-T{序号}` | WHAT：要掌握什么、产出什么          | 该任务下的「操作完成项」+「知识完成项」**全部打勾** |
| **操作** | `docs/stage-N.md`     | `D{天}-O{序号}`   | HOW：今天点哪里、粘哪段、改哪个文件 | 当天「完成判定」清单全部打勾                              |
| **知识** | `notes/` 对应主题笔记 | `K{序号}`         | WHY：必知事实 + 怎么做 + 指定资料   | 该节勾「已读完」（知识已写在节内，不另写作文）            |

**完成规则（硬）**：

1. `S{N}-T*` 打勾 ⇔ 它列出的每一条 `D*-O*` 和每一条 `K*` 都已打勾。
2. 只读笔记、没跑对照实验 → 任务不算完成。
3. 只跑 prompt、笔记里 K 项没打勾 → 任务不算完成。
4. 对照实验表（笔记文末）没按手册填、结论三条模板没填完 → 该阶段不算完成。

**进入阶段 N（N≥4）的第一件事**（改造清单，阶段 1 已做完可当样板；阶段 3 已按天写在本文件，跳过这张清单）：

1. 把本文件该阶段每条任务编成 `S{N}-T*`，每条写上「知识完成项 / 操作完成项」。
2. 新建或补全 `docs/stage-N.md`：按天拆成 `D{天}-O*`，每天有「完成判定」。
3. 新建或补全 `notes/` 主题笔记：知识拆成 `K*`，每节写清「必知 / 怎么做 / 资料」，末尾勾「已读完」。不要用问答题代替教学内容。
4. 在本文件「文档改造记录」追加一行。
5. 未完成这 4 步，不要开始写该阶段 demo 代码。

阶段 1 样板：任务在下面 S1-T1～T7 → 操作 [stage-1.md](stage-1.md) → 知识 [prompt-engineering.md](../notes/prompt-engineering.md)。
阶段 2 样板：S2-T1～T7 → 整合文档 [stage-2.md](stage-2.md)（操作 + 必知 K* + §9 对照表，一份读完）。
阶段 3：知识点在本文件，10 天操作在 [stage-3.md](stage-3.md)。页面路径与 `demos/frontend/demos.config.ts` 里「Agent 核心」菜单一致。

---

## 阶段 0：环境与认知准备（第 1 周）

**目标**：装好 Java AI 开发环境，跑通第一个 LLM 调用，建立对 LLM 的正确心智模型。

- [X] **JDK21（必须）**：Spring AI 2.0 硬性要求 Java 21+（Spring Boot 4.x），LangChain4j 要求 JDK17+。

  - macOS 推荐：`brew install openjdk@21` 或下载 [Eclipse Temurin 21](https://adoptium.net/)
  - ⚠️ **不覆盖系统 JAVA_HOME**（保持 JDK8），使用方式：IDE 里指定 Project SDK，或终端 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
- [X] 安装 Python 3.12（仅用于辅助脚本和阅读 Python 教程时跑示例，不是主线）
- [X] **注册 LLM API**：DeepSeek（API 兼容 OpenAI 格式）
- [X] 理解 LLM 基本心智模型：Token、上下文窗口、温度/采样参数、幻觉、指令遵循
- [X] 用 Python 版 OpenAI SDK 快速跑通第一个 LLM 调用（验证 API 可用）
- [X] **用 Spring AI 跑通第一个 Java 版 LLM 调用**：创建 Spring Boot 4.x 项目 → 引入 `spring-ai-starter` → 配置 DeepSeek API → 写第一个 ChatClient 调用
- [X] IDE 配置：IntelliJ IDEA（你熟）+ 安装 Spring AI 插件 / Copilot 插件（可选）

**产出**：

- `demos/00-env/hello_llm.py` ✅（已有，验证 API 连通性）
- `demos/00-env/hello-llm-java/` —— Spring Boot + Spring AI 项目，4 个 Demo：简单问答、系统提示词+多轮对话、结构化 JSON 输出、流式输出（用 Java 重写）
- `notes/llm-basics.md` ✅

**参考**：

- Spring AI 官方文档（docs.spring.io/spring-ai）
- [Microsoft Spring AI for Beginners](https://github.com/microsoft/Spring-AI-for-Beginners) —— Java 专属入门

---

## 阶段 1：LLM 原理与提示词工程（1-2 周）

> 操作手册：[stage-1.md](stage-1.md) · 知识笔记：[prompt-engineering.md](../notes/prompt-engineering.md) · 博客：[resources.md#stage-1](resources.md#stage-1)
> 编号已对齐：本段 `S1-T*` ⇔ `stage-1.md` 的 `D*-O*` ⇔ 笔记的 `K*`。**某一条 S1-T 打勾 = 它下面列出的操作项 + 知识项全部打勾。**

**目标**：不深究数学，但要懂机制；掌握提示词工程核心技巧，能把复杂需求拆成 LLM 友好的指令，并用 Java 类型接住输出。

阶段 0 解决「能调通」；阶段 1 解决「换一种写法，结果可复现、可对接、可评估」。**本阶段不做 Tool Calling。**

**动手类型**（与 [stage-1.md](stage-1.md) 标签一致）：`【只读】` 只读笔记 · `【调用后端】` curl/前端打已有接口 · `【写后端】` 改 `hello-llm-java` · `【写后端·加分】` 可选 `01-hello-llm/prompt-java`。**阶段 1 唯一硬性写代码日 = Day 5**（`.entity(TicketTriage)` + `/api/test/triage`）。

**阶段完成总开关**（下面 7 条 S1-T 全勾，本阶段才算完）：

- [X] S1-T1 LLM 原理（科普级）
- [ ] S1-T2 提示词五件套
- [ ] S1-T3 上下文工程
- [ ] S1-T4 常见失败模式
- [ ] S1-T5 结构化输出对接 Java
- [ ] S1-T6 Java 实战（PromptTemplate + `.entity()`）
- [ ] S1-T7 对照实验与复盘

---

### S1-T1 LLM 原理（科普级）

**要掌握**：Transformer 直觉、预训练 / SFT / RLHF、为什么上下文是第一等公民。不推公式。

**知识完成项**（[prompt-engineering.md](../notes/prompt-engineering.md) 对应节全部打勾）：

- [X] K1 Transformer 直觉：不是查库，是加权补全
- [X] K2 预训练 / SFT / RLHF：博学 ≠ 听话
- [X] K3 为什么上下文是第一等公民

**操作完成项**（[stage-1.md](stage-1.md)）：

- [X] D0 `【调用后端】` 完成判定全部打勾（启动 8080 + 读完 K1–K3 + 固定工单 A）

---

### S1-T2 提示词五件套

**要掌握**：角色设定、指令清晰化、Few-shot、CoT、结构化输出（JSON Schema 概念）。把模糊需求拆成 LLM 能执行的契约。

**知识完成项**：

- [X] K4 提示词是工程，不是文案
- [X] K5 五件套总览 + 合格骨架
- [X] K6 角色设定
- [X] K7 指令清晰化
- [X] K8 Few-shot
- [X] K9 CoT（思维链）
- [X] K10 结构化输出（概念 + DeepSeek JSON 规则）

**操作完成项**：

- [X] D1 `【调用后端】` 全部完成判定（坏/好 prompt 对照，不改 Java）
- [X] D2 `【调用后端】` 全部完成判定（0-shot vs 3-shot）
- [X] D3 `【调用后端】` 全部完成判定（直接答 vs CoT）
- [X] D4 `【调用后端】` 全部完成判定（强制 JSON 能 parse）

---

### S1-T3 上下文工程

**要掌握**：系统提示词、对话历史管理、上下文压缩。类比「接口入参校验 / 文档规范 / Redis List 裁剪」。

**知识完成项**：

- [X] K11 三层消息（system / history / user）职责别混
- [X] K12 上下文压缩（滑动窗口的缺陷 + 摘要/槽位）
- [X] K13 PromptTemplate：指令与数据分离

**操作完成项**：

- [X] D5-O3 `【写后端】` 角色/边界进 `.system()`，工单正文进 `.user().param("ticket", …)`（手册 Day 5 已给出完整可粘贴代码）
- [X] D7 `【只读】` 读完 K12（压缩只建立概念，不写代码）

---

### S1-T4 常见失败模式

**要掌握**：幻觉、截断、指令冲突（外加指令忽略），以及本阶段能做的缓解手段。

**知识完成项**：

- [X] K14 幻觉
- [X] K15 截断
- [X] K16 指令冲突
- [X] K17 指令忽略

**操作完成项**：

- [X] D6 `【调用后端】` 全部完成判定（三次独立请求：幻觉 / 冲突 / 截断，打 `/api/test/triage`）
- [X] 笔记 §9 对照表「失败」行至少填 3 行

---

### S1-T5 结构化输出对接 Java

**要掌握**：用 Schema 把 LLM 从「聊天对象」变成「上游服务」。

- Spring AI：`BeanOutputConverter` / `ChatClient.entity(Xxx.class)`
- LangChain4j：`AiServices` 接口式调用（本阶段只对照阅读，类比 Feign）
- 类比：Bean 约束 = DTO 契约

**知识完成项**：

- [X] K10 结构化输出（概念 + DeepSeek 三条规则）
- [X] K18 Java `.entity()` 路径与失败兜底
- [X] K19 AiServices 对照（只扫一眼，不交付）

**操作完成项**：

- [X] D4 `【调用后端】` 手工 JSON 能被 Jackson / `json.loads` 解析，翻车点记进笔记
- [X] D5 `【写后端】` 用 `.entity(TicketTriage.class)` 接到 record（改 `hello-llm-java`）
- [X] D7-O5 `【只读】` 扫一眼 LangChain4j AiServices 文档（5 分钟，不写代码）

---

### S1-T6 Java 实战（PromptTemplate + `.entity()`）

**要掌握**：在阶段 0 项目 `hello-llm-java` 里改一处真正跑通。独立工程 `prompt-java`（8081）是加分项，不挡进阶段 2。

**知识完成项**：

- [X] K13 PromptTemplate / `.param`
- [X] K18 `.entity()` 肌肉记忆表

**操作完成项**：

- [X] D5 `【写后端】` 全部完成判定（record + 接口 + `.system()` / `.param()` / `.entity()`）
- [X] `【写后端】` 前端或 curl 能稳定拿到可解析的分诊 JSON / Java record
- [X] `【写后端】` 解析失败不当成业务成功（抛错或 `parseSuccess=false`）

**加分（不挡 S1-T6 打勾）**：

- [X] `【写后端·加分】` 按 [demos/01-hello-llm/README.md](../demos/01-hello-llm/README.md) 拆出端口 8081 的 6 个实验接口

---

### S1-T7 对照实验与复盘

**要掌握**：同一任务只改一个变量；用质量 / 耗时 / 能否解析说话，不靠感觉。temperature 心里按 0 理解。

**知识完成项**：

- [X] K20 与阶段 2 的边界（提示词管格式与推理，Tool 管事实）

**操作完成项**：

- [X] D7 `【只读】` 完成判定全部打勾
- [X] `【只读】` 笔记 §9 至少 6 行有数据（角色 / Few-shot / CoT / JSON 或 entity / 失败）
- [X] `【只读】` 结论 3 条已按手册 Day 7 模板填空（不是开放题）

---

**产出**：

- `notes/prompt-engineering.md`：K1–K20 勾「已读完」+ §9 对照表 + Day 7 三条结论填空
- `docs/stage-1.md`：D0–D7 每天「完成判定」全部打勾
- `demos/00-env/hello-llm-java/` `【写后端】`：至少一处 `.entity(TicketTriage.class)` + `.param`（主线里程碑）
- `demos/01-hello-llm/` `【写后端·加分】`：六个实验规范已有；独立工程为加分项

**里程碑**：在 `hello-llm-java` 里 `ChatClient.entity(TicketTriage.class)` 接到 record。独立 `prompt-java`（8081）为加分项。

**参考（博客优先，课和书不进验收）**：见 [resources.md#stage-1](resources.md#stage-1)

- [Prompt 工程实战：System / Few-shot / CoT](https://jishuzhan.net/article/2065228421654999042)
- [Spring AI 2.0 ChatClient.entity](https://blog.csdn.net/weixin_44094991/article/details/162463806)
- [DeepSeek JSON Output 一页纸](https://api-docs.deepseek.com/zh-cn/guides/json_mode)

---

## 阶段 2：Function Calling / Tool Use（2-3 周）

> 整合学习文档：[stage-2.md](stage-2.md)（必知 K*、操作 D*、§9、示例效果、写代码日历均在同一文件）· 博客：[resources.md#stage-2](resources.md#stage-2)
> 编号已对齐：本段 `S2-T*` ⇔ `stage-2.md` 各天完成判定 + 当天 **K*** 勾选项 + 文末 **§9**。**某一条 S2-T 打勾 = 它列出的操作项 + 知识项全部打勾。**
> Java 写在统一后端 `demos/backend` 的 `stage02` 包（端口 8080），**不要**另开 `02-tool-use-java` 工程。

### 这项技术是干什么的

**Function Calling**（厂商文档里也叫 **Tool Use**、**Tool Calling**，指的是同一件事）是聊天接口上的一种约定：请求里除了用户那句话，再带一份「本系统现在能调用的函数菜单」。模型读完问句，可以返回一份结构化点菜单——函数名和 JSON 参数。你的 Java 按这份点菜单去查库或调接口，把结果交回模型，模型再写成给人看的回答。

订单是否已支付、库存还剩多少，在你的库和 Dubbo/HTTP 接口里。阶段 1 的提示词规定的是「怎么说、输出长什么样」；把几行样例写进提示词，也只能覆盖事先贴进去的那几条。Function Calling 把「调哪个接口、参数从这句话里抽什么」交给模型，数值和状态仍由你的方法返回。阶段 3 的 Agent，就是把下面这一轮反复跑起来。

用你已经会的接口开发来对照：

| 你已经会的                                              | 在 Function Calling 里                                        | 谁来做                   |
| ------------------------------------------------------- | ------------------------------------------------------------- | ------------------------ |
| OpenAPI / Dubbo 接口说明（operationId、字段描述、必填） | 请求里的 `tools`：`name`、`description`、`parameters` | 你写菜单，随每次请求带上 |
| 调用方按文档填 JSON                                     | 响应里的 `tool_calls`：选中的函数名 + 实参                  | 模型                     |
| Provider 查库、返回 DTO                                 | `@Tool` 方法，例如查 `OrderStore`                         | 你的 Spring 进程         |
| 把 DTO 组织成给前端的文案                               | 下一轮模型生成的自然语言                                      | 模型                     |

一次提问在协议上走完这些步。DeepSeek 用的是 OpenAI 兼容格式；Spring AI 的 `ToolCallingAdvisor`、LangChain4j 的 `AiServices` 把第 3、4 步收进一次 `call()`，Controller 对外仍是一个 POST：

| 步 | 发生什么                                                                      | 本仓库里对应                                       |
| -- | ----------------------------------------------------------------------------- | -------------------------------------------------- |
| 1  | 应用把问句和菜单发给模型                                                      | D0 的 curl body；D1 起是 `ChatClient.tools(...)` |
| 2  | 模型返回 `tool_calls`，`finish_reason=tool_calls`。这一轮还没有「已支付」 | D0 停在这里，响应里没有 `PAID`                   |
| 3  | 应用执行 Java 方法，得到 `PAID` 或库存 `10`                               | `getOrder` / `getInventory`                    |
| 4  | 把结果以 `role=tool` 再发给模型                                             | 框架自动做；D0 只先认出这个字段                    |
| 5  | 模型写成给人看的句子。还要再查一处时，回到第 2 步                             | Q3「能不能发货」会连查订单和库存                   |

本阶段用订单和库存把上面 5 步跑通：单工具、多工具、参数校验、调用日志、Spring AI 与 LangChain4j 各一版。菜单里先放查询。手写循环、会话记忆、RAG、MCP 从阶段 3 起再加。

**先读（概念向，每篇十几分钟，读完再做下面的 S2-T1）**：

| 读什么                                     | 链接                                                                                              | 读完你应能说出                                                  |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------- | --------------------------------------------------------------- |
| 这项能力是什么、定义 / 调用 / 整合各谁干活 | [AWS：什么是 Function Calling](https://aws.amazon.com/cn/what-is/function-calling/)                | 模型产出调用请求，应用执行，结果再交回模型写回答                |
| 工具、工具调用、工具输出，以及五步往返     | [OpenAI：函数调用（中文）](https://developers.openai.com/zh-Hans/api/docs/guides/function-calling) | `tool_calls` 是模型发出的请求，执行在你的应用里               |
| 应用端解析函数名和入参、再发起第二次调用   | [阿里云百炼：Function Calling](https://help.aliyun.com/zh/model-studio/qwen-function-calling)      | 工具输出放进 `role=tool` 之后，模型才做自然语言总结           |
| 本仓库实际打的协议字段                     | [DeepSeek Tool Calls](https://api-docs.deepseek.com/zh-cn/guides/tool_calls)                       | `tools` / `tool_calls` / `finish_reason` 在 JSON 里的位置 |
| Java 里菜单怎么挂到方法上                  | [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)                | `@Tool` 是菜单，`.tools()` 声明这次请求带上哪些方法         |

按天打开哪一篇、看完立刻改哪段代码，仍以 [resources.md#stage-2](resources.md#stage-2) 和 [stage-2.md](stage-2.md) 为准。上面这五篇只负责建立「这项技术干什么」。

**目标**：**吃透 Function Calling——这是 Agent 的基石**。模型只点菜，Java 才下厨。Spring AI 与 LangChain4j 各实现一版「查订单 / 查库存」（内存夹具，不接真 MySQL）。

阶段 1 解决「怎么说、怎么输出」；阶段 2 解决「事实从工具来，循环由框架自动跑」。**本阶段不手写 Agent Loop、不上 ChatMemory / RAG / MCP。**

**动手类型**（与 [stage-2.md](stage-2.md) 标签一致）：`【只读】` · `【直连 API】` · `【调用后端】` · `【写后端】` · `【写后端·加分】`。**硬性写代码日 = D1～D6**（D0 直连看协议；D7～D9 对照；D10 填表）。

**阶段完成总开关**（下面 7 条 S2-T 全勾，本阶段才算完）：

- [ ] S2-T1 Function Calling 本质
- [ ] S2-T2 Spring AI `@Tool` + 自动循环
- [ ] S2-T3 LangChain4j `@Tool` + AiServices
- [ ] S2-T4 Tool 工程化（多工具 / Trace / 错误 / 失败）
- [ ] S2-T5 两框架对照 + 复盘
- [ ] S2-T6 工具入参契约
- [ ] S2-T7 上下文缓存与成本

---

### S2-T1 Function Calling 本质

**干什么**：用 curl 直接打 DeepSeek，请求里带一份叫 `getOrder` 的菜单。看模型返回「请调用 `getOrder`，参数是 ORD-1001」。这一天不启动 8080，也没有 Java 方法去执行，所以响应里还没有真实的 `PAID`。

**为了什么**：先看见厂商协议上的点菜，再写 Spring 注解。后面接口能答出「已支付」时，你能分清哪一段是模型选的函数，哪一段是你的代码查出来的。菜单的写法就是一份迷你 OpenAPI：`name`、`description`、`parameters`。

**四个字段各管一段**（都在 DeepSeek 的请求或响应 JSON 里，还不是 Java 类）：

| 字段                         | 出现在哪               | 作用                                                                                          |
| ---------------------------- | ---------------------- | --------------------------------------------------------------------------------------------- |
| `tools`                    | 你发出的请求           | 菜单。模型只能点这里写过的函数                                                                |
| `tool_calls`               | 模型这一轮的响应       | 点菜结果：函数名 + 参数。此时还没查到订单                                                     |
| `finish_reason=tool_calls` | 同一轮响应             | 表示模型停在「等你执行」，这一轮还没有最终回答                                                |
| `role=tool`                | 你把执行结果再发回去时 | 把 Java 查到的 `PAID` 交回模型，模型才写成给人看的句子。D0 只认这个字段，真正回传从 D1 开始 |

**本阶段要打通的用法**（只打云端，body 用仓库里的 `demos/02-tool-use/d0-tool-calls.json`）：

| 用法                 | 请求里有什么                           | 模型会做什么                                                | 你要看到的结果                                           |
| -------------------- | -------------------------------------- | ----------------------------------------------------------- | -------------------------------------------------------- |
| 问状态并给出查单菜单 | 用户问 ORD-1001，菜单里有 `getOrder` | 返回 `tool_calls`，`name=getOrder`，参数含 `ORD-1001` | `finish_reason` 为 `tool_calls`；正文里没有 `PAID` |

**同一套用法对应的业务类型**（D0 只用这一份查单菜单演练）：

1. **问句里的事实在你自己的系统里**：订单状态、库存、用户是否存在。模型先返回要调哪个接口，接口还没被调用时，回答里不该出现库里的值。
2. **菜单等于对外接口说明**：函数名像 operationId，参数说明像 OpenAPI 的字段描述。模型按说明填参，和调用方按接口文档填 JSON 是同一件事。
3. **点菜和查库分成两次**：D0 停在点菜。D1 起才由 Java 执行并把结果交回。

**知识完成项**（在 [stage-2.md](stage-2.md) 对应天「必知」里打勾）：

- [X] K1 模型只点菜，Java 才下厨
- [X] K2 一次循环 4 步 + 时序图
- [X] K3 Tool Schema ≈ OpenAPI
- [X] K4 DeepSeek 线上协议：tools / tool_calls / role=tool

**操作完成项**（[stage-2.md](stage-2.md)）：

- [X] D0 `【直连 API】` 完成判定全部打勾（curl 看到 `tool_calls` 且没有 PAID；K2 时序图已抄）

---

### S2-T2 Spring AI `@Tool` + 自动循环

**干什么**：用户用一句话问业务事实时，Spring 把你的 Bean 方法交给模型当菜单。模型点中哪一个，框架就在本机执行，把查到的结果交回模型，由模型写成回答。对外仍是一次 `POST /api/stage/02/chat`。

**为了什么**：阶段 1 的回答来自提示词。订单是否已支付、库存还剩多少，必须来自 `OrderStore` / `InventoryStore`。这一条把聊天接口接上你自己的查询方法；一个问题要先查订单再查库存时，框架自己再跑一轮，Controller 里保持一次 `call()`。

**四个东西各管一段**（包名 `org.springframework.ai.tool.annotation.Tool`）：

| 你写的                     | 放在哪                                     | 作用                                                          |
| -------------------------- | ------------------------------------------ | ------------------------------------------------------------- |
| `@Tool`                  | `getOrder`、`getInventory` 方法上      | 方法说明进菜单。模型靠这段说明决定查订单还是查库存            |
| `@ToolParam`             | `orderId`、`sku`、`warehouse` 参数上 | 从用户那句话里抽出单号或 SKU，填进方法参数                    |
| `ChatClient.tools(bean)` | 发请求前                                   | 声明这次请求的菜单里有哪些 Bean。挂上的方法才能被点到         |
| `ToolCallingAdvisor`     | `call()` 内部默认开启                    | 模型点菜之后执行 Java，再把结果发回。连查两次时自动进入下一轮 |

**本阶段要打通的用法**（问法就是 [stage-2.md](stage-2.md) 里的 Q1 / Q2 / Q3，数据是内存夹具）：

| 用法             | 用户怎么说                                      | 框架会做什么                             | 你要看到的结果                         |
| ---------------- | ----------------------------------------------- | ---------------------------------------- | -------------------------------------- |
| 按单号查一条     | `ORD-1001 现在什么状态？`                     | 只执行 `getOrder`                      | 回答里是已支付；日志里有 `ORD-1001`  |
| 换一把工具查数量 | `SKU-88 在 WH-A 还有多少库存？`               | 只执行 `getInventory`                  | 回答是 10；主调是库存工具              |
| 一个问题连查两处 | `ORD-1001 能不能发货？先查订单再查对应库存。` | 自动循环至少两轮：先订单，再库存，再总结 | 一次响应里两次工具执行，再给出能不能发 |
| 核对答案来自方法 | 同一句 Q1 连打两次                              | 每次请求都进入 Java 方法                 | 两次日志都有 `[tool] getOrder`       |

**同一套用法对应的业务类型**（这 11 天只用订单和库存演练，不另做天气或真库）：

1. **按编号查主数据**：订单、用户、工单、运单。演练题是 Q1。
2. **按编码加地点查数量**：库存、额度、名额。演练题是 Q2。
3. **先读 A 再读 B 才能回答**：能不能发货、能不能退款、有没有权限。演练题是 Q3，靠上面的自动循环完成。
4. **菜单里只放查询**：改状态、改库存、退款先不注册。用户说「改成 999」时内存仍是 10，这条的验收在 S2-T4。

**知识完成项**：

- [X] K5 Spring AI `@Tool` + `@ToolParam`
- [X] K6 `ChatClient.tools()` 与自动循环
- [X] K7 用日志证明 4 步发生了
- [X] K8 多 Tool：模型按 description 选

**操作完成项**：

- [X] D1 `【写后端】` 全部完成判定（`getOrder` + `/api/stage/02/chat`，Q1 能答 PAID）
- [X] D2 `【写后端】` 全部完成判定（日志两次都有 getOrder + ORD-1001）
- [X] D3 `【写后端】` Q1/Q2/Q3 选择判定通过（库存工具已挂上）

---

### S2-T3 LangChain4j `@Tool` + AiServices

**干什么**：订单表和库存表仍是 S2-T2 那一份。再写一条 `POST /api/stage/02/chat-lc4j`，用 LangChain4j 跑同一句「ORD-1001 现在什么状态」。两边都应答出已支付。

**为了什么**：只走 Spring AI 时，容易把 `.tools()` 当成工具调用本身。换一条接入之后可以看到：`PAID` 来自 `OrderStore`，变的是注解包名、注册方式和谁在跑循环。

**四个东西各管一段**：

| 你写的                           | 放在哪                                               | 作用                                                                                                             |
| -------------------------------- | ---------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `@Tool`                        | 包名 `dev.langchain4j.agent.tool.Tool`，单独一个类 | 把方法登记成 LangChain4j 的菜单。和 Spring 的 `@Tool` 分属两个类，避免 import 串到同一套菜单                   |
| `@P`                           | 方法参数上                                           | 参数说明，作用同 Spring 的 `@ToolParam`：让模型把 `ORD-1001` 填进 `orderId`                                |
| `AiServices.builder().tools()` | 新建代理时                                           | 按你声明的接口生成实现，并在内部跑「点菜 → 执行 → 再问」。用法像 Feign：接口是你的，调用和拦截是框架的         |
| `new OpenAiChatModel`          | D6 手动创建                                          | 指定模型和 DeepSeek 地址。本阶段不加 `langchain4j-spring-boot-starter`，避免和已有的 Spring AI 自动配置抢 Bean |

**本阶段要打通的用法**（同一句 Q1，两个 URL）：

| 用法                 | 打哪里                           | 框架会做什么                           | 你要看到的结果                                                   |
| -------------------- | -------------------------------- | -------------------------------------- | ---------------------------------------------------------------- |
| 原入口仍走 Spring    | `POST /api/stage/02/chat`      | Spring 的自动循环调 `getOrder`       | 回答里是已支付                                                   |
| 新入口走 LangChain4j | `POST /api/stage/02/chat-lc4j` | `AiServices` 调同一份 `OrderStore` | 同一句 Q1 也是已支付；这个类里没有 Spring AI 的 `@Tool` import |

**同一套用法对应的业务类型**（这 11 天只把 Q1 再接一条入口）：

1. **同一查询换接入层**：Spring MVC 一条、另一套客户端再一条，底下仍是同一个 Service / Store。
2. **接口式声明能力**：对外一个 Java 接口，框架生成实现并拦截工具调用，和 Feign 声明远程方法相同。
3. **一个进程里两套客户端并存**：模型客户端自己 `new`，自动配置只留已经在用的那一套。

**知识完成项**：

- [X] K14 LangChain4j `@Tool` + `@P`
- [X] K15 AiServices ≈ Feign + 拦截器

**操作完成项**：

- [X] D6 `【写后端】` 全部完成判定（`POST /api/stage/02/chat-lc4j` 对 Q1 能答对；两套注解不混类）

---

### S2-T4 Tool 工程化

**干什么**：菜单上有两把查询工具时，让模型按说明选对；每次执行留在响应里能验收；查无此单时回业务结果；用户要求改数据时，内存里的数保持不变。再用四句固定问法，把「答错了」分成四类。

**为了什么**：接口能答出 PAID 之后，上线更常见的是选错工具、口头说改成功了、查无此单却编一个状态。这些往往没有 Java 异常。调用方也看不到服务器日志，所以要在响应里留下调了哪一把。

**五个东西各管一段**：

| 你写的                 | 放在哪                           | 作用                                                                     |
| ---------------------- | -------------------------------- | ------------------------------------------------------------------------ |
| 互斥的 `description` | 两把 `@Tool` 上                | 一段写明只能查订单，一段写明只能查库存。模型靠这段话分工                 |
| 不注册写方法           | 菜单里只有查询                   | 用户说「改成 999」时，没有方法可点，`/store` 里的库存仍是 10           |
| `toolsCalled`        | `/chat` 的响应                 | 列出工具名、参数、耗时、是否成功。Q3 应有两行，调用方不用盯终端          |
| `not_found`          | `getOrder` 查不到时的返回 JSON | 告诉模型「没有这张单」。堆栈留在服务器日志，不写进回答                   |
| 四类失败问法           | D9 只 curl、不改代码             | 给以后的评估贴标签：选错工具、参数胡编、该查却直接答、声称改完但数据没变 |

**本阶段要打通的用法**：

| 用法               | 用户怎么说                        | 你要核对什么                     | 你要看到的结果                                              |
| ------------------ | --------------------------------- | -------------------------------- | ----------------------------------------------------------- |
| 两把工具按说明分工 | Q1 / Q2 / Q3                      | 终端里主调哪一把                 | Q1 主调 `getOrder`，Q2 主调 `getInventory`，Q3 两把都有 |
| 口头改数据         | `把 SKU-88 的库存改成 999`      | 再打 `GET /api/stage/02/store` | `available` 仍是 10                                       |
| 一次提问留审计     | Q3                                | 响应里的 `toolsCalled`         | 两行：订单一行、库存一行                                    |
| 查无此单           | 问 `ORD-9999`                   | 回答和工具返回                   | 返回 `not_found`；回答里不编造 PAID，也不贴 Java 堆栈     |
| 语序故意打乱       | `WH-A 的 SKU-88 还有多少库存？` | 日志里的工具名                   | 仍主调 `getInventory`，参数是 SKU-88 和 WH-A              |
| 问句里没有单号     | `查一下那个单`                  | `toolsCalled.success` 或日志   | 不把夹具里的 PAID 安到「那个单」上                          |
| 标准问句必须进方法 | 再打一次 Q1                       | 终端有没有 `[tool] getOrder`   | 回答里的「已支付」要能对上这一行日志                        |

**同一套用法对应的业务类型**：

1. **一个入口挂多只读接口**：查订单、查库存、查物流。说明写互斥，避免拿查单接口去答库存。
2. **写操作先不暴露**：改状态、改库存、退款不进菜单。模型说「已改好」时，以再读一次库为准。
3. **调用可审计**：每次工具执行留工具名、参数、耗时、成功与否，和把 access log 放到响应里相同。
4. **业务未命中单独回传**：单号不存在回 `not_found`，格式不对留到 S2-T6 的 `invalid_argument`。两类分开，话术才分得开。
5. **评估按模型行为分类**：选错、乱填参数、该查不查、声称写成功。阶段 3 统计时按这四类分别计数。

**知识完成项**：

- [X] K8 多 Tool 选择（与 T2 共用）
- [X] K9 选择策略：少而精、互斥、禁止写工具
- [X] K10 Trace：toolName / args / latency / success
- [X] K11 错误处理：not_found 回传模型
- [X] K19 Tool 失败四种

**操作完成项**：

- [X] D3 `【调用后端】` 「改库存 999」后 `/store` 仍是 10
- [X] D4 `【写后端】` 全部完成判定（Q3 有两行 toolsCalled；ORD-9999 不编造）
- [X] D9 `【调用后端】` 全部完成判定（四种失败都打过）
- [X] 笔记 §9 「多工具 / 策略 / 错误 / 失败」行已填

---

### S2-T5 两框架对照 + 复盘

**干什么**：不改代码。同一句 Q1、同一句 Q3，Spring 的 `/chat` 和 LangChain4j 的 `/chat-lc4j` 各打两轮，把现象填进 §9。最后用表里已有的字写成 3 条结论，并按「对应的天数和知识都完成」去勾总计划。

**为了什么**：D6 只证明了 Q1 两边都能答。这里要留下可复核的差异：两边都站在同一份内存表上，差别在注册方式和响应里有没有 `toolsCalled`。同时划清这一阶段的停点：循环由 `ChatClient.call()` 和 `AiServices` 各跑一层。手写 `while` 再调模型、以及 `ChatMemory`，留到阶段 3，避免一次请求里叠两层循环。

**两样产出各管一段**：

| 你留下的           | 放在哪                                      | 作用                                                     |
| ------------------ | ------------------------------------------- | -------------------------------------------------------- |
| 对照表「我的实测」 | [stage-2.md](stage-2.md) 文末，至少 5 行维度 | 记下同一问法下两边的答案、调了哪把工具、响应里有哪些字段 |
| §9 的 3 条结论    | 表格下方，用 Day 10 模板把横线换成表内原文  | 分别收成：事实来自工具、选工具靠说明、错误和成本量得出来 |

**本阶段要打通的用法**（终端 1 保持 D6 之后的进程）：

| 用法             | 怎么打                                     | 你要核对什么                  | 你要看到的结果                                                                |
| ---------------- | ------------------------------------------ | ----------------------------- | ----------------------------------------------------------------------------- |
| 单工具两边各两轮 | Q1 打 `/chat` 和 `/chat-lc4j`，各 2 次 | 答案是否都来自 `OrderStore` | 四次都能对上已支付                                                            |
| 连查两边各两轮   | Q3 同样各 2 次                             | 谁在响应里带回执行记录        | Spring 的 JSON 有 `toolsCalled`；LangChain4j 这条入口只有 `answer`        |
| 收成三句         | 数 §9「一句话现象」非空行                 | 至少 8 行有现象后再填模板     | 三句都能指回某一次 curl 的原文                                                |
| 页面查漏（加分） | 浏览器打开工具调用页，发送 Q1              | 代理是否打到当前 8080         | 页面 JSON 里有已支付。没有 `toolsCalled` 时再补前端字段。不做也不挡本条打勾 |

**同一套用法对应的业务类型**：

1. **同一业务两套接入做选型**：比注册方式和返回给调用方的字段。订单事实应相同，因为读的是同一张表。
2. **复盘要能回溯**：结论抄实验表里的工具名、`PAID`、cache hit 数字。现写「学会了工具调用」无法对照某一次请求。
3. **这一阶段停在框架循环**：自动循环负责点菜和执行。自己写循环、会话记忆，是下一阶段要看见的状态，不提前加进 `stage02`。

**知识完成项**：

- [X] K16 Spring AI vs LangChain4j 对照（≥5 行）
- [X] K20 与阶段 3 的边界

**操作完成项**：

- [X] D7 `【调用后端】` 全部完成判定（Q1/Q3 两边各 2 次）
- [X] D10 `【只读】` 完成判定全部打勾
- [X] `【只读】` 笔记 §9 至少 8 行有数据
- [X] `【只读】` 结论 3 条已按手册 Day 10 模板填空

**加分（不挡 S2-T5 打勾）**：

- [X] `【写后端·加分】` 前端 `tool-use` 菜单接通，或 `demos/02-tool-use/` Python 对照

---

### S2-T6 工具入参契约

**干什么**：在 `getOrder`、`getInventory` 方法开头检查参数。单号不是 `ORD-数字`、或者根本没有单号时，返回 `invalid_argument`，不进 `OrderStore` 去对 `PAID`。同时只读 DeepSeek 的 strict 文档，确认项目地址仍是 `https://api.deepseek.com`。

**为了什么**：`@Tool` / `@ToolParam` 生成的 Schema 是给模型看的说明书，它尽量按格式填参。`ABC` 和空字符串仍会进来。挡在查表之前的是 Java 校验，和 Controller 上 `@Validated` 拦在 Mapper 之前是同一层。格式错误和「查无此单」分开，模型才能说「单号格式不对」，而不是「没有这张单」。

**三个东西各管一段**：

| 你写的                                            | 放在哪                                       | 作用                                                                                                     |
| ------------------------------------------------- | -------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| 方法开头的格式校验                                | 两把工具方法的第一段                         | 非法参数返回 `invalid_argument`，不拿去对内存表                                                        |
| `invalid_argument` 与 `not_found` 分两种 JSON | 工具的返回值                                 | 格式不对走前者，单号合法但表里没有走后者（后者在 S2-T4）                                                 |
| strict 文档                                       | 只读，不改 `application` 里的 `base-url` | 知道厂商有一道更死的 Schema 检查，且它要打到 `/beta`。本阶段两个框架仍打同一个正式地址，本地校验继续做 |

**本阶段要打通的用法**（各打 2 次）：

| 用法           | 用户怎么说               | 工具会返回什么                         | 你要看到的结果                    |
| -------------- | ------------------------ | -------------------------------------- | --------------------------------- |
| 单号格式不对   | 问一个 `ABC` 这类单号  | `invalid_argument`                   | 回答里不把这笔说成已支付          |
| 问句里没有单号 | 没给出 `ORD-数字`      | `invalid_argument`，或模型没点到查询 | 同样不把夹具里的 `PAID` 安上去  |
| 地址保持一条   | 打开 `base-url` 看一眼 | 不保存、不改                           | 仍是 `https://api.deepseek.com` |

**同一套用法对应的业务类型**：

1. **模型填的参数按不可信入参处理**：和前端传来的 JSON 一样，进 Service 前先校验格式。
2. **两种失败两种话术**：单号写错、单号不存在，返回码分开，客服回答才分得开。
3. **厂商侧的严格模式先不启用**：它会换 API 地址。本阶段对照 Spring 和 LangChain4j 时继续打同一个 base-url，校验留在 Java 里。

**知识完成项**：

- [X] K12 工具入参必须本地校验
- [X] K13 DeepSeek strict ≠ 应用侧免检

**操作完成项**：

- [X] D5 `【写后端】` 全部完成判定（ABC / 无单号不当成 PAID；未改 base-url）

---

### S2-T7 上下文缓存与成本

**干什么**：看一次带工具的问答为什么比普通聊天更贵，以及第二次相同请求能不能少算前面那段输入。缓存数字从直连 DeepSeek 的 `usage.prompt_cache_hit_tokens` 读；`/chat` 只用来感受连打两次的耗时。

**为了什么**：工具循环里，模型至少被叫两次：先点菜，再拿查询结果写回答。每次都会把整份工具菜单再发出去。DeepSeek 的磁盘缓存默认开着，但只认从第一个 token 起完全相同的前缀。system 提示和 tools 菜单固定放最前，用户问题放最后，这段前缀才有机会在下一次命中。

**三个东西各管一段**：

| 你看的                      | 在哪看                                 | 作用                                                          |
| --------------------------- | -------------------------------------- | ------------------------------------------------------------- |
| `prompt_cache_hit_tokens` | 直连响应的 `usage`                   | 第二次及以后，有多少输入 token 命中了前缀缓存。没命中就记 0   |
| 前缀顺序                    | 请求体里 system、tools 在前，user 在后 | 菜单稳定放在开头，用户问题变化时，前面那段仍可能命中          |
| `/chat` 的响应            | 只有 `answer` 和 `toolsCalled`     | 厂商的 usage 被框架吃掉了，所以不在 8080 的 JSON 里找缓存字段 |

**本阶段要打通的用法**：

| 用法                   | 怎么打                              | 你要核对什么                         | 你要看到的结果                                  |
| ---------------------- | ----------------------------------- | ------------------------------------ | ----------------------------------------------- |
| 同一请求体连打两次     | curl 直连 DeepSeek，body 与 D0 相同 | 第二次的 `prompt_cache_hit_tokens` | 把数字写入 §9，是 0 也写 0                     |
| 同一句业务问题连打两次 | Q1 打两次 `/chat`                 | 只记耗时                             | 感受带工具的一次问答更慢；响应里不找 cache 字段 |

**同一套用法对应的业务类型**：

1. **固定说明放请求前部**：系统角色、工具菜单、输出格式很少变，放在用户问题前面，重复问答才可能命中缓存。
2. **工具一多，成本先涨在输入上**：每把工具的 Schema 每次请求都会再发一遍，再叠加「点菜一次、总结一次」。
3. **对调用方的接口和成本账单分开看**：业务响应只回答案和审计；token 与缓存命中从厂商 usage 另取。

**知识完成项**：

- [X] K17 上下文缓存：只匹配前缀
- [X] K18 成本：schema 放最前；看 cache hit

**操作完成项**：

- [X] D8 全部完成判定（连续两次相同前缀；§9 缓存行有 hit 数字，含 0）

---

**产出**：

- `docs/stage-2.md`：D0–D10 完成判定 + 各天 K* 勾选项 + §9 对照表 + Day 10 结论
- `demos/backend/.../stage02/` `【写后端】`：Spring AI `@Tool` 查订单/库存 + LangChain4j 对照接口（主线里程碑）
- `demos/02-tool-use/` `【写后端·加分】`：README 已有实验规范；Python 对照为加分项

**里程碑**：`ChatClient.tools(orderQueryTools)` 跑通 Q1～Q3；同一问 `AiServices` 也能查到 ORD-1001。前端接通为加分项。

**参考（博客优先，课和书不进验收）**：见 [resources.md#stage-2](resources.md#stage-2)

- [DeepSeek Tool Calls](https://api-docs.deepseek.com/zh-cn/guides/tool_calls)（勿用已失效的 `.../function_calling`）
- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [LangChain4j Tools](https://docs.langchain4j.dev/tutorials/tools/)
- [DeepSeek Context Caching](https://api-docs.deepseek.com/zh-cn/guides/kv_cache)

---

## 阶段 3：Agent 核心概念与框架（10 天）

> 每天读哪篇、写哪些类、在哪个页面看什么结果，写在 [stage-3.md](stage-3.md)。本节只写这一阶段在学什么、需要了解什么、必须掌握什么。

Java 写在统一后端 `demos/backend` 的 `stage03` 包。订单和库存沿用阶段 2 的内存夹具：**ORD-1001 已支付**，**SKU-88 在 WH-A 可用数量是 10**。事实仍从这两张表来。本阶段多出来的是：循环的每一轮能在页面上看见，下一句话能接着上一句，下一步可以由代码写死，以及循环在什么条件下停住。

**目标**：能解释一次 Agent 运行里谁在决策、谁在执行、状态存在哪、为什么会停。先用 JDK `HttpClient` + Jackson 自己写循环，再在 Spring AI、LangChain4j 里认出同一条循环，最后看固定流程图和停止条件。

**留到后面阶段的事**：文档切块和向量检索（阶段 4）、MCP 与多个 Agent 互相派活（阶段 5）、轨迹落 MySQL。本阶段轨迹放在响应 JSON 的 `steps` 里，记忆仓库默认在进程内存。

### 这一阶段在学什么

阶段 2 已经能查订单、查库存。`ChatClient.call()` 和 `AiServices` 在方法内部把「点菜 → 执行 → 再问」跑完，页面只拿到最后一句和 `toolsCalled`。阶段 3 要你看见并握住这个循环：每一轮的决定、工具结果、还要不要再来一轮，都是你的程序状态。

用已经会的后端来对照：

| 你已经会的 | 在 Agent 循环里 | 作用 |
| --- | --- | --- |
| `while` 拉任务直到完成或超时 | Agent Loop | 模型还在点菜就再跑一轮；写出最终句子或触到上限就停 |
| 一次 RPC 的请求 / 响应日志 | `steps` | 每一轮留下谁发起、选了哪个工具、工具返回了什么 |
| 按 sessionId 存放的会话 | `conversationId` + ChatMemory | 模型本身不记得上一句，下一次请求要把历史再带上 |
| 接口上的只读 / 需审批 | 工具菜单 + 确认停点 | 模型可以要求调用，写操作仍由你的代码决定是否真的执行 |
| 状态机的边 | Graph 的节点和边 | 下一步走哪由 Java 写死，模型只负责节点内部的那一次生成 |

一次「能不能发货」在循环里是这样走的。这三轮就是后面要掌握的 ReAct：

| 轮次 | 模型看到什么 | 模型返回什么 | Java 做什么 |
| --- | --- | --- | --- |
| 1 | 问句 + 工具菜单 | `tool_calls`：`getOrder(ORD-1001)` | 查表，得到已支付 |
| 2 | 上一轮的点菜 + 已支付 | `tool_calls`：`getInventory(SKU-88, WH-A)` | 查表，得到 10 |
| 3 | 两段工具结果 | 给人看的句子，不再点菜 | 把 `steps` 和这句话返回页面 |

做完 [stage-3.md](stage-3.md) 的 10 天，应能对着页面讲清四件事：

- 哪一轮是模型点菜、哪一轮是 Java 查表、哪一轮是给人看的句子
- 同一个 `conversationId` 让第二句「刚才那张单」不用再报单号；窗口变小之后，最早的内容为什么消失
- 三种规划各自把「下一步」交给了谁
- 循环如何停住：步数上限、菜单里没有写方法、退款要等人点确认

### 需要了解的

这些内容建立地图即可。10 天里会在阅读或页面字段中碰到它们，不要求本阶段单独做出可运行版本。

**工作流和 Agent 是两种控制权。** 工作流的下一跳写在代码里，同一类单据每次走过的节点稳定。Agent 的下一跳由模型在循环里决定，步数随问题变化。Anthropic 的工程文章把这件事说成：能用一次模型调用解决的，就不要先做循环；路径必须稳定时用工作流；步数事先写不死时再用 Agent。阶段 3 两种都会碰到：手写循环和规划页是 Agent，固定图是工作流。

**规划、记忆、工具是三块积木。** 工具是阶段 2 已经接上的查表方法。记忆是按会话把消息再送回模型。规划是「下一步谁来定」。Lilian Weng 的综述用这三块描述自主 Agent。本阶段把三块拆开看：第 2～4 天主要是工具循环，第 5～7 天加上记忆，第 8～9 天比较规划和图。

**框架里的循环和手写循环是同一件事。** Spring AI 的 `ToolCallingAdvisor`、LangChain4j 的 `AiServices` 默认把 `while` 藏在一次 `call()` 里。关掉自动执行之后，`ToolCallingManager` 只负责「按点菜单执行 Java」，轮次仍由你的循环掌握。手写那天用 JDK `HttpClient`，是为了先看见 JSON，而不是为了替换 Spring。

**记忆有策略，也有仓库。** 窗口策略决定留最近多少条。仓库决定这些条存在堆里、Redis 还是 JDBC。本阶段默认仓库在内存，用来看懂「进程一停，会话就没了」。Redis 实现知道换的是仓库即可。向量记忆把旧对话做成可检索片段，那是阶段 4 的事，和窗口不是同一层。

**图运行时和 Python 框架共享一组词。** Spring AI Alibaba 的 `StateGraph`、节点、边、全局状态，对应 LangGraph 里的同名概念。LangGraph、CrewAI 这一阶段只建立这个对应，不交 Python 作业。多个 Agent 互相派活（Supervisor）留在阶段 5；固定图里的条件边已经足够说明「分支可以由 Java 读状态来决定」。

**循环必须有停法。** 最大步数、菜单里不存在的方法、写操作前的人工确认，分别对应超时、未暴露的接口、审批流。模型可以一直点菜，停不停由你的 `while` 和工具注册决定。

### 要掌握的知识点

下面每条都要能讲出「它管哪一段」。页面上的字段名和类名以 [stage-3.md](stage-3.md) 为准。

#### 循环与协议

**Agent。** 模型在一个循环里自己决定要不要调工具、调哪一个、看到结果后还要不要再调。路径不事先写死。适合「能不能发货」这种步数随问句变化的问题。

**Workflow（固定工作流）。** 节点和边写在 Java 里。模型只完成节点内部的生成，例如把已经查到的数量写成一句话。适合每单都必须查库存的审核。代价是即使用户只问订单状态，边仍会把库存查出来。

**Agent Loop。** 一段 `while`：响应里还有工具调用，就执行并把结果塞回消息列表，再请求模型；没有工具调用，或到达上限，就退出。阶段 2 的自动顾问是这段循环的隐藏实现。

**`finish_reason=tool_calls`。** 这一轮模型停在「等你执行」。这一轮的正文还不是给用户的答案。手写循环用它决定要不要进入下一轮。

**`role=tool`。** 你把查表结果交回模型的那条消息。没有它，下一轮模型仍然不知道订单是已支付。

**ReAct。** 边想边做。每一轮先决定一个动作，看到观察结果再决定下一动作。上面的三轮表就是 ReAct。页面上的证据是 `steps` 按时间变长，而不是一开始就有一份完整计划。

**Plan-and-Execute。** 先单独要一份步骤清单，再按清单执行。清单生成时还没有查表结果，所以它不会因为中途发现库存为 0 而自己改写，除非再加一轮规划。页面上的证据是执行前就出现的 `plan`。

**Reflection。** 先出草稿，再用另一次模型调用按写死的标准打回重写，例如不超过 40 字且必须带单号。多一次调用换的是句子，不是再查一次库。页面上的证据是 `draft` 和更短的 `answer`。

**`internalToolExecutionEnabled` / 关掉自动顾问。** Spring AI 默认在 `call()` 里自己跑循环。自己写 `while` 的接口要把自动顾问关掉，否则一次请求里有两层循环。

**`ToolCallingManager`。** Spring 提供的「按模型点的菜单执行 Java 方法」。手动循环用它执行工具，避免自己解析参数再反射。它不负责决定还要不要再问模型，那是你的 `while`。

#### 轨迹

**`steps`。** 返回给页面的轨迹，一轮一条：轮次、结束原因、工具名、参数、观察结果、耗时。它代替终端日志，让调用方看见查表发生过。

**`observation`。** 工具方法的返回。`PAID` 表示查到了；`not_found` 表示单号合法但表里没有，所以有一次 `getOrder`；`invalid_argument` 表示格式在进表之前被挡住，不应出现夹具里的已支付。

**`stopReason`。** 循环为什么停。`final` 表示模型不再点菜。`max_steps` 表示到了上限，后面的工具还没跑。`waiting_human` 表示停在确认，写方法还没有执行。

#### 记忆

**ChatMemory。** 按 `conversationId` 存消息，下一次请求再取出来拼进提示。模型权重里没有这场会话。第二句「刚才那张单」能对上 ORD-1001，是因为第一句还在这个 ID 下面。

**`MessageChatMemoryAdvisor`。** 挂在 `ChatClient` 上的顾问，负责取出并写回记忆。每次调用必须带 `ChatMemory.CONVERSATION_ID`，缺了会直接抛异常，没有默认会话。

**窗口记忆。** 只保留最近 N 条。更早的用户句不会进入下一次请求，所以窗口大小决定模型还能不能提起「第一个记号」，不只是省 token。

**工具消息默认不进记忆。** Spring AI 默认只把最终的用户句和最终回答写入记忆，循环中的工具消息不写。下一句若问「你调了哪个函数」，模型常常不知道，因为最终回答里不一定写出函数名。

**记忆放进循环。** 把记忆顾问的顺序放到工具顾问内侧，并调用 `disableInternalConversationHistory()`，关掉工具顾问自己维护的那份内部历史。这样每一轮的工具结果才会进同一个 `conversationId`。两套历史同时写，会重复或互相覆盖。

**`MessageWindowChatMemory`。** LangChain4j 的窗口实现，按条数裁。系统消息保留。若带工具调用的模型消息被裁掉，紧跟的工具结果一并裁掉，避免厂商拒绝「只有结果、没有对应调用」的消息。窗口设成 2 时，发货那种多轮工具调用会把库存数字裁出下一次提示。

**`ChatMemoryRepository`。** 消息真正落地的地方。默认 `InMemoryChatMemoryRepository` 在堆里，进程一停 key 就没了。换成 Redis 或 JDBC 之后，同一个 `conversationId` 在重启后还能取出。策略类不用改。

#### 图与停止条件

**StateGraph / Node / Edge / OverAllState。** 图用来声明工作流。节点是一步（查订单、查库存、写句子）。边是下一步。`OverAllState` 是节点之间传递的 Map，库存数量和分支名都放在这里。

**条件边。** 判断函数是 Java，读状态里的字段。已支付且可用数量大于 0 走向可发，否则走向不可发。模型不修改这条边，所以业务规则不会随措辞漂移。

**最大步数。** `while` 的上限。发货至少要「查订单、查库存、写句子」三轮，上限设为 2 时请求会带着 `max_steps` 返回，回答可以是不完整的。这和给批处理加超时是同一类约束。

**人在环路上。** 写操作执行前把状态置为 `waiting_human`，页面点确认才真正调用。模型点了退款，Java 在审批前不下厨。和写接口在落库前返回「待审批」是同一件事。

**只读菜单。** 没有注册的方法就不会执行。用户说「把库存改成 999」时，夹具里的 10 保持不变。菜单说明要写互斥，否则模型会拿查订单的工具去答库存。

### 和前后阶段怎么接

| 阶段 | 已经握住的 | 阶段 3 补上的 |
| --- | --- | --- |
| 阶段 1 | 提示词决定怎么说、输出长什么样 | 事实仍不写进提示词 |
| 阶段 2 | 模型点菜，Java 查表，框架把循环藏在一次调用里 | 把轮次、会话、规划、停点放到页面 JSON 里 |
| 阶段 4 | — | 窗口记忆继续留用；按语义检索旧对话和文档再做 |
| 阶段 5 | — | 固定图的边继续留用；多个 Agent 派活、MCP 再做 |

操作按 [stage-3.md](stage-3.md) 的 10 天执行。10 天都勾完，阶段 3 完成。

---

## 阶段 4：RAG 与记忆（3-4 周）

> **进入本阶段第一件事**：按「三层约定」新建 `docs/stage-4.md` + `notes/rag.md`，把下面每条 S4-T 拆成每天 `D*-O*` 和知识 `K*`。未完成改造，不要开始写 demo。
> 当前完成判据：本段 S4-T1～T8 全部打勾 + 文档问答 demo 可运行。

**目标**：掌握让 Agent「知道文档内容」的能力（RAG）与记忆设计。**Java 侧 LangChain4j 的 RAG 能力最强，本阶段以它为主线**。类比：RAG = 给 LLM 加一个可查询的企业知识库缓存层。

- [ ] **S4-T1** Embedding 向量化：什么是 embedding、余弦相似度、Chunk 切分策略
  - 知识 / 操作：待拆进 `notes/rag.md` + `docs/stage-4.md`
- [ ] **S4-T2** 向量库选型：Redis（`RedisEmbeddingStore`）/ pgvector / Milvus / Chroma（Python 练手）
  - 知识 / 操作：待拆进 stage-4 + rag 笔记（按已有基础设施选一个主库）
- [ ] **S4-T3** RAG 完整链路（LangChain4j）：Loader → Splitter → Embedding → Store → TopK → RetrievalAugmentor → ContentRetriever/Aggregator
  - 知识 / 操作：待拆进 stage-4 + rag 笔记（本阶段主线里程碑）
- [ ] **S4-T4** Spring AI 版 RAG：`VectorStore` + `SimpleVectorStore` / pgvector / Redis
  - 知识 / 操作：待拆进 stage-4 + rag 笔记
- [ ] **S4-T5** 进阶检索：BM25+向量混合、多路召回+Rerank、Agentic RAG
  - 知识 / 操作：待拆进 stage-4 + rag 笔记
- [ ] **S4-T6** RAG 评估：Recall / Precision / MRR（对标搜索系统经验）
  - 知识 / 操作：待拆进 stage-4 + rag 笔记
- [ ] **S4-T7** 记忆设计：短期 `MessageWindowChatMemory`（≈ LTRIM）；长期向量库；摘要压缩
  - 知识 / 操作：待拆进 stage-4 + rag 笔记
- [ ] **S4-T8** Java 体系类比：向量库 ≈ ES；评估 ≈ 搜索指标；Chunk ≈ 分表策略。铁律：查询与入库必须同一 embedding 模型
  - 知识 / 操作：待拆进 stage-4 + rag 笔记

**产出**：`demos/04-rag-java/` —— 一个「基于你自己公司文档/技术文档的 Java 问答 agent」（LangChain4j 实现，Redis/pgvector 做向量存储）

**参考**：

- LangChain4j 官方文档：RAG 章节（核心，最详细的 Java RAG 教程）
- Spring AI 官方文档：VectorStore / Embedding
- Anthropic：RAG 最佳实践 / Contextual Retrieval 文章（必读）
- ⚠️ 铁律：查询与入库必须使用同一个 embedding 模型

---

## 阶段 5：MCP / 多 Agent / 工程化（2-3 周）

> **进入本阶段第一件事**：按「三层约定」新建 `docs/stage-5.md` + `notes/mcp-engineering.md`，把下面每条 S5-T 拆成每天 `D*-O*` 和知识 `K*`。未完成改造，不要开始写 demo。
> 当前完成判据：本段 S5-T1～T5 全部打勾 + MCP Server demo 可运行。

**目标**：从「能跑的 demo」升级到「能交付的工程」——这部分是你的差异化主战场。

- [ ] **S5-T1** MCP：是什么；Spring AI `@McpTool`；MCP Java SDK；对接现成 Server；v2.0 无状态化 / OAuth；类比 Dubbo 接口 + SPI
  - 知识 / 操作：待拆进 `notes/mcp-engineering.md` + `docs/stage-5.md`
- [ ] **S5-T2** 多 Agent（Java）：Spring AI Alibaba Supervisor；ReAct；A2A；状态共享；冲突与死锁
  - 知识 / 操作：待拆进 stage-5 + mcp-engineering 笔记
- [ ] **S5-T3** 工程化四件套：Eval（测试集 + LLM-as-judge）；可观测（Langfuse + Actuator）；成本/延迟；安全（注入防护、权限最小化、脱敏）
  - 知识 / 操作：待拆进 stage-5 + mcp-engineering 笔记
- [ ] **S5-T4** 部署形态：嵌入现有 Spring Boot `@Service`；独立微服务 + Docker；对接存量 Dubbo
  - 知识 / 操作：待拆进 stage-5 + mcp-engineering 笔记
- [ ] **S5-T5** Python 对照：LangGraph 多 Agent 编排模式（只读概念，不写代码）
  - 知识 / 操作：待拆进 stage-5 + mcp-engineering 笔记

**产出**：`demos/05-multi-agent-java/` —— Spring AI MCP Server + Spring AI Alibaba 多 Agent 协作 demo + 评估脚本

**参考**：

- MCP 官方文档（modelcontextprotocol.io）
- Spring AI 官方文档：MCP 章节
- MCP Java SDK：[github.com/modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk)
- Spring AI Alibaba 文档：Supervisor 多 Agent
- Langfuse 文档（集成 Spring Boot）
- [Spring AI + MCP：绕过 Python 税构建生产 Agent](https://www.javacodegeeks.com/2026/03/spring-ai-1-1-and-themodel-context-protocolbuilding-production-ai-agentswithout-the-python-tax.html)

---

## 阶段 6：综合落地项目（4-6 周，可持续）

> **进入本阶段第一件事**：按「三层约定」新建 `docs/stage-6.md` + `notes/project.md`，把下面每条 S6-T 拆成每周 `D*` 和知识 `K*`。未完成改造，不要开新工程。
> 当前完成判据：本段 S6-T1～T5 全部打勾 + `demos/06-project-java/` 可演示。

**目标**：交付一个「像你工作里真实交付过的系统」一样的完整 Agent 应用，作为简历作品。

- [ ] **S6-T1** 选题立项：从下面 5 个里定 1 个，写一页技术方案（范围 / 非目标 / 里程碑）
  - 知识 / 操作：待拆进 `notes/project.md` + `docs/stage-6.md`
- [ ] **S6-T2** 主链路跑通：对应选题的核心路径可演示（问答 / NL2SQL / 工单 / Review / MCP 操作内部系统）
  - 知识 / 操作：待拆进 stage-6 + project 笔记
- [ ] **S6-T3** 工程化补齐：权限、审计、评估报告（准确率/成本）、失败可回放
  - 知识 / 操作：待拆进 stage-6 + project 笔记
- [ ] **S6-T4** 交付物：技术方案 + Maven 多模块代码 + 评估报告 + Docker 说明
  - 知识 / 操作：待拆进 stage-6 + project 笔记
- [ ] **S6-T5** 简历表述：按本文件文末「简历表述建议」改一版，能用项目里的数据说话

**项目选题建议**（结合 B 端业务经验，任选其一）：

1. **企业知识库问答 Agent**：对接真实业务文档（合同、制度、运维手册），Spring Boot + LangChain4j RAG + Redis/pgvector + Spring Security 权限控制 + 审计日志
2. **智能报表分析 Agent**：NL2SQL 查 MySQL（用 MyBatis Plus 查，你熟）→ 生成图表 → LLM 解读，Spring AI Tool Calling + ECharts
3. **工单/客服智能助手**：意图识别 → 工具调用（查订单/状态）→ 工单处理，Spring AI Alibaba 多 Agent 分工
4. **代码审查 / 需求分析 Agent**：接入 Git 仓库，按团队规范审查，MCP GitHub Server + LangChain4j
5. **内部系统「AI 操作员」**：通过 MCP 操作内部系统（对接 Dubbo 服务——这是你的独特差异化）

**交付标准**：技术方案文档 + 完整代码（Maven 多模块项目）+ 评估报告（准确率/成本数据）+ Docker 部署说明。这对应你熟悉的「从需求到交付」完整流程。

**产出**：`demos/06-project-java/` + 项目 README（像你交付过的任何一个 B 端项目一样完整）

---

## 时间线与里程碑

| 阶段               | 时长（业余学习估算） | 里程碑验证点                                  |
| ------------------ | -------------------- | --------------------------------------------- |
| 0 环境准备         | 1 周                 | Spring AI + DeepSeek 跑通第一个 LLM 调用      |
| 1 提示词工程       | 1-2 周               | BeanOutputConverter 稳定输出结构化 JSON       |
| 2 Function Calling | 2-3 周               | @Tool 注解式工具调用跑通完整循环              |
| 3 Agent 框架       | 10 天                | 控制台「Agent 核心」跑通：手写 steps、会话窗口、三种规划、固定图、停止条件 |
| 4 RAG              | 3-4 周               | LangChain4j 文档问答准确率达标                |
| 5 工程化           | 2-3 周               | Spring AI MCP Server + 自动化评估             |
| 6 落地项目         | 4-6 周               | 完整 Spring Boot 项目交付                     |

> 全职学习约 **4-6 个月**，业余约 **8-12 个月**。每个阶段产出必须"可运行、可演示"，不追求快。

---

## 附录：12 周快速行动方案（Java 主线版）

> 每周有明确的 Java 代码产出，从 Spring Boot 工程师平滑过渡到 Agent 工程师：

| 周次 | 主题                   | 关键动作                                                                                                  | 技术栈                              |
| ---- | ---------------------- | --------------------------------------------------------------------------------------------------------- | ----------------------------------- |
| 1    | Spring AI 初体验       | 创建 Spring Boot 4.x 项目，Spring AI ChatClient 调 DeepSeek，跑通问答/流式/JSON 输出                      | Spring AI + DeepSeek                |
| 1b   | 提示词工程（阶段 1）   | 按 S1-T1～T7 打勾：D0–D7 操作 + K1–K20 知识；里程碑`.entity(TicketTriage)`（[stage-1.md](stage-1.md)） | Spring AI`.param` / `.entity`   |
| 2    | Tool Calling（阶段 2） | 按 S2-T1～T7 打勾：D0–D10 操作 + K1–K20 知识；里程碑 `@Tool` 查订单/库存（[stage-2.md](stage-2.md)）   | Spring AI @Tool / LangChain4j @Tool |
| 3    | Agent 循环（阶段 3）   | 按 [stage-3.md](stage-3.md) 的 10 天：手写循环、会话窗口、三种规划、固定图和停止条件                     | 手写 loop + Spring AI ChatMemory    |
| 4    | Agent 运行时状态设计   | 设计 run/step/tool_call/trace 表结构（MyBatis Plus + MySQL）——后端基本功直接套用                        | MyBatis Plus + MySQL                |
| 5-6  | RAG                    | LangChain4j 文档问答 Demo；加元数据过滤、引用溯源、ACL 权限过滤                                           | LangChain4j + Redis/pgvector        |
| 7    | 记忆                   | ChatMemory 短期/长期记忆设计，Redis 持久化                                                                | LangChain4j ChatMemory + Redis      |
| 8    | 权限与审批流           | Spring Security + 敏感工具确认流 + 审计日志                                                               | Spring Security + AOP               |
| 9    | Eval 与 Tracing        | JUnit 测试集 + Langfuse 集成 Spring Boot，失败可回放                                                      | JUnit 5 + Langfuse                  |
| 10   | 部署                   | Spring Boot Docker 部署、配置管理、成本监控                                                               | Docker + Spring Boot Actuator       |
| 11   | 项目包装               | 输出简历/面试可用项目（建议"技术文档问答 + 工具调用"系统）                                                | 全栈 Java                           |
| 12   | 对比扩展               | 横向看 LangGraph(Python)、CrewAI 的取舍，理解 Java 栈的定位和边界                                         | Python 对照阅读                     |

> 12 周是压缩轨。阶段 1 记为 **第 1b 周**：先动手改 prompt，课和书不进验收。阶段 2 记为 **第 2 周**：按 [stage-2.md](stage-2.md) D0 先直连看 `tool_calls`，再写 `@Tool`。阶段 3 的正本是 [stage-3.md](stage-3.md) 的 10 天。第 4 周的 MySQL 轨迹表、第 7 周的向量长期记忆不在阶段 3 的验收里。

**简历表述建议**：写"基于 Spring AI + LangChain4j 设计并实现了含文档解析、向量检索（Redis/pgvector）、Tool Calling、Spring Security 权限过滤、Langfuse 链路追踪与 JUnit 自动化评估的企业级 Agent 问答系统"，而非"用 LLM 做了个问答"。**你不需要说自己"转行 Python"，你说的是"我把 10 年 Java 工程经验带到了 AI Agent 领域"。**

---

## 学习原则（给你的四条建议）

1. **每阶段必须有可运行的 Java 产出**，学完在 `demos/` 里留代码——你的工程习惯（可交付、可验证）在这里是最大优势。Python 代码只做对照理解，不要求产出
2. **先手写循环，再用框架**：框架会隐藏核心机制。在 Java 里用纯 HTTP Client + Jackson 手写一次 Agent Loop（50 行），所有框架文档都会变简单
3. **尽早结合业务场景**：从阶段 2 开始，Tool 就封装你真实的业务接口（订单查询、用户查询）；从阶段 4 开始 RAG 用你真实的业务文档。这是你与纯 AI 背景学习者的差异化
4. **建立评估习惯**：Prompt 和 Agent 改一次测一次，用 JUnit 测试集说话，别靠感觉——这正是 B 端工程师的测试思维

---

## Java vs Python 学习策略（简明版）

|                    | Java（主线）                                | Python（辅助）                                                          |
| ------------------ | ------------------------------------------- | ----------------------------------------------------------------------- |
| **角色**     | 写代码、做产出、企业落地                    | 读教程、理解概念、快速实验                                              |
| **何时用**   | 阶段 1 起全部 demo 产出                     | 阶段 0 验证 API、阶段 1 快速改 prompt 看现象、读 LangGraph 教程时跑示例 |
| **框架**     | Spring AI / LangChain4j / Spring AI Alibaba | LangGraph / CrewAI（只读文档+跑 Quickstart，不深入）                    |
| **投入比例** | 80%                                         | 20%                                                                     |
| **产出要求** | 每阶段可运行的 Java 项目                    | 无硬性要求，按需                                                        |

**一句话总结**：你是 Java 工程师，Agent 不是要你变成 Python 工程师。Agent 是在你已有的 Spring Boot/MySQL/Redis 技术栈上长出来的一个新能力层。Java 生态（Spring AI 2.0 + LangChain4j + Spring AI Alibaba）已经足够完整地支撑你从入门到企业落地。

---

## 文档改造记录

后续每个阶段做完「三层对齐」都在这里追加一行，方便回看改了什么、以哪次为样板。

| 日期       | 阶段            | 改了什么                                                                                                                                   | 样板文件                                                                        |
| ---------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------- |
| 2026-08-18 | 约定            | 新增「阶段文档三层约定」：任务`S{N}-T*` / 操作 `D*-O*` / 知识 `K*`；完成规则：任务打勾 = 对应操作项 + 知识项全部打勾                 | 本节                                                                            |
| 2026-08-18 | 阶段 1          | 原 7 条笼统任务拆成**S1-T1～T7**，映射到 D/K                                                                                         | [stage-1.md](stage-1.md)、[prompt-engineering.md](../notes/prompt-engineering.md) |
| 2026-08-18 | 阶段 1 教学口径 | 去掉问答题/「用自己的话」：笔记每节改为必知+怎么做+指定资料；手册每天给出粘贴段、判定表、完整 Java 示例和必读链接；结论改为 Day 7 填空模板 | 同上                                                                            |
| 2026-08-19 | 阶段 1 动手标注 | 三份文档统一「动手类型」：`【只读】` / `【调用后端】` / `【写后端】` / `【写后端·加分】`；标明阶段 1 唯一硬性写代码日 = Day 5     | [stage-1.md](stage-1.md)、[prompt-engineering.md](../notes/prompt-engineering.md) |
| 2026-08-18 | 阶段 2–6       | 任务预先编成 S2-T*～S6-T*；详案进入该阶段时按阶段 1**「告诉他做什么」** 样板展开，不要写成自测题                                           | 阶段 1 三份文档                                                                 |
| 2026-09-10 | 阶段 2          | 新建 stage-2.md（S2-T1～T7 + D/K）；产出统一后端 stage02                                                                                   | 阶段 1                                                                          |
| 2026-09-10 | 阶段 2 整合     | 将 tool-use.md 并入 stage-2.md：写代码日历、示例效果、前后端说明、§9 单文件学习                                                           | stage-2.md                                                                      |
| 2026-11-05 | 阶段 2          | 任务清单前补「这项技术是干什么的」：一次请求里谁点菜、谁执行、结果如何回到回答，并挂概念向官方说明                                         | 本节                                                                            |
| 2026-11-12 | 阶段 3          | 知识点留在本文件并写细；10 天的博客、要写的代码和页面结果改到 stage-3.md，博客放进对应的那一天                                   | [stage-3.md](stage-3.md)                                                        |

阶段 4 起每次改造时追加一行。
