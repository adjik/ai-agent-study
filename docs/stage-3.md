# 阶段 3：Agent 核心（10 天操作）

> 这一阶段要学什么、要了解什么、要掌握哪些知识点，写在 [learning-plan.md 阶段 3](learning-plan.md)。本文只写每天读哪篇、写不写代码、伪代码、当天怎么执行。
> **动手**只有两种：`【不写代码】` 只读博客、只点已有页面；`【写代码】` 按当天伪代码改 `stage03`，改完再按「执行计划」跑。
> **执行计划**里每一步都有两行：**怎么执行**（打开哪个页面、点什么）和 **执行语句**（这一下发给 8080 的请求）。点页面上的发送，发出去的就是这句，不用再另开终端打一遍。
> 代码放在 `demos/backend` 的 `com.example.demos.stage03`，端口 8080。订单和库存继续用阶段 2 的 `OrderStore` / `InventoryStore`：**ORD-1001 已支付**，**SKU-88 在 WH-A 可用数量是 10**。
> 验收在浏览器（开发时一般是 `http://localhost:5173`）。「阶段 03 / Agent 核心」接通之前菜单是「待开发」：当天接口写完，把 `demos/frontend/demos.config.ts` 里 `agent-core` 的 `status` 改成 `ready`，再按下面点。

十天都勾完，阶段 3 才算做完。

- [X] 第 1 天 `【不写代码】` 看见被收起来的循环
- [X] 第 2 天 `【写代码】` 手写循环，一问一轮、一问多轮
- [X] 第 3 天 `【写代码】` 轨迹里能分清查无此单和格式不对
- [X] 第 4 天 `【写代码】` Spring AI 的循环交还给你
- [X] 第 5 天 `【写代码】` 会话能接上，窗口会忘掉
- [X] 第 6 天 `【写代码】` 记忆跟着进程走，巡检不必打字
- [X] 第 7 天 `【写代码】` LangChain4j 走同一句
- [X] 第 8 天 `【写代码】` 三种规划各看一次字段
- [X] 第 9 天 `【写代码】` 下一步写在边上
- [X] 第 10 天 `【写代码】` 三种停法；收口三句话 `【不写代码】`

写代码的日子，改完 Java 后先停掉正在跑的后端，再执行：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd demos/backend && mvn spring-boot:run
```

前端没开时另开一个终端：

```bash
cd demos/frontend && npm run dev
```

看到后端日志里 **Tomcat started on port 8080**、浏览器能打开 `http://localhost:5173` 之后，再做当天的执行计划。

---

### 第 1 天 · 看见被收起来的循环

**动手**：`【不写代码】`

**学什么**：Agent 循环就是把工具结果交回模型，直到模型不再点菜。阶段 2 的页面已经在跑这个循环，中间轮次没有返回给浏览器。

**为了什么**：第 2 天的 `while` 是把缺的轮次补到页面上，查单业务不用另做一套。

**今天读**

