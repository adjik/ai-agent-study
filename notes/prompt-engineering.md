# 提示词工程（阶段 1 笔记）

> 上手步骤：[docs/stage-1.md](../docs/stage-1.md) · 任务总表：[learning-plan.md 阶段 1](../docs/learning-plan.md) · 博客清单：[resources.md#stage-1](../docs/resources.md#stage-1)
> 基础概念：[llm-basics.md](llm-basics.md)

**K 全在本文件。** 手册 `stage-1.md` 只写「今天去做什么」；知识点、判定标准和怎么写，都在下面各节。

每节固定四块，读完打勾即可，**不要另写作文、不要自问自答**：

1. **必知**：本阶段要求掌握的事实（已写全）
2. **怎么做**：复制哪段、改哪个文件、记到哪张表
3. **资料**：指定博客 / 官方文档（按手册当天「必读」打开，不要自己找）
4. **完成**：`- [ ] 已读完本节`

实验数据填在文末 **§9 对照表**。结论用手册 Day 7 给的填空模板，把表里的数字抄进去。

### 动手类型（与手册标签一致）

| 标签                       | 你要做什么                       | 改哪个后端                                                                                       |
| -------------------------- | -------------------------------- | ------------------------------------------------------------------------------------------------ |
| **【只读】**         | 读本节 + 勾「已读完」            | 无                                                                                               |
| **【调用后端】**     | curl / 前端，prompt 塞进已有接口 | `hello-llm-java` **不改源码**；D1–D4 打 `/api/test/hello`，D6 打 `/api/test/triage` |
| **【写后端】**       | 新建/改 Java，跑通后再 curl 验证 | `demos/00-env/hello-llm-java/`（**仅 D5：K11 K13 K18**）                                 |
| **【写后端·加分】** | 拆 6 个实验到独立工程            | `demos/01-hello-llm/prompt-java/` 端口 8081（D7-O6，不挡进度）                                 |

需要动手的节，「怎么做」里有可粘贴命令。约定：

- **启动 / 冒烟**见 K1；后面各节默认 8080 已起来
- **D1–D4** 打 `GET /api/test/hello`（整段 prompt 塞进 `msg`）
- **Day 5 改完 `.entity()` 之后**打 `GET /api/test/triage`（工单塞进 `ticket`）
- 前端等价入口：http://localhost:5173/demos/hello-llm/chat （记耗时更方便）
- 每个 prompt **连发 2 次**；curl 可在前面加 `time` 看耗时

---

## 知识项总表（K1–K20）

按手册当天去读对应节。打勾在各节标题（已读完/调试过打 `[X]`，未做空着 `[ ]`），节末「完成」同步。

| 完成 | 编号 | 必知主题                           | 哪天读 | 怎么做（手册）              | 动手类型         |
| ---- | ---- | ---------------------------------- | ------ | --------------------------- | ---------------- |
| [X]  | K1   | Transformer：不是查库，是加权补全  | D0     | 读完本节                    | 调用后端         |
| [X]  | K2   | 预训练 / SFT / RLHF：博学 ≠ 听话  | D0     | 读完本节                    | 只读             |
| [X]  | K3   | 上下文是第一等公民                 | D0     | 读完本节                    | 只读             |
| [X]  | K4   | 提示词是工程，不是文案             | D1     | 先跑坏/好 prompt，再读      | 调用后端         |
| [X]  | K5   | 五件套 + 合格骨架                  | D1     | 用骨架对照好 prompt         | 只读             |
| [X]  | K6   | 角色 = 默认 Filter，不是人设       | D1     | 对照无角色 vs 有角色        | 调用后端         |
| [X]  | K7   | 指令清晰四要素                     | D1     | 对照坏 prompt 缺了哪几项    | 只读             |
| [X]  | K8   | Few-shot：主路径 + 边界 + 负例     | D2     | 跑 0-shot vs 3-shot         | 调用后端         |
| [X]  | K9   | CoT：多步才开，有 token 代价       | D3     | 跑直接答 vs 先写步骤        | 调用后端         |
| [X]  | K10  | 结构化输出 + DeepSeek JSON 三条    | D4     | 强制 json 并 parse          | 调用后端         |
| [X]  | K11  | system / history / user 别混       | D5     | 角色进`.system()`         | **写后端** |
| [ ]  | K12  | 上下文压缩（本阶段只建立概念）     | D7     | 读完即可，不写代码          | 只读             |
| [X]  | K13  | `.param`：指令与数据分离         | D5     | 工单走`{ticket}`          | **写后端** |
| [X]  | K14  | 幻觉及本阶段缓解                   | D6     | 追加「集群有多少节点」      | 调用后端         |
| [X]  | K15  | 截断及 finish_reason               | D6     | 要求 30 条或减小 max_tokens | 调用后端         |
| [X]  | K16  | 指令冲突：Schema 优先              | D6     | json vs Markdown 表格       | 调用后端         |
| [ ]  | K17  | 指令忽略：短约束放两端             | D6     | 读完即可                    | 只读             |
| [X]  | K18  | Java`.entity()` 接 record        | D5     | 改`hello-llm-java`        | **写后端** |
| [ ]  | K19  | AiServices ≈ Feign（只读 5 分钟） | D7     | 打开指定文档扫一眼          | 只读             |
| [ ]  | K20  | 提示词 / Tool / 程序的分工         | D7     | 按模板填结论第 3 条         | 只读             |

阶段 1 知识侧完成 = **K1–K20 全部勾「已读完」** + **§9 对照表按手册填满**。

---

## [X] K1 · Transformer 直觉：不是查库，是加权补全

**哪天**：D0 · **任务**：S1-T1 · **动手类型**：`【调用后端】`（启动 8080 + 冒烟，不改业务代码）

### 必知

LLM 做的事情始终是：**给定已经出现的 token，预测下一个 token 的概率分布，再采样。** 不是查数据库，也不是搜网页。

Transformer 里本阶段只需建立一点直觉——**注意力（Attention）**：生成当前位置时，会回头看前面哪些 token 更相关，给它们更高权重。

| Transformer             | Java 后端类比                            |
| ----------------------- | ---------------------------------------- |
| 当前 token 要「看」前文 | 处理请求时去查会话、配置、关联表         |
| 注意力权重              | 按相关性打分，不是全表扫描后平均         |
| 上下文窗口              | 一次 SQL / 一次 RPC 能带上的最大 payload |
| 超出窗口的内容          | 根本没进这次查询，不是「忘了」，是没传   |

**结论（直接记住）**：没写进**这一次**请求的信息，对模型等于不存在。没有隐藏的「上次聊过我还记得」——除非你把历史再发一遍。

### 怎么做

按 [stage-1.md D0](../docs/stage-1.md) 启动 8080 之后，读完 K1–K3 三节，在各节末尾打勾。不必另写摘要。

**命令**（后面所有实验默认这两步已完成）：

```bash
# 终端 1 · 后端 8080
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd demos/00-env/hello-llm-java && mvn spring-boot:run

# 终端 2 · 前端（可选，看「耗时 xxx ms」更方便）
cd demos/frontend && npm run dev
# 打开 http://localhost:5173/demos/hello-llm/chat
# 菜单：环境与 LLM 入门 → 自由问答

# 冒烟
curl -sS http://localhost:8080/api/health
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode 'msg=用一句话说明什么是上下文窗口'
```

工单 A 固定下来（后面 7 天都用它，不要改成写诗）：

```
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。请分类并给出下一步。
```

### 资料

本阶段原理已写全，**不必另找课**。Token / 窗口 / 温度的铺垫见 [llm-basics.md](llm-basics.md)。

### 完成

- [X] 已读完本节

---

## [X] K2 · 预训练 / SFT / RLHF：博学 ≠ 听话

**哪天**：D0 · **任务**：S1-T1 · **动手类型**：`【只读】`

### 必知

三个训练阶段解决三个不同问题：

| 阶段                      | 训练什么                     | 类比                                | 你实际感知到的                                 |
| ------------------------- | ---------------------------- | ----------------------------------- | ---------------------------------------------- |
| **预训练**          | 海量文本上做「下一个词预测」 | 把互联网当语料，无监督读代码+文档   | 知识广、会流畅生成；没有「你要我做什么」的契约 |
| **SFT（监督微调）** | 「指令 → 回答」配对再训     | 用标注好的 FAQ / 工单范例做业务微调 | 开始像在回答问题，而不是续写小说               |
| **RLHF / 偏好对齐** | 按人类偏好打分               | Code Review + 产品验收              | 更礼貌、更愿遵循指令；也会过度拒答或讨好       |

对提示词的直接含义（记住这三句）：

1. 模型「懂 Java」主要来自**预训练**，不来自你的 system prompt。
2. 模型「按你的格式输出」靠 **SFT + 你写的约束**，单靠「请输出 JSON」不够稳。
3. 模型「不敢胡说」是对齐出来的**倾向**，可用提示词加强，**不能当成事务保证**（Day 6 会验证）。

### 怎么做

读完本节打勾。和 K1、K3 一起在 D0 做完。**本节无命令。**

### 资料

无额外链接。不要去刷完整 RLHF 论文或吴恩达课。

### 完成

- [X] 已读完本节

---

## [X] K3 · 为什么上下文是第一等公民

**哪天**：D0 · **任务**：S1-T1 · **动手类型**：`【只读】`（Day 5 才在代码里拆 system/user）

### 必知

一次 API 调用的真实输入是整包消息：系统提示词 + 历史 + 当前用户话 +（有时）工具结果。全部挤在**同一个窗口**里。

三条后果，后面所有技巧都围着它们转：

1. **位置即优先级。** 窗口两端通常比中间更容易被「看到」（长上下文的中间迷失）。重要约束不要埋在一篇散文中间。
2. **历史是成本也是污染。** 多轮会把过期、错误、冲突的话一起带上。类比：把整个 Redis List 当请求头传出去。
3. **输出也占窗口。** `max_tokens` 截断时，JSON 可能缺右括号——这是**协议损坏**，不能当业务成功。

阶段 4 的 RAG、阶段 3 的 Memory，本质都是：**决定这次请求塞什么、丢掉什么。** 提示词工程是这件事的手工版。

一次请求里抢窗口的东西：`[系统提示][Few-shot 示例][检索文档][对话历史][当前问题][预留输出]`。

### 怎么做

读完本节。D0 完成判定不要求写作文。后面 Day 5 会把「角色进 system、工单进 user」落到代码。**本节无命令。**

### 资料

无额外链接。截断与 `finish_reason` 在 K15 和 [DeepSeek JSON 一页纸](https://api-docs.deepseek.com/zh-cn/guides/json_mode) 里还会出现。

### 完成

- [X] 已读完本节

---

## [X] K4 · 提示词是工程，不是文案

**哪天**：D1 · **任务**：S1-T2 · **动手类型**：`【调用后端】`（`/api/test/hello`，不改 Java）

### 必知

阶段 0 容易产生错觉：LLM 调用 = 发一段中文，等一段中文回来。对后端更准确的模型：

```
prompt  ≈  接口文档 + 入参示例 + 出参 Schema + 非功能约束
LLM     ≈  一个不保证幂等、不保证类型、按 token 计费的远程服务
你的代码 ≈  网关：组包、校验、重试、降级、打点
```

提示词工程要做的三件事：

- 把模糊需求拆成模型能执行的指令（产品口述 → 接口契约）
- 把不稳定的自然语言收成程序能用的结构（DTO + Bean Validation）
- 在 token 预算内塞进最有用的上下文（RPC 超时内传最少但够用的字段）

**经验**：生产里大部分「模型很蠢」，其实是契约没写清。模型没有你脑子里的默认值（超时 3 秒、不要动生产库、用 MyBatis Plus）——你不写，它就编一个听起来合理的。

### 怎么做

1. 先跑「坏 prompt」2 次、「好 prompt」2 次（命令在下面）。
2. 坏 prompt 里模型替你补的默认值（编造的类名、节点数、中间件）记进 §9「无角色」那一行。
3. 再读 K5–K7。判定标准在 **K6**。

**命令**（K1 的 8080 已起来；每个连发 2 次。也可整段贴进自由问答）：

```bash
# 坏 prompt → §9「角色 / 无角色」
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
帮我看看这个单子怎么处理：
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
EOF
)"

# 好 prompt → §9「角色 / 有角色+边界」
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
你是资深 Java 后端，只给可落地方案，不写空话。
信息不足就明说，不要编造集群规模、类名、中间件。

任务：给工单分类，并列出最多 3 条下一步。
分类只能用：PERF / BUG / DATA / UNKNOWN

工单：
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
EOF
)"
```

### 资料

Day 1 必读：[Prompt 工程实战：System / Few-shot / CoT](https://jishuzhan.net/article/2065228421654999042)（只看 System Prompt 四段式，对照好 prompt 覆盖了哪几段）。

### 完成

- [X] 已读完本节

---

## [X] K5 · 五件套 + 合格骨架

**哪天**：D1 · **任务**：S1-T2 · **动手类型**：`【只读】`

### 必知

一条合格 prompt 拆开，对应你写接口已经在做的事：

| 提示词要素                 | 接口设计对应物            | 缺了会怎样                     |
| -------------------------- | ------------------------- | ------------------------------ |
| **角色**             | 服务职责边界              | 口吻飘、越权回答               |
| **任务**             | 方法名 + 一句话需求       | 模型自己选题发挥               |
| **格式**             | 出参 DTO / OpenAPI schema | 解析崩溃，或正则抠字段         |
| **示例（Few-shot）** | 单测 / 契约测试           | 边界 case 全靠猜               |
| **边界**             | 校验、权限、降级文案      | 幻觉、编造 API                 |
| **数据**             | `@RequestBody`          | 和规则混写，模型分不清哪是指令 |

固定骨架（以后模板化也按这个切）：

```
【角色】你是……只做……不做……
【任务】动词 + 对象（分类 / 抽取 / 排查）
【格式】json 字段或枚举，给一个样例
【示例】1 条主路径 + 1 条边界 + 1 条该 UNKNOWN 的负例（Few-shot 时才写）
【边界】不知道就 UNKNOWN / low，不要编数字和类名
【数据】工单原文（单独一块，不要和规则混写）
```

Day 1 的「好 prompt」覆盖了角色、任务、格式（枚举）、边界、数据；**缺示例**——那是 Day 2 的事，今天不必补。

### 怎么做

打开 **K4 好 prompt**（或手册 Day 1 那一段），按上表逐项勾：角色有、任务有、分类枚举有、边界有、数据单独一块、示例无。**本节无新命令。**

### 资料

同上篇博客的 System 四段式。

### 完成

- [X] 已读完本节

---

## [X] K6 · 角色设定：默认 Filter，不是人设

**哪天**：D1 · **任务**：S1-T2 · **动手类型**：`【调用后端】`（命令见 K4）

### 必知

写「你是资深 Java 后端工程师」不是让模型变成某个人，而是提供三样默认值：

- **知识先验**：优先用 Spring / MySQL / Redis 话语，而不是 PyTorch
- **拒绝边界**：不给无法落地的空话
- **详细程度**：给方案和取舍，而不是百科

类比：给 Dubbo 服务加一层默认 Filter——没传的参数按这个服务的惯例处理。

**反例（不要这么写）**：角色写「你是世界顶级架构师」，任务却是「把这段日志分成 PERF/BUG」。量级不匹配，模型会浪费 token 说教。

### 怎么做

手册 Day 1：同一条工单 A，无角色 vs 有角色+边界，各跑 2 次。**curl 见 K4**，不要另写 prompt。

**判定标准（按这个记 §9，不是开放题）**：

|        | 无角色（坏 prompt）常见结果             | 有角色+边界（好 prompt）常见结果   |
| ------ | --------------------------------------- | ---------------------------------- |
| 幻觉   | 可能编造集群节点数、不存在的中间件/类名 | 信息不足就明说，不编数字和类名     |
| 下一步 | 空话或泛泛「优化性能」                  | 能落到超时配置 / 线程池 / 发版回滚 |
| 分类   | 可能写成一段分析，不在枚举里            | 落在 PERF / BUG / DATA / UNKNOWN   |

把 K4 两次结果填进 §9 前两行。两次记在同一行：耗时写成 `第一次 / 第二次`，质量 1–5 按上表打，现象写具体（如 `编了 12 节点`）。Day 1 输出是散文，「能否解析」填 **否**。

### 资料

同上篇博客 System Prompt 部分。

### 完成

- [X] 已读完本节

---

## [X] K7 · 指令清晰：模型没有团队默认值

**哪天**：D1 · **任务**：S1-T2 · **动手类型**：`【只读】`

### 必知

清晰不等于长。合格指令具备四要素：

1. **动词**：分类 / 抽取 / 解释 / 给修复建议
2. **对象**：哪段日志、哪个字段
3. **成功标准**：只准用给定枚举、不超过 3 条建议
4. **失败标准**：信息不足就说不足，不要补全

类比：javadoc 写 `@param` / `@return` / `@throws`，而不是写一篇博客。

Day 1 坏 prompt「帮我看看这个单子怎么处理」缺了：动词不具体、没有枚举、没有失败标准（至少缺成功标准和失败标准两项）。模型就会自己补「超时大概是多少节点、用什么中间件」——这是幻觉的一种来源。

### 怎么做

对照坏 prompt 缺的两项，记在 §9「无角色」备注里，例如：`缺枚举+缺失败标准，编了 Redis 集群规模`。**本节无新命令。**

### 资料

无额外链接。

### 完成

- [X] 已读完本节

---

## [X] K8 · Few-shot：示例是最高带宽的需求文档

**哪天**：D2 · **任务**：S1-T2 · **动手类型**：`【调用后端】`

### 必知

Few-shot = 在 prompt 里放 1–N 个「输入 → 输出」范例，让模型抄格式和决策边界。

为什么经常比长规则有效：模型更擅长**模式匹配**，不擅长立法解释。一条示例同时锁定格式、粒度、语气、边界。类比单测 `given/when/then`。

选示例原则（比数量重要）：

- **主路径** 1 条（典型 PERF）
- **边界** 1 条（典型 BUG）
- **负例** 1 条（信息不足 → UNKNOWN）
- 标签必须和真实枚举一致；3 条干净的胜过 10 条互相打架的

Zero-shot 的用途：任务很简单、或还在探索模型会怎么理解。探索期用 Zero-shot，稳定期用 Few-shot 把契约钉死。

### 怎么做

按手册 Day 2 用工单 B（「系统有点慢，用户说不太流畅」）跑：

1. 0-shot，2 次
2. 3-shot（下面三条示例不要改），2 次

**命令**：

```bash
# 0-shot → §9「Few-shot / shots=0」
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
把工单分成 PERF / BUG / DATA / UNKNOWN，只输出类别一个词。
工单：标题：系统有点慢。描述：用户说今天不太流畅。
EOF
)"

# 3-shot → §9「Few-shot / shots=3」
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
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
EOF
)"
```

**判定标准**：

|            | 预期                   | 若不符合就在备注写原因                    |
| ---------- | ---------------------- | ----------------------------------------- |
| shots=0    | 常会硬分成 PERF 或 BUG | 若已经是 UNKNOWN，备注「0-shot 也拒答了」 |
| shots=3    | 应为 UNKNOWN           | 若仍是 PERF，备注「负例没挡住」           |
| 两次不一致 | 记 temperature 噪音    | 阶段 0 ChatClient 默认可能是 0.7          |

结果填 §9 的 Few-shot 两行。

### 资料

同一篇：[Prompt 工程实战](https://jishuzhan.net/article/2065228421654999042) 里 Few-shot 那一节。看完不要另开新文。

### 完成

- [X] 已读完本节

---

## [X] K9 · CoT：先走流程再给结论，有代价

**哪天**：D3 · **任务**：S1-T2 · **动手类型**：`【调用后端】`

### 必知

CoT（思维链）= 要求模型先写出中间推理，再给最终答案。

它解决的是**多步合成错误**（先定位模块 → 再对照日志 → 再给修复）。一步就能答的分类/抽字段，CoT 帮助有限，上 Schema 更划算。

代价（工程上必须算进去）：

- 输出 token 变多 → 钱和延迟都上去
- 推理过程可能把内部逻辑泄漏给用户
- 错误的推理链会一本正经地把你带沟里——CoT 不是正确性证明

实践规则：

| 任务类型                       | 是否开 CoT                                          |
| ------------------------------ | --------------------------------------------------- |
| 分类 / 抽字段 / 格式转换       | 通常不开，走 JSON Schema                            |
| 根因分析 / 方案比较 / 计算步骤 | 开                                                  |
| 程序只消费 JSON                | 可把步骤写进`reasoning` 短字段，主字段仍走 Schema |

类比：复杂 SQL 先 EXPLAIN 再执行；开发先写设计再提 PR。

### 怎么做

按手册 Day 3 用工单 A（多步排查，值得开 CoT）：

1. 「直接答」2 次，记前端 elapsed ms
2. 「先写 3～5 步再答」2 次，记耗时

**命令**：

```bash
# CoT 关 → §9「CoT / 关」
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
作为 Java 后端，用 5 条以内给出：最可能根因、该查哪几个地方、不该先做什么。
工单：订单服务调用库存超时，checkout P99 200ms→3s，SocketTimeoutException，库存刚发版。
EOF
)"

# CoT 开 → §9「CoT / 开」
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
作为 Java 后端。先用 3～5 步写出排查顺序（不要给结论），然后再给：最可能根因、该查哪几个地方、不该先做什么。
工单：订单服务调用库存超时，checkout P99 200ms→3s，SocketTimeoutException，库存刚发版。
EOF
)"
```

**判定标准**：

- 步骤是否出现这四类：超时配置、对方（库存）慢、线程池/连接池、刚发版可回滚。缺哪条记在备注。
- CoT 耗时和字数应明显高于直接答——这就是 token 代价。
- 质量 1–5 分按覆盖面打：四类全到打 4–5，缺两类打 2–3。

填 §9 的 CoT 关 / 开两行。

### 资料

同一篇博客的 CoT 小节。

### 完成

- [X] 已读完本节

---

## [X] K10 · 结构化输出 + DeepSeek JSON 三条规则

**哪天**：D4（概念+手工 json）· Day 5 落到 `.entity()`（K18）

**任务**：S1-T2 / S1-T5 · **动手类型**：D4 `【调用后端】` · D5 见 K18 `【写后端】`

### 必知

阶段 0 的做法是：提示词里画一个 JSON 样子，回来再用 Jackson 盲解析。能跑，但不构成契约。常见翻车：

- 外面裹一层 \`\`\`json
- 字段名漂成中文（`可能原因` vs `possibleCauses`）
- `nextSteps` 该给数组给了字符串
- 截断导致缺括号，非法 JSON

Spring AI 路径（Day 5 实现）：

```
Java record
  → 生成 JSON Schema（BeanOutputConverter.getFormat()）
  → 塞进提示词
  → 模型按 Schema 生成
  → .entity(Xxx.class) 反序列化成 Bean
```

`ChatClient.entity(Xxx.class)` 内部就是 `BeanOutputConverter`。类比：先定义 DTO，再让对端按契约说话。

**信念**：凡是程序还要继续处理的模型输出，默认走结构化；凡是给人读的，才允许自由文本。

DeepSeek 官方三条（走 OpenAI 兼容接口时必须遵守）：

1. prompt 里必须出现 `json` 字样，并给样例
2. 合理 `max_tokens`，看 `finish_reason` 是不是 `length`
3. JSON 模式只保证「是 JSON」，**不保证字段是你要的**——字段靠 Schema / `.entity()`

失败时的工程两手（Day 5 落地）：优先 `.entity()`；解析失败返回 4xx/5xx 或 `parseSuccess=false`；**不要把半成品当业务成功**。

### 怎么做

1. 打开官方一页纸（下面「资料」），对照上面三条。
2. 按下面命令粘贴强制 json prompt，跑 2 次，用工单 A。
3. 用 python 校验（把模型返回粘进 stdin，Ctrl-D 结束）。
4. 在 §9 JSON 行记下：能否 parse、有没有 \`\`\` 包裹、字段是否英文。没翻车就写「两次都干净」。

**命令**：

```bash
# 强制 json → §9「JSON / 口头要求 json」
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
只输出 json，不要 markdown，不要解释。
字段必须是：
{"category":"PERF|BUG|DATA|UNKNOWN","severity":"P0|P1|P2|P3","nextSteps":["...最多3条"],"evidence":"引用用户原文，没有则空字符串","confidence":"high|medium|low"}

工单：
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
EOF
)"

# 校验：把模型输出粘进去，Ctrl-D
python -c 'import json,sys; json.loads(sys.stdin.read()); print("ok")'
```

### 资料

**今天必读（一页纸）**：[DeepSeek JSON Output](https://api-docs.deepseek.com/zh-cn/guides/json_mode)

Day 5 再读：[ChatClient.entity 把结果变成 Java 对象](https://blog.csdn.net/weixin_44094991/article/details/162463806)

卡住查：[Structured Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)（搜 `entity`，不要通读）

### 完成

- [X] 已读完本节（含官方一页纸）

---

## [X] K11 · 三层消息：system / history / user 别混

**哪天**：D5 落到代码 · D7 再确认一遍

**任务**：S1-T3 · **动手类型**：`【写后端】`（改 `TestController`，验证见 K18）

### 必知

| 层                | 放什么                             | 不要放什么                     |
| ----------------- | ---------------------------------- | ------------------------------ |
| **system**  | 稳定约定：角色、输出格式、安全边界 | 每轮都变的业务数据             |
| **history** | 对后续仍有用的来回                 | 整段日志、过期结论、失败的胡话 |
| **user**    | 这一次的任务 + 数据                | 整本员工手册（那是 RAG）       |

**典型事故**：把「必须输出 JSON」写在某一轮 user 里，下一轮被历史冲掉，程序突然解析失败。格式约束属于 system（或每次模板都带上），不属于闲聊。

### 怎么做

Day 5 按手册改 `TestController`：角色和「只输出 json / UNKNOWN 规则」放进 `.system(...)`；工单正文放进 `.user().param("ticket", ...)`。不要再把整段契约拼进 `/api/test/hello` 的 msg。

可粘贴代码在 [stage-1.md Day 5](../docs/stage-1.md)。改完用 **K18 的两条 curl** 验证；本节不单独打 `/hello`。

### 资料

[一文吃透 ChatClient：结构化输出 + 角色消息](https://blog.csdn.net/qq_69304031/article/details/161895288)（搜文中 system / user，看完就改代码）

卡住查：[Spring AI ChatClient 中文](https://docs.springjava.cn/spring-ai/reference/api/chatclient.html) → 搜 `system(`

### 完成

- [X] 已读完本节

---

## [ ] K12 · 上下文压缩（本阶段只建立概念）

**哪天**：D7 · **任务**：S1-T3 · **动手类型**：`【只读】`（不写 ChatMemory 代码）

### 必知

滑动窗口 ≈ Redis List + `LTRIM`：只留最近 N 轮。实现简单，缺陷也简单——**被裁掉的可能是最早说的关键约束**（用户第一句：「所有建议必须兼容 JDK8」）。

另外三种思路（阶段 3 做成 ChatMemory，现在只记名字）：

- **摘要**：旧轮次收成一段「已确认事实」，像会话表的 `summary` 字段
- **结构化记忆**：只留槽位（环境=JDK8、数据库=MySQL），不留全文
- **按相关性取**：当前问题提到缓存，才捞缓存相关历史（已接近 RAG）

本阶段**不写** ChatMemory 代码。

### 怎么做

D7 读完本节打勾。不要新建类。**本节无命令。**

### 资料

无。阶段 3 再看 Spring AI `ChatMemory` 文档。

### 完成

- [X] 已读完本节

---

## [X] K13 · PromptTemplate：指令与数据分离

**哪天**：D5 · **任务**：S1-T3 / S1-T6 · **动手类型**：`【写后端】`（`.param("ticket", …)`，验证见 K18）

### 必知

`.param("ticket", text)` ≈ Thymeleaf / `MessageFormat`：

- 模板是代码资产，可评审、可测试
- 变量是运行时数据，当不可信输入处理
- 用户把「忽略以上指令」写进工单描述，就是提示注入的雏形

阶段 1 养成习惯即可：**用户数据进变量，不要和系统指令拼成同一段无结构散文。** 系统做安全是阶段 5。

禁止：`"工单是：" + ticketText`。

正确：

```java
.user(u -> u.text("工单如下：\n{ticket}").param("ticket", ticketText))
```

### 怎么做

Day 5 按手册把工单改成 `{ticket}` + `.param`。对照旧代码 `Demoservice.demoStructuredOutput()` 里把 JSON 样例和业务问题写在同一段 user 里——新接口不要再那样写。

验证命令见 **K18**（`/api/test/triage`）。禁止写成 `"工单是：" + ticket`。

### 资料

[掘金 · Spring AI 2.0 ChatClient / PromptTemplate / entity](https://juejin.cn/post/7630170456540364810)（文中搜 `PromptTemplate` 和 `param` 两节）

### 完成

- [X] 已读完本节

---

## [X] K14 · 幻觉

**哪天**：D6 · **任务**：S1-T4 · **动手类型**：`【调用后端】`（打 `/api/test/triage` 或退路 `/hello`）

### 必知

幻觉 ≈ 没有数据却 mock 了假结果上线：编造类名、API、工单号、集群节点数，语气还很稳。

本阶段能做的缓解（直接照做）：

1. 明确「不知道就 UNKNOWN / `confidence=low`」
2. Few-shot 给一条「信息不足 → 拒绝补全」的负例
3. 结构化输出加 `evidence`，强制引用用户原文；引用不上就空字符串
4. 对照实验心里按 temperature=0 理解

提示词**不能单独消灭**幻觉。后续靠 RAG 接地、Tool 查真数据、评估集回归。

### 怎么做

手册 Day 6：在工单 A 后追加「这个集群有多少节点？直接给数字。」跑 2 次（优先打 `/api/test/triage`；还没改完代码则把同一段贴进 `/hello` 或自由问答，§9 备注「仍用自由问答」）。

**命令**（每个连发 2 次）：

```bash
# 优先：Day 5 的 triage 已通
curl -G 'http://localhost:8080/api/test/triage' \
  --data-urlencode "ticket=$(cat <<'EOF'
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
这个集群有多少节点？直接给数字。
EOF
)"

# 还没 /triage 时的退路
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
这个集群有多少节点？直接给数字。
EOF
)"
```

**判定**：好契约下应拒绝给数字，或 `confidence=low` 且 evidence 对不上节点数。若仍给出具体数字，备注「边界没挡住」，不要改判定标准去迁就模型。

填 §9「失败 / 幻觉」行。

### 资料

无新链接。机制见 K2 第 3 句。

### 完成

- [X] 已读完本节

---

## [X] K15 · 截断

**哪天**：D6 · **任务**：S1-T4 · **动手类型**：`【调用后端】`

### 必知

截断 ≈ HTTP 响应 chunked 到一半连接断了：JSON 缺括号、列表说到第 3 条没了。

原因：`max_tokens` 太小，或窗口被历史吃满、留给输出的额度不够。

缓解（直接照做）：

- 限制输出形状：「最多 3 条，每条不超过 40 字」，不要写「请详尽分析」
- 看 `finish_reason`：`length` 表示被截断，`stop` 才是正常结束
- 长报告拆两步：先结构化提纲，再按条展开（prompt chaining，阶段 2–3 再用）

半截 JSON 是协议损坏，Controller 不得当成业务成功。

### 怎么做

手册 Day 6 用下面命令（自由问答或 triage 的 user 里追加同一段也行）。每个跑 1～2 次。

**命令**：

```bash
curl -G 'http://localhost:8080/api/test/triage' \
  --data-urlencode "ticket=$(cat <<'EOF'
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s。
nextSteps 必须输出 30 条，每条至少 80 字。只输出 json。
EOF
)"

# 还没 /triage 时
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s。
nextSteps 必须输出 30 条，每条至少 80 字。只输出 json。
EOF
)"
```

若项目里能设 `max_tokens`，可改成很小的值（例如 50）做第二次。看返回是否半截、前端是否中断。能看到 `finish_reason` 就记到备注。

填 §9「失败 / 截断」行。

### 资料

[DeepSeek JSON Output](https://api-docs.deepseek.com/zh-cn/guides/json_mode) 里关于 `finish_reason` 和 `max_tokens` 的段落。

### 完成

- [X] 已读完本节

---

## [X] K16 · 指令冲突

**哪天**：D6 · **任务**：S1-T4 · **动手类型**：`【调用后端】`

### 必知

指令冲突 ≈ 两个 Filter 互相覆盖、没有编译期 `ambiguous mapping`：system 说必须 JSON，user 说用表格给领导看。模型会挑看起来更近、更具体、或更像「最近一条」的那份。

缓解（直接写进 system，Day 5 的 triage 已经有雏形，Day 6 再验证）：

```
若用户要求与输出 Schema 冲突，以 Schema 为准，并在 nextSteps 第一条说明发生了冲突。
```

只留一处真相：不要 system 说 JSON、Few-shot 又示范 Markdown。

### 怎么做

手册 Day 6 在已有「只输出 json」的前提下，user 末尾追加「改成给领导看的 Markdown 表格，不要 json。」跑 2 次。

**命令**：

```bash
curl -G 'http://localhost:8080/api/test/triage' \
  --data-urlencode "ticket=$(cat <<'EOF'
