# AI Agent 学习资料汇总（Java 主线版）

> 更新日期：2026-09-10（信息已核对为 2025-2026 最新）
> 配套路线见 [learning-plan.md](learning-plan.md)；阶段 1 见 [stage-1.md](stage-1.md)；阶段 2 见 [stage-2.md](stage-2.md)；阶段 3 的每天阅读写在 [stage-3.md](stage-3.md) 当天小节里

**怎么用这份清单**：每个阶段 **先动手、再看博客、卡住才查官方文档**。长课程和书放在文末当手册，**不进阶段验收**。阶段专题（文末）才是当天要打开的东西。

**目录**：[框架选型](#sec-stack) · [MCP](#sec-mcp) · [Demo 清单](#sec-demos) · [课程](#sec-courses) · [书籍](#sec-books) · [开源仓库](#sec-repos) · [落地路线图](#sec-roadmap) · **[阶段 1：提示词工程](#stage-1)** · **[阶段 2：Tool Use](#stage-2)** · 阶段 3 每天的博客在 [stage-3.md](stage-3.md)

---

<a id="sec-stack"></a>

## 一、框架选型指南（2026 年）

### 你的技术栈定位

**你是 Java 工程师，Agent 不是要你变成 Python 工程师。** Java 生态（Spring AI 2.0 + LangChain4j + Spring AI Alibaba）已经足够完整地支撑你从入门到企业落地。

### Java 主线框架（必须掌握）

| 框架 | 定位 | 版本 | 在你的学习中承担的角色 |
|---|---|---|---|
| **Spring AI** | Spring 生态 AI 中间件层，ChatClient / Tool Calling / MCP 原生集成 | **2.0 GA（2026.6）**，要求 Java 21+ / Spring Boot 4.x | **主线框架 #1**：从阶段 0 到阶段 5 全程使用，最贴近你的 Spring Boot 心智模型 |
| **LangChain4j** | Java 版 LangChain，RAG 能力最强，20+ 模型 / 30+ 向量库 | 1.18.0（2026.7），要求 JDK 17+ | **主线框架 #2**：阶段 2 Tool 对照；阶段 4 RAG 主力 |
| **Spring AI Alibaba** | Java 版 LangGraph：Graph 工作流 + 多 Agent（Supervisor/ReAct） | 1.0 GA（2026.5） | **主线框架 #3**：阶段 5 多 Agent 编排主力 |
| **MCP Java SDK** | MCP 协议的 Java 实现，构建 MCP Server/Client | 随 MCP 规范更新 | 阶段 5 构建自己的 MCP Server 用 |

### Python 对照框架（了解即可，不要求产出）

| 框架 | 定位 | 何时看 |
|---|---|---|
| **LangGraph** | 生产级有状态 Agent 编排（状态机 + checkpoint） | 阶段 3-5：理解 Agent 编排的核心概念（StateGraph、节点、边），概念和 Spring AI Alibaba 一一对应 |
| **CrewAI** | 角色化多 Agent，最快跑通多 Agent 体验 | 阶段 5：20 行代码感受多 Agent 协作的感觉 |
| **LlamaIndex** | RAG 专用框架 | 阶段 4：读文档理解 RAG 设计模式 |
| **Claude Agent SDK** | Anthropic 官方 Agent 运行时 | 阶段 5：了解，工程化参考 |

### 低代码 Agent 平台（验证想法用）

| 平台 | 说明 |
|---|---|
| **Dify** | 开源低代码 Agent 工作流平台，90k+ stars。适合在写代码前快速验证"这个 Agent 想法能不能 work" |
| **FastGPT** | 轻量 RAG+Agent 平台，OpenAI 兼容 API，25k+ stars |

> ⚠️ 不要从 AutoGen 开始（已进入维护模式，微软转向 Microsoft Agent Framework）。Java 路线优先级：Spring AI（入门+Tool Calling+MCP）→ LangChain4j（RAG）→ Spring AI Alibaba（多 Agent）。

**参考链接**：
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [Spring AI Alibaba GitHub](https://github.com/alibaba/spring-ai-alibaba)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [Spring AI 2.0 GA 解读](https://cloud.tencent.cn/developer/article/2697474)
- [Spring AI 2.0 与 Alibaba 对比](https://cloud.tencent.cn/developer/article/2708407)
- [LangChain4j 万字实战](https://cloud.tencent.com.cn/developer/article/2681782)

---

<a id="sec-mcp"></a>

## 二、MCP（Model Context Protocol）——必学

- **是什么**：2024.11 Anthropic 推出的开放标准，统一 LLM 与外部工具/数据源的接入，被称为"AI 界的 USB-C"。2025.12 已捐赠 Linux 基金会下属 AAIF，OpenAI/谷歌/微软/亚马逊共同参与
- **为什么是你（Java 工程师）的最佳切入点**：Spring AI 2.0 原生集成 MCP，`@McpTool` 注解式开发——在 Spring Bean 上加个注解就是 MCP Tool。这是将你已有的 Java 业务能力暴露给 AI 的最低成本方式
- **2026.7 大事件**：MCP v2.0 无状态化大改版——移除握手/会话 ID，可水平扩容、适配无服务器架构；新增 MRTR（工具调用中可向人提问）；OAuth 2.1/OIDC 企业身份授权。**协议战已从"工具对接"转向"身份验证、授权审计与追责"**——安全治理，又是你的主场
- **Java 生态支持**：
  - Spring AI 2.0：`spring-ai-starter-mcp-server` + `spring-ai-starter-mcp-client`，开箱即用
  - MCP Java SDK：`modelcontextprotocol/java-sdk`，脱离 Spring 也可以用
- **类比**：MCP = Dubbo 的服务接口定义 + SPI 机制。定义 Tool Schema（接口）→ 注册 Tool（服务注册）→ Agent 发现并调用（服务发现+调用）

**链接**：
- [MCP 官方文档](https://modelcontextprotocol.io)
- [MCP 无状态化规范解读（ithome）](https://www.ithome.com/0/983/102.htm)
- [Ars Technica 分析](https://arstechnica.com/ai/2026/07/with-a-stateless-makeover-new-mcp-spec-targets-enterprise-scale/)
- [Simon Willison 观点](https://simonwillison.net/2026/Jul/31/stateless-mcp/)
- [Spring AI + MCP：绕过 Python 税构建生产 Agent](https://www.javacodegeeks.com/2026/03/spring-ai-1-1-and-themodel-context-protocolbuilding-production-ai-agentswithout-the-python-tax.html)

---

<a id="sec-demos"></a>

## 三、练手 Demo 清单（Java 实现）

> 每个 Demo 都基于你的 Java 技术栈，从简单到复杂递进。

| 难度 | Demo | 推荐技术栈 | 关键学习点 |
|---|---|---|---|
| ★ | 客服问答 Bot | Spring AI ChatClient + DeepSeek | 第一个 agent loop：模型 ↔ 工具循环 |
| ★ | 文档问答 RAG Bot | LangChain4j + Redis/pgvector | RAG 七步：加载→分块→Embedding→向量库→查询→TopK→生成。**查询与入库必须同一 embedding 模型** |
| ★★ | 邮件自动分类 Agent | Spring AI BeanOutputConverter + Tool Calling | 结构化输出分类 + 生成回复。⚠️ 只起草不自动发送，保留人工审核 |
| ★★ | 日程安排 Agent | Spring AI Alibaba Supervisor 模式 | "监督者-子代理"多 Agent 架构 |
| ★★★ | 代码审查 Agent | Spring AI + GitHub API (MCP Server) | MCP Server 开发 + 结构化 Review 报告 |
| ★★★ | 多 Agent 研究管线 | Spring AI Alibaba Graph 工作流 | 规划 Agent + 搜索 Agent + 写作 Agent 流水线 |
| ★★★★ | 内部系统 AI 操作员 | Spring AI MCP + Dubbo 服务对接 | 通过 MCP 将已有 Dubbo 服务暴露给 AI |

---

<a id="sec-courses"></a>

## 四、课程（卡住再看，不进每天任务）

> 阶段内默认 **不看完整课**。只有某个概念在笔记 + 博客 + 自己跑 demo 之后仍不清楚，再点开对应模块。Python 课只听思路，不跟敲。

### Java，跟着仓库跑（比看视频有效）

- **[Microsoft Spring AI for Beginners](https://github.com/microsoft/Spring-AI-for-Beginners)** —— 全程 Java。阶段 1 只跑 [02-prompt-engineering](https://github.com/microsoft/Spring-AI-for-Beginners/blob/main/02-prompt-engineering/README.md) 里的示例，不要从头刷完全部模块。
- [Build Enterprise AI Agents with Java and Spring](https://learn.microsoft.com/en-us/training/modules/build-enterprise-ai-agents-with-java-spring/) —— 阶段 5–6 部署时再看。
- [Spring AI Alibaba 文档](https://java2ai.com/) —— 阶段 5 查阅。

### 概念课（全程可选）

- [吴恩达 Prompt Engineering for Developers](https://www.deeplearning.ai/courses/chatgpt-prompt-engineering-for-developers/) —— 约 2h。阶段 1 **不必看完**；想听人讲 Few-shot / CoT 时挑两节即可。
- [Agentic AI（吴恩达）](https://www.deeplearning.ai/courses/agentic-ai/) —— 阶段 3 再听设计模式，现在不要开。
- [Hugging Face AI Agents Course](https://huggingface.co/learn/agents-course) —— 阶段 3 可选。

### 框架文档（当 Javadoc 用，不要当教材从头读）

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/) / [中文 ChatClient](https://docs.springjava.cn/spring-ai/reference/api/chatclient.html)
- [LangChain4j](https://docs.langchain4j.dev/) —— 阶段 2 Tools；阶段 4 RAG
- [DeepSeek JSON Output](https://api-docs.deepseek.com/zh-cn/guides/json_mode) —— 一页纸操作说明，阶段 1 结构化输出必看
- [DeepSeek Tool Calls](https://api-docs.deepseek.com/zh-cn/guides/tool_calls) —— 阶段 2 必看（原 `function_calling` 路径已废弃）
- [DeepSeek Context Caching](https://api-docs.deepseek.com/zh-cn/guides/kv_cache) —— 阶段 2 D8

### 中文付费课（可选，完全不挡进度）

- [企业级 Agents 开发实战营（彭靖田）](https://u.geekbang.org/subject/agents)
- [Agentic AI 智能体开发行动营](https://www.aiexpress.news/63519.html)

---

<a id="sec-books"></a>

## 五、推荐书籍（当手册翻，阶段内不要求读完）

> 不要为了“打基础”先读完一本书再写代码。需要某章再查某章。

| 书名 | 什么时候翻 | 关联阶段 |
|---|---|---|
| **《Spring AI in Action》**（Craig Walls） | 写 Spring AI 代码卡住时当官方书查 | 0–5 |
| **《AI Engineering》**（Chip Huyen，中文版《AI工程》） | 阶段 5 做评估 / 成本时再翻前几章 | 5 |
| **《AI Agents and Applications》**（Manning） | 阶段 3 理解编排时翻概念，忽略 Python 细节 | 3–5 |
| **《LLMs in Production》**（Manning） | 部署、延迟、可观测 | 5–6 |

链接：[Spring AI in Action](https://www.manning.com/books/spring-ai-in-action) · [AI Engineering](https://www.oreilly.com/library/view/ai-engineering-building/9781098166304/) / [中文版](https://book.douban.com/subject/38236657/) · [AI Agents and Applications](https://www.manning.com/books/ai-agents-and-applications) · [LLMs in Production](https://www.manning.com/books/llms-in-production)

---

<a id="sec-repos"></a>

## 六、开源学习仓库

### Java 工程师专属（必 Star）

- [Spring AI](https://github.com/spring-projects/spring-ai) —— Spring 官方 AI 框架
- [LangChain4j](https://github.com/langchain4j/langchain4j) —— Java 版 LangChain
- [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba) —— Java 版 LangGraph
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) —— MCP 协议 Java 实现
- [Microsoft Spring AI for Beginners](https://github.com/microsoft/Spring-AI-for-Beginners) —— Java 专属 AI 入门教程

### 系统学习（概念理解用，Python 代码）

- [hardness1020/awesome-agent-architecture](https://github.com/hardness1020/awesome-agent-architecture) —— 从零学 Agent，22 章分 8 层，每节可运行代码 + diff 式教学。**概念完全通用，你可以在 Java 里复现每章的核心逻辑**
- [WenyuChiou/awesome-agentic-ai-zh](https://github.com/WenyuChiou/awesome-agentic-ai-zh) —— **中文**学习地图，240+ 资源、65 个 MCP/Skill 目录、23 个练习

### 资源清单与模板

- [Shubhamsaboo/awesome-llm-apps](https://github.com/Shubhamsaboo/awesome-llm-apps) —— 100+ 可直接运行的 Agent/RAG 模板（Python，看设计思路）
- [NipunaRanasinghe/awesome-ai-agents](https://github.com/NipunaRanasinghe/awesome-ai-agents) —— 100+ 框架/工具分类清单

---

<a id="sec-roadmap"></a>

## 七、给 Java 工程师的落地路线图（一张图说清楚）

```
你现在的位置（阶段 0 完成）
        │
        ▼
┌──────────────────────────────────────────────────────┐
│  阶段 1-2：基础设施（Spring AI ChatClient + Tool Calling） │
│  产出：Spring Boot 项目，能对话、能调工具                 │
│  关键注解：@Tool、BeanOutputConverter                   │
├──────────────────────────────────────────────────────┤
│  阶段 3：Agent 核心（手写循环 → 框架实现）                 │
│  产出：能自主决策+执行工具的 Agent                        │
│  关键概念：Agent Loop、ReAct、ChatMemory                │
├──────────────────────────────────────────────────────┤
│  阶段 4：RAG（LangChain4j 主力）                        │
│  产出：能查文档的 Agent                                  │
│  关键：EmbeddingStore、RetrievalAugmentor、向量库选型    │
├──────────────────────────────────────────────────────┤
│  阶段 5：工程化（MCP + 多 Agent + 评估）                  │
│  产出：可交付的 Agent 系统                               │
│  关键：Spring AI MCP、Spring AI Alibaba Supervisor      │
├──────────────────────────────────────────────────────┤
│  阶段 6：综合项目                                       │
│  产出：像你交付过的任何一个 B 端项目一样完整的 Agent 应用   │
└──────────────────────────────────────────────────────┘
```

**三个关键转换**：
1. **从"写接口"到"写 Agent"**：你原来写 Controller → Service → DAO，现在写 Agent → Tool → Memory。结构没变，只是多了一个 LLM 决策层
2. **从"定义 API"到"定义 Tool Schema"**：`@PostMapping` → `@Tool`，入参出参描述从 Swagger 注解变成 JSON Schema 描述
3. **从"Spring Boot 微服务"到"Agent 即服务"**：Agent 可以作为一个 Spring Boot 微服务独立部署，通过 MCP 或 HTTP 与其他服务交互——和你现在的架构一模一样

**一句话总结**：你不是"转行做 AI"，你是"给已有的 Java 工程能力加一个 AI 决策层"。Spring AI + LangChain4j + Spring AI Alibaba 就是你在这条路上的三把利器。

---

<a id="stage-1"></a>

## 阶段 1：提示词工程专题（实操 + 博客）

> 当天打开 [stage-1.md](stage-1.md) 按 `D*-O*` 做（步骤、可粘贴 prompt、判定标准、指定博客都在手册里）。知识点全文在 [prompt-engineering.md](../notes/prompt-engineering.md) 对应 `K*`，每节有必知 / 怎么做 / 资料。
> **本阶段不要求看完任何一门课、不要求读任何一本书。**

### 今天就动手（用阶段 0 已有服务，不新开工程）

1. 启动 `demos/00-env/hello-llm-java`（端口 8080）。
2. 打开前端「自由问答」，或：

```bash
curl -G 'http://localhost:8080/api/test/hello' --data-urlencode 'msg=这里粘贴笔记里的整段 prompt'
```

3. 按 [stage-1.md](stage-1.md) **D0** 读完 K1–K3，**Day 1** 起把「坏 prompt / 好 prompt」各跑一遍，把现象记进笔记末尾表格。

前端路径：`http://localhost:5173/demos/hello-llm/chat`

### 建议看的博客 / 短文（每篇 10–20 分钟，看完马上去改代码）

| 顺序 | 文章 | 看完立刻做什么 |
| --- | --- | --- |
| 1 | [Prompt 工程实战：System / Few-shot / CoT](https://jishuzhan.net/article/2065228421654999042) | 对照自己的 system 提示，检查缺了角色、格式、边界哪一项 |
| 2 | [Spring AI 2.0：ChatClient.entity 把结果变成 Java 对象](https://blog.csdn.net/weixin_44094991/article/details/162463806) | 在阶段 0 项目里把 Demo 3 的手工 JSON 改成 `.entity(Record.class)` 试一次 |
| 3 | [掘金 · Spring AI 2.0 ChatClient / PromptTemplate / entity](https://juejin.cn/post/7630170456540364810)（搜文中「PromptTemplate」「结构化输出」两节即可） | 用 `.param("ticket", ...)` 替换字符串拼接 |
| 4 | [一文吃透 ChatClient：结构化输出 + 角色消息](https://blog.csdn.net/qq_69304031/article/details/161895288) | 把角色放到 `.system()`，工单正文放到 `.user()` |
| 5 | [DeepSeek JSON Output（官方一页纸）](https://api-docs.deepseek.com/zh-cn/guides/json_mode) | prompt 里必须出现 `json` 字样 + 给样例；看 `finish_reason` 防截断 |

可选对照（Java 示例仓库，跑一段就停）：

- [Spring AI for Beginners · 02 Prompt Engineering](https://github.com/microsoft/Spring-AI-for-Beginners/blob/main/02-prompt-engineering/README.md)

### 卡住再查（当 Javadoc，不要通读）

- [Spring AI ChatClient 中文](https://docs.springjava.cn/spring-ai/reference/api/chatclient.html) → 搜 `entity(`、`param(`、`system(`
- [Structured Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- [LangChain4j AiServices](https://docs.langchain4j.dev/tutorials/ai-services) —— 阶段 1 末扫 5 分钟即可，阶段 2 D6 才写代码

### 本阶段不要看

- 吴恩达完整课、Chip Huyen 全书、Anthropic 9 章 notebook
- Tool Calling / RAG / 多 Agent 任何文档（阶段 2 再打开）
- 提示注入攻防专题（阶段 5；现在只要求：用户数据进变量，不要和系统指令拼成一段散文）

---

<a id="stage-2"></a>

## 阶段 2：Function Calling / Tool Use 专题（实操 + 博客）

> 当天只打开 [stage-2.md](stage-2.md)（必知、操作、§9、示例效果均在同一文件）。
> Java 写在 `demos/backend` 的 `stage02` 包，端口 8080。**本阶段不要求看完任何一门课、不要求读任何一本书。**

### 今天就动手

1. **D0**：不要先写 Java。用手册里的 curl **直连 DeepSeek**，确认响应是 `tool_calls` 且没有夹具里的 `PAID`。
2. **D1 起**：在 `stage02` 写 `@Tool getOrder`，打 `POST /api/stage/02/chat`。
3. 场景固定为订单 / 库存夹具（见笔记文首），不要改成天气 Demo。

```bash
curl -sS http://localhost:8080/api/stage/02/chat \
  -H 'Content-Type: application/json' \
  -d '{"question":"ORD-1001 现在什么状态？"}'
```

前端占位：`http://localhost:5173/demos/tool-use/chat`（未接通前用 curl）

### 建议看的博客 / 短文（每篇 10–20 分钟，看完马上去改代码）

| 顺序 | 文章 | 看完立刻做什么 |
| --- | --- | --- |
| 1 | [DeepSeek Tool Calls（官方）](https://api-docs.deepseek.com/zh-cn/guides/tool_calls) | D0：对照 `tools` / `tool_calls` / `finish_reason`；D5 读 strict；不改 base-url |
| 2 | [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html) | D1：`@Tool` + `ChatClient.tools()`；搜 `ToolCallingAdvisor` 理解自动循环 |
| 3 | [中文镜像 · 工具调用](https://springdoc.cn/spring-ai/api/tools.html) | 与上一篇同一章，卡住时对照中文 |
| 4 | [腾讯云 · Tool Calling 实战](https://cloud.tencent.com/developer/article/2649974) | 只看 `@Tool` + `.tools()` 骨架，场景仍用订单夹具 |
| 5 | [掘金 · Spring AI 工具调用循环](https://juejin.cn/post/7626969207809556520) | D3：只看「框架控制的工具执行」 |
| 6 | [LangChain4j Tools](https://docs.langchain4j.dev/tutorials/tools/) | D6：`@Tool` + `@P` + `AiServices.tools()` |
| 7 | [DeepSeek Context Caching](https://api-docs.deepseek.com/zh-cn/guides/kv_cache) | D8：看 `prompt_cache_hit_tokens`；前缀从 token 0 开始 |

可选对照：

- [Anthropic Tool use](https://docs.anthropic.com/en/docs/build-with-claude/tool-use/overview) —— 协议心智（点菜 / 下厨）与厂商无关，语言无关，通读概念即可
- [Microsoft Spring AI for Beginners](https://github.com/microsoft/Spring-AI-for-Beginners) —— 有 Tool 模块就跑一段停，不要刷完全库

### 卡住再查（当 Javadoc，不要通读）

- [Spring AI ChatClient 2.0](https://docs.spring.io/spring-ai/reference/2.0/api/chatclient.html) → 搜 `tools(`、`ToolCallingAdvisor`
- [ToolCallingAdvisor](https://docs.spring.io/spring-ai/reference/api/tools/tool-calling-advisor.html) —— 想关掉自动循环时再看（阶段 3）
- [LangChain4j AiServices](https://docs.langchain4j.dev/tutorials/ai-services)
- [DeepSeek usage / cache 字段说明](https://chat-deep.ai/docs/deepseek-token-usage-fields/) —— Spring AI 没露出 hit 字段时用直连 curl 看

### 本阶段不要看

- 手写 Agent Loop、ChatMemory、RAG、MCP 文档（阶段 3～5）
- 吴恩达 Agentic AI 课、Chip Huyen 全书
- 把库存改成真 MySQL / Dubbo（阶段 6）；本阶段内存 Store 即可

