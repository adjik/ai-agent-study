# 阶段 2 Demo：Function Calling / Tool Use

> 目录：`demos/02-tool-use/`（Python 对照）| Java 代码：`demos/backend/stage02` | 前端菜单 id：`tool-use`
> 计划：[docs/stage-2.md](../../docs/stage-2.md)（整合文档）| 任务：[learning-plan.md S2-T1～T7](../../docs/learning-plan.md)

本目录放 **Python 对照** 与 D0 用的 **请求体 JSON**。步骤以 **[docs/stage-2.md D0](../../docs/stage-2.md)** 为准：**一条 curl + `d0-tool-calls.json`**，不依赖脚本。**Java 在 D1 起**写到 `demos/backend/stage02`。

---

## 目标

用同一组 B 端订单/库存夹具，证明「模型点菜、Java 下厨」：

1. 直连 API 只看到点菜，看不到 PAID
2. Spring AI `@Tool getOrder` + `.tools()` 跑通 Q1
3. 多工具：Q1 查单 / Q2 查库存 / Q3 两把都调
4. not_found / invalid_argument 不编造
5. LangChain4j AiServices 对照同一问

阶段 1 证明「换一种写法，结果可解析」。这里证明「事实从工具来，循环由框架自动跑」。

---

## 约定目录

```
demos/02-tool-use/
├── README.md              # 本文件
├── d0-tool-calls.json     # D0 curl 请求体（user + getOrder 菜单）
├── d0_deepseek_tool_calls.sh  # 可选：与手册同 curl 的本地快捷方式，**手册不要求**
└── （Java stage02 在 demos/backend，D1 起）
```

- **Java 是验收必须项**，写在 `demos/backend` 的 `stage02` 包：JDK 21、Spring AI 2.0、根目录 `.env` 的 `DEEPSEEK_API_KEY`。Day 6 另加 LangChain4j（不要 `langchain4j-spring-boot-starter`）。
- **Python 是对照**，激活 `venv/` 后跑。不挡 S2-T 打勾。
- 配置方式与统一后端共用，单服务端口 **8080**。

---

## 统一业务场景（不要改成天气）

夹具与 Q1/Q2/Q3 以 [stage-2.md](../../docs/stage-2.md) 为准。

建议接口（与手册、前端 planned 对齐）：

| 方法 | 路径 | 哪天 | 作用 |
| --- | --- | --- | --- |
| GET | `/api/stage/02/tools` | D1 | 列出当前工具名 |
| GET | `/api/stage/02/store` | D1 | dump 内存夹具 |
| POST | `/api/stage/02/chat` | D1 起 | Spring AI `@Tool` 对话 |
| POST | `/api/stage/02/chat-lc4j` | D6 | LangChain4j 对照 |

请求体：`{"question":"ORD-1001 现在什么状态？"}`。D4 起响应带 `toolsCalled`。

---

## Java 落地时要做的事（按手册天数执行，不要一天写完）

1. D1：`OrderStore` + `OrderQueryTools.getOrder` + `Stage02ToolController`
2. D2：Tool 方法打 `[tool] getOrder ...` 日志
3. D3：`getInventory`；禁止写工具
4. D4：`ToolCallLog` 进响应；ORD-9999 → not_found
5. D5：入参校验 `ORD-\\d+`
6. D6：`Lc4jOrderTools` + `OrderAssistant` + `/chat-lc4j`
7. 改前端 `demos.config.ts`：`tool-use` 的 `status` 改为 `ready`（加分，D10-O5）

推荐代码骨架：

```
com.example.demos.stage02
├── api/          # OrderRecord / InventoryRecord / ToolChatRequest
├── store/        # 内存夹具
├── tool/         # Spring AI @Tool 与 LangChain4j @Tool 分两个类
├── service/      # OrderAssistant + AiServices 装配
└── controller/   # /api/stage/02/*
```

ChatClient 调用形态（示意）：

```java
chatClient.prompt()
        .system("你是电商售后助手。只能通过工具查询订单和库存。")
        .user(question)
        .tools(orderQueryTools)
        .options(OpenAiChatOptions.builder().temperature(0.0))
        .call()
        .content();
```

---

## Python 对照脚本约定（加分）

`tool_call_raw.py` 用 OpenAI SDK 指向 DeepSeek（与阶段 0 `hello_llm.py` 相同），**只发 tools、不执行函数**，打印 `finish_reason` 与 `tool_calls`。

```
python tool_call_raw.py          # 等价 D0 curl
```

只打印：是否 `tool_calls`、arguments 原文、响应里有没有 PAID。不写框架。

---

## 对照实验怎么记

记在 [stage-2.md §9](../../docs/stage-2.md)。

---

## 本目录当前状态

| 项 | 状态 |
| --- | --- |
| 学习计划 / 资料 / 笔记 | 已就绪（docs + notes） |
| Python 对照脚本 | 待实现（加分） |
| Java `stage02` | 待按 stage-2.md D1 起实现 |
| 前端菜单接通 | 待 Java 接口就绪后改 `demos.config.ts` |
