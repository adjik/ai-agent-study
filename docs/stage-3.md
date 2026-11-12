# 阶段 3：Agent 核心（10 天操作）

> 这一阶段要学什么、要了解什么、要掌握哪些知识点，写在 [learning-plan.md 阶段 3](learning-plan.md)。本文只写每天读哪篇、写哪些代码、在控制台哪一页看结果。
> 代码放在 `demos/backend` 的 `com.example.demos.stage03`，端口 8080。订单和库存继续用阶段 2 的 `OrderStore` / `InventoryStore`：**ORD-1001 已支付**，**SKU-88 在 WH-A 可用数量是 10**。
> 验收在浏览器（开发时一般是 `http://localhost:5173`）。「阶段 03 / Agent 核心」接通之前菜单是「待开发」：当天接口写完，把 `demos/frontend/demos.config.ts` 里 `agent-core` 的 `status` 改成 `ready`，再按下面点。

十天都勾完，阶段 3 才算做完。

- [ ] 第 1 天 看见被收起来的循环
- [ ] 第 2 天 手写循环，一问一轮、一问多轮
- [ ] 第 3 天 轨迹里能分清查无此单和格式不对
- [ ] 第 4 天 Spring AI 的循环交还给你
- [ ] 第 5 天 会话能接上，窗口会忘掉
- [ ] 第 6 天 记忆跟着进程走，巡检不必打字
- [ ] 第 7 天 LangChain4j 走同一句
- [ ] 第 8 天 三种规划各看一次字段
- [ ] 第 9 天 下一步写在边上
- [ ] 第 10 天 三种停法，再用三句话收口