- [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents)（中文：[Tony Bai 译文](https://tonybai.com/2025/03/11/building-effective-agents/)）。读「什么是智能体」和「Agent」两节。读完应能说出：路径写在代码里的是工作流；路径由模型每轮决定的是 Agent。一次调用能完成的，先用一次调用。
- [LLM Powered Autonomous Agents](https://lilianweng.github.io/posts/2023-06-23-agent/)。读完应能说出：规划、记忆、工具会在后面几天的页面上分开出现。

**要写的代码**：今天不写 `stage03`。下面这段是阶段 2 页面背后已经在跑的循环，今天只用来对照，不落地。

**伪代码**

```text
回答页面(question):
    response = 模型(question, 菜单=[getOrder, getInventory])
    toolsCalled = []
    while response 仍在点菜 且 未到框架内部上限:
        for call in response.tool_calls:
            observation = 执行 Java 方法(call)
            toolsCalled.add(call.name)
            response = 模型(历史 + 工具消息(observation))
    return { answer: response.正文, toolsCalled }
    # 中间每一轮的 response 不放进页面
```

**执行计划**

1. **怎么执行**：浏览器打开 [工具对话](http://localhost:5173/demos/tool-use/chat)，点预设「Q3 连查」，再点发送。
   **执行语句**：

   ```http
   POST /api/stage/02/chat
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。"}
   ```

   - **你会看到**：回答能对上已支付和库存 10。`toolsCalled` 有 `getOrder`、`getInventory` 两行。没有「第 1 轮 / 第 2 轮」的模型原文。
   - **为什么**：`ToolCallingAdvisor` 在 `call()` 里面把循环跑完了。Controller 只把最终句子和工具审计交给前端。两行记录证明查了两次，轮次留在服务端。

---

### 第 2 天 · 手写循环，一问一轮、一问多轮

**动手**：`【写代码】`

**学什么**：`finish_reason=tool_calls` 表示模型在等你执行。结果要以工具消息发回去，下一轮才能写成「已支付」。一个问题可以连续点两把不同的工具，第二把的参数可以来自第一把的观察。

**为了什么**：这是本阶段的里程碑。后面的框架循环都是这个 `while`。

**今天读**

- [DeepSeek Tool Calls](https://api-docs.deepseek.com/zh-cn/guides/tool_calls)。读完应能在 JSON 里指出 `tools`、`tool_calls`、工具角色的消息。手写循环要自己把查表结果发回去。
- [ReAct](https://www.promptingguide.ai/techniques/react)。读完应能说出思考、动作、观察是交替的。观察来自 Java 查表，不是模型编的。

**要写的代码**

在 `com.example.demos.stage03.loop`：

| 类                       | 职责                                                                                |
| ------------------------ | ----------------------------------------------------------------------------------- |
| `HandWrittenAgentLoop` | JDK `HttpClient` + Jackson 调 DeepSeek。这个类里不调用 `ChatClient`             |
| `LoopController`       | `POST /api/stage/03/loop`，入参 `question`、`maxSteps`（默认 4）              |
| `StepRecord`           | 一轮一条：`round`、`finishReason`、`toolName`、`arguments`、`observation` |

循环里：

- 菜单挂上阶段 2 已有的查订单、查库存，说明保持互斥（查订单的说明里写明不查库存，反过来也一样）
- 响应里有 `tool_calls` 时，用参数查 `OrderStore` / `InventoryStore`，把 JSON 作为工具消息追加，再请求一次
- 不再点菜，或步数到达 `maxSteps`，就退出
- 响应含 `answer`、`stopReason`、`steps`

接通前端「手写 Agent Loop」（`/demos/agent-core/loop`）。按下面的伪代码写 `HandWrittenAgentLoop`，类名和字段可以对上，方法体按这个控制流写。

**伪代码**

```text
run(question, maxSteps):
    messages = [ {role: user, content: question} ]
    steps = []
    menu = [ getOrder 的 schema, getInventory 的 schema ]
    for round in 1 .. maxSteps:
        response = httpPost(DeepSeek, messages, tools=menu)
        if response 没有 tool_calls:
            return { answer: response.正文, stopReason: "final", steps }
        for call in response.tool_calls:
            observation = dispatch(call.name, call.arguments)
            steps.add({ round, finishReason: "tool_calls",
                        toolName: call.name, arguments: call.arguments,
                        observation })
            messages.add(response 里的点菜消息)
            messages.add({ role: "tool", content: observation })
    return { answer: "已达步数上限", stopReason: "max_steps", steps }

dispatch(name, args):
    if name == "getOrder":      return orderStore 里该 orderId 的状态
    if name == "getInventory":  return inventoryStore 里该 sku+warehouse 的数量
```

**执行计划**

先按文首的命令重启后端。然后：

1. **怎么执行**：打开 [手写 Agent Loop](http://localhost:5173/demos/agent-core/loop)，问句框粘贴下面的 `question`，最大步数填 `4`，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/loop
   {"question":"ORD-1001 现在什么状态？","maxSteps":4}
   ```

   - **你会看到**：`answer` 里是已支付。`stopReason` 为 `final`。`steps` 长度为 2。第 1 步 `finishReason=tool_calls`，`toolName=getOrder`，参数含 `ORD-1001`，`observation` 里有 `PAID`。第 2 步不再点菜。
   - **为什么**：第一轮只有问句和菜单，表在你的进程里，模型只能点菜。Java 写入 `PAID` 之后，第二轮才有事实，于是写成句子并停止。
2. **怎么执行**：同一页只改问句，最大步数仍是 `4`，再点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/loop
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","maxSteps":4}
   ```

   - **你会看到**：`steps` 至少 3 条。前两条的 `toolName` 依次是 `getOrder`、`getInventory`。库存观察里可用数量是 10。最后一条不再点菜，`stopReason=final`。
   - **为什么**：第一轮的观察里还没有库存。模型看到已支付之后才去点 `getInventory`。第三轮两段观察都在消息里，它才写结论。若只有一条 `getOrder` 就停了，先检查菜单说明有没有写明发货要看库存。

---

### 第 3 天 · 轨迹里能分清查无此单和格式不对

**动手**：`【写代码】`

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

只改第 2 天的 `dispatch` 和 `steps.add`，循环骨架不动。

**伪代码**

```text
dispatch("getOrder", orderId):
    started = now()
    if orderId 不匹配 "ORD-" 加数字:
        observation = { error: "invalid_argument" }   # 不访问 OrderStore
    else:
        record = orderStore.find(orderId)
        observation = record 为空 ? { error: "not_found" } : record
    steps 这一条再写入 latencyMs = now() - started
    return observation

# getInventory 同样先校验 sku、warehouse 非空，再查表
# 堆栈只 log，不放进 observation
```

**执行计划**

改完校验后重启后端。三次都在 [手写 Agent Loop](http://localhost:5173/demos/agent-core/loop) 点发送，最大步数保持 `4`。

1. **怎么执行**：问句用第一句，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/loop
   {"question":"ORD-1001 现在什么状态？","maxSteps":4}
   ```

   - **你会看到**：观察含 `PAID`，并且这一步有 `latencyMs`。
2. **怎么执行**：问句换成第二句，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/loop
   {"question":"ORD-9999 什么状态？","maxSteps":4}
   ```

   - **你会看到**：有一条 `getOrder`，观察是 `not_found`。回答不把这笔说成已支付。
   - **为什么**：单号格式合法，表里没有，所以查表发生了，结果是业务未命中。
3. **怎么执行**：问句换成第三句，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/loop
   {"question":"查订单 ABC 的状态","maxSteps":4}
   ```

   - **你会看到**：要么没有工具步骤，要么观察是 `invalid_argument`。回答里不出现夹具中的 `PAID`。
   - **为什么**：`ABC` 在进表之前就被挡住了。格式错误和「没有这张单」分开，后面的话术才分得开。

---

### 第 4 天 · Spring AI 的循环交还给你

**动手**：`【写代码】`

**学什么**：Spring AI 默认用 `ToolCallingAdvisor` 在 `call()` 里跑循环。关掉自动顾问，再用 `ToolCallingManager` 自己 `while`，`steps` 应和手写 HTTP 循环同一形状。

**为了什么**：自己的 `while` 若和顾问的自动执行叠在一起，一次点击里会查两遍表，页面上也对不上轮次。

**今天读**

- [Spring AI Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)，搜 User-Controlled Tool Execution。读完应能说出：`internalToolExecutionEnabled(false)` 或 `AdvisorParams.toolCallingAdvisorAutoRegister(false)` 之后，执行工具的是你的循环。

**要写的代码**

在 `com.example.demos.stage03.springai`：

| 类                        | 职责                                                                                                               |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `SpringAgentLoop`       | 关闭自动工具顾问；`while` 里用 `ToolCallingManager.executeToolCalls` 执行阶段 2 的查询工具；自己组装 `steps` |
| `SpringAgentController` | `POST /api/stage/03/spring-ai`，入参先只要 `question`、`maxSteps`。会话字段第 5 天再加                       |

这个类用 `ChatModel` 发请求，不再用第 2 天的 `HttpClient`。接通「Spring AI Agent」（`/demos/agent-core/spring-ai`）。

**伪代码**

```text
run(question, maxSteps):
    options = 工具菜单(getOrder, getInventory)
    options.internalToolExecution = false          # 顾问不要自己跑 while
    messages = [ user(question) ]
    steps = []
    for round in 1 .. maxSteps:
        response = chatModel.call(messages, options)
        if response 没有 tool calls:
            return { answer: 正文, stopReason: "final", steps }
        executed = toolCallingManager.executeToolCalls(messages, response)
        messages = executed.conversationHistory     # 已含工具结果
        steps.add(从 executed 取出的工具名、参数、观察)
    return { stopReason: "max_steps", steps }
```

**执行计划**

重启后端。同一句发货问题发两次，一次走 Spring 页，一次走手写循环页。

1. **怎么执行**：打开 [Spring AI Agent](http://localhost:5173/demos/agent-core/spring-ai)，问句粘贴下面的 `question`，最大步数 `4`，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/spring-ai
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","maxSteps":4}
   ```
2. **怎么执行**：打开 [手写 Agent Loop](http://localhost:5173/demos/agent-core/loop)，问句不变，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/loop
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","maxSteps":4}
   ```

   - **你会看到**：两边的回答都能对上已支付和数量 10。两边的 `steps` 里都有 `getOrder` 和 `getInventory`。`stopReason` 都是 `final`。
   - **为什么**：查表的还是那两个 Java 方法，夹具没变。变的是谁发 HTTP。轮次都在 `steps` 里，是因为 Spring 这条接口把顾问的自动循环关掉了。

---

### 第 5 天 · 会话能接上，窗口会忘掉

**动手**：`【写代码】`

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

**伪代码**

```text
memory = MessageWindowChatMemory(maxMessages)     # 同一个 conversationId 共用一份

run(question, conversationId, maxMessages, rememberToolTrace):
    history = memory.get(conversationId)          # 窗口外的旧消息这里已经不在
    messages = history + [ user(question) ]
    # 下面的 while 与第 4 天相同，顾问的自动循环仍然关闭
    if rememberToolTrace:
        每一轮把点菜消息和工具结果都 memory.add(conversationId, ...)
    else:
        循环结束后只 memory.add(用户句) 和 memory.add(最终回答)
    return { answer, steps, conversationId, memoryMessages: memory.get(conversationId) }
```

**执行计划**

重启后端。下面都在 [Spring AI Agent](http://localhost:5173/demos/agent-core/spring-ai) 点发送。同一会话的第二句，`conversationId` 用页面上一次响应里的那个值，不要点「新会话」。

1. **怎么执行**：点「新会话」，窗口条数 `20`，「记住工具来回」关闭。问句用第一句，点发送。看响应里的 `conversationId`，记成这一会话的 id。
   **执行语句**：

   ```http
   POST /api/stage/03/spring-ai
   {"question":"ORD-1001 现在什么状态？","maxSteps":4,"maxMessages":20,"rememberToolTrace":false,"conversationId":"<这一会话的 id>"}
   ```
2. **怎么执行**：不要点「新会话」。问句改成第二句，再点发送。`conversationId` 与上一句相同。
   **执行语句**：

   ```http
   POST /api/stage/03/spring-ai
   {"question":"刚才那张单对应的 SKU 在 WH-A 还有多少库存？","maxSteps":4,"maxMessages":20,"rememberToolTrace":false,"conversationId":"<与上一句相同的 id>"}
   ```

   - **你会看到**：第一句的 `steps` 是 `getOrder`，回答是已支付。第二句的 `steps` 是 `getInventory`，参数能对上 SKU-88 和 WH-A，数量是 10。两句的 `conversationId` 相同。`memoryMessages` 里看得到第一句。
   - **为什么**：第二句原文没有单号。记忆把第一句带进了第二次请求。
3. **怎么执行**：点「新会话」（id 会变），只发送第二句。
   **执行语句**：

   ```http
   POST /api/stage/03/spring-ai
   {"question":"刚才那张单对应的 SKU 在 WH-A 还有多少库存？","maxSteps":4,"maxMessages":20,"rememberToolTrace":false,"conversationId":"<新的 id>"}
   ```

   - **你会看到**：回答说不清是哪张单。不会把夹具里的 `PAID` 安到「刚才」上。
   - **为什么**：新的 `conversationId` 在仓库里是空的。
4. **怎么执行**：再点「新会话」，窗口条数改成 `2`，「记住工具来回」保持关闭。按顺序点三次发送。
   **执行语句**：

   ```http
   POST /api/stage/03/spring-ai
   {"question":"记住这个记号：ALPHA","maxSteps":4,"maxMessages":2,"rememberToolTrace":false,"conversationId":"<本会话 id>"}
   ```

   ```http
   POST /api/stage/03/spring-ai
   {"question":"记住这个记号：BETA","maxSteps":4,"maxMessages":2,"rememberToolTrace":false,"conversationId":"<同一个 id>"}
   ```

   ```http
   POST /api/stage/03/spring-ai
   {"question":"我的第一个记号是什么？","maxSteps":4,"maxMessages":2,"rememberToolTrace":false,"conversationId":"<同一个 id>"}
   ```

   - **你会看到**：第三句说不出 ALPHA。`memoryMessages` 里没有第一句。
   - **为什么**：每一轮至少写入用户句和模型回答，窗口只有 2 条时，最早那一轮被丢掉。第三次请求的提示里已经没有 ALPHA。
5. **怎么执行**：点「新会话」，窗口调到 `20`，打开「记住工具来回」。先发送货问句，再发追问。两句 id 相同。
   **执行语句**：

   ```http
   POST /api/stage/03/spring-ai
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","maxSteps":4,"maxMessages":20,"rememberToolTrace":true,"conversationId":"<本会话 id>"}
   ```

   ```http
   POST /api/stage/03/spring-ai
   {"question":"你刚才为了回答，依次调用了哪两个工具？","maxSteps":4,"maxMessages":20,"rememberToolTrace":true,"conversationId":"<同一个 id>"}
   ```

   - **你会看到**：`memoryMessages` 里出现工具结果，第二句能按顺序说出 `getOrder` 和 `getInventory`。若把 `rememberToolTrace` 改回 `false` 再做这一对，第二句经常说不出函数名。
   - **为什么**：`false` 时只存最终问答，工具来回留在那一次调用内部。`true` 时每一轮的工具消息也放进同一个会话。

---

### 第 6 天 · 记忆跟着进程走，巡检不必打字

**动手**：`【写代码】`

**学什么**：`ChatMemory` 决定留哪些消息，`ChatMemoryRepository` 决定存在哪。默认仓库在内存里，跟进程走。循环是一个方法，聊天框只是其中一个调用方。

**为了什么**：会话放在 JVM 里，重启就没了。后面换成 Redis 时换的是仓库，`conversationId` 的用法不变。本阶段验收停在看见这份丢失；本机没有 Redis 也不挡今天完成。

**今天读**

- 仍是 [Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)，读仓库那一节（`ChatMemoryRepository` / `InMemoryChatMemoryRepository`）。读完应能说出：记忆策略和落地仓库是两层。进程退出后，内存仓库里的 key 不在了。

**要写的代码**

- 响应里增加 `memoryRepository`，值为当前仓库的类名
- 把循环收成 `@Service` 方法，例如 `run(question, conversationId, maxSteps)`
- `SpringAgentController` 增加 `POST /api/stage/03/spring-ai/patrol`：不读输入框，固定发送发货问句，响应里 `trigger=schedule`。页面按钮「跑一次巡检」打这个地址，用来模拟 `@Scheduled` 调用同一个 Service

**伪代码**

```text
@Service
AgentService:
    repository = InMemoryChatMemoryRepository()
    run(question, conversationId, ...):
        结果 = 第 5 天的循环(question, conversationId)
        结果.memoryRepository = repository.getClass().getName()
        return 结果

    patrol():
        结果 = run("ORD-1001 能不能发货？先查订单再查对应库存。", "patrol")
        结果.trigger = "schedule"
        return 结果

# @Scheduled 以后只调用 patrol()，不读 HTTP 入参
```

**执行计划**

1. **怎么执行**：打开 [Spring AI Agent](http://localhost:5173/demos/agent-core/spring-ai)，点「新会话」，记下页面上的 `conversationId` 和 `memoryRepository`。问句用下面这句，点发送，确认回答是已支付。
   **执行语句**：

   ```http
   POST /api/stage/03/spring-ai
   {"question":"ORD-1001 现在什么状态？","maxSteps":4,"maxMessages":20,"rememberToolTrace":false,"conversationId":"<记下来的 id>"}
   ```
2. **怎么执行**：在跑后端的终端按 `Ctrl+C` 停掉进程，再执行启动语句。浏览器不要点「新会话」，把会话 ID 填回刚才记下的值，问句换成第二句，点发送。
   **执行语句**：

   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   cd demos/backend && mvn spring-boot:run
   ```

   ```http
   POST /api/stage/03/spring-ai
   {"question":"刚才那张单是什么状态？","maxSteps":4,"maxMessages":20,"rememberToolTrace":false,"conversationId":"<重启前记下的同一个 id>"}
   ```

   - **你会看到**：`memoryRepository` 仍是内存实现的类名。回答不知道「刚才」是哪张单。`memoryMessages` 为空。
   - **为什么**：仓库在堆里。浏览器还留着会话 ID，进程里这个 key 已经没了。换成 Redis 或 JDBC 仓库后，同一 key 在重启后仍能取出第一句。
3. **怎么执行**：输入框留空，点「跑一次巡检」。
   **执行语句**：

   ```http
   POST /api/stage/03/spring-ai/patrol
   ```

   - **你会看到**：`steps` 里仍有查订单和查库存，`trigger` 为 `schedule`。问句不来自输入框，是 Service 里写死的发货那句。
   - **为什么**：循环是 Service 方法。这个按钮代表定时器，没有人打字也会跑。

---

### 第 7 天 · LangChain4j 走同一句

**动手**：`【写代码】`

**学什么**：`AiServices` 内部也在跑工具循环。`MessageWindowChatMemory` 按条数留最近的消息；系统消息保留；带工具调用的模型消息被裁掉时，后面的工具结果一并裁掉。

**为了什么**：换框架时业务表不变。要在页面上确认「已支付」仍来自 `OrderStore`，变的是记忆类和轨迹字段。

**今天读**

- [LangChain4j AI Services](https://docs.langchain4j.dev/tutorials/ai-services)。读完应能说出：你声明的接口方法背后仍是模型调用加工具循环。
- [MessageWindowChatMemory](https://docs.langchain4j.dev/apidocs/dev/langchain4j/memory/chat/MessageWindowChatMemory.html)。读完应能说出：按条数裁旧消息；系统消息留下；工具调用和它的结果一起裁，避免下一次请求带上无处安放的工具结果。

**要写的代码**

在 `com.example.demos.stage03.lc4j`：

| 类                      | 职责                                                                                                                                                                                      |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Stage03Lc4jTools`    | 使用 `dev.langchain4j.agent.tool.Tool`，与 Spring 的 `@Tool` 分属两个类。方法内部仍查阶段 2 的 Store                                                                                  |
| `Stage03Assistant`    | 一个接口，由 `AiServices` 生成实现                                                                                                                                                      |
| `Lc4jAgentController` | `POST /api/stage/03/langchain4j`。入参 `question`、`conversationId`、`maxMessages`。记忆用 `MessageWindowChatMemory`。响应含 `answer`、`conversationId`、`memoryMessages` |

接通「LangChain4j Agent」（`/demos/agent-core/langchain4j`）。

**伪代码**

```text
tools = new Stage03Lc4jTools(orderStore, inventoryStore)   # 注解是 langchain4j 的 @Tool

memories = Map<conversationId, ChatMemory>

assistant(conversationId, maxMessages) =
    AiServices.builder(Stage03Assistant)
        .chatModel(deepSeek)
        .tools(tools)
        .chatMemoryProvider(id ->
            memories.computeIfAbsent(id,
                MessageWindowChatMemory.maxMessages(maxMessages)))
        .build()

run(question, conversationId, maxMessages):
    answer = assistant(conversationId, maxMessages).chat(question)
    return { answer, conversationId,
             memoryMessages: memories.get(conversationId).messages() }
```

**执行计划**

重启后端。都在 [LangChain4j Agent](http://localhost:5173/demos/agent-core/langchain4j) 点发送。

1. **怎么执行**：点「新会话」，`maxMessages` 填 `20`。先发第一句，记下 `conversationId`，不要点「新会话」，再发第二句。
   **执行语句**：

   ```http
   POST /api/stage/03/langchain4j
   {"question":"ORD-1001 现在什么状态？","conversationId":"<本会话 id>","maxMessages":20}
   ```

   ```http
   POST /api/stage/03/langchain4j
   {"question":"刚才那张单是付过款了吗？","conversationId":"<同一个 id>","maxMessages":20}
   ```

   - **你会看到**：两句都对得上已支付。第二句的 `conversationId` 与第一句相同。`memoryMessages` 里有第一句。
   - **为什么**：第二句没有单号。窗口里还留着 `ORD-1001`。
2. **怎么执行**：点「新会话」，`maxMessages` 改成 `2`。先发货问句，再发追问。两句用新会话的同一个 id。
   **执行语句**：

   ```http
   POST /api/stage/03/langchain4j
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","conversationId":"<新会话 id>","maxMessages":2}
   ```

   ```http
   POST /api/stage/03/langchain4j
   {"question":"刚才查到的可用库存是多少？","conversationId":"<同一个 id>","maxMessages":2}
   ```

   - **你会看到**：第二句经常说不出 10。`memoryMessages` 很短。
   - **为什么**：发货这一问会写入用户句、工具调用、工具结果和最终回答，条数很容易超过 2。裁掉带工具调用的模型消息时，紧跟的工具结果一并被裁掉，库存 10 就不会出现在下一次提示里。

---

### 第 8 天 · 三种规划各看一次字段

**动手**：`【写代码】`

**学什么**：同样一句「ORD-1001 能不能发货」，只换 `strategy`，就换了一种「下一步由谁决定」。边想边做是模型每轮看到观察之后再决定下一步；先计划再执行是一次性把清单写完、Java 照着清单做；做完再评是先把答案写出来，再用第二次调用按写死的标准改句子。三种查的都是同一份夹具，所以页面上的差别只在于多出来哪个字段、`steps` 什么时候变长。

**为了什么**：前七天只有一种循环形状。生产里选错形状，代价不在代码难写，而在步数和延迟不可控，或者计划一步错就整条跑偏。今天要能指着页面上的 `plan`、`draft`、`review` 说清「这一步是谁定的、什么时候定的」，并知道遇到什么任务该选哪一种。

**今天读**

- 回到 [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents) 里的工作流模式。只读三处，各读一遍就够：
  - **Prompt chaining（提示链）**：固定顺序，前一步输出是后一步输入。对应「先计划再执行」——清单就是链条的第一环。
  - **Evaluator-optimizer（评估器—优化器）**：一个模型生成、另一个按标准评估并打回，循环到通过为止。对应「做完再评」。
  - **Agent（自主循环）**：模型自己在循环里选工具，没有事先写好的路径。对应「边想边做」。
  - 读完应能说出：文章为什么建议「能用一次调用解决就别上 Agent」——每多一个环节就多一次调用、多一段延迟、多一个失败点。
- 第 2 天读过的 [ReAct](https://www.promptingguide.ai/techniques/react) 今天不用重读全文，对照页面上按时间变长的 `steps` 即可。重点看它那句「Thought → Action → Observation 交替」，今天的 react 策略就是这三段在 JSON 里的落点。

**三种规划到底在比什么**

一句话：**规划 = 决定下一步做什么，以及什么时候决定。** 三种策略只在这两件事上不同。

| 策略                             | 谁决定下一步                     | 什么时候决定       | 模型调用次数            | 观察什么时候可见 | 典型失败               |
| -------------------------------- | -------------------------------- | ------------------ | ----------------------- | ---------------- | ---------------------- |
| ReAct（边想边做）                | 模型，每一轮                     | 看到上一次观察之后 | 至少 2 次，随问句增长   | 下一轮才可见     | 步数不可预测，可能绕圈 |
| Plan-and-Execute（先计划再执行） | 模型一次写完清单，Java 照着执行  | 任何观察之前       | 2 次（写清单 + 写句子） | 执行阶段才可见   | 清单写错，后面全错     |
| Reflection（做完再评）           | 第一次是模型，第二次是写死的标准 | 草稿出来之后       | 草稿的次数 + 1          | 草稿阶段已可见   | 审稿顺着草稿的错走     |

这三种不是三个框架，而是三段控制流的不同写法。Spring AI 的顾问、LangChain4j 的 `AiServices`、LangGraph 的图，包装的也是这几种形状，只是把 `while` 藏进了方法内部。

**原理：边想边做（ReAct）**

- 每一轮模型只做一件事：要么点一个工具，要么写最终句子。它看不到未来，只能基于消息列表里已有的事实决定。
- 为什么发货这个问句天然是两跳：库存工具要 `sku` 和 `warehouse`，这两个值只有查过订单才知道。所以第一轮不可能同时点两把工具，第二把的参数必须来自第一把的观察。这是 ReAct 的存在理由——下一步依赖上一步的结果。
- 等价于 Java 里的一个 `while`：每次请求都把全量消息带给模型，模型返回工具调用就执行、把结果追加进消息，再请求。token 随轮次线性增长，第 3 轮的提示里带着前两轮的点菜和观察。上下文过长怎么办是记忆和阶段 4 的事，今天先接受这个成本。
- 页面证据：`steps` 是按时间一条条长出来的，第一行只有 `getOrder`，第二行才出现 `getInventory`；响应里根本没有 `plan` 字段。

**原理：先计划再执行（Plan-and-Execute）**

- 拆成三段：planner（不挂工具，只输出 JSON 清单）→ executor（Java 按清单查表，全程不经过模型）→ synthesizer（不挂工具，把观察写成一句话）。
- 关键约束在 planner：那一刻 `PAID` 和 `10` 都还不存在，清单只能根据问句和菜单猜。所以模型会把 `sku`、`warehouse` 写成 `null`，执行时由 `fillArguments` 从上一步的订单观察里补。补不出来就带着 `null` 去查，得到 `invalid_argument`——这就是「清单写得太早」的代价，也是它和边想边做最本质的区别。
- 为什么 `steps` 里的 `finishReason` 写 `plan` 而不是 `tool_calls`：这一轮模型没有点菜，是 Java 照着清单在查。协议字段要如实反映谁发起了这次执行，否则线上会把「按计划执行」误读成「模型又点了一次菜」。同理，这里的 `round` 是清单里的序号，不是模型请求次数。
- 为什么执行完还要再调一次模型：观察是 JSON，用户要的是一句话。这一次同样不挂工具，避免它看到观察后又想再查一遍。
- 生产用途：步骤能事先拆清、且希望「先给计划再执行」的任务，比如多步数据核对、理赔 SOP、报表生成的固定步骤。好处是清单可以先落库、走审批、被缓存；坏处是中途发现异常不会自动改计划，除非再加一轮重规划。
- 类比：工作流引擎里先出执行计划 → 人工确认 → 按 DAG 执行。

**原理：做完再评（Reflection）**

- 形状是「生成 → 批评 → 重写」。批评的标准写在提示词里，是硬约束：不超过 40 字、必须包含 `ORD-1001`、必须说清能不能发。
- 为什么不再查表：库存 10 已经在草稿的上下文里，审稿这次调用只需要草稿正文。所以 `steps` 和纯 ReAct 一样多，多出来的只有 `draft` 和 `review` 两个字段。
- 代价是多一次模型调用，延迟大约多一倍；换到的是输出口径稳定。标准越模糊，改出来的东西越像没改；标准越硬，它越接近一个格式校验器。
- 局限：审稿模型看到的是草稿，没有独立观察。草稿把 PAID 说错了，审稿也可能顺着改。真正的事实校验仍然只能看 `steps` 里的观察。
- 生产用途：对客话术、合规文案、结构化输出前的自检；也可以当作最便宜的 LLM-as-judge，阶段 5 的评估会展开。
- 类比：接口返回前过一道网关校验，不合格打回重写。

**在这份代码里，三种策略各自落在哪**

- `PlanningController` 的 `switch` 就是「选形状」的地方。三个实现共用 `FixtureToolDispatcher` 和 `StepRecord`，所以 `not_found`、`invalid_argument`、`latencyMs` 的规则和前几天的页面完全一致。
- `PlanningModel` 单独成类，是因为 plan 的清单和 reflect 的审稿都需要「不挂工具的一次调用」。它没有 `toolCallbacks`，温度固定 `0.0`：不挂工具，模型这一轮只能写字；温度归零，同一问句的差异才只来自策略，而不是采样抖动。
- `maxSteps` 只对 react 和 reflect 有意义，plan 的轮次等于清单长度。页面仍统一校验，是为了输入框不用随策略变灰。
- 三种响应共用一个 `PlanningResponse`，靠哪个字段有值区分：`plan` 只有 plan 策略有，`draft` 和 `review` 只有 reflect 有，react 两个都是 null。页面因此不用写三套渲染。

**落到自己的系统，怎么选**

按这个顺序问自己：

1. 步骤能不能事先写死、而且每单必须一样？ → 固定图（第 9 天），下一跳由 Java 决定，最稳。
2. 能写死但需要模型帮忙拆？ → 先计划再执行。计划可预览、可审批、可缓存。
3. 步数随问句变化、下一步依赖上一步结果？ → ReAct。适合排障、客服、多跳查询。
4. 输出质量要求高、能接受多一次调用？ → 在前面任一种之上加 Reflection。

生产里常见的是组合而不是单选：plan 的每一步内部可以是一个 ReAct，ReAct 的最终句子再过一次 Reflection 审稿。页面把三种分开展示，只是为了字段单独看得清。

成本上，调用次数 × 上下文长度就是费用和延迟：ReAct 的上下文随轮次增长；plan 固定两次但清单可能一次写错；reflect 是在前两者基础上 +1。页面上留的 `elapsedMs` 就是给这个对照用的。

**要写的代码**

在 `com.example.demos.stage03.plan`：

| 类                                 | 职责                                                                                                                                                                                                       |
| ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PlanningController`             | `POST /api/stage/03/plan`，入参 `question`、`strategy`（`react` / `plan` / `reflect`）、`maxSteps`。`switch` 只做分发，不在 Controller 里写任何一份循环                                    |
| `ReactPlanning`                  | 复用第 4 天的手动循环（`SpringAgentLoop`），不新做一套查单。响应 `strategy=react`，不带执行前的 `plan`。传空 `conversationId`，让每次点击都是新会话                                                |
| `PlanThenExecute`                | 第一次调用只要求 JSON 步骤，此时不注册工具。然后 Java 按清单查表，把清单里写成 `null` 的库存参数用订单观察补上。最后一次调用把观察交给模型写回答。响应先有 `plan`，再有带 `observation` 的 `steps` |
| `ReflectPlanning`                | 先走边想边做得到 `draft`，再发一次检查。标准写在提示里：不超过 40 个字，必须包含 `ORD-1001`，必须说明能不能发。响应含 `draft`、`review`、改写后的 `answer`                                       |
| `PlanningModel`                  | 不挂工具的单次调用。plan 的「写清单」和 reflect 的「审稿」都用它，温度固定 `0.0`                                                                                                                         |
| `PlanningResponse`               | 三种策略共用一种响应形状，靠 `plan` / `draft` / `review` 哪个有值来区分。页面因此不用写三套渲染                                                                                                      |
| `PlanningRequest` / `PlanStep` | 请求体；以及清单里的一步 `{tool, arguments}`                                                                                                                                                             |

接通「规划对照」（`/demos/agent-core/plan`）。

**三种策略在字段上的差别（今天真正要记住的）**

| 字段           | react                     | plan                              | reflect                       | 这个字段证明什么                              |
| -------------- | ------------------------- | --------------------------------- | ----------------------------- | --------------------------------------------- |
| `plan`       | `null`                  | 有值，查表前就有                  | `null`                      | 清单是在任何观察之前写下的                    |
| `steps`      | 每轮一条，随时间变长      | 按清单执行，`finishReason=plan` | 与 react 相同，仍是草稿那几轮 | plan 的执行是 Java 发起的，不是模型点菜       |
| `draft`      | `null`                  | `null`                          | 有值                          | 第二次调用有东西可改                          |
| `review`     | `null`                  | `null`                          | 有值，含「通过」或「过长」    | 检验标准是写死在提示里的，不是模型自己想的    |
| `stopReason` | `final` / `max_steps` | 固定 `final`                    | 固定 `final`                | plan 没有模型反复点菜的循环，轮次等于清单长度 |

两个容易写错的点：

- **plan 的 `finishReason` 不能照抄 `tool_calls`。** 那一轮模型根本没收到工具菜单，是 Java 照着清单在查。字段要如实反映「谁发起这次执行」，否则线上会把「按计划执行」误读成「模型又点了一次菜」。
- **plan 的 `round` 是清单序号，不是模型请求次数。** react 里 `round` 等于第几次 POST 模型，plan 里它只表示清单的第几步。同一个字段名，在两种策略下含义不同，看页面时要留意。

**伪代码**

```text
run(question, strategy):
    if strategy == "react":
        return 第 4 天的循环(question)          # 响应里没有 plan 字段

    if strategy == "plan":
        plan = 模型(question, 不注册工具,
                    只要求 JSON: [{tool, arguments}, ...])
        # 清单此刻只能猜：库存的 sku/warehouse 还不知道，模型会写 null
        known = {}                              # 执行中从订单观察里补的参数
        steps = []
        for i, item in plan:
            arguments = fillArguments(item, known)   # null 用上一步观察补
            observation = dispatch(item.tool, arguments)   # 这时才查表
            steps.add({ round: i + 1, finishReason: "plan",
                        tool: item.tool, arguments, observation })
            known += 从订单观察抄出 sku、warehouse
        answer = 模型(question + steps, 不注册工具)
        return { plan, steps, answer }

    if strategy == "reflect":
        draft = 第 4 天的循环(question)          # 查表只发生在这里
        review = 模型("检查草稿：不超过 40 字，必须含 ORD-1001，必须说明能不能发",
                      draft.answer)             # 不注册工具，不再查表
        answer = review 改写后的句子
        return { draft, review, answer, steps: draft.steps }
```

`fillArguments` 是 plan 独有的。没有它，库存那一步会带着 `null` 去查，得到 `invalid_argument`——这本身也是一种可观察的结果，说明「清单写得太早，参数拿不到」。加上它，才是工程上可用的版本。

**执行计划**

重启后端。三次都打开 [规划对照](http://localhost:5173/demos/agent-core/plan)，问句相同，只改策略后点发送。三次都看页面上方的 `耗时`，它把调用次数的代价直接摊开：react 是「查订单、查库存、写句子」三次调用；plan 是「写清单、写句子」两次，通常最快；reflect 在 react 之上再加一次审稿，最慢。

1. **怎么执行**：策略选「边想边做」，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/plan
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","strategy":"react","maxSteps":4}
   ```

   - **你会看到**：`strategy` 为 `react`。没有事先填好的 `plan`。`steps` 先出现 `getOrder`，其后才出现 `getInventory`。第 3 行是「不再点菜」，`finishReason` 是 `stop`。
   - **为什么**：每一轮只决定当前要不要点菜。库存工具出现在第二轮，是因为第一轮的观察里已经有订单。这正是 ReAct 的定义——下一步依赖上一步的观察，所以事先无法写出完整计划。
   - **要记的**：`steps` 的长度不是固定的。换一句更复杂的问句，它可能变成 4 条、5 条。这个「不可预测」就是 ReAct 的代价，也是后面 max_steps 存在的理由。
2. **怎么执行**：策略改成「先计划再执行」，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/plan
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","strategy":"plan","maxSteps":4}
   ```

   - **你会看到**：先有 `plan` 表格，两项工具名是 `getOrder`、`getInventory`，此时还没有观察；`getInventory` 那行的参数里 `sku` / `warehouse` 可能是 `null`。随后 `steps` 补上已支付和数量 10，`finishReason` 全部是 `plan` 而不是 `tool_calls`。`answer` 在观察填完之后出现。
   - **为什么**：规划那一次请求里还没有 `PAID` 和 `10`，模型看不到表，只能按问句和菜单猜。`sku` 和 `warehouse` 只有查过订单才知道，所以清单里是 `null`，执行时由 `fillArguments` 从上一步的订单观察补上。
   - **要记的**：清单里的 `sku` / `warehouse` 是模型猜的，不是查出来的。这份实现用 `fillArguments` 在查表前用真实的订单观察覆盖它，所以即使模型猜了 `SKU-88` 也会被改成真实值。朴素实现若省掉这一步，猜错就会查错库存——这是先计划再执行最常见的线上事故。
3. **怎么执行**：策略改成「做完再评」，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/plan
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","strategy":"reflect","maxSteps":4}
   ```

   - **你会看到**：`draft` 通常比 `answer` 长。`review` 第一行是「过长」或「通过」，后面跟改写后的句子。最终 `answer` 含 `ORD-1001`，并且短于草稿。`steps` 和 react 那次相同，查表仍是发货所需的那两次。
   - **为什么**：第二次调用检查的是句子约束，不是事实。库存已经在草稿的上下文里，改句子不必再查库，所以 `steps` 没有变长。
   - **要记的**：`review` 给出的标准是写死在提示里的三条。标准之外的东西它不管——比如草稿把 PAID 说成未支付，只要字数、单号、结论三项满足，审稿不会纠正。事实校验永远只能看 `steps` 里的 `observation`。
4. **怎么执行**：`【不写代码】` 把策略切回「边想边做」，问句换成只问状态，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/plan
   {"question":"ORD-1001 现在什么状态？","strategy":"react","maxSteps":4}
   ```

   - **你会看到**：`steps` 只有 `getOrder` 一条点菜记录，随后就是「不再点菜」。不会有 `getInventory`。
   - **为什么**：问句里没有发货和库存，模型判断只查订单就够。这和第 9 天固定图的行为正好相反——固定图不管问什么都会把库存查出来。这一个对比就是「Agent 与工作流」在页面上的分界。

**收口：今天要能回答的三个问题**

- 三种策略谁的步数不可预测？——react。它的 `steps` 长度取决于模型每一轮的决定。
- 哪一种在查表之前就能看到全部步骤？——plan。`plan` 字段在任何 `observation` 之前就有值。
- 哪一种多花一次调用却没有多查一次表？——reflect。`steps` 与 react 相同，多出来的是 `draft` 和 `review`。

---

### 第 9 天 · 下一步写在边上

**动手**：`【写代码】`

**学什么**：前八天的「下一步做什么」都由模型在各种 `tool_calls` 里决定。今天换成另一种：**下一步写在代码里**。用 `StateGraph` 把「查订单 → 查库存 → 按条件选文案」摆成一张有向图，节点是一步，边是下一步，`OverAllState` 是贯穿所有节点的那个 Map。模型仍然参与——但它只在最后一个节点里「把已有的事实写成一句话」，不参与选路。

**为了什么**：发货审核这种业务，要求每一单都走过同一组检查，且事后能对着审计说「这一单经过了哪几个节点」。如果选路由模型决定，模型完全可能因为问句措辞而跳过库存检查。把边写死，`available = 0` 时就**不可能**被说成「可以发」——不是「模型通常不会这么说」，是代码里根本没有那条路径。

**今天读**

- [什么是 Spring AI Alibaba Graph](https://java2ai.com/docs/1.0.0.2/tutorials/graph/whats-spring-ai-alibaba-graph/)。读的时候盯住四样东西：`StateGraph`（图本身）、`addNode`（加一步）、`addEdge` / `addConditionalEdges`（连下一步）、`OverAllState`（贯穿的状态）。读完应能说出：节点之间不直接传参，全靠状态 Map。
- [LangGraph 概述](https://docs.langchain.com/oss/python/langgraph/overview)。只读 State、Node、Edge 三节，不运行任何 Python。读完应能说出：这是同一组概念的另一套实现，Spring AI Alibaba 的 Graph 就是照着 LangGraph 的思路做的。

**三种控制权，分清楚再动手**

这是今天最容易混的地方。Agent 系统里「next 由谁定」有三个层次：

| 层次                          | 谁决定下一步                | 你怎么写                                            | 今天在哪见                       |
| ----------------------------- | --------------------------- | --------------------------------------------------- | -------------------------------- |
| 模型决定                      | 模型每一轮看情况选          | 挂工具菜单，写 `while`                            | 第 2～8 天                       |
| **Java 决定（固定边）** | 代码写死：A 之后必然是 B    | `builder.addEdge(A, B)`                           | `queryOrder → queryInventory` |
| **Java 决定（条件边）** | 代码读状态判断：走 C 还是 D | `addConditionalEdges(...)` + 一个返回字符串的方法 | `decideBranch`                 |

**关键认知：图和模型不是对立的。** 图不是「不用模型」，而是「模型被限制在节点内部」。今天的 `writeCopy` 节点照样调模型写句子——但模型拿到的只有已经查好的事实，它没有工具菜单，想再查也没得查。**模型负责表达，Java 负责流程和判断**，这就是工作流的本质。

**原理：StateGraph 的四件套**

用你已经写过的东西类比，这套 API 就是个**带类型的流程编排器**：

- **节点（Node）= 一个方法**：签名是 `Map<String,Object> method(OverAllState state)`。读状态 → 干活 → 返回一个 Map，框架把它合进状态。跟写 `@Service` 方法没本质区别，只是它不把返回值给调用方，而是给「状态」。
- **边（Edge）= 下一步**：`addEdge(A, B)` 就是「A 跑完必跑 B」。没有条件、没有判断、不看问句。
- **条件边（Conditional Edge）= 一个返回字符串的方法**：`decideBranch` 读状态里的 `paid` 和 `available`，返回 `"canShip"` 或 `"cannotShip"`，框架拿这个字符串去查一张映射表，决定下一跳。**判断逻辑是纯 Java，模型碰不到。**
- **状态（OverAllState）= 贯穿全程的 Map**：所有节点共享。节点之间**不直接传参**，这是图编程和普通方法调用的最大差别——`queryInventory` 拿不到 `queryOrder` 的返回值，只能从状态里读。

**原理：状态里的 key 怎么合并（今天最容易踩的坑）**

`KeyStrategyFactory` 声明每个 key 被写第二次时怎么办：

- `KeyStrategy.REPLACE`：后写的覆盖先写的。订单、库存、分支、答案都是这种——各写一次，没有合并需求。
- `KeyStrategy.APPEND`：累积成一个 List。`nodes` 和 `steps` 用这个，因为「走过哪些节点」「查了几次表」需要越积越多。

**如果 `nodes` 错用成 REPLACE**，页面上的 `nodes` 永远只剩最后一个节点名，你就看不出这条链路经过了哪几步。这是新手第一个 bug。

**原理：图编译一次，跑很多次**

`ShipGraph` 的构造函数里调 `build()`，`build()` 里 `builder.compile(...)` 得到 `CompiledGraph`，之后所有请求共用这一个实例。编译后的图是**只读的**，所以可以做成单例 Bean——跟你把一份规则配置在启动时加载好、请求时只读，是同一个道理。

`compile(CompileConfig.builder().recursionLimit(10).build())` 里的 `recursionLimit` 是**图的死循环保护**。图允许有环（ReAct 在图上就是一个环：模型节点 → 工具节点 → 回到模型节点），既然有环就可能转不停，所以框架给了上限。第 10 天讲停止条件时会再碰到它——这里的 10 就是「最多走 10 步」。今天这张图只有 3～4 步，上限根本到不了，但写了是对的。

**原理：这条边不看问句，所以代价是多查一次**

`queryOrder → queryInventory` 是**固定边**。用户只问「ORD-1001 现在什么状态」，图照样会去查库存——因为那条边不读问句，它只知道「发货审核要看库存」。

这就是工作流的定价：**用「多做一步」换「每次都一样」**。第 8 天的 ReAct 遇到同样的问题只查订单就停了，省了一次调用；但你也无法保证它每次都记得查库存。**一个省钱但不确定，一个费钱但确定**，选哪个取决于业务能不能承受不确定。

**实际作用：生产里怎么用**

工作流适合**规则稳定、要留痕、出错代价高**的场景：

- **发货 / 退款审核**：能不能发由库存和订单状态决定，规则不能随措辞漂移。
- **理赔、报销**：每一步都要能向审计说明「经过了哪几个节点、每个节点的输入输出是什么」。
- **风控规则链**：命中哪条规则、走了哪个分支，必须是代码判断，不能是模型判断。
- **定时巡检**：每天固定跑一遍同一组检查，没有人提问，也就没有「问句」可依据。

配套好处：`nodes` 这个数组天然就是**审计日志**。存进数据库，任何一个结果都能回答「它是怎么来的」。这比只有一句 `answer` 强得多。

**什么时候不该用图**

- 步骤没法预先枚举——比如「这单为什么发不出去」可能牵扯五个系统，走哪条路取决于前一步发现了什么。硬画成图会变成一堆条件边，比 ReAct 还难维护。
- 问句变化大、每次需要不同的查询组合。这时图的固定边就是在浪费调用。
- 探索性任务（调研、排障）。图的确定性在这里反而是枷锁。

判据回到第 8 天那句：**能写死就别让模型决定；必须让模型决定时，把上限和确认点钉死在代码里。**

**要写的代码**

在 `com.example.demos.stage03.graph`：

| 类                                   | 职责                                                                                                                                                    |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ShipGraph`                        | 建图（四个节点 + 两条固定边 + 一条条件边），并对每个请求 `invoke` 一次。图在构造函数里 `compile` 好，之后只读复用                                   |
| `GraphController`                  | `POST /api/stage/03/graph`。入参 `question`，以及可选的 `availableOverride`（只放进这一次的状态，不写回夹具）                                     |
| `GraphRequest` / `GraphResponse` | 请求体；响应含 `nodes`（走过的节点，按顺序）、`branch`、`answer`、`fixtureAvailable`（夹具真实值）、`observations`（两个节点各自看到的 JSON） |

四个节点分别是：

| 节点               | 干什么             | 往状态里写                                                 |
| ------------------ | ------------------ | ---------------------------------------------------------- |
| `queryOrder`     | 查 ORD-1001        | `orderJson`、追加 `nodes` / `steps`                  |
| `queryInventory` | 查 SKU-88 在 WH-A  | `available`（override 优先）、追加 `nodes` / `steps` |
| `canShipCopy`    | 让模型写「可以发」 | `answer`、`branch`、追加 `nodes`                     |
| `cannotShipCopy` | 让模型写「不能发」 | 同上                                                       |

`availableOverride` 是为了让你**在不改夹具的前提下**把一次运行的判断条件改成 0，从而看到条件边走向另一边。类比：演练/预演环境里改一个参数跑一遍，生产数据不动。响应里同时给出 `fixtureAvailable`，页面上一眼就能对照「状态里是 0，表里还是 10」。

**伪代码**

```text
建图（只做一次）:
    strategies = { 其余 key: REPLACE, nodes: APPEND, steps: APPEND }
    g = StateGraph(strategies)
    g.addNode("queryOrder",     读状态 → 查订单 → 返回 {orderJson, nodes, steps})
    g.addNode("queryInventory", 读状态 → 查库存 → 返回 {available, nodes, steps})
    g.addNode("canShipCopy",    读状态 → 模型写一句 → 返回 {answer, branch, nodes})
    g.addNode("cannotShipCopy", 同上)
    g.addEdge(START, "queryOrder")
    g.addEdge("queryOrder", "queryInventory")           # 固定边：不看问句
    g.addConditionalEdges("queryInventory", decideBranch,
        { canShip: "canShipCopy", cannotShip: "cannotShipCopy" })
    g.addEdge("canShipCopy", END)
    g.addEdge("cannotShipCopy", END)
    return g.compile(recursionLimit = 10)

decideBranch(state):                                     # 纯 Java，模型改不了
    paid = state.orderJson 含 "status":"PAID"
    available = state.available
    return paid 且 available > 0 ? "canShip" : "cannotShip"

run(question, availableOverride):                        # 每个请求都走这里
    result = 编译好的图.invoke({question, availableOverride?})
    return { nodes: result.nodes, branch: result.branch,
             answer: result.answer,
             fixtureAvailable: 10,                       # 从 Store 重新读，override 不影响它
             observations: result.steps }
```

三个要盯住的细节：

- **`decideBranch` 返回的是字符串，不是布尔。** 框架拿这个字符串去查 `Map.of("canShip", ...)`，所以**返回值和映射表的 key 必须对得上**，拼错会直接运行时报错。这是条件边唯一的坑。
- **`queryInventory` 的参数写死 `SKU-88` / `WH-A`，不从问句解析。** 因为这条边根本不关心用户问了什么——它只知道「发货审核要看库存」。真实系统里这里会从状态里的订单 JSON 读 sku，今天的写法是简化版。
- **`fixtureAvailable` 和 `available` 是两个数。** 前者永远从 `InventoryStore` 读，后者可能是 override。两者都返回，就是为了让「状态可改、夹具不可改」这件事在页面上看得见。

**执行计划**

重启后端。三步都在 [固定工作流](http://localhost:5173/demos/agent-core/graph)。

1. **怎么执行**：打开 [固定工作流](http://localhost:5173/demos/agent-core/graph)。问句只问状态，「本次判断用的可用数量」留空，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/graph
   {"question":"ORD-1001 现在什么状态？"}
   ```

   - **你会看到**：`nodes` 是 `queryOrder`、`queryInventory`、`canShipCopy` 三个。`branch` 为 `canShip`。回答里会提到库存。`fixtureAvailable` 是 10。`observations` 里有两条，分别是订单和库存的 JSON。
   - **为什么**：下一跳是 `addEdge` 写上的，不是 `tool_calls` 选的。用户只问状态，图仍然查了库存——那条边不看问句。
   - **要记的**：这就是工作流的定价。用它换来了「每一单都必然查库存」的确定性。
2. **怎么执行**：打开 [规划对照](http://localhost:5173/demos/agent-core/plan)，策略选「边想边做」，问句与上一句相同，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/plan
   {"question":"ORD-1001 现在什么状态？","strategy":"react","maxSteps":4}
   ```

   - **你会看到**：`steps` 里通常只有 `getOrder`，然后就没再点菜。
   - **为什么**：菜单允许模型只点它认为需要的工具。问句里没有发货和库存，它就停在订单。
   - **要记的**：**这一对对比就是今天的全部主题。** 同一句问句，Agent 省了一次查询但不保证查了库存；工作流多查一次但每次都一样。生产里选哪个，取决于「漏查库存能不能接受」。
3. **怎么执行**：回到固定工作流，把「本次判断用的可用数量」填 `0`，问句改成发货，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/graph
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","availableOverride":0}
   ```

   - **你会看到**：`nodes` 变成 `queryOrder`、`queryInventory`、`cannotShipCopy`。`branch` 为 `cannotShip`。而 `fixtureAvailable` 仍然是 10。
   - **为什么**：条件边读的是这次放进状态里的 `available = 0`，而 `fixtureAvailable` 是从 `InventoryStore` 重新读的，override 从头到尾没碰过 Store。
   - **要记的**：`available` 和 `fixtureAvailable` 是两个数，这个对照证明了「状态（一次运行的临时数据）」和「夹具（持久数据）」是两层。这也是演练、灰度、dry-run 能不改生产数据就验证分支逻辑的原因。

**收口：今天要能回答的三个问题**

- 下一跳是谁定的？——`addEdge` 和 `addConditionalEdges`，都是 Java。模型只在节点内部写句子。
- 只问状态为什么还查了库存？——因为 `queryOrder → queryInventory` 是固定边，它不看问句。
- 数量为 0 为什么不可能被说成「可以发」？——因为 `decideBranch` 是 Java，走不到 `canShipCopy` 那个节点。

---

### 第 10 天 · 三种停法，再用三句话收口

**动手**：前半 `【写代码】`（停止与确认）；第 5 步收口 `【不写代码】`，只点已有页面。

**学什么**：循环要有明确的结束。今天看三种：步数上限、菜单里没有对应方法、写操作停在确认。同一问句在不同页面上，变的是循环被谁握住、历史有没有带上、下一步由谁决定。

**为了什么**：没有上限时模型可以反复点同一个工具。没有确认时，一句「退款」就会进写方法。三句话用来把前九天的字段收成能指着页面讲出来的结论。

**今天读**

- 再读 [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents) 里「最大迭代次数」和工具接口要写清楚的两节。读完应能说出：上限和菜单说明是循环的一部分。

**要写的代码**

在 `com.example.demos.stage03.guard`：

| 类                  | 职责                                                                                                                                       |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `GuardedAgent`    | 复用手动循环。`maxSteps` 由请求传入，到达上限时 `stopReason=max_steps`，并停止下一次模型请求                                           |
| 菜单                | 查询工具照常。不注册「改库存」。另注册退款工具，执行前若未经确认则 `stopReason=waiting_human`，该步 `executed=false`，退款标记保持未退 |
| `GuardController` | `POST /api/stage/03/guard` 开始一次运行。`POST /api/stage/03/guard/confirm` 用同一个 `runId` 接上暂停的运行，这次才调用退款方法      |

接通「停止与边界」（`/demos/agent-core/guard`）。响应里带夹具：可用数量、是否已退款。

**伪代码**

```text
paused = Map<runId, 未执行的退款调用>

run(question, maxSteps):
    menu = [ getOrder, getInventory, refundOrder ]    # 没有改库存
    for round in 1 .. maxSteps:
        response = 模型(messages, menu)
        if 没有 tool_calls:
            return { stopReason: "final", ... }
        for call in tool_calls:
            if call.name 不在 menu:
                steps.add({ executed: false })
                continue
            if call.name == "refundOrder" 且 这次请求没有 confirm:
                paused.put(runId, call)
                return { stopReason: "waiting_human",
                         steps: [..., { toolName: "refundOrder", executed: false }],
                         refunded: false }
            observation = dispatch(call)               # 查询在这里查表
            steps.add({ executed: true, observation })
    return { stopReason: "max_steps", steps }

confirm(runId):
    call = paused.remove(runId)
    执行退款(call.orderId)                            # 只有这条路径改退款标记
    return { stopReason: "final", refunded: true, runId }
```

**执行计划**

重启后端。前 4 步都在 [停止与边界](http://localhost:5173/demos/agent-core/guard)。第 5 步不再改代码。

1. **怎么执行**：最大步数填 `2`，问句用发货那句，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/guard
   {"question":"ORD-1001 能不能发货？先查订单再查对应库存。","maxSteps":2}
   ```

   - **你会看到**：`stopReason` 为 `max_steps`。`steps` 停在 2 条，通常还没有最终句子。
   - **为什么**：发货至少要三轮。上限写在 `while` 里，模型还想点菜，Java 不再发下一次请求。
2. **怎么执行**：最大步数改成 `4`，问句换成改库存，点发送。
   **执行语句**：

   ```http
   POST /api/stage/03/guard
   {"question":"把 SKU-88 的库存改成 999","maxSteps":4}
   ```

   - **你会看到**：可用数量仍是 10。`steps` 里没有一个把库存写成 999 的已执行工具。
   - **为什么**：菜单里没有改库存的方法，就没有代码去改那份内存。
3. **怎么执行**：问句换成退款，点发送。出现确认条时先不要点。
   **执行语句**：

   ```http
   POST /api/stage/03/guard
   {"question":"给 ORD-1001 退款","maxSteps":4}
   ```

   - **你会看到**：`stopReason` 为 `waiting_human`。退款标记仍是未退。`steps` 里退款那条 `executed` 为 `false`。响应里有 `runId`。
   - **为什么**：模型点了菜，写方法在确认前没有执行。
4. **怎么执行**：点「确认执行」。`runId` 用上一句响应里的值。
   **执行语句**：

   ```http
   POST /api/stage/03/guard/confirm
   {"runId":"<上一步响应里的 runId>"}
   ```

   - **你会看到**：同一个 `runId` 继续。`stopReason` 变为 `final`。退款标记变为已退。
   - **为什么**：确认是第二次请求，把暂停的运行接上。写方法这次才进入。
5. `【不写代码】` **怎么执行**：不再改 Java。用 A、B、C 三句到下表的页面各点一次发送。B 必须和同一页刚发出的 A 使用同一个 `conversationId`。
   **执行语句**：

   ```http
   A  {"question":"ORD-1001 现在什么状态？"}
   B  {"question":"刚才那张单对应的 SKU 在 WH-A 还有多少库存？","conversationId":"<与本页 A 相同>"}
   C  {"question":"ORD-1001 能不能发货？先查订单再查对应库存。"}
   ```

   | 页面                                                                   | 怎么执行                                                             | 发哪一句                                                                                          |
   | ---------------------------------------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
   | [手写 Agent Loop](http://localhost:5173/demos/agent-core/loop)          | `POST /api/stage/03/loop`，`maxSteps` 为 4                       | A，然后 C。A 为两轮且第一轮是 `getOrder`；C 至少三轮，观察里有 10                               |
   | [Spring AI Agent](http://localhost:5173/demos/agent-core/spring-ai)     | `POST /api/stage/03/spring-ai`，同一会话先 A 再 B                  | B 的库存参数能对上 SKU-88，两句会话 ID 相同                                                       |
   | [LangChain4j Agent](http://localhost:5173/demos/agent-core/langchain4j) | `POST /api/stage/03/langchain4j`，`maxMessages` 为 20，先 A 再 B | B 能答出 10                                                                                       |
   | [规划对照](http://localhost:5173/demos/agent-core/plan)                 | `POST /api/stage/03/plan`，C 发两次                                | 第一次 `"strategy":"react"`，没有执行前的 `plan`；第二次 `"strategy":"plan"`，先有 `plan` |
   | [固定工作流](http://localhost:5173/demos/agent-core/graph)              | `POST /api/stage/03/graph`                                         | 只发 A。`nodes` 仍包含查库存                                                                    |
   | [停止与边界](http://localhost:5173/demos/agent-core/guard)              | `POST /api/stage/03/guard`，`maxSteps` 为 2                      | 只发 C。`stopReason` 为 `max_steps`                                                           |

   写在下面：


   1. 事实来自工具：我在 ______ 页看到 C 的观察里有 ______。
   2. 下一句靠会话：我在 ______ 页看到 B 的 `conversationId` 与 A 相同，库存是 ______。
   3. 停和分支是 Java 的：最大步数 2 时 `stopReason` 是 ______；只问状态时固定图仍经过 ______ 节点。

三句写完，并且上面 10 天都已勾上，阶段 3 完成。
