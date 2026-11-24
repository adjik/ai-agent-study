# RAG 与记忆（阶段 4 笔记）

> 上手步骤：[docs/stage-4.md](../docs/stage-4.md) · 任务总表：[learning-plan.md 阶段 4](../docs/learning-plan.md)
> 基础回顾：[llm-basics.md](llm-basics.md)（Token / 上下文窗口）· 前置阶段：[stage-3.md](../docs/stage-3.md)（窗口记忆、仓库分层）

**K 全在本文件。** 手册 `stage-4.md` 只写「今天去做什么」；知识点、判定标准和怎么写，都在下面各节。

每节固定四块，读完打勾即可，**不要另写作文、不要自问自答**：

1. **必知**：本阶段要求掌握的事实（已写全）
2. **怎么做**：复制哪段、改哪个文件、记到哪张表
3. **资料**：指定博客 / 官方文档（按手册当天「今天读」打开，不要自己找）
4. **完成**：`- [ ] 已读完本节`

实验数据填在文末 **§9 对照表**。结论用 `stage-4.md` 第 14 天给的填空模板，把表里的数字抄进去。

### 动手类型（与手册标签一致）

| 标签               | 你要做什么                       | 改哪个后端                                                      |
| ------------------ | -------------------------------- | --------------------------------------------------------------- |
| **【不写代码】**   | 读本节 + 建夹具 + 勾「已读完」   | 不改 Java（第 1 天要建四份 md 和评估文件）                      |
| **【写代码】**     | 新建/改 Java，跑通后按手册验收   | `demos/backend/src/.../stage04`（**D2–D12**）                  |
| **【调用后端】**   | curl / 前端，只改参数不改源码    | **不改 Java**；D13 改 `topK` / `chunkSize` 属于改配置重跑     |

需要动手的节，「怎么做」里给可粘贴命令。约定：

- **启动**见 `stage-4.md` 文首；后面各节默认 8080 已起来
- 文档夹具在 `demos/04-rag/data/`，评估集在 `demos/04-rag/eval/questions.jsonl`
- 前端入口：`http://localhost:5173/demos/rag/*`（接通前一律用 curl）
- 每个检索实验**至少跑两组参数**，把分数和命中文件一起记

---

## 知识项总表（K1–K28）

| 完成 | 编号 | 必知主题                                   | 哪天读 | 怎么做（手册）              | 动手类型     |
| ---- | ---- | ------------------------------------------ | ------ | --------------------------- | ------------ |
| [ ]  | K1   | 事实不在参数里：RAG 解决什么               | D1     | 先问再手贴资料对照          | 不写代码     |
| [ ]  | K2   | RAG 七步链路总览                           | D1     | 读完本节                    | 不写代码     |
| [ ]  | K3   | Embedding：文本到向量                     | D2     | 跑 similarity 接口           | 写代码       |
| [ ]  | K4   | 余弦相似度与 1.0 / 0.8 / 0.3 三个锚点      | D2     | 三组句子对照                | 写代码       |
| [ ]  | K5   | Embedding 模型选型：本地 ONNX vs 云端     | D2     | 读 pom 依赖与模型卡         | 写代码       |
| [ ]  | K6   | 切块：块大小 / 重叠 / 按段落               | D3     | 三种参数对照                | 写代码       |
| [ ]  | K7   | 切块丢上下文与 Contextual Retrieval        | D3     | 读完本节                    | 不写代码     |
| [ ]  | K8   | 入库与 top-k 检索                          | D4     | 跑 retrieve 接口             | 写代码       |
| [ ]  | K9   | minScore 是硬门槛；向量库永远返回「最像」 | D4     | 问库里没有的问题            | 写代码       |
| [ ]  | K10  | 向量索引：FT.CREATE / DIM / 距离度量      | D5     | 看 FT.INFO                   | 写代码       |
| [ ]  | K11  | 策略与仓库分层（≈ ChatMemory 分层）       | D5     | 关掉 Redis 看故障表现       | 写代码       |
| [ ]  | K12  | Retriever / Aggregator / Injector / Augmentor | D6 | 装 DefaultRetrievalAugmentor | 写代码     |
| [ ]  | K13  | 提示词规则与「查不到」兜底                 | D6     | 问库里没有的问题            | 写代码       |
| [ ]  | K14  | 元数据过滤 = 行级权限，过滤在检索前        | D7     | dept=all / tech 对照        | 写代码       |
| [ ]  | K15  | Spring AI 映射：VectorStore + RAG Advisor | D8     | 跑 query-springai            | 写代码       |
| [ ]  | K16  | 铁律：查询与入库必须同一 embedding 模型    | D9     | 故意换维度再修              | 写代码       |
| [ ]  | K17  | Recall / Precision / MRR 各管什么          | D10    | 跑 eval 三组 topK           | 写代码       |
| [ ]  | K18  | 评估集怎么造                               | D10    | 看 questions.jsonl          | 写代码       |
| [ ]  | K19  | BM25 与向量互补                            | D11    | 跑 strategy=hybrid           | 写代码       |
| [ ]  | K20  | RRF：按排名倒数融合                        | D11    | 读三行融合代码              | 写代码       |
| [ ]  | K21  | Rerank 只改排序，不改召回                  | D11    | 跑 hybrid+rerank             | 写代码       |
| [ ]  | K22  | 短期窗口与长期向量记忆是两层               | D12    | 两个 conversationId 对照   | 写代码       |
| [ ]  | K23  | 记忆仓库：进程内存 / Redis                 | D12    | 重启后端再问                | 写代码       |
| [ ]  | K24  | RAG 的成本构成                             | D13    | 读定价页 + 记 promptChars   | 调用后端     |
| [ ]  | K25  | topK / chunk / promptChars 与钱             | D13    | 填成本表四行                | 调用后端     |
| [ ]  | K26  | Java 类比：向量库≈ES、chunk≈分表、评估≈搜索指标 | D14 | 读完本节              | 不写代码     |
| [ ]  | K27  | 与阶段 5 / 6 的边界                        | D14    | 读完本节                    | 不写代码     |
| [ ]  | K28  | 常见失败模式与定位顺序                     | D14    | 读完本节                    | 不写代码     |

阶段 4 知识侧完成 = **K1–K28 全部勾「已读完」** + **§9 对照表按手册填满** + **三句话填空写完**。

---

## [ ] K1 · 事实不在参数里：RAG 解决什么

**哪天**：D1 · **任务**：S4-T3 · **动手类型**：`【不写代码】`

### 必知

LLM 的参数是训练时冻住的。你公司的报销标准、运维手册、合同模板，**训练语料里没有**，所以模型只能按「一般公司通常这样」来编。这不是模型坏了，是它没有这份资料。

三条路能给它补资料：

| 办法                 | 代价                                   | 什么时候用                     |
| -------------------- | -------------------------------------- | ------------------------------ |
| 微调（fine-tune）    | 要训练数据、要 GPU、改一次要重训       | 改**说话风格**，不是改事实     |
| 塞进提示词（长上下文） | 每次请求都带全量文档，token 直接爆炸 | 文档总量小于几万字             |
| **RAG（检索后塞）**  | 多一次检索，文档可以到百万级           | **默认选这个**                 |

RAG 的核心动作只有一句：**先找到相关的那几段，再把它们放进提示词**。第 1 天手贴参考那段对照，就是在人工做这件事。

### 怎么做

按 `stage-4.md` 第 1 天执行计划跑两条 curl：一次不给资料（模型会编数字），一次把 `travel-expense.md` 的住宿标准那段贴进 `question`（回答出现 600）。把两次回答的第一句记进 §9 第 1 行，标注「无检索 / 手贴参考」。

### 资料

- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval) —— 开篇讲为什么朴素切块会丢信息
- [Lilian Weng：LLM Powered Autonomous Agents](https://lilianweng.github.io/posts/2023-06-23-agent/) 的 Memory 一节

### 完成

- [ ] 已读完本节

---

## [ ] K2 · RAG 七步链路总览

**哪天**：D1 · **任务**：S4-T3 · **动手类型**：`【不写代码】`

### 必知

后面 12 天都在填这张表。**先记住七步的顺序和归属**，写代码时才知道当前在填哪一格：

| 步 | 做什么                       | 什么时候发生   | LangChain4j 对应                                   | 本阶段哪天 |
| -- | ---------------------------- | -------------- | -------------------------------------------------- | ---------- |
| 1  | 加载文档                     | 入库时一次     | `FileSystemDocumentLoader`                       | D3         |
| 2  | 切块                         | 入库时一次     | `DocumentSplitters.recursive`                     | D3         |
| 3  | 每块算向量                   | 入库时一次     | `EmbeddingModel.embed`                            | D2 / D4    |
| 4  | 存进向量库                   | 入库时一次     | `EmbeddingStore.add`                              | D4 / D5    |
| 5  | 问题算向量，查 top-k         | **每次提问**   | `EmbeddingStore.search`                           | D4         |
| 6  | 把块拼成参考资料             | **每次提问**   | `ContentInjector`                                 | D6         |
| 7  | 带参考资料生成答案           | **每次提问**   | `ChatModel` / `AiServices`                        | D6         |

关键分界：**1–4 是一次性离线任务，5–7 是每次请求都要跑的在线路径**。搞错这个分界，就会出现「每次提问都重新入库」这种性能灾难。

### 怎么做

把这张表抄进你自己的笔记（或在 `stage-4.md` 上做标记），写代码时每天在右上角标「今天填第 N 步」。

### 资料

- [LangChain4j：RAG](https://docs.langchain4j.dev/tutorials/rag) 开头的流水线图

### 完成

- [ ] 已读完本节

---

## [ ] K3 · Embedding：文本到向量

**哪天**：D2 · **任务**：S4-T1 · **动手类型**：`【写代码】`

### 必知

Embedding 是把一段文本映射成**固定长度的一串小数**。它有三个性质，你只需要这三个：

1. **定长**：不管输入 5 个字还是 500 个字，输出都是 512 个数（本阶段的模型）
2. **语义相近则方向相近**：意思接近的两句，向量在空间里指得差不多
3. **不能反解**：拿到向量还原不出原文，所以它适合做索引，不适合当存储

| Embedding                | Java 后端类比                                  |
| ------------------------ | ---------------------------------------------- |
| 文本 → 512 维向量        | 文本 → 一个定长的 hash / 特征向量（但保语义）   |
| 语义相近则向量相近       | 相似用户在特征空间里距离近                     |
| 向量入库做近邻检索       | 建索引，按距离查候选                           |

**生成模型（DeepSeek）和 embedding 模型是两个东西。** DeepSeek 没有 embeddings 接口（实测 `/embeddings` 返回 404），所以本阶段生成用 DeepSeek、向量化用本地 ONNX 模型。**这不是妥协，是常见架构**——很多系统的生成和检索本来就用不同厂商。

### 怎么做

跑 `POST /api/stage/04/embedding/similarity`，传两句意思相同、字面不同的话，看 `dimension=512` 和 `cosine≈0.7~0.9`。再换主题完全不同的一对，看 `cosine` 掉到 0.2~0.4。两个数字记进 §9。

### 资料

- [LangChain4j：Embedding Models](https://docs.langchain4j.dev/tutorials/embedding-models)

### 完成

- [ ] 已读完本节

---

## [ ] K4 · 余弦相似度与三个锚点

**哪天**：D2 · **任务**：S4-T1 · **动手类型**：`【写代码】`

### 必知

余弦相似度衡量两个向量的夹角，值域 −1 ~ 1，实际文本检索里几乎总在 0 ~ 1。公式只有一行：

```text
cos(a, b) = (a · b) / (|a| × |b|)
```

**记住三个锚点，比记公式有用**：

| 分数区间   | 含义                     | 怎么用                             |
| ---------- | ------------------------ | ---------------------------------- |
| `1.0`      | 同一段文本               | 自相似上界，用来验证实现没写错     |
| `0.7 ~ 0.9`| 语义相同、字面不同       | 这是「命中」的常见区间             |
| `< 0.3`    | 基本无关                 | 这个区间出现，说明检索没找到东西   |

**分数不能跨模型比。** 换成另一个 embedding 模型，同样的两句话可能得 0.5 或 0.95。所以 `minScore` 的阈值只能在**当前模型 + 当前数据**上量出来（第 10 天做），抄别人的阈值一定翻车。

### 怎么做

三组对照：同义句（0.7~0.9）、无关句（0.2~0.4）、同一句（1.0）。三组分数写进 §9。第 6 天设 `minScore` 时回来看这张表。

### 资料

- [LangChain4j：Embedding Models](https://docs.langchain4j.dev/tutorials/embedding-models)

### 完成

- [ ] 已读完本节

---

## [ ] K5 · Embedding 模型选型：本地 ONNX vs 云端

**哪天**：D2 · **任务**：S4-T1 · **动手类型**：`【写代码】`

### 必知

选 embedding 模型看五件事：**语言**（中文优先）、**维度**（影响存储和速度）、**质量**（检索准不准）、**部署方式**（本地 / 云端）、**成本**。

本阶段的选择：

```xml
<dependency>
  <groupId>dev.langchain4j</groupId>
  <artifactId>langchain4j-embeddings-bge-small-zh-v15-q</artifactId>
  <version>1.12.1-beta21</version>
</dependency>
```

```java
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;
EmbeddingModel embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();  // 512 维
```

| 选择                | 优点                                     | 缺点                             |
| ------------------- | ---------------------------------------- | -------------------------------- |
| 本地 ONNX（本阶段） | 免 key、免网络、模型打进 jar、0 成本     | 质量弱于云端大模型；占 CPU/内存  |
| 云端 embedding API  | 质量好、维度可选、免运维                 | 多一个 key、按量计费、有网络延迟 |
| 自建推理服务        | 质量和成本都可控                         | 要部署维护，本阶段不值得         |

**维度是硬约束**：向量库索引建的时候写了 512，之后所有入库和查询都必须是 512 维。换模型 = 换维度 = 重建索引（第 9 天）。

### 怎么做

打开 `demos/backend/pom.xml` 确认依赖和版本；在 `EmbeddingConfig` 里 new 一次，全阶段共用一个 `@Bean`。**不要在每个类里各 new 一个**——那会加载多份模型文件。

### 资料

- [BAAI/bge-small-zh-v1.5 模型卡](https://huggingface.co/BAAI/bge-small-zh-v1.5)（只读简介表）
- [LangChain4j：Embedding Models](https://docs.langchain4j.dev/tutorials/embedding-models)

### 完成

- [ ] 已读完本节

---

## [ ] K6 · 切块：块大小 / 重叠 / 按段落

**哪天**：D3 · **任务**：S4-T1 · **动手类型**：`【写代码】`

### 必知

**块是检索的最小单位，也是引用溯源的最小单位。** 切法决定检索质量，而且事后调参救不回来。

三个旋钮：

| 旋钮         | 小了会怎样                       | 大了会怎样                         |
| ------------ | -------------------------------- | ---------------------------------- |
| `chunkSize`  | 一句规则被劈开，块里没有完整答案 | 一块混多个主题，相似度被稀释；贵   |
| `overlap`    | 跨块的信息断掉                   | 重复内容多，存储和 token 都浪费    |
| 切分依据     | 按字符切会切在词中间             | 按段落切尊重作者，但长度不可控     |

`DocumentSplitters.recursive(maxSize, overlap)` 的做法是**逐级退让**：先按段落切，某段还是超长就按句子切，还超长就按字符切。所以它比纯按字符切安全。

**中文的坑**：`chunkSize` 在 LangChain4j 里是 **token 数**（不是字符数）。中文 1 字 ≈ 1.5~2 token，所以 300 token 大约是 150~200 个汉字。

| 切块          | Java 后端类比                          |
| ------------- | -------------------------------------- |
| 按段落切      | 按业务单元分表，一个单元一件事          |
| 重叠 overlap  | 分页查询带一点重叠，避免边界数据丢失    |
| 递归退让      | 先按大粒度拆，拆不动再降级             |

### 怎么做

跑 `POST /api/stage/04/split` 三组：`300/30 recursive`、`100/0 recursive`、`300/60 paragraph`。每组记 `totalChunks`、最短块 / 最长块的 `chars`，并挑一个 `tail` 是半句话的块记下来（证明切坏了会怎样）。三组记进 §9。

### 资料

- [LangChain4j：RAG · Document Splitter](https://docs.langchain4j.dev/tutorials/rag)

### 完成

- [ ] 已读完本节

---

## [ ] K7 · 切块丢上下文与 Contextual Retrieval

**哪天**：D3 · **任务**：S4-T1 · **动手类型**：`【不写代码】`

### 必知

一块正文可能长这样：

> 每人每晚不超过 600 元，凭发票实报实销。

单看这块，**不知道它是哪个制度、哪个城市档位**。如果问题问「一线城市住宿标准」，这块的向量可能匹配不上，因为「一线城市」这几个字在上一块里。

Anthropic 的 Contextual Retrieval 给的办法很简单：**入库前给每块前面补一句它属于哪份文档、哪一节**，再拿去算向量：

```text
【差旅报销制度 · 二、住宿标准】每人每晚不超过 600 元，凭发票实报实销。
```

代价是入库时每块多一次 LLM 调用（或者用规则拼标题，本阶段用规则拼）。收益是检索准确率明显上升。

**本阶段怎么做**：不加 LLM，**用规则拼**——切块时把最近的 `##` 标题拼到块前面。第 10 天的评估会体现这个差别（如果时间够，可以试一次带标题 vs 不带标题）。

### 怎么做

读完本节，在 `notes/rag.md` 空白处记一句：「本阶段用规则拼标题替代 LLM 生成上下文」。第 10 天评估失败样例时，如果发现「块内容对但检索不到」，优先怀疑这里。

### 资料

- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)

### 完成

- [ ] 已读完本节

---

## [ ] K8 · 入库与 top-k 检索

**哪天**：D4 · **任务**：S4-T2 / S4-T3 · **动手类型**：`【写代码】`

### 必知

入库和检索是**同一件事的两个方向**：

```java
// 入库：向量 + 原文一起存
store.add(embeddingModel.embed(segment).content(), segment);

// 检索：问题算向量，找最近的 k 条
EmbeddingSearchRequest req = EmbeddingSearchRequest.builder()
        .queryEmbedding(embeddingModel.embed(question).content())
        .maxResults(3)
        .minScore(0.0)
        .build();
List<EmbeddingMatch<TextSegment>> matches = store.search(req).matches();
```

`EmbeddingMatch` 给你三样：`score()`（相似度）、`embedded()`（原文 + 元数据）、`embeddingId()`（库里的主键）。**元数据是跟着块一起存的**，第 7 天的权限过滤就靠它。

| 检索             | Java 后端类比                              |
| ---------------- | ------------------------------------------ |
| `add(向量, 原文)` | 写一条记录（主键 = 向量 id，值 = 块）     |
| `search(问题向量, k)` | 按索引查 top-k，等价于 `ORDER BY distance LIMIT k` |
| `score`          | 相似度，不是距离；越大越像                 |

### 怎么做

先跑内存版（`InMemoryEmbeddingStore`）的 `POST /api/stage/04/retrieve`，看一眼 `rank=1` 的块里有没有「600」。**这一步只验检索，不看生成**——检索不通，后面全白做。

### 资料

- [LangChain4j：RAG · Embedding Store / Content Retriever](https://docs.langchain4j.dev/tutorials/rag)

### 完成

- [ ] 已读完本节

---

## [ ] K9 · minScore 是硬门槛；向量库永远返回「最像」

**哪天**：D4 · **任务**：S4-T2 · **动手类型**：`【写代码】`

### 必知

**向量检索没有「找不到」这个结果。** 你问「公司年会在哪办」，库里没有一条相关内容，它照样返回 top-k——只是分数都低（0.3~0.45）。

这一点和 SQL 完全不同：

| 场景           | SQL                              | 向量检索                          |
| -------------- | -------------------------------- | --------------------------------- |
| 没有匹配       | 返回 0 行                        | **返回 k 行，分数很低**           |
| 门槛           | `WHERE` 精确条件                | `minScore` 相似度下限            |
| 谁负责「没有」 | 数据库                           | **你的应用**（minScore + 提示词） |

所以「查不到」要两层兜底：`minScore` 挡掉明显无关的块，提示词规则让模型在没资料时说「查不到」。

`minScore` 的两个副作用：

- **设太高**：明明库里有，也检索不到（假阴性）
- **设太低**：无关块进提示词，模型硬答（假阳性）

**阈值只能在当前数据上量**（第 10 天），不能抄。

### 怎么做

按手册第 4 天第 4 步问「公司年会在哪办」，看返回 3 条低分块。再把 `minScore` 调到 0.8 看返回空。两处现象记进 §9 的「兜底」行。

### 资料

- [LangChain4j：RAG](https://docs.langchain4j.dev/tutorials/rag)

### 完成

- [ ] 已读完本节

---

## [ ] K10 · 向量索引：FT.CREATE / DIM / 距离度量（路线 A）与本地索引（路线 B）

**哪天**：D5 · **任务**：S4-T2 · **动手类型**：`【写代码】`

### 必知

先看你走哪条路（判定方法见 `stage-4.md` 文首第 2 节）：

- **路线 A**（Redis 带 RediSearch + RedisJSON）：向量真正存在 Redis，索引要建
- **路线 B**（模块不全）：向量写在进程内，用 `serializeToFile` 落盘；**这一节前半部分只读，不落地**，但要能讲清「服务端索引多了什么」

**路线 A：Redis 需要建索引。** `RedisEmbeddingStore` 第一次 `add()` 时会自动 `FT.CREATE`，但它写库用的是 `JSON.SET`、建索引用的是 `ON JSON`——**所以需要 RediSearch 和 RedisJSON 两个模块**：

```text
FT.CREATE rag-idx ON JSON PREFIX 1 rag:
  SCHEMA
    $.vector AS vector VECTOR HNSW 6
      TYPE FLOAT32
      DIM 512
      DISTANCE_METRIC COSINE
    $.file AS file TAG
    $.dept AS dept TAG
    $.chunk AS chunk NUMERIC
```

逐个字段：

| 字段             | 作用                                                   | 改错的后果                       |
| ---------------- | ------------------------------------------------------ | -------------------------------- |
| `DIM 512`        | 向量维度，**必须等于 embedding 模型输出维度**          | 维度不符，检索直接报错           |
| `DISTANCE_METRIC COSINE` | 用余弦距离；`L2` / `IP` 是另外两种            | 换了度量，分数含义全变           |
| `HNSW`           | 近似最近邻算法，快但可能有漏；`FLAT` 是暴力全扫，准但慢 | 数据量小时两者差别可忽略         |
| `TAG` 字段       | 可做等值 / 集合过滤（`dept in [all, tech]`）          | **没声明就不能过滤**（第 7 天）  |
| `NUMERIC` 字段   | 可做范围过滤（比如按时间）                              | 同上                             |

**关键**：`metadataKeys(...)` 里没写的字段，**不能用于过滤**。第 7 天要过滤 `dept`，第 5 天就要把它声明进去，否则要重建索引。

| 向量索引         | Java 后端类比                        |
| ---------------- | ------------------------------------ |
| HNSW             | 跳表 / B+ 树的近似版，牺牲一点精度换速度 |
| FLAT             | 全表扫，小数据够用                   |
| TAG 字段         | 联合索引里的等值列                   |

**路线 B：索引只是一个本地文件。** 没有服务端索引，`InMemoryEmbeddingStore` 在内存里维护一个 `CopyOnWriteArrayList`，检索时逐条算相似度（等于 `FLAT` 全扫）。它的持久化只有两个方法：

```java
store.serializeToFile(Path.of("demos/04-rag/vector-data/index.json"));  // 写
InMemoryEmbeddingStore<TextSegment> store =
        InMemoryEmbeddingStore.fromFile(Path.of("demos/04-rag/vector-data/index.json")); // 读
```

| 对比项         | 路线 A（Redis 索引）             | 路线 B（本地文件）             |
| -------------- | -------------------------------- | ------------------------------ |
| 存哪           | Redis 服务端                     | 进程内存 + 一个 JSON 文件      |
| 多进程共享     | 可以                             | **不行**（各进程各一份）       |
| 重启后         | 自动还在                         | 要 `fromFile` 显式加载         |
| 检索复杂度     | HNSW 近似，百万级也快            | 全扫，几万条就明显变慢         |
| 元数据过滤     | 索引层过滤（TAG）                | 内存里逐条判断                 |
| 依赖           | RediSearch + RedisJSON           | 无                             |

**路线 B 丢的就是「多进程共享 + 服务端索引」这两件事**，其余能力（检索、过滤、评估）完全一样。这也正是为什么后面第 7、10、11 天两条路都照做。

### 怎么做

**路线 A**：`redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning FT.INFO rag-idx`，逐项确认 `DIM`、`DISTANCE_METRIC`、以及 `file/dept/chunk` 三个字段都在。把 `FT.INFO` 里 `num_docs` 和块数对一遍（应该相等）。

**路线 B**：`ls -lh demos/04-rag/vector-data/index.json`，确认文件存在且大小随块数增长；再重启后端打一次检索，确认结果不变（说明 `fromFile` 生效）。

### 资料

- [Redis：FT.CREATE](https://redis.io/docs/latest/commands/ft.create/)
- [Redis：Vector similarity search](https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/)

### 完成

- [ ] 已读完本节

---

## [ ] K11 · 策略与仓库分层（≈ ChatMemory 分层）

**哪天**：D5 · **任务**：S4-T2 · **动手类型**：`【写代码】`

### 必知

第 4 天用内存库、第 5 天换 Redis，**业务代码没改一行**，只换了 `EmbeddingStore` 的实现。这和阶段 3 的 `ChatMemory` / `ChatMemoryRepository` 是同一组分层：

| 层       | 管什么                             | 阶段 3（记忆）            | 阶段 4（RAG）              |
| -------- | ---------------------------------- | ------------------------- | -------------------------- |
| **策略** | 留多少、怎么排序、怎么切           | `MessageWindowChatMemory` | 切块器 + top-k + filter    |
| **仓库** | 存在哪                             | `InMemoryChatMemoryRepository` / Redis | `InMemoryEmbeddingStore`（+ 文件落盘）/ `RedisEmbeddingStore` |

两条推论：

1. **换仓库不影响效果**：同一份数据、同一套策略，两条路的检索结果应该一致（第 5 天验证）
2. **仓库会引入新故障**：外部依赖（Redis）挂了，整条检索断掉；本地文件路线没有这个故障源，但换了「多进程不能共享」

所以「学习用内存，上线用外部仓库」不是偷懒，是分阶段降低复杂度。

**内网 Redis 特有的三种失败**，你以前搭 Dubbo / DB 也遇到过：

| 现象                         | 原因                                   | 怎么区分                       |
| ---------------------------- | -------------------------------------- | ------------------------------ |
| 启动就失败，连接超时         | 网段不通 / 需要 VPN / IP 白名单没加     | `telnet host port` 或 `nc -vz` |
| 能连上但认证失败             | 密码错 / ACL 用户没有权限              | 错误信息里有 `WRONGPASS` / `NOPERM` |
| 能连但命令报 unknown command | 模块缺失（RediSearch / RedisJSON）      | 第 5 天第 1 步的探测           |

### 怎么做

按手册第 5 天第 4 步把 `REDIS_PASSWORD` 改错再重启后端，记下启动时的异常信息。这一条写进 §9 的「故障表现」栏，作为「仓库会带来新的失败模式」的证据。**路线 B 时改做**：删掉 `index.json` 再重启，看检索结果为空——同样是「仓库没了就查不到」。

### 资料

- [LangChain4j：Redis Embedding Store](https://docs.langchain4j.dev/integrations/embedding-stores/redis)
- 回看 [stage-3.md](../docs/stage-3.md) 第 5–6 天的窗口与仓库分层

### 完成

- [ ] 已读完本节

---

## [ ] K12 · Retriever / Aggregator / Injector / Augmentor

**哪天**：D6 · **任务**：S4-T3 · **动手类型**：`【写代码】`

### 必知

LangChain4j 把「检索 → 拼提示词」拆成四个可替换的组件，`DefaultRetrievalAugmentor` 把它们串起来：

| 组件                 | 一句话职责                                     | 换它能改什么                     |
| -------------------- | ---------------------------------------------- | -------------------------------- |
| `ContentRetriever`   | 给一个问题，返回若干 `Content`（块 + 元数据）  | 向量 / BM25 / 混合 / 网络搜索    |
| `ContentAggregator`  | 多路结果怎么合并成一份                         | 去重、RRF、Rerank（第 11 天）    |
| `ContentInjector`    | 把块拼成提示词里的一段文字                     | 模板格式、带不带出处             |
| `RetrievalAugmentor` | 串起上面三个，对外一个 `augment()`             | 整个策略                         |

挂到 `AiServices` 上之后，**接口方法签名不用改**：

```java
RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
        .contentRetriever(retriever)
        .build();

RagAssistant assistant = AiServices.builder(RagAssistant.class)
        .chatModel(chatModel)
        .retrievalAugmentor(augmentor)
        .build();
```

| 组件             | Java 后端类比                          |
| ---------------- | -------------------------------------- |
| ContentRetriever | 一个数据源（DAO / RPC 客户端）         |
| ContentAggregator| 多个数据源结果的合并策略               |
| ContentInjector  | 把 DTO 渲染成报文                      |
| RetrievalAugmentor | 编排层，等价于一个 Service 串起三者  |

### 怎么做

写 `RagConfig`，按上面骨架装 `EmbeddingStoreContentRetriever` → `DefaultRetrievalAugmentor` → `AiServices`。跑 `POST /api/stage/04/query`，确认 `sources` 里的块和 `answer` 里的数字对得上。

### 资料

- [LangChain4j：RAG · Retrieval Augmentor](https://docs.langchain4j.dev/tutorials/rag)

### 完成

- [ ] 已读完本节

---

## [ ] K13 · 提示词规则与「查不到」兜底

**哪天**：D6 · **任务**：S4-T3 · **动手类型**：`【写代码】`

### 必知

检索总会返回点什么（K9）。所以在生成这一侧必须写死两条规则：

1. **只根据参考资料回答；资料里没有的，直接说「查不到」**
2. **回答末尾列出用到的出处（文件名 + 小节）**

规则写在 `@SystemMessage` 里，不是写在 user 消息里。原因和阶段 1 一样：system 是这轮对话的默认约束，user 是这一句的输入。

不写的后果很具体：问「公司年会在哪办」，模型会拿着差旅制度的块，编一个「一般在一线城市酒店宴会厅」的答案。**它看起来很像真的**，这就是最危险的失败。

| 兜底层     | 挡什么                       | 失效表现                     |
| ---------- | ---------------------------- | ---------------------------- |
| `minScore` | 明显无关的块                 | 设太低 → 噪音进提示词        |
| system 规则| 资料不全时的编造             | 没写 → 模型自由发挥          |
| 出处要求   | 让答案可核对（人肉验证手段） | 没写 → 无法判断答案从哪来    |

### 怎么做

问一个库里没有的问题，看 `answer` 是不是「查不到」。如果它编了，先检查 `@SystemMessage` 有没有被后续消息覆盖。把这次现象记进 §9「兜底」行。

### 资料

- [LangChain4j：RAG](https://docs.langchain4j.dev/tutorials/rag)
- 回看 [prompt-engineering.md](prompt-engineering.md) K11（system / history / user 别混）

### 完成

- [ ] 已读完本节

---

## [ ] K14 · 元数据过滤 = 行级权限，过滤在检索前

**哪天**：D7 · **任务**：S4-T3 · **动手类型**：`【写代码】`

### 必知

向量检索只按「像不像」排序，**不看权限**。技术部手册和法务合同都会因为语义相近被捞出来。企业 RAG 必须有权限过滤。

关键区别：**过滤发生在检索之前，不是检索完再筛**。

| 做法             | 结果                                                       |
| ---------------- | ---------------------------------------------------------- |
| 检索前过滤 ✅    | top-k 名额全给有权看的块                                  |
| 检索后筛 ❌      | 先取 3 条，筛掉 2 条无权看的，剩 1 条；名额被浪费         |

LangChain4j 的写法：

```java
MetadataFilterBuilder.metadataKey("dept").isIn("all", "tech")
```

这个 `Filter` 传给 `EmbeddingStoreContentRetriever.builder().dynamicFilter(...)`，底层会变成 Redis 的 TAG 过滤条件，**作用在索引查询上**。

| 行为                    | Java 后端类比                        |
| ----------------------- | ------------------------------------ |
| `dept in (all, tech)`   | 数据行级权限，条件拼进 SQL WHERE     |
| 过滤在检索前            | 权限条件进 SQL，不是查完再在内存里删 |
| 忘了带 `all`            | 部门成员看不到公共文档，越权反向     |

**最容易犯的错**：写成 `isEqualTo("tech")`，于是技术部看不到公共制度。正确是 `isIn("all", "tech")`——「本部门 + 公共」。

### 怎么做

三组对照：`dept=all` 问技术部手册的问题（应该查不到）、`dept=tech` 问同一句（应该答出来）、`dept=tech` 问差旅问题（应该还能答，因为有 `all`）。三组结果记进 §9「权限」栏。

### 资料

- [LangChain4j：RAG · Metadata Filter](https://docs.langchain4j.dev/tutorials/rag)

### 完成

- [ ] 已读完本节

---

## [ ] K15 · Spring AI 映射：VectorStore + RAG Advisor

**哪天**：D8 · **任务**：S4-T4 · **动手类型**：`【写代码】`

### 必知

同一套 RAG，Spring AI 压成两件东西：

| LangChain4j                                | Spring AI                                        |
| ------------------------------------------ | ------------------------------------------------ |
| `EmbeddingStore` + `add` / `search`      | `VectorStore` + `add` / `similaritySearch`     |
| `ContentRetriever`                       | `VectorStoreDocumentRetriever`                 |
| `ContentAggregator` + `ContentInjector`  | 由 advisor 内部完成                              |
| `RetrievalAugmentor`                     | `RetrievalAugmentationAdvisor`（一个 Advisor） |
| 挂在 `AiServices.retrievalAugmentor`     | 挂在 `ChatClient.prompt().advisors(...)`       |

装配：

```java
Advisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
        .documentRetriever(VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(3)
                .build())
        .build();

chatClient.prompt().advisors(ragAdvisor).system("只根据参考资料回答…").user(q).call().content();
```

**两条注意**：

1. `similarityThreshold` 和 LangChain4j 的 `minScore` **不是同一个刻度**，分数不能跨框架比
2. 自动配置会把 Spring Data Redis 拉进来，和已有的 Jedis **各连各的**；索引用不同的 `indexName` 避免覆盖

### 怎么做

跑 `POST /api/stage/04/query-springai`，确认能答对。再把 `similarityThreshold` 调到 0.85 看检索不到。把两边答案和 `promptChars` 并排记进 §9「框架对照」栏。

### 资料

- [Spring AI：Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Spring AI：Redis Vector Store](https://docs.spring.io/spring-ai/reference/api/vectordbs/redis.html)

### 完成

- [ ] 已读完本节

---

## [ ] K16 · 铁律：查询与入库必须同一 embedding 模型

**哪天**：D9 · **任务**：S4-T8 · **动手类型**：`【写代码】`

### 必知

向量库里存的是**旧模型算出来的向量**。换模型之后如果只改代码不重建索引：

- 问题向量（新模型）和块向量（旧模型）**不在同一个坐标空间**
- 相似度算出来没有意义，排序等于随机
- 更糟的是**它可能不报错**，只是返回一堆分数很低的无关块，答案全错还看不出来

这就是为什么它是「铁律」。和你见过的「缓存和数据库编码不一致」是同一类问题：**两边的编码必须相同**。

三条推论：

| 场景                 | 必须做什么                         |
| -------------------- | ---------------------------------- |
| 换 embedding 模型    | 整库重建（`FT.DROPINDEX` + 重新入库） |
| 升级模型版本         | 同上，即使是同名模型的新版本       |
| 多模型混跑           | 每个模型一个独立索引，不能混存     |

**工程上要加一道检查**：入库时记录模型名 + 维度（比如 `rag:meta`），检索前比对，不一致直接返回 409，**把静默错误变成显式拒绝**。

### 怎么做

按手册第 9 天四步：正常检索记基线 → 换一个 256 维的假模型 → 看报错或低分 → 加一致性检查 → 重建恢复。把「假模型时的现象」逐字记进 §9「铁律」行（是报错还是低分，两种都要写清楚）。

### 资料

- [LangChain4j：RAG](https://docs.langchain4j.dev/tutorials/rag)（embedding model 与 store 维度一致的要求）
- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)（换模型的成本讨论）

### 完成

- [ ] 已读完本节

---

## [ ] K17 · Recall / Precision / MRR 各管什么

**哪天**：D10 · **任务**：S4-T6 · **动手类型**：`【写代码】`

### 必知

三个指标，各回答一个问题：

| 指标            | 回答的问题                         | 公式（本阶段口径）                                  |
| --------------- | ---------------------------------- | --------------------------------------------------- |
| `Recall@k`      | 该找到的，找到了吗                 | 命中的问题数 / 总问题数                             |
| `Precision@k`   | 前 k 条里，多少是相关的            | 相关块数 / (问题数 × k)                             |
| `MRR`           | 第一条相关的排得多靠前             | 平均(1 / 第一条命中的 rank)，没命中记 0             |

**它们互相牵制**：

- k 调大 → recall ↑，precision ↓（塞进来的噪音多）
- 加 Rerank → precision / MRR ↑，recall 基本不变（只是重排，不增加候选）
- 加混合检索 → recall ↑（补上了向量漏掉的关键词命中）

所以「哪个指标该涨」取决于你改的是哪一层：

| 改动         | 该看哪个指标        |
| ------------ | ------------------- |
| 切块策略     | Recall 优先         |
| 加混合检索   | Recall              |
| 加 Rerank    | Precision、MRR      |
| 调 minScore  | Precision（会掉 Recall） |

### 怎么做

跑 `POST /api/stage/04/eval`，`topK` 取 1 / 3 / 5 各一次，把三组指标记进 §9。重点看 k 变大时 recall 和 precision 的**反向变化**。

### 资料

- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval) 的评估部分

### 完成

- [ ] 已读完本节

---

## [ ] K18 · 评估集怎么造

**哪天**：D10 · **任务**：S4-T6 · **动手类型**：`【写代码】`

### 必知

评估集是**问题 + 期望命中的文件 + 期望出现的关键词**，12 条就够起步。三条设计原则：

1. **每条答案都能在文档里找到原句**，否则你没法判断「答对没答对」
2. **问题用用户的口语，不用文档的原词**——这才是真实检索难度。用原词提问，BM25 和向量都会 100% 命中，指标失去意义
3. **覆盖每份文档至少 2 条**，避免某份文档根本没被考到

```jsonl
{"id":1,"question":"去上海出差住宿一晚最多报多少","expectedFile":"travel-expense.md","expectedKeyword":"600"}
{"id":2,"question":"线上回滚需要谁审批","expectedFile":"ops-runbook.md","expectedKeyword":"审批"}
```

**判定规则要写死**：一条问题算命中 ⇔ 前 k 条里有任意一条的 `file == expectedFile` 且 `text` 含 `expectedKeyword`。规则不写死，每次跑的结论都不一样。

**12 条太少，单条波动大**。所以看的是**趋势**（改一个变量前后比），不是绝对值。想更稳就扩到 30 条以上，本阶段不要求。

### 怎么做

打开 `demos/04-rag/eval/questions.jsonl`，确认 12 条都在、每条三个字段齐全。跑 eval 时看 `failures` 具体是哪几条，逐条归因（切块问题 / 语义问题）。归因记进 §9。

### 资料

- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)

### 完成

- [ ] 已读完本节

---

## [ ] K19 · BM25 与向量互补

**哪天**：D11 · **任务**：S4-T5 · **动手类型**：`【写代码】`

### 必知

两种检索的强项正好错开：

| 检索  | 强项                                     | 弱项                             |
| ----- | ---------------------------------------- | -------------------------------- |
| 向量  | 同义改写、口语化提问（「住酒店能报多少」） | 精确词、型号、编号、专有名词     |
| BM25  | 词精确匹配，稀有词权重高                 | 不懂同义词，换个说法就漏         |

所以真实系统几乎都是两路并用。**用什么时候分**：问题里有明确的术语 / 编号 → BM25 立功；问题全是口语 → 向量立功。

BM25 核心就两件事：

```text
IDF   = log(N / df)                       // 这个词在多少文档里出现过，越稀有越值钱
score = Σ IDF × (tf × (k1+1)) / (tf + k1 × (1 - b + b × len/avgLen))
```

`k1=1.2`、`b=0.75` 是通用默认值，不用调。中文没有空格，本阶段**按 2-gram 切**（「差旅报销」→ 差旅、旅报、报销），不引分词器。

| BM25          | Java 后端类比                 |
| ------------- | ----------------------------- |
| 倒排索引      | 数据库索引 / ES 倒排          |
| IDF           | 稀有词权重高，等价于「区分度高」 |
| 2-gram 切词   | 特征工程里的 n-gram           |

### 怎么做

跑 `strategy=hybrid` 的 eval，和第 10 天基线比 `recall`。**如果没涨，说明失败原因不是关键词**，回去看 K18 的归因，别硬加组件。

### 资料

- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)（BM25 + embedding 混合那一节）

### 完成

- [ ] 已读完本节

---

## [ ] K20 · RRF：按排名倒数融合

**哪天**：D11 · **任务**：S4-T5 · **动手类型**：`【写代码】`

### 必知

两路检索的**分数不能直接相加**：向量的 0.7 和 BM25 的 12.5 不是一个刻度。RRF（Reciprocal Rank Fusion）绕开这个问题——**只看排名，不看分数**：

```text
score(d) = Σ over 每个检索列表:  1 / (k + rank(d))
```

`k` 通常取 60。一行例子：某块在向量里排第 1、在 BM25 里排第 4：

```text
1/(60+1) + 1/(60+4) = 0.0164 + 0.0156 = 0.0320
```

而只在向量里排第 1、BM25 里没有的块：

```text
1/(60+1) = 0.0164
```

**两路都靠前的块，融合后分数最高**——这就是 RRF 的全部逻辑。

| RRF              | Java 后端类比                          |
| ---------------- | -------------------------------------- |
| 只用排名        | 多路召回结果按「位次」加权，避免量纲问题 |
| `k=60` 平滑     | 让排名差距不极端，等价于 softmax 前的平滑 |
| 缺一路的块     | 分数只有一半，自然排到后面             |

### 怎么做

读手册第 11 天给的三行融合代码，确认你看得懂 `merge(id, 1/(60+rank+1))`。跑 `hybrid` 的 eval，看 `mrr` 是否比基线高。

### 资料

- [LangChain4j：RAG · Content Aggregator](https://docs.langchain4j.dev/tutorials/rag)（`ReciprocalRankFuser`）

### 完成

- [ ] 已读完本节

---

## [ ] K21 · Rerank 只改排序，不改召回

**哪天**：D11 · **任务**：S4-T5 · **动手类型**：`【写代码】`

### 必知

Rerank 是「候选已经定了，重新排一次」。它**不会把没进候选的块捞回来**。所以：

- `recall` 由**召回层**（向量 / BM25 / 混合）决定
- `precision` / `MRR` 由**重排层**决定

这个分工决定了排查顺序：**recall 上不去，加 Rerank 没用**；recall 够了但排得不好，才轮到 Rerank。

常见的 Rerank 实现两种：

| 方式                   | 做法                                              | 成本                 |
| ---------------------- | ------------------------------------------------- | -------------------- |
| Cross-encoder 打分模型 | 把「问题 + 块」一起喂进模型，输出一个相关性分数   | 每块一次模型调用     |
| LLM 打分               | 让 LLM 给每条候选打 0-10 分                       | 一次调用，看候选数量 |

本阶段用 **LLM 打分**：LangChain4j 的 `ReRankingContentAggregator` 需要一个 `ScoringModel`，而官方预打包的 ONNX 打分模型没有对应的现成 artifact（只有抽象的 `OnnxScoringModel`，要自己找模型文件）。为不再拉第二个本地模型，用一次 DeepSeek 调用完成重排，`ReRankingContentAggregator` 只作只读对照。

### 怎么做

跑 `strategy=hybrid+rerank` 的 eval，和 `hybrid` 并排：`precision` / `mrr` 该涨，`recall` 该基本不动。**如果 recall 掉了，说明你把候选集缩得太小**（Rerank 输入应该是 top-10 而不是 top-3）。

### 资料

- [LangChain4j：RAG · Content Aggregator](https://docs.langchain4j.dev/tutorials/rag)
- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)

### 完成

- [ ] 已读完本节

---

## [ ] K22 · 短期窗口与长期向量记忆是两层

**哪天**：D12 · **任务**：S4-T7 · **动手类型**：`【写代码】`

### 必知

「记忆」不是一个东西，是两层：

| 层       | 存什么                 | 怎么取                       | 成本                 | 阶段 3 / 4          |
| -------- | ---------------------- | ---------------------------- | -------------------- | ------------------- |
| 短期窗口 | 最近 N 条消息          | 全量塞进提示词               | 低，但随 N 线性涨    | 阶段 3 已做         |
| 长期记忆 | 历史对话的可检索片段   | 按语义检索 top-k 再塞        | 高（要 embed + 检索）| **阶段 4 新增**     |

两者解决的**不是同一个问题**：

- 窗口解决「刚才说了什么」——它保证多轮连贯，但过了 N 条就忘
- 长期记忆解决「很久以前提过什么」——它能把已经被窗口裁掉的内容按语义捞回来

**不能只留一个**：只有窗口，会话一长就丢；只有长期记忆，每轮都要检索，成本高且不保证顺序。

```text
提示词 = system 规则
       + 【历史对话片段】← 长期记忆检索（可能有）
       + 【近期对话】    ← 窗口全量（最近 N 条）
       + 本轮问题
```

| 记忆层     | Java 后端类比                     |
| ---------- | --------------------------------- |
| 短期窗口   | 本地缓存（最近 N 条）             |
| 长期记忆   | 带检索的持久存储（按条件捞历史）  |

### 怎么做

按手册第 12 天四步：`c1` 说一句「预算 30 万花在差旅」→ 重启 → `c1` 问「预算花在哪」（窗口 + 长期都该生效）→ 换 `c2` 问同一句（只有长期生效）。把三次的 `windowSize` / `recalledCount` 记进 §9「记忆」栏。

### 资料

- [LangChain4j：Chat Memory](https://docs.langchain4j.dev/tutorials/chat-memory)

### 完成

- [ ] 已读完本节

---

## [ ] K23 · 记忆仓库：进程内存 / Redis

**哪天**：D12 · **任务**：S4-T7 · **动手类型**：`【写代码】`

### 必知

和 K11 同一套分层，只是换到记忆上：

| 层       | 阶段 3（内存）                   | 阶段 4（Redis）                              |
| -------- | -------------------------------- | -------------------------------------------- |
| 策略     | `MessageWindowChatMemory`      | 不变                                          |
| 仓库     | `InMemoryChatMemoryRepository` | `RedisChatMemoryStore`                      |
| 重启之后 | 会话消失                         | 会话还在                                      |

```java
RedisChatMemoryStore store = RedisChatMemoryStore.builder()
        .host(host).port(port).password(password)   // 内网 Redis，从环境变量来
        .storeType(StoreType.STRING)                // 没有 RedisJSON 时必须显式指定
        .ttl(Duration.ofDays(7).toMillis())         // 生产必须设 TTL
        .build();
```

**`StoreType` 是这条路线的关键开关**：默认 `JSON` 需要 Redis 的 JSON 模块；你的内网 Redis 如果不带，会直接报 `unknown command 'JSON.SET'`。改成 `STRING` 就走普通字符串 key，**不需要任何模块**——所以第 12 天无论 A 还是 B 都能做。

ChatMemory memory = MessageWindowChatMemory.builder()
        .chatMemoryStore(store)
        .maxMessages(10)
        .id(conversationId)
        .build();
```

**换成 Redis 之后要多想一件事**：key 怎么命名、什么时候过期。生产上会话要设 TTL，否则 Redis 会被历史撑满。本阶段不设 TTL（方便你看到 key），但要知道这是**必须补的**。

### 怎么做

重启后端，用同一个 `conversationId` 再问，确认 `windowSize` 不是 0。用 `redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning --scan --pattern '*c1*'` 看 key 命名。记进 §9「记忆」栏，并写明 `StoreType` 用的是 `JSON` 还是 `STRING`。

### 资料

- [LangChain4j：Chat Memory](https://docs.langchain4j.dev/tutorials/chat-memory)
- 回看 [stage-3.md](../docs/stage-3.md) 第 6 天（仓库分层）

### 完成

- [ ] 已读完本节

---

## [ ] K24 · RAG 的成本构成

**哪天**：D13 · **任务**：S4-T8 · **动手类型**：`【调用后端】`

### 必知

RAG 的账单分三块，只有第三块是真花钱：

| 环节               | 频率       | 成本来源                                 |
| ------------------ | ---------- | ---------------------------------------- |
| 入库（切块 + embed） | 一次性     | 本地模型：CPU 时间；云端模型：按 token 计费 |
| 检索（算问题向量 + top-k） | 每次提问 | 本地模型：几十毫秒；向量库：内存 / CPU   |
| **生成（块 + 问题进提示词）** | **每次提问** | **按输入 token 计费，这是主要开销** |

**最容易被忽略的是第三块**：`topK` 从 3 调到 5，成本涨约 60%，而 recall 只涨几个点。你在做接口优化时算过「多查一张表值不值」，这是同一道题。

成本优化的顺序（从收益大到小）：

1. **调 topK**：从 10 降到 3，常常效果不掉、成本砍一半
2. **调 chunkSize**：块小则每次塞的字少
3. **压缩提示词模板**：去掉不必要的说明文字
4. 换更便宜的生成模型（本阶段不涉及）

### 怎么做

打开 DeepSeek 定价页记下输入单价；跑手册第 13 天那三条 curl，把 `promptChars` 抄进成本表。**不要背单价**，用的时候现查。

### 资料

- [DeepSeek 定价](https://api-docs.deepseek.com/zh-cn/quick_start/pricing)
- [llm-basics.md](llm-basics.md) 的 Token 估算（中文 1 字 ≈ 1.5~2 token）

### 完成

- [ ] 已读完本节

---

## [ ] K25 · topK / chunk / promptChars 与钱

**哪天**：D13 · **任务**：S4-T8 · **动手类型**：`【调用后端】`

### 必知

换算是三行：

```text
估算输入 token ≈ promptChars × 1.6
单次输入成本  = token × 单价 / 1e6
1000 次/天    = 单次成本 × 1000
```

`promptChars` 是本阶段特意加在响应里的字段——**没有它就无法算钱**。生产系统里这类「成本可观测」字段应该和 trace 一起留。

决策方法是二维的：

|            | recall 高 | recall 低 |
| ---------- | --------- | --------- |
| **成本低** | 目标区    | 切块 / 检索有问题 |
| **成本高** | 调 topK / chunk | 两样都要改 |

**结论要写成一句可复述的话**：「在当前评估集上，topK=3 / chunkSize=300 时 recall@3 = x，单次输入约 y token；topK 从 1 到 3 提升最大，从 3 到 5 边际收益低」。这句话生产评审能直接用。

### 怎么做

填完 §9 末尾的四行成本表（`topK` × `chunkSize`），写一句你选的配置和理由。这是第 14 天收口第 2 条的素材。

### 资料

- [DeepSeek 定价](https://api-docs.deepseek.com/zh-cn/quick_start/pricing)

### 完成

- [ ] 已读完本节

---

## [ ] K26 · Java 类比：向量库≈ES、chunk≈分表、评估≈搜索指标

**哪天**：D14 · **任务**：S4-T8 · **动手类型**：`【不写代码】`

### 必知

把这一阶段映射回你已有的经验，是为了**以后讲给别人听时不用 AI 术语**：

| RAG 概念         | 你已有的经验                             | 差别在哪                                   |
| ---------------- | ---------------------------------------- | ------------------------------------------ |
| 向量库           | Elasticsearch / 倒排索引                 | 排序依据从词频换成语义距离                 |
| Chunk 切块       | 分库分表 / 分页                          | 边界怎么划直接决定检索质量，比分区更敏感   |
| 元数据过滤       | 行级数据权限（`WHERE dept IN (...)`）  | 条件拼在索引查询里，不是查完再筛           |
| Recall / MRR     | 搜索系统的评估指标、接口 SLA             | 一模一样，不用重新理解                     |
| Embedding 模型   | 序列化协议 / 字符集                      | 两端必须一致，否则「解码」出来是乱码       |
| 索引重建         | 数据迁移 / 重建索引                      | 换 embedding 模型 = 一次全量重建           |

**一句话**：RAG 在工程上没有任何你沒见过的东西——它是「索引 + 检索 + 拼装」的组合，只是把「关键词匹配」换成了「语义匹配」。难的是**参数怎么定**，那要靠评估数据说话。

### 怎么做

读完本节，试着用上表任一行的类比，向一个没学过 AI 的 Java 同事解释你在做什么。**讲不清就说明还没掌握**。

### 资料

- 无需额外资料，回看本文件各节

### 完成

- [ ] 已读完本节

---

## [ ] K27 · 与阶段 5 / 6 的边界

**哪天**：D14 · **任务**：S4-T8 · **动手类型**：`【不写代码】`

### 必知

阶段 4 停在哪，后面接着做什么：

| 本阶段已经有的                   | 留给后面的                                   |
| -------------------------------- | -------------------------------------------- |
| 单机 Redis 向量库                | 阶段 6：分布式 / 托管向量库（数据量与运维）  |
| 规则拼标题替代上下文生成         | 阶段 5：更复杂的 query 改写（HyDE / Multi-Query） |
| LLM 打分做 Rerank                | 阶段 6：专用 rerank 模型 / 服务化            |
| 内存 BM25                        | 阶段 6：ES / OpenSearch 承担关键词检索       |
| 固定 `dept` 过滤                 | 阶段 5/6：接 Spring Security 真实身份        |
| 手动跑 eval 脚本                 | 阶段 5：进 CI，Eval 作为发布门禁             |
| 夹具文档                         | 阶段 6：真实业务文档 + 增量更新 + 删除       |

**阶段 4 的验收不看这些**。它们的共同点是**工程化**而不是**机制**——机制在阶段 4 已经拆开看过了。

### 怎么做

读完本节，在 §9 末尾写一句：「阶段 4 到此为止，MCP 与多 Agent 留到阶段 5」。第 14 天收口第 3 条要用。

### 资料

- [learning-plan.md 阶段 5 / 阶段 6](../docs/learning-plan.md)

### 完成

- [ ] 已读完本节

---

## [ ] K28 · 常见失败模式与定位顺序

**哪天**：D14 · **任务**：S4-T8 · **动手类型**：`【不写代码】`

### 必知

RAG 答错的时候，**按这个顺序查**，不要一上来就调模型：

| 顺序 | 查什么                       | 现象                                   | 怎么确认                              |
| ---- | ---------------------------- | -------------------------------------- | ------------------------------------- |
| 1    | **检索结果对不对**           | `sources` 里没有正确答案所在的块       | 看 `sources[].text` 和 `answer` 对不对 |
| 2    | **块切得对不对**             | 正确答案跨两块，或块里缺关键数字       | 回到 `POST /split` 看块边界           |
| 3    | **分数门槛**                 | 正确块在库里，但被 `minScore` 挡掉     | `minScore=0` 再检索一次               |
| 4    | **权限过滤**                 | 换了身份就查不到                       | 对照 `dept` 过滤条件                  |
| 5    | **embedding 一致性**         | 全库都检索不准                         | 查 `rag:meta` 的维度                  |
| 6    | **提示词规则**               | 资料对，但模型编了                     | 检查 `@SystemMessage`                 |
| 7    | **生成模型本身**             | 资料对、规则在，还是答错               | 把资料和问题手贴进对话再试一次        |

**前五步都是「检索层」，第六步是「提示词层」，第七步才是「模型层」**。九成的 RAG 问题在前三步。

### 怎么做

在 §9 末尾抄下这个顺序。第 14 天收口后，如果还有答错的问题，按这张表逐项排查，把「查出来是哪一层」写在 `failures` 旁边。

### 资料

- 无需额外资料

### 完成

- [ ] 已读完本节

---

## §9 对照表

**填写规则**：每行只改一个变量；「一句话现象」抄页面或 curl 的原文，不要写「效果更好」这种判断；「结论」只写这一行能支撑的事实。

### 9.0 环境与路线（先填这一行）

| # | 项目 | 结论 |
| - | --- | --- |
| 0 | 内网 Redis 路线 | 路线 A / 路线 B（依据：`FT.CREATE` 返回 ___，`JSON.SET` 返回 ___） |
| 0b | `RedisChatMemoryStore.StoreType` | `JSON` / `STRING`（依据：___） |

### 9.1 检索与链路

| #  | 实验                    | 变量                     | 一句话现象（抄原文/数字） | 结论 |
| -- | ----------------------- | ------------------------ | ------------------------- | ---- |
| 1  | 无检索 vs 手贴参考      | 提示词里有没有资料       |                           |      |
| 2  | Embedding 同义句        | 换一种说法               | cosine =                  |      |
| 3  | Embedding 无关句        | 换主题                   | cosine =                  |      |
| 4  | 切块 300/30             | chunkSize=300, overlap=30 | totalChunks =            |      |
| 5  | 切块 100/0              | chunkSize=100, overlap=0  | totalChunks =            |      |
| 6  | 切块 300/60 paragraph   | 按段落切                  | 最短 / 最长 chars =       |      |
| 7  | 首次检索 topK=3         | 正常问题                  | rank1 file=, score=       |      |
| 8  | topK=1                  | k 变小                    | score =                   |      |
| 9  | minScore=0.8            | 门槛抬高                  | 返回条数 =                |      |
| 10 | 库里没有的问题          | 无关问题                  | 条数=, 最高 score=        |      |
| 11 | 换 Redis 后同一句       | 换仓库                    | rank1 file=, score=       |      |
| 12 | Redis 关掉              | 仓库不可用                | HTTP =, 日志 =            |      |
| 13 | 完整链路 promptChars    | topK=3                    | promptChars =             |      |

### 9.2 权限与框架

| #  | 实验                  | 变量              | 一句话现象             | 结论 |
| -- | --------------------- | ----------------- | ---------------------- | ---- |
| 14 | dept=all 问技术部手册 | 身份=公共         | sources 里有 ops?      |      |
| 15 | dept=tech 问同一句    | 身份=技术部       | sources[0].file =      |      |
| 16 | dept=tech 问差旅      | 公共文档可见性    | sources 里有 travel?   |      |
| 17 | dept=tech 问法务      | 横向隔离          | sources 里有 contract? |      |
| 18 | Spring AI 同一句      | 换框架            | answer=, promptChars=  |      |
| 19 | threshold 0.85        | 门槛抬高          | sources 条数 =         |      |

### 9.3 铁律与评估

| #  | 实验                     | 变量                     | 一句话现象                 | 结论 |
| -- | ------------------------ | ------------------------ | -------------------------- | ---- |
| 20 | 换 256 维假模型          | embedding 模型不一致     | 报错原文 / 最高 score =    |      |
| 21 | rag:meta 改错维度        | 加一致性检查后           | HTTP =, error =            |      |
| 22 | 重建后恢复               | 整库重建                 | indexed=, score=           |      |
| 23 | eval topK=1              | k=1                      | recall=, precision=, mrr=  |      |
| 24 | eval topK=3（基线）      | k=3                      | recall=, precision=, mrr=  |      |
| 25 | eval topK=5              | k=5                      | recall=, precision=, mrr=  |      |
| 26 | eval paragraph           | 换切块                   | recall=, precision=, mrr=  |      |
| 27 | eval hybrid              | 加 BM25                  | recall=, precision=, mrr=  |      |
| 28 | eval hybrid+rerank       | 加 Rerank                | recall=, precision=, mrr=  |      |

### 9.4 记忆与成本

| #  | 实验                          | 变量                    | 一句话现象                        | 结论 |
| -- | ----------------------------- | ----------------------- | --------------------------------- | ---- |
| 29 | c1 第一句                     | 写入记忆                | windowSize=, recalledCount=       |      |
| 30 | 重启后 c1 追问                | 仓库持久化              | windowSize=, recalledCount=       |      |
| 31 | c2 追问同一句                 | 跨会话                  | windowSize=, recalledCount=       |      |
| 32 | Redis 里的 key                | `--scan --pattern '*c1*'` | key 数量 =                      |      |

### 9.5 成本表

| topK | chunkSize | 平均 promptChars | 估算输入 token | 单次输入成本 | 1000 次/天 | recall@3 |
| ---- | --------- | ---------------- | -------------- | ------------ | ---------- | -------- |
| 1    | 300       |                  |                |              |            |          |
| 3    | 300       |                  |                |              |            |          |
| 5    | 300       |                  |                |              |            |          |
| 3    | 600       |                  |                |              |            |          |

> 估算输入 token ≈ `promptChars × 1.6`；单次输入成本 = token × 单价 / 1e6。

---

## 三句话收口（用 §9 的原文填空）

```text
1. 检索质量：基线 recall@3 = ___，hybrid 后 = ___，hybrid+rerank 后 = ___；
   precision 从 ___ 到 ___，MRR 从 ___ 到 ___；
   改进最大的是 ___，因为失败样例里 ___ 类问题最多。

2. 成本取舍：topK 从 1 到 3，recall 从 ___ 涨到 ___，promptChars 从 ___ 涨到 ___；
   topK 从 3 到 5，recall 只从 ___ 到 ___；
   我选 topK=___、chunkSize=___，单次输入约 ___ token。

3. 边界：短期窗口存在 ___，长期记忆存在 ___；文档级权限用 ___ 实现，
   过滤发生在 ___；换 embedding 模型必须 ___，否则 ___；
   检索答错时先查 ___，MCP 与多 Agent 留到阶段 ___。
```