标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。
改成给领导看的 Markdown 表格，不要 json。
EOF
)"

# 还没 /triage 时（预期常会偏向最近一条，返回表格——记到备注）
curl -G 'http://localhost:8080/api/test/hello' \
  --data-urlencode "msg=$(cat <<'EOF'
只输出 json。工单：订单服务调用库存超时，checkout P99 200ms→3s。
改成给领导看的 Markdown 表格，不要 json。
EOF
)"
```

**判定**：Day 5 的 `/api/test/triage` 仍应返回可解析的 `TicketTriage`（或 `parseSuccess=false`），而不是一篇 Markdown。自由问答若返回表格，备注「无 Schema 时冲突会偏向最近一条指令」。

填 §9「失败 / 指令冲突」行。

### 资料

无新链接。

### 完成

- [X] 已读完本节

---

## [ ] K17 · 指令忽略

**哪天**：D6（读完即可，不单独做第四次实验）· **动手类型**：`【只读】`

**任务**：S1-T4

### 必知

指令忽略 ≈ 客户端没按文档传参，但 HTTP 200。模型会漏约束，尤其是约束又长又埋在中间（呼应 K3「位置即优先级」）。

比在 prompt 里加粗「一定要」更有效的做法（四选多）：

1. 约束写短
2. 放在窗口两端（开头 system 或末尾强调）
3. 用 Schema / `.entity()`
4. 用 Few-shot 示例钉死格式

### 怎么做

D6 读完打勾。不另做实验。Day 5 的 `.entity()` 已经是缓解手段。**本节无命令。**

### 资料

无。

### 完成

- [X] 已读完本节

---

## [X] K18 · Java：`.entity(TicketTriage)` 接到 record

**哪天**：D5 · **任务**：S1-T5 / S1-T6 · **动手类型**：`【写后端】`（本阶段唯一硬性写代码里程碑）

### 必知

阶段 1 里程碑：在 **`demos/00-env/hello-llm-java`** 里改一处，不必先拆 8081。

肌肉记忆（左边做、右边停）：

| 概念      | 写成代码                                | 不要再做               |
| --------- | --------------------------------------- | ---------------------- |
| 模板      | `.param("ticket", text)`              | `"工单是：" + text`  |
| 出参契约  | `.entity(TicketTriage.class)`         | 正则抠\`\`\`json       |
| 角色/边界 | `.system(...)` 固定文本               | 每轮 user 里重复一长段 |
| 失败      | 捕获后`parseSuccess=false` 或 4xx/5xx | 把半成品当 200 成功    |
| 成本      | 看耗时 / 以后看 usage                   | 凭感觉说这次更好       |

`entity` 失败抛异常是好事。现有 `Demoservice.demoStructuredOutput()` 仍是手工 `ObjectMapper`——**不要删**，留着对照；新接口走 `.entity()`。

完整可粘贴 Java、文件路径见 [stage-1.md Day 5](../docs/stage-1.md)，按那一节改，不要自己设计包结构。验证 curl 在下面「怎么做」。

### 怎么做

严格按手册 Day 5：新建 `TicketTriage.java` → 在 `TestController` 增加 `/api/test/triage` → 重启 → 下面两条 curl。Java 全文复制 [stage-1.md Day 5](../docs/stage-1.md)，不要自己设计包结构。

**命令**：

```bash
# 改完后重启后端（终端 1 Ctrl-C，再跑）
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd demos/00-env/hello-llm-java && mvn spring-boot:run