启动后端（第 2 天起，占着别关）：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd demos/backend && mvn spring-boot:run
```

看到 Tomcat 在 **8080** 之后再开前端、再点页面。

---

### 第 1 天 · 看见被收起来的循环

**学什么**：Agent 循环就是把工具结果交回模型，直到模型不再点菜。阶段 2 的页面已经在跑这个循环，中间轮次没有返回给浏览器。

**为了什么**：第 2 天的 `while` 是把缺的轮次补到页面上，查单业务不用另做一套。

**今天读**

- [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents)（中文：[Tony Bai 译文](https://tonybai.com/2025/03/11/building-effective-agents/)）。读「什么是智能体」和「Agent」两节。读完应能说出：路径写在代码里的是工作流；路径由模型每轮决定的是 Agent。一次调用能完成的，先用一次调用。
- [LLM Powered Autonomous Agents](https://lilianweng.github.io/posts/2023-06-23-agent/)。读完应能说出：规划、记忆、工具会在后面几天的页面上分开出现。

**要写的代码**：今天不写 `stage03`。

**在页面上做**

1. 打开 [工具对话](http://localhost:5173/demos/tool-use/chat)，点预设「Q3 连查」，发送。问句是「ORD-1001 能不能发货？先查订单再查对应库存。」
   - **你会看到**：回答能对上已支付和库存 10。`toolsCalled` 有 `getOrder`、`getInventory` 两行。没有「第 1 轮 / 第 2 轮」的模型原文。
   - **为什么**：`ToolCallingAdvisor` 在 `call()` 里面把循环跑完了。Controller 只把最终句子和工具审计交给前端。两行记录证明查了两次，轮次留在服务端。

---

### 第 2 天 · 手写循环，一问一轮、一问多轮

**学什么**：`finish_reason=tool_calls` 表示模型在等你执行。结果要以工具消息发回去，下一轮才能写成「已支付」。一个问题可以连续点两把不同的工具，第二把的参数可以来自第一把的观察。

**为了什么**：这是本阶段的里程碑。后面的框架循环都是这个 `while`。

**今天读**

- [DeepSeek Tool Calls](https://api-docs.deepseek.com/zh-cn/guides/tool_calls)。读完应能在 JSON 里指出 `tools`、`tool_calls`、工具角色的消息。手写循环要自己把查表结果发回去。
- [ReAct](https://www.promptingguide.ai/techniques/react)。读完应能说出思考、动作、观察是交替的。观察来自 Java 查表，不是模型编的。

**要写的代码**

在 `com.example.demos.stage03.loop`：

| 类 | 职责 |
| --- | --- |
| `HandWrittenAgentLoop` | JDK `HttpClient` + Jackson 调 DeepSeek。这个类里不调用 `ChatClient` |
| `LoopController` | `POST /api/stage/03/loop`，入参 `question`、`maxSteps`（默认 4） |
| `StepRecord` | 一轮一条：`round`、`finishReason`、`toolName`、`arguments`、`observation` |

循环里：

- 菜单挂上阶段 2 已有的查订单、查库存，说明保持互斥（查订单的说明里写明不查库存，反过来也一样）
- 响应里有 `tool_calls` 时，用参数查 `OrderStore` / `InventoryStore`，把 JSON 作为工具消息追加，再请求一次
- 不再点菜，或步数到达 `maxSteps`，就退出
- 响应含 `answer`、`stopReason`、`steps`

接通前端「手写 Agent Loop」（`/demos/agent-core/loop`）。

**在页面上做**

1. 打开 [手写 Agent Loop](http://localhost:5173/demos/agent-core/loop)，问句填 `ORD-1001 现在什么状态？`，最大步数 4，发送。
   - **你会看到**：`answer` 里是已支付。`stopReason` 为 `final`。`steps` 长度为 2。第 1 步 `finishReason=tool_calls`，`toolName=getOrder`，参数含 `ORD-1001`，`observation` 里有 `PAID`。第 2 步不再点菜。
   - **为什么**：第一轮只有问句和菜单，表在你的进程里，模型只能点菜。Java 写入 `PAID` 之后，第二轮才有事实，于是写成句子并停止。

2. 问句改成 `ORD-1001 能不能发货？先查订单再查对应库存。`，再发送。
   - **你会看到**：`steps` 至少 3 条。前两条的 `toolName` 依次是 `getOrder`、`getInventory`。库存观察里可用数量是 10。最后一条不再点菜，`stopReason=final`。
   - **为什么**：第一轮的观察里还没有库存。模型看到已支付之后才去点 `getInventory`。第三轮两段观察都在消息里，它才写结论。若只有一条 `getOrder` 就停了，先检查菜单说明有没有写明发货要看库存。

---

### 第 3 天 · 轨迹里能分清查无此单和格式不对

**学什么**：`steps` 的每个字段对应协议或业务表上的一段。查无此单是查表之后的业务结果；单号格式不对要挡在查表之前。

**为了什么**：后面记忆、规划和停止条件都靠这些字段判断刚刚发生了什么。回答好听不能代替轨迹。

**今天读**

- 再打开 [DeepSeek Tool Calls](https://api-docs.deepseek.com/zh-cn/guides/tool_calls)，只看工具结果如何放回消息列表。读完应能说出：`not_found` 和 `invalid_argument` 都是你的 Java 放进观察里的 JSON，不是厂商的错误码。

**要写的代码**

改第 2 天的 `HandWrittenAgentLoop` / `StepRecord`，不要新开一条循环：

- 每一步补上 `latencyMs`
- `getOrder` 查不到时，`observation` 为 `not_found`，堆栈只留在服务端日志
- 单号不是 `ORD-数字` 时，返回 `invalid_argument`，不拿去对 `OrderStore`
- 页面响应里带上当前夹具摘要，方便和第 10 天的「改库存」对照

**在页面上做**

打开 [手写 Agent Loop](http://localhost:5173/demos/agent-core/loop)，分三次发送：

1. `ORD-1001 现在什么状态？`
   - **你会看到**：观察含 `PAID`。
2. `ORD-9999 什么状态？`
   - **你会看到**：有一条 `getOrder`，观察是 `not_found`。回答不把这笔说成已支付。
   - **为什么**：单号格式合法，表里没有，所以查表发生了，结果是业务未命中。
3. `查订单 ABC 的状态`
   - **你会看到**：要么没有工具步骤，要么观察是 `invalid_argument`。回答里不出现夹具中的 `PAID`。
   - **为什么**：`ABC` 在进表之前就被挡住了。格式错误和「没有这张单」分开，后面的话术才分得开。

---

### 第 4 天 · Spring AI 的循环交还给你

**学什么**：Spring AI 默认用 `ToolCallingAdvisor` 在 `call()` 里跑循环。关掉自动顾问，再用 `ToolCallingManager` 自己 `while`，`steps` 应和手写 HTTP 循环同一形状。

**为了什么**：自己的 `while` 若和顾问的自动执行叠在一起，一次点击里会查两遍表，页面上也对不上轮次。

**今天读**

- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)，搜 User-Controlled Tool Execution。读完应能说出：`internalToolExecutionEnabled(false)` 或 `AdvisorParams.toolCallingAdvisorAutoRegister(false)` 之后，执行工具的是你的循环。

**要写的代码**

在 `com.example.demos.stage03.springai`：

| 类 | 职责 |
| --- | --- |
| `SpringAgentLoop` | 关闭自动工具顾问；`while` 里用 `ToolCallingManager.executeToolCalls` 执行阶段 2 的查询工具；自己组装 `steps` |
| `SpringAgentController` | `POST /api/stage/03/spring-ai`，入参先只要 `question`、`maxSteps`。会话字段第 5 天再加 |

这个类用 `ChatModel` 发请求，不再用第 2 天的 `HttpClient`。接通「Spring AI Agent」（`/demos/agent-core/spring-ai`）。

**在页面上做**

1. 打开 [Spring AI Agent](http://localhost:5173/demos/agent-core/spring-ai)，发送发货问句。再打开 [手写 Agent Loop](http://localhost:5173/demos/agent-core/loop)，发送同一句。
   - **你会看到**：两边的回答都能对上已支付和数量 10。两边的 `steps` 里都有 `getOrder` 和 `getInventory`。`stopReason` 都是 `final`。
   - **为什么**：查表的还是那两个 Java 方法，夹具没变。变的是谁发 HTTP。轮次都在 `steps` 里，是因为 Spring 这条接口把顾问的自动循环关掉了。

---

### 第 5 天 · 会话能接上，窗口会忘掉

**学什么**：模型不保存会话。`ChatMemory` 按 `conversationId` 把上一轮取出，拼进下一次请求。`MessageChatMemoryAdvisor` 每次调用都要带 `ChatMemory.CONVERSATION_ID`。窗口只留最近若干条。默认还不把循环中的工具消息写入记忆；要记住工具来回，把记忆顾问放到工具循环内侧，并关掉工具顾问的内部历史。

**为了什么**：第二句经常是「刚才那张单」。没有记忆就没有单号。窗口太小，最早的记号会从提示里消失。工具名字若没写进记忆，下一句就说不出调用了哪个函数。

**今天读**

- [Spring AI Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)。读完应能说出：默认是窗口记忆加内存仓库；缺会话 ID 会抛 `IllegalArgumentException`；默认记忆不含循环中的工具消息。
- [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)，搜 Chat Memory。读完应能说出：`disableInternalConversationHistory()` 配上更靠内的记忆顾问顺序，工具来回才会进同一个 `conversationId`。

**要写的代码**

在 `SpringAgentLoop` 上补记忆，不新做一套查单：

- `MessageWindowChatMemory`，条数由请求里的 `maxMessages` 决定
- 每个请求带 `conversationId`。页面连续发送时保持同一个 ID，点「新会话」才换
- 请求字段 `rememberToolTrace`：关闭时只存最终问答；打开时按升级说明把记忆放进循环
- 响应增加 `conversationId`、`memoryMessages`

**在页面上做**

1. 打开 [Spring AI Agent](http://localhost:5173/demos/agent-core/spring-ai)。先发送 `ORD-1001 现在什么状态？`。不要点新会话，再发送 `刚才那张单对应的 SKU 在 WH-A 还有多少库存？`
   - **你会看到**：第一句的 `steps` 是 `getOrder`，回答是已支付。第二句的 `steps` 是 `getInventory`，参数能对上 SKU-88 和 WH-A，数量是 10。两句的 `conversationId` 相同。`memoryMessages` 里看得到第一句。
   - **为什么**：第二句原文没有单号。记忆把第一句带进了第二次请求。

2. 点「新会话」，只发送第二句。
   - **你会看到**：回答说不清是哪张单。不会把夹具里的 `PAID` 安到「刚才」上。
   - **为什么**：新的 `conversationId` 在仓库里是空的。

3. 点新会话，窗口条数设为 2，「记住工具来回」关闭。依次发送 `记住这个记号：ALPHA`、`记住这个记号：BETA`、`我的第一个记号是什么？`
   - **你会看到**：回答说不出 ALPHA。`memoryMessages` 里没有第一句。
   - **为什么**：每一轮至少写入用户句和模型回答，窗口只有 2 条时，最早那一轮被丢掉。第三次请求的提示里已经没有 ALPHA。

4. 点新会话，打开「记住工具来回」，窗口调到 20。发送发货问句，再发送 `你刚才为了回答，依次调用了哪两个工具？`
   - **你会看到**：开关关闭时，第二句经常说不出两个函数名，`memoryMessages` 里只有最终问答。开关打开后，列表里出现工具结果，第二句能按顺序说出 `getOrder` 和 `getInventory`。
   - **为什么**：默认顾问只存最终问答，工具来回留在那一次调用内部。打开开关，是把每一轮的工具消息也放进同一个会话。

---

### 第 6 天 · 记忆跟着进程走，巡检不必打字

**学什么**：`ChatMemory` 决定留哪些消息，`ChatMemoryRepository` 决定存在哪。默认仓库在内存里，跟进程走。循环是一个方法，聊天框只是其中一个调用方。

**为了什么**：会话放在 JVM 里，重启就没了。后面换成 Redis 时换的是仓库，`conversationId` 的用法不变。本阶段验收停在看见这份丢失；本机没有 Redis 也不挡今天完成。

**今天读**

- 仍是 [Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)，读仓库那一节（`ChatMemoryRepository` / `InMemoryChatMemoryRepository`）。读完应能说出：记忆策略和落地仓库是两层。进程退出后，内存仓库里的 key 不在了。

**要写的代码**

- 响应里增加 `memoryRepository`，值为当前仓库的类名
- 把循环收成 `@Service` 方法，例如 `run(question, conversationId, maxSteps)`
- `SpringAgentController` 增加 `POST /api/stage/03/spring-ai/patrol`：不读输入框，固定发送发货问句，响应里 `trigger=schedule`。页面按钮「跑一次巡检」打这个地址，用来模拟 `@Scheduled` 调用同一个 Service

**在页面上做**

1. 打开 [Spring AI Agent](http://localhost:5173/demos/agent-core/spring-ai)，记下 `conversationId` 和 `memoryRepository`。发送 `ORD-1001 现在什么状态？`，确认是已支付。停掉后端再启动。把会话 ID 填回刚才的值，发送 `刚才那张单是什么状态？`
   - **你会看到**：`memoryRepository` 仍是内存实现的类名。回答不知道「刚才」是哪张单。`memoryMessages` 为空。
   - **为什么**：仓库在堆里。浏览器还留着会话 ID，进程里这个 key 已经没了。换成 Redis 或 JDBC 仓库后，同一 key 在重启后仍能取出第一句。

2. 点「跑一次巡检」。输入框可以空着。
   - **你会看到**：`steps` 里仍有查订单和查库存，`trigger=schedule`。
   - **为什么**：循环是 Service 方法。按钮代表定时器，没有人打字也会跑。

---

### 第 7 天 · LangChain4j 走同一句

**学什么**：`AiServices` 内部也在跑工具循环。`MessageWindowChatMemory` 按条数留最近的消息；系统消息保留；带工具调用的模型消息被裁掉时，后面的工具结果一并裁掉。

**为了什么**：换框架时业务表不变。要在页面上确认「已支付」仍来自 `OrderStore`，变的是记忆类和轨迹字段。

**今天读**

- [LangChain4j AI Services](https://docs.langchain4j.dev/tutorials/ai-services)。读完应能说出：你声明的接口方法背后仍是模型调用加工具循环。
- [MessageWindowChatMemory](https://docs.langchain4j.dev/apidocs/dev/langchain4j/memory/chat/MessageWindowChatMemory.html)。读完应能说出：按条数裁旧消息；系统消息留下；工具调用和它的结果一起裁，避免下一次请求带上无处安放的工具结果。

**要写的代码**

在 `com.example.demos.stage03.lc4j`：

| 类 | 职责 |
| --- | --- |
| `Stage03Lc4jTools` | 使用 `dev.langchain4j.agent.tool.Tool`，与 Spring 的 `@Tool` 分属两个类。方法内部仍查阶段 2 的 Store |
| `Stage03Assistant` | 一个接口，由 `AiServices` 生成实现 |
| `Lc4jAgentController` | `POST /api/stage/03/langchain4j`。入参 `question`、`conversationId`、`maxMessages`。记忆用 `MessageWindowChatMemory`。响应含 `answer`、`conversationId`、`memoryMessages` |

接通「LangChain4j Agent」（`/demos/agent-core/langchain4j`）。

**在页面上做**

1. 打开该页，`maxMessages` 设为 20。发送 `ORD-1001 现在什么状态？`，同一会话再发送 `刚才那张单是付过款了吗？`
   - **你会看到**：两句都对得上已支付。第二句的 `conversationId` 与第一句相同。`memoryMessages` 里有第一句。
   - **为什么**：第二句没有单号。窗口里还留着 `ORD-1001`。

2. 点新会话，`maxMessages` 设为 2。先发送发货长问句，再发送 `刚才查到的可用库存是多少？`
   - **你会看到**：第二句经常说不出 10。`memoryMessages` 很短。
   - **为什么**：发货这一问会写入用户句、工具调用、工具结果和最终回答，条数很容易超过 2。裁掉带工具调用的模型消息时，紧跟的工具结果一并被裁掉，库存 10 就不会出现在下一次提示里。

---

### 第 8 天 · 三种规划各看一次字段

**学什么**：边想边做时，下一步在看到上一次观察之后才决定，响应里没有事先写好的计划。先计划再执行时，步骤清单出现在查表之前。做完再评时，多一次模型调用只改句子，不再查表。

**为了什么**：三种做法查的是同一份夹具。页面上多出来的 `plan`、`draft`、`review` 用来区分「下一步是谁定的」。

**今天读**

- 回到 [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents) 里的工作流模式。读完应能对上：提示链接近「先计划再执行」；评估器—优化器接近「做完再评」；由模型在循环里自己选工具的那一节，就是边想边做。
- 第 2 天读过的 [ReAct](https://www.promptingguide.ai/techniques/react) 今天不用重读全文，对照页面上按时间变长的 `steps` 即可。

**要写的代码**

在 `com.example.demos.stage03.plan`：

| 类 | 职责 |
| --- | --- |
| `PlanningController` | `POST /api/stage/03/plan`，入参 `question`、`strategy`（`react` / `plan` / `reflect`） |
| `ReactPlanning` | 复用第 4 天的手动循环。响应 `strategy=react`，不带执行前的 `plan` |
| `PlanThenExecute` | 第一次调用只要求 JSON 步骤，此时不注册工具。然后 Java 按清单查表。最后一次调用把观察交给模型写回答。响应先有 `plan`，再有带 `observation` 的 `steps` |
| `ReflectPlanning` | 先走边想边做得到 `draft`，再发一次检查。标准写在提示里：不超过 40 个字，必须包含 `ORD-1001`，必须说明能不能发。响应含 `draft`、`review`、改写后的 `answer` |

接通「规划对照」（`/demos/agent-core/plan`）。

**在页面上做**

发货问句发送三次，每次换一个策略。

1. 策略「边想边做」。
   - **你会看到**：`strategy=react`。没有事先填好的 `plan`。`steps` 先出现 `getOrder`，其后才出现 `getInventory`。
   - **为什么**：每一轮只决定当前要不要点菜。库存工具出现在第二轮，是因为第一轮的观察里已经有订单。
2. 策略「先计划再执行」。
   - **你会看到**：先有 `plan`，两项工具名是 `getOrder`、`getInventory`，此时还没有观察。随后 `steps` 补上已支付和数量 10。`answer` 在观察填完之后出现。
   - **为什么**：规划那一次请求里还没有 `PAID` 和 `10`。清单是按问句和菜单写下的。
3. 策略「做完再评」。
   - **你会看到**：`draft` 通常比 `answer` 长。`review` 里能看到过长或通过。最终 `answer` 含 `ORD-1001`，并且短于草稿。查表仍是发货所需的那两次。
   - **为什么**：第二次调用检查的是句子约束。库存已经在草稿的上下文里，改句子不必再查库。

---

### 第 9 天 · 下一步写在边上

**学什么**：`StateGraph` 的节点是一步，边是下一步，`OverAllState` 是贯穿节点的 Map。条件边是一段 Java：读状态里的数量，决定走向可发还是不可发。模型不改这条边。

**为了什么**：发货审核希望每单都走过同一组检查。规则放在代码里，数量为 0 时不会被写成「可以发」。

**今天读**

- [什么是 Spring AI Alibaba Graph](https://java2ai.com/docs/1.0.0.2/tutorials/graph/whats-spring-ai-alibaba-graph/)。读完应能说出节点、边、全局状态。ReAct 在这套图上只是一种预置走法。
- [LangGraph 概述](https://docs.langchain.com/oss/python/langgraph/overview)。只读 State、Node、Edge。今天不运行 Python。读完应能说出它和上一篇是同一组概念。

**要写的代码**

在 `com.example.demos.stage03.graph`：

| 类 | 职责 |
| --- | --- |
| `ShipGraph` | 固定边：查订单 → 查库存 → 按条件走向「可发货文案」或「不可发货文案」。两个文案节点只套句子，不再查表。条件：已支付且可用数量大于 0 |
| `GraphController` | `POST /api/stage/03/graph`。入参 `question`，以及可选的 `availableOverride`（只放进这一次的状态，不写回夹具） |

响应含 `nodes`、`branch`（`canShip` / `cannotShip`）、`answer`，以及夹具里的真实可用数量。接通「固定工作流」（`/demos/agent-core/graph`）。

**在页面上做**

1. 打开 [固定工作流](http://localhost:5173/demos/agent-core/graph)，发送 `ORD-1001 现在什么状态？`。这句并没有要求查库存。
   - **你会看到**：`nodes` 依次含查订单、查库存、可发货文案。`branch=canShip`。回答里会提到库存。夹具数量是 10。
   - **为什么**：下一跳是 `addEdge` 写上的，不是 `tool_calls` 选的。多查一次库存是这条边的代价。

2. 打开 [规划对照](http://localhost:5173/demos/agent-core/plan)，策略「边想边做」，发送同一句只问状态的话。
   - **你会看到**：`steps` 里通常只有 `getOrder`。
   - **为什么**：菜单允许模型只点它认为需要的工具。问句里没有发货和库存，它就停在订单。

3. 回到固定工作流，把「本次判断用的可用数量」改成 0，再发送发货问句。
   - **你会看到**：`branch=cannotShip`，走不可发货文案。页面上夹具里的 SKU-88 仍是 10。
   - **为什么**：条件边读的是这次放进状态的数字。夹具没有被写方法改掉。

---

### 第 10 天 · 三种停法，再用三句话收口

**学什么**：循环要有明确的结束。今天看三种：步数上限、菜单里没有对应方法、写操作停在确认。同一问句在不同页面上，变的是循环被谁握住、历史有没有带上、下一步由谁决定。

**为了什么**：没有上限时模型可以反复点同一个工具。没有确认时，一句「退款」就会进写方法。三句话用来把前九天的字段收成能指着页面讲出来的结论。

**今天读**

- 再读 [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents) 里「最大迭代次数」和工具接口要写清楚的两节。读完应能说出：上限和菜单说明是循环的一部分。

**要写的代码**

在 `com.example.demos.stage03.guard`：

| 类 | 职责 |
| --- | --- |
| `GuardedAgent` | 复用手动循环。`maxSteps` 由请求传入，到达上限时 `stopReason=max_steps`，并停止下一次模型请求 |
| 菜单 | 查询工具照常。不注册「改库存」。另注册退款工具，执行前若未经确认则 `stopReason=waiting_human`，该步 `executed=false`，退款标记保持未退 |
| `GuardController` | `POST /api/stage/03/guard` 开始一次运行。`POST /api/stage/03/guard/confirm` 用同一个 `runId` 接上暂停的运行，这次才调用退款方法 |

接通「停止与边界」（`/demos/agent-core/guard`）。响应里带夹具：可用数量、是否已退款。

**在页面上做**

1. 打开 [停止与边界](http://localhost:5173/demos/agent-core/guard)，最大步数设为 2，发送发货问句。
   - **你会看到**：`stopReason=max_steps`。`steps` 停在 2 条，通常还没有最终句子。
   - **为什么**：发货至少要三轮。上限写在 `while` 里，模型还想点菜，Java 不再发下一次请求。

2. 最大步数调回 4，发送 `把 SKU-88 的库存改成 999`。
   - **你会看到**：可用数量仍是 10。`steps` 里没有一个把库存写成 999 的已执行工具。
   - **为什么**：菜单里没有改库存的方法，就没有代码去改那份内存。

3. 发送 `给 ORD-1001 退款`。出现确认条时先不要点。
   - **你会看到**：`stopReason=waiting_human`。退款标记仍是未退。`steps` 里退款那条 `executed=false`。
   - **为什么**：模型点了菜，写方法在确认前没有执行。

4. 点「确认执行」。
   - **你会看到**：同一个 `runId` 继续。`stopReason` 变为 `final`。退款标记变为已退。
   - **为什么**：确认是第二次请求，把暂停的运行接上。写方法这次才进入。

5. 用下面三句话把前面的页面再点一遍，然后把横线换成你在 JSON 里看到的字：

   - A：`ORD-1001 现在什么状态？`
   - B：同一会话紧接着 `刚才那张单对应的 SKU 在 WH-A 还有多少库存？`
   - C：`ORD-1001 能不能发货？先查订单再查对应库存。`

   | 页面 | 发送 |
   | --- | --- |
   | [手写 Agent Loop](http://localhost:5173/demos/agent-core/loop) | A 和 C。A 为两轮且第一轮是 `getOrder`；C 至少三轮，观察里有 10 |
   | [Spring AI Agent](http://localhost:5173/demos/agent-core/spring-ai) | 同一会话先 A 再 B。B 的库存参数能对上 SKU-88，两句会话 ID 相同 |
   | [LangChain4j Agent](http://localhost:5173/demos/agent-core/langchain4j) | `maxMessages=20`，再做一次 A 再 B。B 能答出 10 |
   | [规划对照](http://localhost:5173/demos/agent-core/plan) | C 分别用「边想边做」和「先计划再执行」。前者没有执行前的 `plan`；后者先有 `plan` |
   | [固定工作流](http://localhost:5173/demos/agent-core/graph) | A。只问状态时 `nodes` 仍包含查库存 |
   | [停止与边界](http://localhost:5173/demos/agent-core/guard) | 最大步数 2，发送 C。`stopReason=max_steps` |

   写在下面：

   1. 事实来自工具：我在 ______ 页看到 C 的观察里有 ______。
   2. 下一句靠会话：我在 ______ 页看到 B 的 `conversationId` 与 A 相同，库存是 ______。
   3. 停和分支是 Java 的：最大步数 2 时 `stopReason` 是 ______；只问状态时固定图仍经过 ______ 节点。

三句写完，并且上面 10 天都已勾上，阶段 3 完成。
