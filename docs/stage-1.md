# 阶段 1：提示词工程（上手手册）

> 业余约 7 天（每天 1.5–2 小时）| 主线：Java / 已有阶段 0 项目
> 任务总表：[learning-plan.md 阶段 1](learning-plan.md) · 知识全文：[prompt-engineering.md](../notes/prompt-engineering.md) · 资料：[resources.md#stage-1](resources.md#stage-1)

**这份手册只下任务。** 知识点、判定标准、示例已经写在笔记对应 K 节。每天：按下面步骤做 → 打开指定 K 和指定博客 → 把结果填进笔记 §9。不要自己找资料，不要另写作文。

**完成规则**：`learning-plan.md` 里某条 `S1-T*` 打勾 = 它列出的本手册当天完成判定 + 笔记对应 K「已读完」全部打勾。

每天只改 **一个变量**。对照时 temperature 按 0 理解（阶段 0 ChatClient 若默认 0.7，两次结果抖动是正常的，在 §9 备注写「温度噪音」）。

### 动手类型（看标签就知道要不要改 Java）

| 标签                       | 含义                                                       | 典型动作                                                                      |
| -------------------------- | ---------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **【只读】**         | 读笔记 / 博客，无实验                                      | 勾「已读完」、填结论模板                                                      |
| **【调用后端】**     | 用已有接口，**不改** `demos/backend/stage00` 源码  | curl / 前端自由问答，prompt 塞进`msg` 或 `ticket`                         |
| **【写后端】**       | **必须改** `demos/backend/stage00/` 才能理解该能力 | 新建 record、加 Controller 方法、`.system()` / `.param()` / `.entity()` |
| **【写后端·加分】** | 可选，不挡进阶段 2                                         | `demos/backend/stage01/` 提示词 6 实验                                      |

**阶段 1 只有 Day 5 是硬性【写后端】**；D1–D4、D6 全是【调用后端】（D6 依赖 Day 5 已写的 `/triage`）。D0 启动服务 + 冒烟也算【调用后端】。

---

## 任务 ↔ 天 ↔ 知识

| 任务                      | 做完这些天                    | 读完这些 K            | 动手（哪天）     |
| ------------------------- | ----------------------------- | --------------------- | ---------------- |
| S1-T1 LLM 原理            | D0                            | K1 K2 K3              | D0 调用          |
| S1-T2 提示词五件套        | D1 D2 D3 D4                   | K4 K5 K6 K7 K8 K9 K10 | D1–D4 调用      |
| S1-T3 上下文工程          | D5 + D7 读 K12                | K11 K12 K13           | D5 写后端        |
| S1-T4 失败模式            | D6                            | K14 K15 K16 K17       | D6 调用          |
| S1-T5 结构化输出对接 Java | D4 D5 D7（AiServices 5 分钟） | K10 K18 K19           | D4 调用；D5 写   |
| S1-T6 Java 实战           | D5                            | K13 K18               | D5 写后端        |
| S1-T7 对照表 + 结论填空   | D7                            | K20                   | D7 只读；O6 加分 |

---

## D0 · 启动环境 + 读原理（约 30 分钟）`【调用后端】` `【只读】`

**对应**：S1-T1 · K1 K2 K3
**必读**：笔记 K1、K2、K3（原理已写全，今天**不要**另开博客或课）

阶段 0 服务已经能调 DeepSeek。前几天**不新开 Maven 工程**。

### 去做

- [X] **D0-O1** `【调用后端】` 终端 1 启动后端：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd demos/backend && mvn spring-boot:run
```

日志出现端口 **8080**。浏览器打开 `http://localhost:8080/api/health` 能通。

- [X] **D0-O2** `【调用后端】` 终端 2 启动前端，或只用 curl：

```bash
cd demos/frontend && npm run dev
```

打开：http://localhost:5173/demos/hello-llm/chat （菜单：**环境与 LLM 入门 → 自由问答**）

curl 示例（prompt 有换行和中文，必须 `--data-urlencode`）：

```bash
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode 'msg=用一句话说明什么是上下文窗口'
```

当前 `/api/test/hello` **没有 system 参数**。D1–D4 把角色+任务+数据写成**一整段**贴进自由问答即可。D5 再拆成 `.system()` / `.user()`。

- [X] **D0-O3** `【只读】` 打开 [prompt-engineering.md](../notes/prompt-engineering.md)，从标题 **K1** 读到 **K3** 结束。每节末尾勾「已读完」。K1/K2/K3 就在笔记前三节，不在本手册。
- [X] **D0-O4** `【只读】` 把工单 A 固定下来（复制到备忘录或钉在笔记顶部）。后面 7 天都用它，不要改成写诗或换业务。

工单 A（直接复制）：

```
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。请分类并给出下一步。
```

### D0 完成判定

- [X] 8080 自由问答或 curl 可用
- [X] 笔记 K1 K2 K3 已勾「已读完」
- [X] 工单 A 已固定

---

## 阶段目标（做完能演示这 5 件事）

1. 同一条工单，无角色 vs 有角色，口吻和方案明显不同。
2. 0 条示例 vs 3 条示例，「信息不足」能落到 UNKNOWN。
3. 多步排查加上「先写步骤」后覆盖更全，同时更慢。
4. `ChatClient.entity(TicketTriage.class)` 接到 Java record。
5. 幻觉 / 指令冲突 / 截断能按笔记里的标准认出来。

**本阶段不做**：`@Tool`、Agent Loop、RAG、独立 stage01 全套实验（加分项，不挡进阶段 2）。

进阶段 2 的门槛：S1-T1～T7 全勾。独立工程 `prompt-java` 不挡进度。

---

## Day 1 · 坏 prompt vs 好 prompt `【调用后端】`

**对应**：S1-T2 · K4 K5 K6 K7 · **不改 Java**，整段 prompt 贴进 `GET /api/test/hello?msg=…` 或前端自由问答
**只改**：有没有角色 + 边界
**必读**：先跑下面两段 prompt；再读笔记 K4–K7；博客只看这一篇的 System 四段式：[Prompt 工程实战](https://jishuzhan.net/article/2065228421654999042)

### 去做

- [X] **D1-O1** 确认 D0 完成判定已全勾。
- [X] **D1-O2** `【调用后端】` 自由问答粘贴「坏 prompt」，连发 **2 次**。两次完整回复先复制到别处，再填 §9。

**坏 prompt（整段粘贴）：**

```
帮我看看这个单子怎么处理：
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
```

- [X] **D1-O3** `【调用后端】` 粘贴「好 prompt」，连发 **2 次**。

**好 prompt（整段粘贴）：**

```
你是资深 Java 后端，只给可落地方案，不写空话。
信息不足就明说，不要编造集群规模、类名、中间件。

任务：给工单分类，并列出最多 3 条下一步。
分类只能用：PERF / BUG / DATA / UNKNOWN

工单：
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
```

- [X] **D1-O4** `【只读】` 按笔记 **K6 判定表** 填 §9 两行（`角色 / 无角色`、`角色 / 有角色+边界`）。标准已经写在 K6，按表打勾现象，不要另列开放题。
- [X] **D1-O5** `【只读】` 读完笔记 K4、K5、K6、K7，四节都勾「已读完」。
- [X] **D1-O6** `【只读】` 打开上面那篇博客，只对照 System 四段式：好 prompt 有角色、任务、边界；缺 Few-shot（明天补）。看完不要点下一章。

### Day 1 完成判定

- [X] 坏 / 好各 2 次已跑
- [X] §9 角色两行已填
- [X] K4–K7 已读完
- [X] 指定博客 System 四段式已看过

---

## Day 2 · Few-shot `【调用后端】`

**对应**：S1-T2 · K8 · **不改 Java**，仍打 `/api/test/hello`
**只改**：示例条数 0 vs 3
**必读**：先跑；再读笔记 K8；博客同一篇的 Few-shot 节：[Prompt 工程实战](https://jishuzhan.net/article/2065228421654999042)

工单 B（信息不足，今天用它，不用工单 A）：

```
标题：系统有点慢
描述：用户说今天不太流畅。
```

### 去做

- [X] **D2-O1** `【调用后端】` 粘贴 0-shot，连跑 **2 次**。

```
把工单分成 PERF / BUG / DATA / UNKNOWN，只输出类别一个词。
工单：标题：系统有点慢。描述：用户说今天不太流畅。
```

- [X] **D2-O2** `【调用后端】` 粘贴 3-shot，连跑 **2 次**。中间三条示例不要改。

```
把工单分成 PERF / BUG / DATA / UNKNOWN，只输出类别一个词。

示例1：
输入：checkout P99 从 200ms 升到 3s，大量 SocketTimeoutException
输出：PERF

示例2：
输入：同一用户下单后库存扣了两次
输出：BUG

示例3：
输入：用户说今天不太流畅，没有任何日志、接口、环境
输出：UNKNOWN

现在分类：
输入：标题：系统有点慢。描述：用户说今天不太流畅。
输出：
```

- [X] **D2-O3** `【只读】` 按笔记 **K8 判定表** 填 §9：`Few-shot / shots=0`、`Few-shot / shots=3`。预期：0-shot 常硬分成 PERF；3-shot 应为 UNKNOWN。
- [X] **D2-O4** `【只读】` 读完 K8，勾「已读完」。看博客 Few-shot 那一节。

### Day 2 完成判定

- [X] 0-shot / 3-shot 各 2 次
- [X] §9 两行已填
- [X] K8 已读完

---

## Day 3 · CoT `【调用后端】`

**对应**：S1-T2 · K9 · **不改 Java**，仍打 `/api/test/hello`
**只改**：是否要求先写步骤
**工单**：工单 A（多步排查才值得开 CoT）
**必读**：笔记 K9；同一篇博客的 CoT 节

### 去做

- [X] **D3-O1** `【调用后端】` 粘贴「直接答」，连跑 **2 次**。记下页面 elapsed ms。

```
作为 Java 后端，用 5 条以内给出：最可能根因、该查哪几个地方、不该先做什么。
工单：订单服务调用库存超时，checkout P99 200ms→3s，SocketTimeoutException，库存刚发版。
```

- [X] **D3-O2** `【调用后端】` 粘贴「先推理再答」，连跑 **2 次**。同样记耗时。

```
作为 Java 后端。先用 3～5 步写出排查顺序（不要给结论），然后再给：最可能根因、该查哪几个地方、不该先做什么。
工单：订单服务调用库存超时，checkout P99 200ms→3s，SocketTimeoutException，库存刚发版。
```

- [X] **D3-O3** `【只读】` 按笔记 **K9 判定标准** 填 §9：`CoT / 关`、`CoT / 开`。四类排查（超时配置 / 对方慢 / 线程池 / 发版回滚）缺哪条写在备注；CoT 应更慢、更长。
- [X] **D3-O4** `【只读】` 读完 K9，勾「已读完」。

### Day 3 完成判定

- [X] 两种各 2 次，耗时都记了
- [X] §9 两行已填
- [X] K9 已读完

---

## Day 4 · 强制 JSON `【调用后端】`

**对应**：S1-T2、S1-T5 · K10 · **不改 Java**（手工 json + 终端 parse）；阶段 0 已有 `/student-scores` 可作 `.entity()` 预习，本日不强制改代码
**只改**：输出改成带字段样例的 json
**必读（今天就要打开）**：[DeepSeek JSON Output 一页纸](https://api-docs.deepseek.com/zh-cn/guides/json_mode) + 笔记 K10

### 去做

- [X] **D4-O1** `【只读】` 打开官方一页纸，对照笔记 K10 的三条规则（prompt 出现 `json`+样例；看 `finish_reason`；JSON 模式不保证字段）。读完勾 K10。
- [X] **D4-O2** `【调用后端】` 粘贴下面 prompt，用工单 A，连跑 **2 次**。

```
只输出 json，不要 markdown，不要解释。
字段必须是：
{"category":"PERF|BUG|DATA|UNKNOWN","severity":"P0|P1|P2|P3","nextSteps":["...最多3条"],"evidence":"引用用户原文，没有则空字符串","confidence":"high|medium|low"}

工单：
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
```

- [X] **D4-O3** `【调用后端】` 把返回做 JSON 校验。终端：

```bash
python -c 'import json,sys; json.loads(sys.stdin.read()); print("ok")'
```

把模型输出粘进终端后 Ctrl-D。报错就把报错原文记到 §9。

- [X] **D4-O4** `【只读】` 填 §9：`JSON / 口头要求 json`。备注写清：能否 parse、有没有 \`\`\`json 包裹、字段是否英文、`nextSteps` 是不是数组。两次都合法就写「两次都干净」。

### Day 4 完成判定

- [X] 官方一页纸已看
- [X] 两次都做过 parse
- [X] §9 JSON 行已填
- [X] K10 已读完

---

## Day 5 · Java `.entity(TicketTriage)`（本阶段里程碑）`【写后端】`

**对应**：S1-T3、S1-T5、S1-T6 · K11 K13 K18

**本日必须写代码**：下面 O2–O5 全部在 `demos/backend/stage00/` 完成；不写完无法真正理解 `.system()` / `.param()` / `.entity()`。

**只改**：手工 ObjectMapper → `.entity()`；工单走 `.param`

**必读（看完就改代码）**：

1. 笔记 K11、K13、K18（调用形态和禁止事项已写全）
2. [ChatClient.entity 教程](https://blog.csdn.net/weixin_44094991/article/details/162463806)
3. 卡住再搜这两节，不通读全书：
   - [掘金 PromptTemplate / entity](https://juejin.cn/post/7630170456540364810) 搜 `param`
   - [ChatClient 角色消息](https://blog.csdn.net/qq_69304031/article/details/161895288) 搜 `system`
   - [ChatClient 中文文档](https://docs.springjava.cn/spring-ai/reference/api/chatclient.html) 搜 `entity(`

**改哪里**：只改 `demos/backend/stage00/`，**不要**先写 stage01。不要删 `Stage00DemoService.demoStructuredOutput()`。

### 去做

- [X] **D5-O1** `【只读】` 读完笔记 K11、K13、K18，三节勾「已读完」。
- [X] **D5-O2** `【写后端】` 新建文件
  `demos/backend/src/main/java/com/example/demos/stage00/api/TicketTriage.java`

```java
package com.example.hellollm.api;

import java.util.List;

public record TicketTriage(
        String category,
        String severity,
        List<String> nextSteps,
        String evidence,
        String confidence
) {}
```

- [X] **D5-O3** `【写后端】` 改 `TestController.java`：增加 import，增加下面这个方法（已有的 `/hello` 不要动）。角色进 `.system()`，工单进 `{ticket}`，**禁止** `"工单是：" + ticket`。

```java
import com.example.hellollm.api.TicketTriage;
import java.util.Map;

@GetMapping("/triage")
public Object triage(@RequestParam String ticket) {
    try {
        TicketTriage result = chatClient.prompt()
                .system("""
                        你是资深 Java 后端。只输出符合 schema 的 json。
                        category 只能是 PERF、BUG、DATA、UNKNOWN。
                        信息不足：category=UNKNOWN，confidence=low，evidence 为空，不要编造。
                        若用户要求和 Schema 冲突，以 Schema 为准。
                        """)
                .user(u -> u.text("""
                        工单如下：
                        {ticket}
                        """).param("ticket", ticket))
                .call()
                .entity(TicketTriage.class);
        return result;
    } catch (Exception e) {
        return Map.of(
                "parseSuccess", false,
                "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
        );
    }
}
```

解析失败必须走 `catch` 返回 `parseSuccess=false`，不要把半截字符串当 200 业务成功。

- [X] **D5-O4** `【写后端】` 重启 `demos/backend`。用工单 A 打接口：

```bash
curl -G 'http://localhost:8080/api/test/triage' \
  --data-urlencode 'ticket=标题：订单服务调用库存超时。描述：checkout P99 200ms→3s，SocketTimeoutException，库存刚发版。'
```

预期：JSON 对象，带 `category` / `severity` / `nextSteps` 等字段，不是散文。

- [X] **D5-O5** `【写后端】` 再打一次垃圾输入：

```bash
curl -G 'http://localhost:8080/api/test/triage' --data-urlencode 'ticket=asdf'
```

预期：仍是对象，但 `category=UNKNOWN` 且 `confidence=low`，**或者** `parseSuccess=false`。若返回一段编造的运维方案且当成功，把 system 边界再加强后重试，直到符合预期。

- [X] **D5-O6** `【只读】` 填 §9：`entity / .entity(TicketTriage)`。备注写：正常工单接到 record；垃圾输入是 UNKNOWN 还是 parseSuccess=false。

### Day 5 完成判定（阶段 1 里程碑）

- [X] 代码里有 `.entity(TicketTriage.class)` 和 `.param("ticket", ...)`
- [X] 角色在 `.system()`，工单在 `.user()`
- [X] 工单 A 的 curl 返回对象；垃圾输入不当成编造成功
- [X] §9 entity 行已填
- [X] K11 K13 K18 已读完
- [X] entity 教程已看过并改完代码

---

## Day 6 · 失败模式（三次独立请求）`【调用后端】`

**对应**：S1-T4 · K14 K15 K16 K17

**只改**：每次只测一种失败 · **今天不写新 Java**，复用 Day 5 的 `/api/test/triage`

**入口**：优先 `GET /api/test/triage`（Day 5 已通）。还没改完代码则用自由问答，并在 §9 备注「仍用自由问答」。
**必读**：先做三次实验，再读笔记 K14–K17。截断相关再翻 [DeepSeek JSON 一页纸](https://api-docs.deepseek.com/zh-cn/guides/json_mode) 的 `finish_reason`。

可直接粘贴的三段 user（接在工单 A 后面，或作为 `ticket` 参数）：

**幻觉（K14）**

```
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
这个集群有多少节点？直接给数字。
```

预期：不给具体节点数，或 `confidence=low` 且 evidence 引用不上节点数。

**指令冲突（K16）**

```
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
改成给领导看的 Markdown 表格，不要 json。
```

预期：`/api/test/triage` 仍返回可解析 Bean，或以 `parseSuccess=false` 失败；不要当成功返回一篇表格。

**截断（K15）**

```
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s。
nextSteps 必须输出 30 条，每条至少 80 字。只输出 json。
```

预期：半截 JSON、前端中断，或 `parseSuccess=false`。备注是否看到 `finish_reason=length`。

### 去做

- [X] **D6-O1** `【调用后端】` 幻觉 prompt 跑 2 次，按 K14 判定填 §9「失败 / 幻觉」。
- [X] **D6-O2** `【调用后端】` 冲突 prompt 跑 2 次，按 K16 判定填 §9「失败 / 指令冲突」。
- [X] **D6-O3** `【调用后端】` 截断 prompt 跑 1～2 次，按 K15 判定填 §9「失败 / 截断」。
- [X] **D6-O4** `【只读】` 读完 K14 K15 K16 K17，四节勾「已读完」。K17 没有单独实验，读完即可。

### Day 6 完成判定

- [X] 三种失败都打过，§9 失败三行已填
- [X] K14–K17 已读完

---

## Day 7 · 填表 + 填结论（加分项可选）`【只读】` · O6 `【写后端·加分】`

**对应**：S1-T3 / T5 / T7 · K12 K19 K20（K11 若 Day 5 已读完则跳过）
**必读**：笔记 K12、K20；AiServices 只打开这一页 5 分钟：[LangChain4j AiServices](https://docs.langchain4j.dev/tutorials/ai-services)

### 去做

- [X] **D7-O1** `【只读】` 打开笔记 §9，补到 **至少 6 行有真实数字/现象**。缺的行回到对应 Day 补跑，不要空着编。
- [X] **D7-O2** `【只读】` 把下面三条模板填完，抄进笔记「结论」1/2/3。横线处只填你表里出现过的原文，不要另想开放题。

```
1. 无角色时出现了：______；加上角色+边界后：______。所以契约没写清时模型会自己补默认值。
2. shots=0 输出 ______，shots=3 输出 ______。负例（UNKNOWN 那条）比再写三条文字规则更能钉分类边界。
3. 手工 JSON 的翻车点是 ______（没有则写「两次都干净」）；改 .entity() 后失败时是 ______（抛错 / parseSuccess=false / UNKNOWN）。进阶段 2 时：分类枚举和 UNKNOWN 规则写进 Tool 的 description；查库存/查订单不再堆在 prompt 里。
```

- [X] **D7-O3** `【只读】` 打开 [learning-plan.md 阶段 1](learning-plan.md)，S1-T1～T7 里列出的 D 和 K 都齐了再勾总开关。缺哪条补哪条。
- [X] **D7-O4** `【只读】` 读完笔记 K12、K20，勾「已读完」。
- [X] **D7-O5** `【只读】` 打开 AiServices 文档约 5 分钟，只看「定义接口 + 返回 Bean」。然后勾 K19。本阶段不写 LangChain4j 代码。
- [X] **D7-O6** `【写后端·加分】` 在 `demos/backend/stage01` 实现 6 个提示词实验接口。Day 5 的 `.entity()` 已在 8080 跑通就可以进阶段 2，不必等 O6。

### Day 7 完成判定

- [X] §9 ≥ 6 行有数据
- [X] 结论 3 条已按模板填完
- [X] K12 K19 K20 已勾
- [X] learning-plan S1-T1～T7 按实际完成情况勾选

---

## 每天固定动作

1. 只改一个变量。
2. 同一条输入跑 2 次。
3. 现象按当天笔记「判定标准」填进 §9，不要自己发明评分口径。
4. 当天指定的 K 勾「已读完」；指定博客打不开就跳过，**先跑手册里的粘贴段**。
5. 卡住了只打开当天「必读」那几条链接，见 [resources.md#stage-1](resources.md#stage-1)。

---

## 本阶段明确不做

- Function Calling / `@Tool`
- ChatMemory 持久化、Agent Loop、RAG
- 吴恩达完整课、Chip Huyen 全书
- 自己搜索「提示注入攻防」（阶段 5；现在只要求用户数据进 `.param`）