# 工单 A → 应返回带 category / nextSteps 的对象
curl -G 'http://localhost:8080/api/test/triage' \
  --data-urlencode 'ticket=标题：订单服务调用库存超时。描述：checkout P99 200ms→3s，SocketTimeoutException，库存刚发版。'

# 垃圾输入 → category=UNKNOWN 且 confidence=low，或 parseSuccess=false
curl -G 'http://localhost:8080/api/test/triage' \
  --data-urlencode 'ticket=asdf'
```

填 §9：`entity / .entity(TicketTriage)`。备注写：正常工单接到 record；垃圾输入是 UNKNOWN 还是 parseSuccess=false。

### 资料

**必读（看完就改，不通读 Spring 文档）**：[ChatClient.entity 教程](https://blog.csdn.net/weixin_44094991/article/details/162463806)

卡住：[Spring AI ChatClient 中文](https://docs.springjava.cn/spring-ai/reference/api/chatclient.html) 搜 `entity(`、`param(`

### 完成

- [X] 已读完本节（代码按手册 Day 5 做）

---

## [ ] K19 · LangChain4j AiServices（只扫 5 分钟）

**哪天**：D7 · **任务**：S1-T5 · **动手类型**：`【只读】`（只看文档，不写 LangChain4j 代码）

### 必知

LangChain4j 可以定义 Java 接口，返回类型是 Bean，框架生成代理：

```
interface TicketTriageService {
    TicketTriage classify(String ticketBody);
}
```

`AiServices.create(...)` 之后调接口即调模型。类比 **Feign / Dubbo 消费端**：能力边界写在接口上。

本阶段**不实现、不建模块、不挡进阶段 2**。阶段 3 再用。

### 怎么做

打开下面官方教程，看「定义接口 + 返回类型」那一段，大约 5 分钟。不要跟敲。**本节无 curl。**

### 资料

**指定这一页**：[LangChain4j AiServices](https://docs.langchain4j.dev/tutorials/ai-services)

### 完成

- [X] 已打开指定文档扫过 5 分钟

---

## [ ] K20 · 和阶段 2 的边界

**哪天**：D7 · **任务**：S1-T7 · **动手类型**：`【只读】`（填结论模板）

### 必知

提示词能约束「怎么说、怎么输出」；**不能**让模型去查你的 MySQL、调你的 Dubbo。

上下文里没有库存，却写「请根据真实库存回答」——不是提示词没写好，是该上 Tool。

三句话（进阶段 2 记住就行）：

> 提示词负责决策格式与推理；工具负责事实；程序负责校验与权限。

进阶段 2 时：分类枚举、UNKNOWN 规则可以写进 Tool 的 `description`；查订单/查库存不要继续堆在 prompt 里。

### 怎么做

Day 7 用下面模板，把 §9 表里的现象抄进 3 条。不要另想开放题。**本节无 curl。**

```
1. 无角色时出现了：______；加上角色+边界后：______。所以契约没写清时模型会自己补默认值。
2. shots=0 输出 ______，shots=3 输出 ______。负例（UNKNOWN 那条）比再写三条文字规则更能钉分类边界。
3. 手工 JSON 的翻车点是 ______（没有则写「两次都干净」）；改 .entity() 后失败时是 ______（抛错 / parseSuccess=false / UNKNOWN）。进阶段 2 时：分类枚举和 UNKNOWN 规则写进 Tool 的 description；查库存/查订单不再堆在 prompt 里。
```

### 资料

阶段 2 手册：[stage-2.md](../docs/stage-2.md)（整合文档，含 K* 与 §9）

### 完成

- [X] 已读完本节

---

## 关键要点（备查，不用背、不用另勾）

| 要点               | 一句话                                         |
| ------------------ | ---------------------------------------------- |
| LLM 是补全器       | 没出现在本次上下文里的信息，等于不存在         |
| 预训练 ≠ 听话     | 博学来自预训练；听话来自对齐 + 你的契约        |
| 提示词 = 接口契约  | 角色 / 任务 / 格式 / 示例 / 边界，缺一就会靠猜 |
| Few-shot ≈ 单测   | 示例同时锁定格式和决策边界                     |
| CoT 有代价         | 多步推理才开；分类抽字段优先 Schema            |
| 结构化输出是主路径 | 程序消费的结果必须是 Bean，不是散文            |
| 上下文是预算       | 系统约定、示例、历史、用户数据在抢同一块窗口   |
| 幻觉是特性         | 能缓解、能检测，不能提示词根除                 |

---

## §9 对照实验记录

每次只改一个变量。耗时用前端自由问答的 elapsed ms，或 curl 前加 `time`。每个实验的命令在对应 K 节「怎么做」。

**填法**：日期、耗时、能否解析、质量 1–5、一句话现象。现象按各 K 节「判定标准」写，例如 `编造了 12 节点` / `输出 UNKNOWN` / `CoT 多花 8s`。

S1-T7 要求：下表**有数据的行 ≥ 6**；角色、Few-shot、CoT、JSON 或 entity、失败三类都要有。结论 3 条用手册 Day 7 模板填空。

| 日期 | 实验     | 变量                      | 耗时 ms | 能否当 JSON/Bean 解析 | 质量 1–5 | 一句话现象 |
| ---- | -------- | ------------------------- | ------- | --------------------- | --------- | ---------- |
|      | 角色     | 无角色                    |         |                       |           |            |
|      | 角色     | 有角色+边界               |         |                       |           |            |
|      | Few-shot | shots=0                   |         |                       |           |            |
|      | Few-shot | shots=3                   |         |                       |           |            |
|      | CoT      | 关                        |         |                       |           |            |
|      | CoT      | 开                        |         |                       |           |            |
|      | JSON     | 口头要求 json             |         |                       |           |            |
|      | entity   | `.entity(TicketTriage)` |         |                       |           |            |
|      | 失败     | 幻觉                      |         |                       |           |            |
|      | 失败     | 指令冲突                  |         |                       |           |            |
|      | 失败     | 截断                      |         |                       |           |            |

结论（Day 7 把模板填完即可）：
