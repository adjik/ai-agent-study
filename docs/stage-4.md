# 阶段 4：RAG 与记忆（14 天操作）

> 这一阶段要学什么、要了解什么、要掌握哪些知识点，写在 [learning-plan.md 阶段 4](learning-plan.md)。本文只写每天读哪篇、写不写代码、伪代码、当天怎么执行。
> 知识点全文在 [notes/rag.md](../notes/rag.md)，每节 `K*` 有必知 / 怎么做 / 资料 / 完成。
> **动手**只有两种：`【不写代码】` 只读文档、只点已有页面；`【写代码】` 按当天伪代码改 `stage04`，改完再按「执行计划」跑。
> **执行计划**里每一步都有两行：**怎么执行**（打开哪个页面、点什么）和 **执行语句**（这一下发给 8080 的请求）。点页面上的发送，发出去的就是这句，不用再另开终端打一遍。
> 代码放在 `demos/backend` 的 `com.example.demos.stage04`，端口 8080。文档夹具放在 `demos/04-rag/data/`。

---

## 这一阶段的技术前提（先读完再动手）

### 1. DeepSeek 没有 embeddings 接口，本阶段必须另选一个 embedding 模型

已实测：`POST https://api.deepseek.com/embeddings` 返回 404。DeepSeek 只提供 chat（`deepseek-v4-flash` / `deepseek-v4-pro`），**没有任何向量化接口**。

所以生成（写答案）继续用 DeepSeek，**向量化换成本地 ONNX 模型**：

```xml
<!-- pom.xml，版本跟已有的 langchain4j 1.12.1 对齐 -->
<dependency>
  <groupId>dev.langchain4j</groupId>
  <artifactId>langchain4j-embeddings-bge-small-zh-v15-q</artifactId>
  <version>1.12.1-beta21</version>
</dependency>
```

```java
// 类名就在这个 artifact 里，不需要自己写
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;

EmbeddingModel embeddingModel = new BgeSmallZhV15QuantizedEmbeddingModel();
embeddingModel.embed("你好").content().dimension();  // 512（Embedding#dimension()）
```

- **中文优先**：BAAI 的 `bge-small-zh-v1.5`，中文检索比英文小模型好，512 维
- **完全本地**：模型文件（约 24MB ONNX）已经打进 jar，**第一次运行不需要下载，也不花钱**
- **不新增 key**：不碰 `DASHSCOPE_API_KEY` 之类，环境变量还是只有一个 `DEEPSEEK_API_KEY`

> 代价说在前面：本地小模型的检索质量弱于云端大模型。本阶段学的是**链路、评估方法和工程约束**，这些和质量无关。真要上线可以换成云端 embedding 模型，第 9 天专门讲「换模型要付什么代价」。

### 2. 向量库用你现成的内网 Redis（不用本机装）

本阶段连**你已经能访问的内网 Redis**（`host:port` + 密码），不 `brew install`、不起本地服务。配置全部走环境变量，**密码不进仓库**。

**第 1 天就要做的连通性检查**（不要等到第 5 天才发现连不上）：

```bash
# 变量先 export 或写进项目根目录 .env（.env 已在 .gitignore 里）
export REDIS_HOST=你的内网地址
export REDIS_PORT=6379
export REDIS_PASSWORD=你的密码

# ① 必须过：连不上就什么都做不了
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning ping
# ② 版本信息（可能被 ACL 拒，被拒不影响继续）
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning INFO server | grep redis_version
```

> 内网 Redis 常常禁用 `MODULE LIST` / `INFO` 这类管理命令（`rename-command` 或 ACL 限制），所以**不要用 `MODULE LIST` 当判据**。真正的判据是第 5 天那两条真实命令：`JSON.SET` 和 `FT.CREATE`——**能建索引、能写 JSON 才算真的支持**。

**判定标准与两条路**。`RedisEmbeddingStore` 在内部是这样做的：入库用 **`JSON.SET`**，建索引用 **`FT.CREATE ... ON JSON`**，查询用 **`FT.SEARCH`**。所以它需要**两个模块**：RediSearch 和 RedisJSON。

| 你的 Redis | 判定依据 | 本阶段走哪条路 |
| --- | --- | --- |
| 带 RediSearch + RedisJSON（Redis Stack / Redis 8 自建 / 云厂商开了模块） | `FT.CREATE` 与 `JSON.SET` 都不报 unknown command | **路线 A**：用 `RedisEmbeddingStore`，第 5 天换仓库 |
| 只有 RedisJSON、没有 RediSearch | `JSON.SET` 可以，`FT.CREATE` 报 `unknown command` | **路线 B**：向量检索留在进程内，Redis 只当会话记忆仓库 |
| 两个都没有（多数云托管社区版） | `FT.CREATE` 和 `JSON.SET` 都报 unknown command | **路线 B**（记忆仓库还要改 `StoreType.STRING`） |

**路线 B 不是降级敷衍，它有明确的代价和明确的收益**：

- **收益**：不依赖任何 Redis 模块，第 6–14 天的**全部内容照做**——切块、评估、混合检索、Rerank、元数据过滤、记忆，一个都不少
- **代价**：向量索引不在 Redis 里，进程重启要重新加载。用 `InMemoryEmbeddingStore.serializeToFile(Path)` / `fromFile(Path)` 落到 `demos/04-rag/vector-data/index.json`（该目录已在 `.gitignore`），就能跨重启
- **学不到的那一点**：「索引建在服务端、多个进程共享同一份向量」——这一条留到阶段 6

**为什么不用 pgvector / Milvus**：本机没有 Docker 和 Postgres，而 Redis 你已经有了。一个服务干两件事（向量 + 记忆）比装两套省事。pgvector 只读文档对照。

### 2.1 连接配置（两条路共用）

`.env`（**不入库**，项目根目录，`DotenvEnvironmentPostProcessor` 会在启动时读进 Environment）：

```properties
DEEPSEEK_API_KEY=sk-xxxx
REDIS_HOST=10.0.0.12
REDIS_PORT=6379
REDIS_PASSWORD=你的密码
REDIS_DB=0
```

`application.properties` 里只引用变量名，不写值：

```properties
# 阶段 4：内网 Redis（密码走环境变量，不入库）
stage04.redis.host=${REDIS_HOST:localhost}
stage04.redis.port=${REDIS_PORT:6379}
stage04.redis.password=${REDIS_PASSWORD:}
stage04.redis.database=${REDIS_DB:0}
# A = 用 Redis 向量库；B = 向量库在进程内，Redis 只存记忆
stage04.redis.vector-mode=${STAGE04_VECTOR_MODE:A}
```

连接用一个配置类收口，两条路都从这里拿 `UnifiedJedis`：

```java
@Configuration
public class RedisConnectionConfig {

    @Value("${stage04.redis.host}")     private String host;
    @Value("${stage04.redis.port}")     private int port;
    @Value("${stage04.redis.password}") private String password;
    @Value("${stage04.redis.database}") private int database;

    @Bean(destroyMethod = "close")
    UnifiedJedis unifiedJedis() {
        DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
                .password(password.isEmpty() ? null : password)
                .database(database)
                .connectionTimeoutMillis(3000)   // 内网也要设，别用默认的无限等
                .socketTimeoutMillis(5000)
                .build();
        return new UnifiedJedis(new HostAndPort(host, port), config);
    }
}
```

> ⚠️ **`destroyMethod = "close"` 必须写**。`UnifiedJedis` 持有连接池，Spring 容器关闭时不关会留下悬空连接，重启后端几次就把内网 Redis 的连接数占满。

**启动时立刻验证连接**，不要等第一次检索才失败（照 `DeepSeekConfig` 的 `ApplicationRunner` 写法）：

```java
@Override
public void run(ApplicationArguments args) {
    try (UnifiedJedis probe = ...) {          // 或用注入的 Bean 打一次 ping
        probe.ping();
    } catch (Exception e) {
        throw new IllegalStateException("""
                连不上 Redis（%s:%d）。检查 REDIS_HOST / REDIS_PORT / REDIS_PASSWORD，
                以及是否需要 VPN 或 IP 白名单。原始错误：%s
                """.formatted(host, port, e.getMessage()), e);
    }
}
```

### 3. 两框架分工：LangChain4j 主线，Spring AI 对照

| 天     | 框架        | 干什么                                              |
| ------ | ----------- | --------------------------------------------------- |
| D2–D7  | LangChain4j | 主线：切块、入库、检索、Augmentor、元数据过滤       |
| D8     | Spring AI   | 对照：`VectorStore` + `RetrievalAugmentationAdvisor` |
| D9 起  | LangChain4j | 评估、混合检索、Rerank、记忆                        |

原因和阶段 2 一样：一条主线写透，另一条只用来对照，避免两边都半懂。LangChain4j 的 RAG 组件分得更细（`ContentRetriever` / `ContentAggregator` / `ContentInjector` 可以分别换），适合学机制；Spring AI 的 `VectorStore` + `RetrievalAugmentationAdvisor` 更省代码，适合看工程收敛后的样子。

---

## 14 天总表

- [ ] 第 1 天 `【不写代码】` 先看见「模型不知道你公司的事」
- [ ] 第 2 天 `【写代码】` Embedding：把两句话变成两个向量
- [ ] 第 3 天 `【写代码】` 切块：同一份文档切成什么样
- [ ] 第 4 天 `【写代码】` 第一次检索：向量进库、top-k 出库
- [ ] 第 5 天 `【写代码】` 换 Redis：索引、维度、元数据字段
- [ ] 第 6 天 `【写代码】` 完整链路：检索结果拼进提示词（里程碑）
- [ ] 第 7 天 `【写代码】` 元数据过滤：查不到别人的部门
- [ ] 第 8 天 `【写代码】` Spring AI 版同一件事
- [ ] 第 9 天 `【写代码】` 铁律：换 embedding 模型会怎样
- [ ] 第 10 天 `【写代码】` 评估：Recall / Precision / MRR
- [ ] 第 11 天 `【写代码】` 混合检索、RRF、Rerank
- [ ] 第 12 天 `【写代码】` 记忆：窗口留在 Redis，旧对话进向量库
- [ ] 第 13 天 `【调用后端】` 成本：top-k 与 chunk 长度怎么换算成钱
- [ ] 第 14 天 `【不写代码】` 收口：填表、写三句话

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

## 夹具：四份文档，一个权限字段

`demos/04-rag/data/` 放四份 Markdown，全部是虚构但格式完整的公司制度。**这四份已经建好，后面 13 天都用它们**，不要中途换成自己公司的真文档（换成真文档是阶段 6 的事）。

| 文件                    | 内容                             | `dept` 元数据 | 大约字数 |
| ----------------------- | -------------------------------- | ------------- | -------- |
| `travel-expense.md`   | 差旅报销制度（含标准、发票、流程） | `all`       | 1200     |
| `leave-policy.md`     | 请假与考勤制度                   | `all`       | 900      |
| `ops-runbook.md`      | 生产故障应急手册（技术部内部）   | `tech`      | 1500     |
| `contract-review.md`  | 合同审查要点（法务部内部）       | `legal`     | 1100     |

**第 1 天先把这四份通读一遍**（内容已就绪，位于 `demos/04-rag/data/`），确认每份的结构。要求：

- 每份都有明确的小标题（`## 一、住宿标准`），因为第 3 天要用标题切块
- 数字要具体（「一线城市每晚不超过 600 元」），因为第 10 天要算检索对不对
- `ops-runbook.md` 和 `contract-review.md` 里各埋一段「只有本部门能看」的敏感内容，第 7 天验收用

**评估集的 12 条问题**（第 10 天用，已建好 `demos/04-rag/eval/questions.jsonl`）：

| #  | 问题                             | 期望命中文件       | 期望命中关键词 |
| -- | -------------------------------- | ------------------ | -------------- |
| 1  | 去上海出差住宿一晚最多报多少     | travel-expense.md  | `600`        |
| 2  | 出差坐高铁能坐几等座             | travel-expense.md  | `二等座`     |
| 3  | 出差回来几天内要交报销单         | travel-expense.md  | `15 个自然日` |
| 4  | 年假有多少天，怎么算             | leave-policy.md    | `年假`       |
| 5  | 请病假要交什么材料               | leave-policy.md    | `病假证明`   |
| 6  | 连续旷工几天公司可以解除合同     | leave-policy.md    | `连续旷工 3 天` |
| 7  | 生产库挂了第一步做什么           | ops-runbook.md     | `确认影响面` |
| 8  | 数据库主从延迟告警怎么处理       | ops-runbook.md     | `主从延迟`   |
| 9  | 线上回滚需要谁审批               | ops-runbook.md     | `值班负责人审批` |
| 10 | 合同里的违约金上限一般写多少     | contract-review.md | `30%`        |
| 11 | 对方要求独家条款怎么处理         | contract-review.md | `评估`       |
| 12 | 合同盖章前要核对哪几项           | contract-review.md | `盖章前核对` |

---

### 第 1 天 · 先看见「模型不知道你公司的事」

**动手**：`【不写代码】`（但要确认夹具与 Redis 连通性）

**学什么**：RAG 解决的是「模型参数里没有的事实」。今天用已有的 Agent 接口问一个只有你公司制度里才有答案的问题，看它编；再手工把文档片段贴进提示词，看它答对。两相对照，RAG 要自动化的就是「贴片段」这一步。

**为了什么**：第 2 天开始写向量，很容易把它当成「更聪明的搜索」就完事。今天先确立目标：**检索是为了把对的片段送到提示词里**。如果片段是错的，后面每一步都白做。

**今天读**

- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)。读「问题是什么」和「三种改进手段」两节。读完应能说出：切块会丢上下文，补救办法是在块前面补一句它属于哪份文档的哪一节。
- [Lilian Weng：LLM Powered Autonomous Agents](https://lilianweng.github.io/posts/2023-06-23-agent/) 的「Memory」一节。读完应能说出：参数里的记忆改不了，上下文里的记忆是一次性的。RAG 属于后者。

**今天要做的事**

1. 通读 `demos/04-rag/data/` 下四份文档，对照上面的要求确认结构
2. 打开 `demos/04-rag/eval/questions.jsonl`，确认 12 条都在（**注意第 2/3/6/9/10 条的关键词是原文片段，不是随意写的**）
3. **确认内网 Redis 能连上**（见文首第 2 节的三条命令）。连不上就先解决 VPN / 白名单 / 密码，不要等到第 5 天
4. 用两条 curl 对比

**执行计划**

1. **怎么执行**：不打开页面，直接在终端问。先问一个故意超出模型知识的问题。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/03/loop \
     -H 'Content-Type: application/json' \
     -d '{"question":"我们公司去上海出差，住宿一晚最多报销多少？","maxSteps":4}'
   ```

   - **你会看到**：一个像是答案的数字（比如「一般 400-800 元」），`steps` 里没有任何查表记录。
   - **为什么**：`OrderStore` / `InventoryStore` 里没有「报销标准」这件事。模型没查任何东西，只能按训练语料里的通用说法编。这就是要 RAG 的原因——**事实不在参数里**。

2. **怎么执行**：把 `travel-expense.md` 里「住宿标准」那一段手工复制进提示词，再问一次。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/03/loop \
     -H 'Content-Type: application/json' \
     -d '{"question":"【参考资料】一线城市（含上海）住宿标准：每人每晚不超过 600 元，凭发票实报实销。\n\n问题：我们公司去上海出差，住宿一晚最多报销多少？","maxSteps":2}'
   ```

   - **你会看到**：回答里出现 600，并且不再自由发挥。
   - **为什么**：同一个模型，差别只在提示词里多了一段。**答案的来源从参数变成了上下文**。RAG 要自动化的就是「找到这段、放进去」——找到靠向量检索，放进去靠 Augmentor。

3. **怎么执行**：在 `notes/rag.md` 的 §9 对照表里记下两次回答的第一句，标上「无检索 / 手贴参考」。
   - **你会看到**：表格第一行有数据了。
   - **为什么**：第 14 天要用这张表写结论。手贴参考那一行是后面所有自动检索的**质量上限**——自动检索再好，也不会超过「人工挑对的那一段」。

---

### 第 2 天 · Embedding：把两句话变成两个向量

**动手**：`【写代码】`

**学什么**：embedding 是把一段文本映射成固定长度的一串小数。语义接近的两句，向量方向接近。余弦相似度就是两个方向的夹角。今天不涉及任何库，只做「文本 → 向量 → 一个 0~1 之间的数」。

**为了什么**：第 4 天的检索、第 10 天的评估，本质上都是在对这个数排序。今天先把它变成你能打印出来的东西。

**今天读**

- [LangChain4j：Embedding Models](https://docs.langchain4j.dev/tutorials/embedding-models)。读到能看到 `EmbeddingModel` 接口和 `embed()` 用法为止。读完应能说出：`embed()` 返回的是 `Embedding`，里面是 `float[]`。
- [BGE-small-zh 模型卡](https://huggingface.co/BAAI/bge-small-zh-v1.5)。只读简介表格，记住：中文、512 维、检索任务用 `query` 前缀。**不要读训练细节。**

**要写的代码**

在 `com.example.demos.stage04.embedding`：

| 类                            | 职责                                                                                    |
| ----------------------------- | --------------------------------------------------------------------------------------- |
| `EmbeddingConfig`           | 一个 `@Bean EmbeddingModel`，返回 `new BgeSmallZhV15QuantizedEmbeddingModel()`。全阶段只在这里 new 一次 |
| `EmbeddingController`       | `POST /api/stage/04/embedding/similarity`，入参 `a`、`b`，返回两个向量维度、余弦相似度、前 8 维数值 |
| `SimilarityResponse`        | `dimension`、`cosine`、`aHead`、`bHead`（各取前 8 维，证明它不是全 0）                |

余弦相似度自己写 6 行，不要引第三方数学库：

```java
static double cosine(float[] a, float[] b) {
    double dot = 0, na = 0, nb = 0;
    for (int i = 0; i < a.length; i++) {
        dot += a[i] * b[i];
        na += a[i] * a[i];
        nb += b[i] * b[i];
    }
    return dot / (Math.sqrt(na) * Math.sqrt(nb));
}
```

**伪代码**

```text
similarity(a, b):
    ea = embeddingModel.embed(a).content().vector()
    eb = embeddingModel.embed(b).content().vector()
    return {
      dimension: ea.length,           // 512
      cosine: cosine(ea, eb),         // 0~1
      aHead: ea[0..8], bHead: eb[0..8] // 证明不是全 0
    }
    # 这里还没有任何存储，也没有检索
```

**执行计划**

1. **怎么执行**：重启后端。这一步还没有页面，用终端打。先试一组**意思相同、字面不同**的句子。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/embedding/similarity \
     -H 'Content-Type: application/json' \
     -d '{"a":"去上海出差住酒店能报多少钱","b":"一线城市住宿费报销上限是多少"}'
   ```

   - **你会看到**：`dimension` 是 512，`cosine` 大概在 0.7~0.9 之间。两句话没有一个共同的词。
   - **为什么**：embedding 编码的是意思，不是字面。这正是它比 `LIKE '%报销%'` 强的地方——用户不会用文档里的原词提问。

2. **怎么执行**：换一组**主题完全不同**的句子。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/embedding/similarity \
     -H 'Content-Type: application/json' \
     -d '{"a":"去上海出差住酒店能报多少钱","b":"数据库主从延迟告警怎么处理"}'
   ```

   - **你会看到**：`cosine` 掉到 0.2~0.4。
   - **为什么**：这就是第 4 天 top-k 排序的依据。把这两个数字写进 §9，它们是后面判断「分数多少算命中」的锚点。

3. **怎么执行**：把同一句话原样传两次（`a` 和 `b` 一样）。
   - **你会看到**：`cosine` 等于 1.0。
   - **为什么**：确认实现没写错。1.0 是自相似的上界，0.7~0.9 是「同义」的常见区间，0.3 以下基本无关。三个锚点记进笔记。

---

### 第 3 天 · 切块：同一份文档切成什么样

**动手**：`【写代码】`

**学什么**：整篇文档不能直接进向量库——一是太长，一次 embed 会丢细节；二是检索命中后整篇塞进提示词会超预算。所以要切成块。切法决定检索质量：切得太大，一块里混着多个主题，相似度被稀释；切得太小，一块里没有完整答案。

**为了什么**：第 4 天入库、第 10 天评估，用的都是今天切出来的块。**块是检索的最小单位，也是引用溯源的最小单位**。切坏了，后面调参也救不回来。

**今天读**

- [LangChain4j：Document Splitters](https://docs.langchain4j.dev/tutorials/rag#document-splitter)（RAG 页的 Document Splitter 一节）。读完应能说出：`DocumentSplitters.recursive(maxSize, overlap)` 会先按段落、再按句子、最后按字符逐级退让。
- 回看第 1 天读的 Contextual Retrieval 里「为什么切块会丢上下文」那一段。

**要写的代码**

在 `com.example.demos.stage04.split`：

| 类                     | 职责                                                                                             |
| ---------------------- | ------------------------------------------------------------------------------------------------ |
| `SplitController`    | `POST /api/stage/04/split`，入参 `file`（夹具文件名）、`chunkSize`、`overlap`、`strategy` |
| `SplitResponse`      | `totalChunks`、每块的 `index` / `chars` / `head`（前 40 字）/ `tail`（后 20 字）         |
| 切块策略             | `recursive` 用 `DocumentSplitters.recursive(chunkSize, overlap)`；`paragraph` 用 `DocumentByParagraphSplitter(chunkSize, overlap)` |

读取用 `FileSystemDocumentLoader.loadDocument(Path.of("demos/04-rag/data", file))`。**路径从工作目录算**，所以启动后端必须在 `demos/backend` 下（`mvn spring-boot:run` 本来就在那）。

**伪代码**

```text
split(file, chunkSize, overlap, strategy):
    doc = FileSystemDocumentLoader.loadDocument(data/ + file)
    splitter = strategy == "paragraph"
             ? new DocumentByParagraphSplitter(chunkSize, overlap)
             : DocumentSplitters.recursive(chunkSize, overlap)
    segments = splitter.split(doc)
    return {
      totalChunks: segments.size(),
      chunks: segments.map((s, i) -> {
        index: i, chars: s.text().length(),
        head: s.text()[0..40], tail: s.text()[-20..]
      })
    }
    # 还不涉及向量和存储
```

**执行计划**

1. **怎么执行**：重启后端。趁页面还没接通，用终端打。先看默认参数切出多少块。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/split \
     -H 'Content-Type: application/json' \
     -d '{"file":"travel-expense.md","chunkSize":300,"overlap":30,"strategy":"recursive"}'
   ```

   - **你会看到**：`totalChunks` 在 5~9 之间（1200 字、每块 300 字）。
   - **为什么**：块数是「检索粒度」。块多则命中更精确但提示词更长；块少则相反。这个数字第 13 天要拿来算钱。

2. **怎么执行**：把 `chunkSize` 改成 `100`，`overlap` 改成 `0`，再打一次。
   - **你会看到**：块数涨到 15 以上，很多 `tail` 是半句话（「……实报实」这种）。
   - **为什么**：切太小会把一句规则劈成两半。问题是「住宿一晚最多报多少」，答案在「不超过 600 元」这半句里，如果切在「600」前面，检索到的那块就没有数字。

3. **怎么执行**：把 `chunkSize` 改回 `300`，`overlap` 改成 `60`，再用 `strategy=paragraph` 打一次。
   - **你会看到**：块数变少，`chars` 差异变大（有的 80 字，有的 280 字）。
   - **为什么**：按段落切尊重文档作者的意图——一个 `##` 小节讲一件事。缺点是长度不可控。**第 4 天两种都试，看哪种检索更准**，不要凭感觉选。三种参数组合的结果填进 §9。

---

### 第 4 天 · 第一次检索：向量进库、top-k 出库

**动手**：`【写代码】`

**学什么**：把第 2 天的向量和第 3 天的块接起来：每块算一个向量，连同原文存进向量库。查询时把问题也算成向量，在库里找最接近的 k 块。**今天只要检索，不要生成**——先确认「找到的块对不对」，再谈「答案对不对」。

**为了什么**：这一步是整个 RAG 里唯一决定「能不能答对」的地方。答案错了，九成是块没找对。先把检索结果直接打印出来，是最快的定位手段。

**今天读**

- [LangChain4j：RAG 页的 Embedding Store 与 Content Retriever 两节](https://docs.langchain4j.dev/tutorials/rag)。读完应能说出：`EmbeddingStore.add(embedding, segment)` 入库，`ContentRetriever.retrieve(query)` 出库。
- [Redis：Vector similarity search](https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/)。**只读概念部分**（KNN、余弦距离、为什么归一化后可以用内积），命令细节第 5 天再看。

**要写的代码**

在 `com.example.demos.stage04.retrieve`：

| 类                          | 职责                                                                                                    |
| --------------------------- | ------------------------------------------------------------------------------------------------------- |
| `InMemoryRagStore`        | 用 `new InMemoryEmbeddingStore<TextSegment>()` 建库。今天先用内存，明天换 Redis                     |
| `IngestService`           | 读四份文档 → 切块 → 每块 `embeddingModel.embed(segment)` → `store.add(embedding, segment)`。启动时跑一次 |
| `RetrieveController`      | `POST /api/stage/04/retrieve`，入参 `question`、`topK`、`minScore`                        |
| `RetrieveResponse`        | 每条：`rank`、`score`、`file`、`dept`、`chars`、`text`（块的原文）                                |

检索用核心 API，不用 ContentRetriever（那是第 6 天的事，今天要让索引和分数都露出来）：

```java
Embedding queryEmbedding = embeddingModel.embed(question).content();
EmbeddingSearchRequest req = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .maxResults(topK)
        .minScore(minScore)
        .build();
List<EmbeddingMatch<TextSegment>> matches = store.search(req).matches();
```

每个块的元数据在入库时就要带上：`Metadata.from(Map.of("file", name, "dept", dept, "chunk", String.valueOf(i)))`。第 7 天要用 `dept` 过滤。

**伪代码**

```text
启动时 ingest():
    for file in [travel-expense, leave-policy, ops-runbook, contract-review]:
        doc = load(data/ + file + ".md")
        segments = recursive(300, 30).split(doc)
        for (i, s) in segments:
            s.metadata.put("file", file, "dept", deptOf(file), "chunk", i)
            store.add(embeddingModel.embed(s), s)

retrieve(question, topK, minScore):
    q = embeddingModel.embed(question)
    matches = store.search(q, maxResults=topK, minScore=minScore)
    return matches.map(m -> { score: m.score(), text: m.embedded().text(), file, dept })
    # 没有生成，没有提示词
```

**执行计划**

1. **怎么执行**：重启后端（启动时会跑一次入库，日志里应有 `ingested N segments`）。用第 1 天那个问题打检索。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/retrieve \
     -H 'Content-Type: application/json' \
     -d '{"question":"去上海出差住宿一晚最多报多少","topK":3,"minScore":0}'
   ```

   - **你会看到**：`rank=1` 的块里出现「不超过 600 元」，`file` 是 `travel-expense.md`，`score` 在 0.6 以上。
   - **为什么**：问题和块的语义对上了。**这一条通了，RAG 的检索部分就成立**。第 6 天只是把这块拼进提示词。

2. **怎么执行**：把 `topK` 改成 `1`，再打一次。
   - **你会看到**：只剩一条，通常还是那条对的。
   - **为什么**：topK=1 省 token，但没有容错——一旦第一名是错的，答案就必错。第 13 天会算 topK 与成本的关系。

3. **怎么执行**：把 `minScore` 改成 `0.8`，再打一次。
   - **你会看到**：可能一条都不返回（空数组）。
   - **为什么**：`minScore` 是**硬门槛**，低于它的块直接丢掉。设太高会出现「明明库里有，却检索不到」。它适合用来挡完全无关的块，不适合用来做精确筛选。把 topK 与 minScore 的四种组合记进 §9。

4. **怎么执行**：问一个库里绝对没有的问题。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/retrieve \
     -H 'Content-Type: application/json' \
     -d '{"question":"公司年会在哪办","topK":3,"minScore":0}'
   ```

   - **你会看到**：仍然返回 3 条，`score` 都在 0.3~0.45，内容是差旅或请假制度。
   - **为什么**：**向量库永远会返回「最像的」，不会返回「没有」**。这就是第 6 天要用 `minScore` 或提示词兜底的原因——没有门槛的话，模型会拿着不相关的块硬答。

---

### 第 5 天 · 换 Redis：索引、维度、元数据字段

**动手**：`【写代码】`

**学什么**：把第 4 天的 `InMemoryEmbeddingStore` 换成一个「不在本进程里」的仓库。**业务代码一行不改**，只换 `EmbeddingStore` 这个 Bean 的构造。同时第一次看见「索引」这个东西：Redis 的向量检索不是自动的，要先 `FT.CREATE` 建索引，声明哪个字段是向量、几维、用什么算法、哪些字段可过滤。

**为了什么**：这是阶段 4 唯一一次换仓库。换完之后你能说清「策略（怎么切、怎么排）和仓库（存在哪）是两层」——和阶段 3 的 `ChatMemory` / `ChatMemoryRepository` 是同一组概念。

> **两条路由本天第 1 步的探测结果决定**（命令在下面执行计划里）：你的内网 Redis 带 RediSearch + RedisJSON 就走**路线 A**（向量真正存 Redis）；不带就走**路线 B**（向量落本地文件，Redis 只当第 12 天的记忆仓库）。**路线 B 不影响第 6–14 天任何一节**，只是少学「服务端索引」这一件事。

**今天读**

- [LangChain4j：Redis Embedding Store](https://docs.langchain4j.dev/integrations/embedding-stores/redis)。读完应能说出：`RedisEmbeddingStore.builder()` 需要 `unifiedJedis`、`indexName`、`dimension`。
- [Redis：FT.CREATE 与向量字段](https://redis.io/docs/latest/commands/ft.create/)。只读 `VECTOR` 那一段的类型参数（`FLOAT32`、`DIM`、`DISTANCE_METRIC`）。

**要写的代码**

先在 `pom.xml` 加依赖，版本跟已有的 `langchain4j` 对齐：

```xml
<dependency>
  <groupId>dev.langchain4j</groupId>
  <artifactId>langchain4j-community-redis</artifactId>
  <version>1.12.1-beta21</version>
</dependency>
```

在 `com.example.demos.stage04.store`：

| 类                     | 职责                                                                                                        |
| ---------------------- | ----------------------------------------------------------------------------------------------------------- |
| `RedisConnectionConfig` | 第 2.1 节那个类：`@Bean(destroyMethod="close") UnifiedJedis`，host/port/password 全从环境变量来 |
| `RedisStoreConfig`   | **路线 A**：`@Bean EmbeddingStore<TextSegment>`，用注入的 `UnifiedJedis` 建 `RedisEmbeddingStore` |
| `VectorStoreFactory` | **路线 B**：`@Bean EmbeddingStore<TextSegment>`，返回 `InMemoryEmbeddingStore`，启动时 `fromFile`，入库后 `serializeToFile` |
| `IngestService`      | **改成注入 `EmbeddingStore`，不 new 内存库**。加一个 `POST /api/stage/04/ingest` 手动触发重建         |
| `RetrieveController` | 不变                                                                                                        |

**路线 A 的 store 装配**（元数据字段必须提前声明，否则第 7 天过滤不了）：

```java
RedisEmbeddingStore.builder()
    .unifiedJedis(unifiedJedis)                      // 来自 RedisConnectionConfig
    .indexName("rag-idx")
    .prefix("rag:")                                  // 和探测用的 stage04:probe 分开
    .dimension(512)
    .metadataKeys(Set.of("file", "dept", "chunk"))   // 这三个才能被过滤
    .build();
```

**路线 B 的 store 装配**：

```java
private static final Path INDEX_FILE = Path.of("demos/04-rag/vector-data/index.json");

@Bean
EmbeddingStore<TextSegment> embeddingStore() throws IOException {
    Files.createDirectories(INDEX_FILE.getParent());
    return Files.exists(INDEX_FILE)
            ? InMemoryEmbeddingStore.fromFile(INDEX_FILE)
            : new InMemoryEmbeddingStore<>();
}
```

两种装配只影响 `EmbeddingStore` 这个 Bean，`IngestService` 和 `RetrieveController` 完全一样——**这就是接口的价值**。

**伪代码**

```text
启动 / POST ingest:
    若 路线 A: FT.DROPINDEX rag-idx DD（清旧索引）
    for file in 四份文档:
        segments = recursive(300, 30).split(load(file))
        for (i, s) in segments:
            s.metadata = {file, dept, chunk: i}
            store.add(embeddingModel.embed(s), s)
    若 路线 B: store.serializeToFile(index.json)
    return { indexed: totalSegments, mode: A 或 B }

retrieve(question, topK, minScore):
    # 与第 4 天的代码完全一样，store 是注入进来的
```

**执行计划**

1. **怎么执行**：确认内网 Redis 连通，并判定走 A 还是 B。这一步是**硬门槛**——三条命令里第一二条必须过，第三条只是探测。
   **执行语句**：

   ```bash
   # ① 连通性（第一条必须成功）
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning ping

   # ② 是否有 JSON 模块（RedisEmbeddingStore 入库用 JSON.SET）
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning \
     JSON.SET stage04:probe '$' '{"ok":1}'
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning \
     DEL stage04:probe

   # ③ 是否有搜索模块（建索引用 FT.CREATE ... ON JSON）
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning \
     FT.CREATE stage04:probe-idx ON JSON PREFIX 1 stage04:probe: SCHEMA $.ok NUMERIC
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning \
     FT.DROPINDEX stage04:probe-idx DD
   ```

   - **你会看到**：① `PONG`；② 返回 `OK`（有 RedisJSON）；③ 返回 `OK`（有 RediSearch）。
   - **怎么判定**：
     - ② ③ 都是 `OK` → **路线 A**，`STAGE04_VECTOR_MODE=A`，继续第 2 步
     - ③ 报 `unknown command 'FT.CREATE'` → **路线 B**，`STAGE04_VECTOR_MODE=B`，跳到第 5 步
     - ② 报 unknown command 但 ③ 成功 → 少见，`RedisEmbeddingStore` 仍不能用（它写库靠 JSON.SET），走**路线 B**
   - **为什么**：`RedisEmbeddingStore` 同时依赖 JSON 和 Search。只验证一个会漏——只查 `MODULE LIST` 还可能被 ACL 挡住，直接用真实命令试探才是准的。探测用的 key 前缀是 `stage04:probe`，和正式数据（`rag:`）分开，删干净不留垃圾。

2. **怎么执行**（路线 A）：把 `RedisStoreConfig` 的 `EmbeddingStore` Bean 建出来，注入第 2.1 节的 `UnifiedJedis`，然后清掉残留索引并重建。
   **执行语句**：

   ```bash
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning FT.DROPINDEX rag-idx DD
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning --scan --pattern 'rag:*' | head
   ```

   - **你会看到**：第一条可能报 `unknown index name`（第一次跑就是这样，正常）；第二条为空。
   - **为什么**：**改了切块参数或 embedding 模型后，必须整库重建**。索引里存的是旧维度的旧向量，混着新的会算错。这个动作第 9 天会专门复盘。

3. **怎么执行**（路线 A）：重启后端（自动入库），再打一次第 4 天那句检索，然后去 Redis 里看真实存了什么。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/retrieve \
     -H 'Content-Type: application/json' \
     -d '{"question":"去上海出差住宿一晚最多报多少","topK":3,"minScore":0}'
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning FT.INFO rag-idx
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning --scan --pattern 'rag:*' | wc -l
   ```

   - **你会看到**：检索结果和第 4 天**一模一样**；`FT.INFO` 里有 `vector` 字段、`DIM 512`、`num_docs`；`rag:*` 的 key 数量等于块数。
   - **为什么**：业务结果没变，仓库变了——这就是「策略与仓库分层」的证据。`rag:*` 的 key 数就是你要的钱：每个块一条，第 13 天按它算存储。

4. **怎么执行**（路线 A）：临时改错密码，重启后端，看启动时的连通性检查拦住了什么。
   **执行语句**：

   ```bash
   REDIS_PASSWORD=wrong-password mvn spring-boot:run   # 或临时改 .env 后重启
   ```

   - **你会看到**：启动直接失败，异常信息里把 host、port 和原始错误都打出来了。
   - **为什么**：**内网 Redis 最常见的故障是「能连上端口但密码错 / 没权限」**。启动时一次性说清楚，比跑到检索才 500 好定位。改回正确密码重启恢复。

5. **怎么执行**（路线 B，若第 1 步判定不支持）：把向量库留在进程内，只把记忆仓库交给 Redis。要改两处。
   **执行语句**：

   ```bash
   # .env 里设为 B
   STAGE04_VECTOR_MODE=B
   ```

   代码上：

   ```java
   // 向量库：进程内 + 落盘，跨重启不丢
   private static final Path INDEX_FILE = Path.of("demos/04-rag/vector-data/index.json");

   InMemoryEmbeddingStore<TextSegment> store =
           Files.exists(INDEX_FILE)
                   ? InMemoryEmbeddingStore.fromFile(INDEX_FILE)
                   : new InMemoryEmbeddingStore<>();

   // 每次 ingest 结束后落盘
   store.serializeToFile(INDEX_FILE);
   ```

   ```java
   // 记忆仓库：走 Redis，不依赖 JSON 模块
   RedisChatMemoryStore memoryStore = RedisChatMemoryStore.builder()
           .host(host).port(port).password(password)
           .storeType(StoreType.STRING)   // 没 RedisJSON 时必须写 STRING
           .ttl(Duration.ofDays(7).toMillis())
           .build();
   ```

   - **你会看到**：检索结果和第 4 天一模一样；`redis-cli --scan --pattern '*chat*'` 能看到会话 key，但 `rag:*` 一个都没有（向量在本地文件里）。
   - **为什么**：路线 B 拿掉的是「服务端索引」，不是 RAG 本身。`EmbeddingStore` 这个接口没变，第 6–14 天的 retriever、评估、混合检索全部照旧。**`StoreType.STRING` 是关键**——默认的 `JSON` 需要 RedisJSON，你的 Redis 可能没有。

6. **怎么执行**（两条路都要）：重启后端（模拟进程重启），再打一次检索，确认向量没丢。
   - **你会看到**：路线 A 从 Redis 读，结果不变；路线 B 从 `index.json` 读，结果也不变。
   - **为什么**：这是两条路的**共同验收**——「重启后检索还在」才说明仓库选对了。把结果记进 §9 的「故障表现」栏，同时写明你走的是 A 还是 B。

---

### 第 6 天 · 完整链路：检索结果拼进提示词（里程碑）

**动手**：`【写代码】`

**学什么**：把前五天串起来：问题 → 检索 top-k → 把块拼成一段参考资料 → 连同问题发给 DeepSeek → 返回答案 + 引用来源。LangChain4j 用一个对象收口这件事：`ContentRetriever` 负责取，`ContentAggregator` 负责合并，`ContentInjector` 负责拼提示词，`RetrievalAugmentor` 把三者串起来。挂到 `AiServices` 上之后，业务代码里看不到检索。

**为了什么**：这是本阶段的里程碑。做完之后，第 7 天的过滤、第 10 天的评估、第 11 天的混合检索都是在换这三个组件里的某一个——**知道每个组件管哪一段，才知道该改哪个**。

**今天读**

- [LangChain4j：RAG 全链路（Retrieval Augmentor 一节）](https://docs.langchain4j.dev/tutorials/rag)。读完应能说出：`DefaultRetrievalAugmentor.builder()` 能挂 retriever / aggregator / injector，不挂就用默认的。
- [LangChain4j：AiServices 的 RAG 用法](https://docs.langchain4j.dev/tutorials/rag#connecting-retrieval-augmentor-with-ai-services)。读完应能说出：`.retrievalAugmentor(...)` 挂在 builder 上，接口方法不用改签名。

**要写的代码**

在 `com.example.demos.stage04.rag`：

| 类                          | 职责                                                                                                     |
| --------------------------- | -------------------------------------------------------------------------------------------------------- |
| `RagAssistant`            | `interface`，一个方法 `String answer(String question)`。由 `AiServices` 生成实现                    |
| `RagConfig`               | 装配 `ContentRetriever` → `DefaultRetrievalAugmentor` → `AiServices.builder(RagAssistant.class).retrievalAugmentor(...)` |
| `RagController`           | `POST /api/stage/04/query`，入参 `question`、`topK`，返回 `answer` + `sources`（每次检索到的块）    |
| `RagQueryResponse`        | `answer`、`sources`（`file` / `chunk` / `score` / `text`）、`promptChars`（拼完的提示词长度）    |

装配骨架：

```java
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(redisStore)
        .embeddingModel(embeddingModel)
        .maxResults(3)
        .minScore(0.5)
        .build();

RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
        .contentRetriever(retriever)
        .build();

RagAssistant assistant = AiServices.builder(RagAssistant.class)
        .chatModel(chatModel)               // 阶段 2 已有的 DeepSeek
        .retrievalAugmentor(augmentor)
        .build();
```

**系统提示词里必须写死两条规则**（写进 `RagAssistant` 接口上的 `@SystemMessage`）：

1. 「只根据参考资料回答；资料里没有的，直接说『查不到』，不要推测。」
2. 「回答末尾列出用到的资料出处（文件名 + 小节）。」

**伪代码**

```text
query(question, topK):
    contents = retriever.retrieve(question, topK)      // 第 4 天的检索
    if contents 为空:
        prompt = 问题（不带资料）
    else:
        prompt = injector.inject(问题, contents)       // 把块拼成「参考资料：…」
    answer = deepSeek.chat(system=两条规则, user=prompt)
    return { answer, sources: contents.map(出处), promptChars: prompt.length() }
```

**执行计划**

1. **怎么执行**：重启后端。这一步前端「RAG / 检索问答」要接通，才能看到 `answer` 和 `sources` 并排显示。**没接通之前先用 curl**。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/query \
     -H 'Content-Type: application/json' \
     -d '{"question":"去上海出差住宿一晚最多报多少","topK":3}'
   ```

   - **你会看到**：`answer` 里是 600，末尾带出处；`sources` 有 1~3 条，第一条是 `travel-expense.md`；`promptChars` 在 800~1200。
   - **为什么**：`answer` 里的数字来自 `sources` 里那条块的原文，不是模型编的。**核对方法：把 `sources[0].text` 和 `answer` 里的数字对一遍**。这是本阶段每天的验收动作。

2. **怎么执行**：问一个库里没有的问题，验证兜底规则。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/query \
     -H 'Content-Type: application/json' \
     -d '{"question":"公司年会在哪办","topK":3}'
   ```

   - **你会看到**：`answer` 说「查不到」或「参考资料中没有相关信息」，`sources` 里那几条是无关的块。
   - **为什么**：检索层永远会返回 k 条（第 4 天验证过）。挡住这一层的是**提示词规则 + minScore**。如果 `answer` 编了地点，说明规则没生效——检查是不是 `@SystemMessage` 被后面的话冲掉了。

3. **怎么执行**：把 `minScore` 调到 `0.75`，再问第 2 步那个问题。
   - **你会看到**：`sources` 变成空，`answer` 更干脆地说查不到。
   - **为什么**：两层兜底同时生效。但代价是：第 1 步那个真问题可能也被挡掉（分数在 0.6~0.7）。**minScore 是在「不漏」和「不乱」之间取平衡**，第 10 天用评估集把这条线量出来。

4. **怎么执行**：把所有 `sources[].text` 和 `answer` 抄进 `notes/rag.md` §9 的「完整链路」行。
   - **你会看到**：表里有了「问句 / 命中文件 / 答案是否与原文一致 / promptChars」四列。
   - **为什么**：第 10 天的评估集就是把这四列扩成 12 行。

---

### 第 7 天 · 元数据过滤：查不到别人的部门

**动手**：`【写代码】`

**学什么**：向量检索只按「像不像」排序，不看权限。四份文档里有两份是部门内部的（`tech` / `legal`），一个普通员工问「回滚谁审批」，语义检索照样会把技术部手册里的答案捞出来。今天在检索前加一道**元数据过滤**，让不同角色只看到自己能看的块。

**为了什么**：这是企业 RAG 和玩具 RAG 的分界线。你做过权限系统，会立刻认出这就是「数据行级权限」。向量库里没有 `WHERE`，但有 `filter`——它作用在检索**之前**，不是检索完再筛（后者会把 top-k 占满然后剩不下几条）。

**今天读**

- [LangChain4j：Metadata Filter](https://docs.langchain4j.dev/tutorials/rag#metadata-filter)。读完应能说出：`MetadataFilterBuilder.metadataKey("dept").isIn("all", "tech")` 生成一个 `Filter`，传给 `EmbeddingStoreContentRetriever.builder().dynamicFilter(...)`。
- 回看你熟的 Spring Security 数据权限那套思路，对照「过滤条件在 SQL 里」与「过滤条件在检索里」。

**要写的代码**

| 类                       | 职责                                                                                          |
| ------------------------ | --------------------------------------------------------------------------------------------- |
| `RagQueryRequest`      | 加一个字段 `dept`（调用方身份，取值 `all` / `tech` / `legal`）                          |
| `RagConfig`            | `EmbeddingStoreContentRetriever` 改用 `.dynamicFilter(query -> filterFor(dept))`          |
| `RagController`        | 把请求里的 `dept` 通过 `@V`（LangChain4j 的变量）传进查询，`dynamicFilter` 才能读到      |

**过滤器要自己拼**：LangChain4j 没有叫 `MetadataFilter` 的类，入口是 `MetadataFilterBuilder.metadataKey("dept").isIn("all", "tech")`，返回 `dev.langchain4j.store.embedding.filter.Filter`。`EmbeddingStoreContentRetriever` 的 builder 上叫 `dynamicFilter(Function<Query, Filter>)`（还有 `filter(Filter)` 是固定条件）。

`@V` 的用法（`dev.langchain4j.service.V`）：

```java
interface RagAssistant {
    @SystemMessage("只根据参考资料回答……")
    String answer(@V("question") String question, @V("dept") String dept);
}
```

过滤表达式：

```java
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

static Filter filterFor(String dept) {
    if ("tech".equals(dept))  return metadataKey("dept").isIn("all", "tech");
    if ("legal".equals(dept)) return metadataKey("dept").isIn("all", "legal");
    return metadataKey("dept").isEqualTo("all");
}
```

注意 `dynamicFilter` 拿到的是 `Query`，`aiServices` 的 `@V` 变量会挂在 `query.metadata()` 里——如果版本对不上，退一步：**在 Controller 里直接调 `retriever.retrieve()` 拿 contents，再自己拼提示词调 chat**，过滤照样生效。功能优先，装配方式可以降级。

**伪代码**

```text
query(question, dept):
    filter = dept == "tech"  ? dept in [all, tech]
           : dept == "legal" ? dept in [all, legal]
           :                   dept == all
    contents = retriever.retrieve(question, filter, topK)   // 过滤发生在检索之前
    answer = chat(只根据资料回答, 拼(contents) + question)
    return { answer, sources }
    # ops-runbook.md 的 dept 是 tech，contract-review.md 是 legal
```

**执行计划**

1. **怎么执行**：重启后端。先用「普通员工」身份问技术部手册里的问题。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/query \
     -H 'Content-Type: application/json' \
     -d '{"question":"线上回滚需要谁审批","topK":3,"dept":"all"}'
   ```

   - **你会看到**：`sources` 里**没有** `ops-runbook.md`，`answer` 说查不到。
   - **为什么**：过滤在检索前执行，`ops-runbook.md` 的块根本没进候选。这和「检索完再筛」有本质区别——后者会把 3 个名额浪费在看不到的块上。

2. **怎么执行**：同一个问题，换技术部身份。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/query \
     -H 'Content-Type: application/json' \
     -d '{"question":"线上回滚需要谁审批","topK":3,"dept":"tech"}'
   ```

   - **你会看到**：`sources[0].file` 是 `ops-runbook.md`，`answer` 能答出审批人。
   - **为什么**：同一个问题、同一个库，**只因为身份不同，结果不同**。这就是行级权限在 RAG 里的样子。

3. **怎么执行**：用 `dept=tech` 问一个差旅问题。
   - **你会看到**：照样能答出来，`sources` 里有 `travel-expense.md`。
   - **为什么**：`isIn("all","tech")` 里带了 `all`，公共文档对所有人可见。**别把过滤写成 `isEqualTo("tech")`**，那样技术部就看不到公共制度了。这一条记进 §9 的「权限」栏。

4. **怎么执行**：用 `dept=tech` 问法务的违约金问题。
   - **你会看到**：查不到，`sources` 里没有 `contract-review.md`。
   - **为什么**：部门之间横向隔离。企业 RAG 的权限往往是「本部门 + 公共」，不是「所有内部部门」。

---

### 第 8 天 · Spring AI 版同一件事

**动手**：`【写代码】`

**学什么**：Day 6 那套 `ContentRetriever` / `ContentAggregator` / `ContentInjector` / `RetrievalAugmentor`，在 Spring AI 里被压成两件东西：`VectorStore`（入库 + 检索）和 `RetrievalAugmentationAdvisor`（把检索结果拼进提示词，作为一个 Advisor 挂在 `ChatClient` 上）。

**为了什么**：看清同一套 RAG 在两个框架里的**映射关系**。你在阶段 2 已经做过一次 `@Tool` 的对照，这次是把「四个组件」对成「两个组件」。以后选框架时，判断依据是「谁的可替换点更细」，而不是「谁更流行」。

**今天读**

- [Spring AI：Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)。读完应能说出：`VectorStoreDocumentRetriever` + `RetrievalAugmentationAdvisor.builder()` 的装配顺序。
- [Spring AI：Redis Vector Store](https://docs.spring.io/spring-ai/reference/api/vectordbs/redis.html)。**只读配置项**（`spring.ai.vectorstore.redis.index`、`prefix`、`initialize-schema`），不要照抄依赖坐标——版本要和项目的 2.0.0 BOM 对齐。

**要写的代码**

**两条路的依赖不同，先看你在哪条路**：

路线 A（Redis 带 RediSearch + RedisJSON）——`pom.xml` 加两个依赖（版本走已有的 `spring-ai-bom` 2.0.0，不写 version）：

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-rag</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-vector-store-redis</artifactId>
</dependency>
```

路线 B（Redis 模块不全）——**只加 rag 一个**，向量库用 `SimpleVectorStore`：

```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-rag</artifactId>
</dependency>
```

```java
// SimpleVectorStore：纯 Java，不依赖任何 Redis 模块
VectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
vectorStore.add(chunks);                          // 入库
vectorStore.save(new File("demos/04-rag/vector-data/springai-store.json"));  // 落盘
```

> ⚠️ 三个约束：
>
> 1. `spring-ai-starter-vector-store-redis` 会把 `spring-boot-starter-data-redis` 拉进来，和已有的 LangChain4j `UnifiedJedis` 各连各的（一个走 `RedisTemplate`，一个走 Jedis）。两边共用一个 Redis 实例没问题，但**索引要用不同的 indexName**（`rag-idx` vs `rag-sai-idx`），避免自动配置覆盖昨天的索引。
> 2. **Spring Data Redis 不吃 `stage04.redis.*` 这套自定义配置**，它读 `spring.data.redis.*`。所以 `application.properties` 里要额外映射一份：
>
>    ```properties
>    spring.data.redis.host=${REDIS_HOST:localhost}
>    spring.data.redis.port=${REDIS_PORT:6379}
>    spring.data.redis.password=${REDIS_PASSWORD:}
>    spring.data.redis.database=${REDIS_DB:0}
>    spring.data.redis.timeout=5s
>    ```
>
> 3. **路线 B 时不要加 `spring-ai-starter-vector-store-redis`**。`RedisVectorStore` 同样依赖 RediSearch + RedisJSON，加了也起不来。用上面那段 `SimpleVectorStore`，把比较的重点放在「advisor 装配方式」而不是「存储实现」。

在 `com.example.demos.stage04.springai`：

| 类                            | 职责                                                                                          |
| ----------------------------- | --------------------------------------------------------------------------------------------- |
| `SpringAiRagConfig`         | `@Bean VectorStore`。如果自动配置不生效，手写 `RedisVectorStore.builder(jedisConnectionFactory).indexName("rag-sai-idx").initializeSchema(true).build()` |
| `SpringAiRagService`        | 注入 `ChatClient`（已有）+ advisor，`POST /api/stage/04/query-springai`                   |
| `RetrievalAugmentationAdvisor` | `builder().documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(store).similarityThreshold(0.5).topK(3).build()).build()` |

装配：

```java
Advisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
        .documentRetriever(VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(3)
                .build())
        .build();

String answer = chatClient.prompt()
        .advisors(ragAdvisor)
        .system("只根据参考资料回答；资料里没有的，直接说『查不到』。")
        .user(question)
        .call()
        .content();
```

**伪代码**

```text
入库（启动时或 POST /ingest-springai）:
    docs = 四份 markdown -> List<Document>（带 metadata: file, dept）
    // Spring AI 的 Document 需要自己切块，或用 TokenTextSplitter
    vectorStore.add(chunks)

查:
    answer = chatClient.prompt()
                .advisors( RetrievalAugmentationAdvisor(vectorStore, topK=3, threshold=0.5) )
                .system(只根据资料回答)
                .user(question).call().content()
    return { answer, source: "springai" }
```

**执行计划**

1. **怎么执行**：重启后端。这一步**不要求**结果和 Day 6 完全一样——两边的切块器、相似度阈值算法不同，分数不可比。只核对「答对没答对」。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/query-springai \
     -H 'Content-Type: application/json' \
     -d '{"question":"去上海出差住宿一晚最多报多少"}'
   ```

   - **你会看到**：`answer` 里有 600，`source` 是 `springai`。
   - **为什么**：两套框架都能完成「检索 → 拼提示词 → 生成」。差别在装配方式和可替换点，不在能力。

2. **怎么执行**：用同一个问题打 Day 6 的 LangChain4j 接口，把两边答案并排记进 §9。
   - **你会看到**：答案基本一致，`promptChars` 差异明显（两边的模板不同）。
   - **为什么**：模板不同，塞进提示词的字数就不同，钱也不同。第 13 天算成本时要用这个差异。

3. **怎么执行**：只改 `similarityThreshold`，从 `0.5` 调到 `0.85`，再问一次。
   - **你会看到**：检索不到任何资料，`answer` 说查不到。
   - **为什么**：两边都有「分数门槛」，但**分数不可跨框架比较**——LangChain4j 的 0.6 和 Spring AI 的 0.6 不是一回事（余弦 vs 其他度量、归一化方式不同）。所以第 10 天的评估脚本**只对一条链路建基线**，不要混着比。

---

### 第 9 天 · 铁律：换 embedding 模型会怎样

**动手**：`【写代码】`（故意做错，再修）

**学什么**：向量库里存的是**旧模型算出来的向量**。换 embedding 模型之后，如果只改代码不重建索引，新问题的向量和旧块的向量不在同一个坐标空间里，相似度没有任何意义。今天故意踩一次，看检索怎么坏掉，再用整库重建修好。

**两条路的现象不一样，都要知道**：

| | 路线 A（Redis 索引） | 路线 B（本地文件） |
| --- | --- | --- |
| 换维度后 | Redis 报 `Vector dimension mismatch`（硬报错） | `InMemoryEmbeddingStore` **不检查维度**，返回一堆低分块（静默错误） |
| 风险 | 一眼看出 | **更危险**：不报错，答案全错 |

所以 `rag:meta` 那道一致性检查在两条路上都必须做——它是路线 B 唯一能挡住静默错误的手段。

**为了什么**：这是「铁律」为什么是铁律。你见过「缓存和数据库不一致」的线上事故，这是同一类问题的 AI 版——**两边的编码必须相同**。以后换模型、升级模型、上多模型混跑，都要先问「索引里是什么模型算的」。

**今天读**

- [LangChain4j：Embedding Model 与 Embedding Store 的关系](https://docs.langchain4j.dev/tutorials/rag)。重点看「Embedding model and store must use the same dimension」相关段落。
- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval) 里换 embedding 模型的成本讨论（只读结论段）。

**要写的代码**

| 类                     | 职责                                                                                            |
| ---------------------- | ----------------------------------------------------------------------------------------------- |
| `EmbeddingConfig`    | 加一个 `@Bean("embeddingModelV2")`，换成另一个维度的模型（见下），**默认不启用**            |
| `IngestService`      | 记录入库时用的模型名 + 维度，存进 Redis 的一个 key `rag:meta`（`{"model":"bge-small-zh","dim":512}`） |
| `RetrieveController` | 检索前读 `rag:meta`，和当前配置的模型比对，不一致时**直接返回 409 并说明要重建**，不要静默检索 |

**用什么当「另一个模型」**：本机只有一个 ONNX 模型，不要再下一份。用一个**维度不同**的假 embedding 就够暴露问题——它会把问题向量算成 256 维，而索引里是 512 维：

```java
@Bean("embeddingModelV2")
EmbeddingModel broken() {
    return new EmbeddingModel() {
        public Response<Embedding> embed(String text) {
            return Response.from(Embedding.from(new float[256]));  // 故意错维度
        }
        public Response<Embedding> embed(TextSegment s) { return embed(s.text()); }
        public Response<List<Embedding>> embedAll(List<TextSegment> s) {
            return Response.from(s.stream().map(t -> embed(t.text()).content()).toList());
        }
    };
}
```

第 9 天的重点不是这个假模型多像真的，而是**它让维度对不上，故障一定暴露**。

**伪代码**

```text
ingest():
    store.add(embed(块))                  // 512 维
    redis.set("rag:meta", {model:"bge-small-zh", dim:512})

retrieve(question):
    meta = redis.get("rag:meta")
    if meta.dim != 当前模型.dimensions():
        return 409 { error:"EMBEDDING_MISMATCH", indexed: meta, current: {...}, hint:"POST /api/stage/04/ingest" }
    ...
```

**执行计划**

1. **怎么执行**：先正常检索一次，确认基线。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/retrieve \
     -H 'Content-Type: application/json' \
     -d '{"question":"去上海出差住宿一晚最多报多少","topK":3,"minScore":0}'
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning GET rag:meta
   ```

   - **你会看到**：`score` 0.6+，`rag:meta` 里是 `{"model":"bge-small-zh","dim":512}`。
   - **为什么**：先把「正常长什么样」记下来，待会对比。

2. **怎么执行**：把 `RetrieveController` 里注入的 `EmbeddingModel` 改成 `@Qualifier("embeddingModelV2")`，重启，再打同一句。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/retrieve \
     -H 'Content-Type: application/json' \
     -d '{"question":"去上海出差住宿一晚最多报多少","topK":3,"minScore":0}'
   ```

   - **你会看到**：两种可能，**都是问题**：
     - Redis 直接报维度不匹配（`Vector dimension mismatch`）→ 检索整个报错
     - 或者返回一堆 `score` 接近 0 的无关块 → 更危险，因为它**不报错**，只是答案全错
   - **为什么**：新向量和旧向量不在同一个空间。0.3 分的「最像」只是数学上最近，语义上毫无关系。**静默错误比报错难查**，所以第 3 步要加一致性检查。

3. **怎么执行**：把注入改回正确的模型，同时把 `rag:meta` 手动改成一个错的维度，验证一致性检查。
   **执行语句**：

   ```bash
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning SET rag:meta '{"model":"bge-small-zh","dim":1024}'
   curl -sS -i http://localhost:8080/api/stage/04/retrieve -H 'Content-Type: application/json' -d '{"question":"住宿标准","topK":3,"minScore":0}' | head -3
   ```

   - **你会看到**：HTTP 409，`error` 是 `EMBEDDING_MISMATCH`，提示去重建。
   - **为什么**：把「静默错误」变成「显式拒绝」。这和你在接口层做「版本号不匹配就 409」是同一个思路。

4. **怎么执行**：按提示重建，再检索一次恢复正常。
   **执行语句**：

   ```bash
   # 路线 A
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning FT.DROPINDEX rag-idx DD
   # 路线 B
   rm -f demos/04-rag/vector-data/index.json
   # 两条路都靠这一步重建
   curl -sS -X POST http://localhost:8080/api/stage/04/ingest
   curl -sS http://localhost:8080/api/stage/04/retrieve -H 'Content-Type: application/json' -d '{"question":"去上海出差住宿一晚最多报多少","topK":3,"minScore":0}'
   ```

   - **你会看到**：`indexed` 是块数，检索 `score` 回到 0.6+，`rag:meta` 是 512。
   - **为什么**：**换模型 = 重建索引**，没有第二条路。块可以复用（切法没变），向量必须重算。

---

### 第 10 天 · 评估：Recall / Precision / MRR

**动手**：`【写代码】`

**学什么**：RAG 的「效果好不好」不能靠感觉。用第 1 天那 12 条问题当测试集，对每条算三个指标：**Recall@k**（该找到的文件有没有出现在前 k 条里）、**Precision@k**（前 k 条里有多少是相关的）、**MRR**（第一条相关的排第几）。这三个指标你在搜索系统里用过，这里只是把「文档」换成「块」。

**为了什么**：第 11 天要改检索策略（混合、Rerank），第 13 天要动 top-k。**没有基线，任何「感觉变好了」都不算数**。这也是你熟悉的「改一次测一次」。

**今天读**

- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval) 的评估部分。读完应能说出：他们用「top-20 检索失败率」衡量，而不是看单个例子。
- 快速回顾你熟的搜索指标定义（Recall / Precision / MRR / NDCG 的区别），**只建立对应关系，不要推导公式**。

**要写的代码**

在 `com.example.demos.stage04.eval`：

| 类                   | 职责                                                                                             |
| -------------------- | ------------------------------------------------------------------------------------------------ |
| `EvalQuestion`     | `id`、`question`、`expectedFile`、`expectedKeyword`                                          |
| `EvalRunner`       | 读 `demos/04-rag/eval/questions.jsonl`，逐条跑检索，算指标                                        |
| `EvalController`   | `POST /api/stage/04/eval`，入参 `topK`、`strategy`，返回逐条明细 + 汇总                  |
| `EvalReport`       | `recallAtK`、`precisionAtK`、`mrr`、`failures`（哪几条没命中）                            |

判定规则（写死在代码里，12 条都按这一条判）：

```java
// 一条问题算「命中」，当且仅当：前 k 条里有任意一条的 file == expectedFile
// 且它的 text 里包含 expectedKeyword
boolean hit = matches.stream()
    .anyMatch(m -> m.file().equals(q.expectedFile()) && m.text().contains(q.expectedKeyword()));

// Precision@k = 命中条数 / k   （这里把「同文件且含关键词」的块算相关）
// Recall@k    = 命中的问题数 / 12
// MRR         = 平均(1 / 第一条命中的 rank)，一条都没命中记 0
```

指标计算：

```java
double recall = hits / (double) total;
double precision = totalRelevantBlocks / (double) (total * topK);
double mrr = sumOfReciprocalRanks / total;
```

**伪代码**

```text
eval(topK):
    qs = read(questions.jsonl)              // 12 条
    for q in qs:
        matches = retrieve(q.question, topK, minScore=0)
        relevant = matches.where(file == q.expectedFile && text 含 q.expectedKeyword)
        rank = relevant.firstRankOrZero()
        record(q.id, hit = rank > 0, rank, relevant.size())
    return {
      recallAtK: hits / 12,
      precisionAtK: sum(relevant.size) / (12 * topK),
      mrr: sum(1/rank for rank>0) / 12,
      failures: records.where(!hit)
    }
    # 这一步不调生成模型，只测检索
```

**执行计划**

1. **怎么执行**：重启后端，跑一次基线。今天是整阶段最重要的一次测量，**结果要抄进笔记**。
   **执行语句**：

   ```bash
   curl -sS -X POST http://localhost:8080/api/stage/04/eval \
     -H 'Content-Type: application/json' \
     -d '{"topK":3,"strategy":"recursive"}'
   ```

   - **你会看到**：`recallAtK` 大概在 0.7~0.9，`mrr` 在 0.6~0.85，`failures` 里两三条。
   - **为什么**：`recall` 不到 1 是正常的——本地小模型 + 简单切块，能到 0.8 已经够学后面的东西。**这就是基线**，第 11 天要和它比。

2. **怎么执行**：把 `topK` 改成 `1` 和 `5`，各跑一次，把三个数字记进 §9。
   **执行语句**：

   ```bash
   curl -sS -X POST http://localhost:8080/api/stage/04/eval -H 'Content-Type: application/json' -d '{"topK":1,"strategy":"recursive"}'
   curl -sS -X POST http://localhost:8080/api/stage/04/eval -H 'Content-Type: application/json' -d '{"topK":5,"strategy":"recursive"}'
   ```

   - **你会看到**：topK=1 时 `recall` 掉到 0.5~0.7；topK=5 时 `recall` 上升但 `precision` 下降。
   - **为什么**：**recall 和 precision 是此消彼长的**。k 越大越不容易漏，但塞进提示词的噪音也越多。第 13 天用这个曲线和成本一起定 k。

3. **怎么执行**：看 `failures` 里具体哪几条没命中，打开对应文档找原因。
   - **你会看到**：通常是两类——问题里的词和文档里的词完全对不上（纯语义问题，小模型弱），或者答案被切在两块之间（切块问题）。
   - **为什么**：**失败样例比总分有用**。把每条失败的原因写进 §9（「切块问题」还是「语义问题」），第 11 天的改进就按原因来，而不是盲目加 Rerank。

4. **怎么执行**：把 `strategy` 改成 `paragraph`（如果 Day 3 实现了），再跑一次。
   - **你会看到**：`recall` 可能略高或略低，但 `precision` 通常更差（块长短不一）。
   - **为什么**：指标能分辨出「哪种切法更好」，这是第 3 天凭肉眼看块长看不出来的。**结论写进 §9，别凭印象**。

---

### 第 11 天 · 混合检索、RRF、Rerank

**动手**：`【写代码】`

**学什么**：向量检索擅长语义，但会漏掉**精确词**（型号、编号、专有名词）。关键词检索（BM25）正好相反：词对得上就命中，但不懂同义词。今天把两路结果**融合**（RRF），再用一次模型调用**重排**（Rerank）。

**为了什么**：第 10 天失败样例里，凡是「问句和原文用词完全对不上」的，向量也救不了；凡是「问的是某个具体编号」，BM25 一击必中。真实系统几乎都是两路并用。这一天的目标是**用第 10 天的指标证明它确实变好了**，而不是「业界都这么做」。

**今天读**

- [Anthropic：Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)。**这次读全文**，重点看「BM25 + embedding 混合」和「为什么混合比单路好」。
- [LangChain4j：Content Aggregator / RRF](https://docs.langchain4j.dev/tutorials/rag#content-aggregator)。读完应能说出：`ReciprocalRankFuser` 就是 RRF，按排名倒数相加，不看原始分数。

**要写的代码**

在 `com.example.demos.stage04.hybrid`：

| 类                        | 职责                                                                                     |
| ------------------------- | ---------------------------------------------------------------------------------------- |
| `Bm25Index`             | 内存倒排。入库时对每个块分词（中文按 2-gram 切，避免引分词器），算 TF-IDF / BM25 分数    |
| `HybridRetriever`       | 向量 top-k + BM25 top-k 两路各取，用 **RRF** 融合：`score = Σ 1/(60 + rank)`       |
| `LlmReranker`           | 把融合后的 10 条候选一起发给 DeepSeek，让它按「能否回答这个问题」打 0-10 分，取前 3 |
| `EvalController`        | `strategy` 增加 `hybrid` 和 `hybrid+rerank` 两个取值                                   |

不用引第三方 BM25 库，**自己写够用**：中文按 2-gram 切，IDF 用 `log(N / df)`，BM25 的 `k1=1.2`、`b=0.75`。约 60 行。

RRF 核心就三行：

```java
Map<String, Double> fused = new HashMap<>();
for (List<Match> list : List.of(vectorHits, bm25Hits)) {
    for (int rank = 0; rank < list.size(); rank++) {
        fused.merge(list.get(rank).id(), 1.0 / (60 + rank + 1), Double::sum);
    }
}
```

**为什么 Rerank 用 LLM 而不是再下一个模型**：LangChain4j 的 `ReRankingContentAggregator` 需要一个 `ScoringModel`，而官方预打包的 cross-encoder 打分模型没有对应的 `-embeddings-*` 式 artifact（只有抽象的 `OnnxScoringModel`，要自己找模型文件）。**本阶段不再拉第二个本地模型**，用一次 DeepSeek 调用做重排更省事，也符合「生产上常用 LLM rerank」的做法。`ReRankingContentAggregator` 留作只读对照。

**伪代码**

```text
hybridRetrieve(question, k):
    vHits  = 向量检索(question, k)          // 第 6 天那套
    bHits  = bm25.search(question, k)
    fused  = RRF(vHits, bHits)              // 两路排名倒数相加，与原始分数无关
    return fused.take(k)

hybridPlusRerank(question):
    cands = hybridRetrieve(question, 10)
    scores = deepSeek("给每条候选打 0-10 分，只输出 JSON 数组: [{id, score}]", cands)
    return cands.sortedBy(scores).take(3)
```

**执行计划**

1. **怎么执行**：重启后端。用第 10 天同样的参数跑混合检索，和第 10 天的基线并排比。
   **执行语句**：

   ```bash
   curl -sS -X POST http://localhost:8080/api/stage/04/eval \
     -H 'Content-Type: application/json' \
     -d '{"topK":3,"strategy":"hybrid"}'
   ```

   - **你会看到**：`recall` 比 `recursive` 基线高，`failures` 少一两条。
   - **为什么**：向量漏掉的精确词问题被 BM25 补上了。**如果 recall 没涨，说明失败原因不是关键词，回去看第 10 天 step 3 的分类**——别硬加组件。

2. **怎么执行**：再加 Rerank。
   **执行语句**：

   ```bash
   curl -sS -X POST http://localhost:8080/api/stage/04/eval \
     -H 'Content-Type: application/json' \
     -d '{"topK":3,"strategy":"hybrid+rerank"}'
   ```

   - **你会看到**：`precision` 明显提升，`mrr` 上升（第一条更常是对的），`recall` 基本不变或略降。
   - **为什么**：Rerank 只是**重排候选**，不会把没进候选的块捞回来。所以它提升的是「排序质量」而不是「覆盖率」——**recall 靠召回，precision/MRR 靠重排**，两个问题两种药。

3. **怎么执行**：只对一条具体问题，把两种策略的结果并排看。
   **执行语句**：

   ```bash
   curl -sS -X POST http://localhost:8080/api/stage/04/retrieve -H 'Content-Type: application/json' -d '{"question":"线上回滚需要谁审批","topK":3,"minScore":0,"strategy":"hybrid"}'
   ```

   - **你会看到**：`ops-runbook.md` 相关块排到前面。
   - **为什么**：这个问题里有「回滚」「审批」两个明确的词，BM25 命中率高。**混合检索的收益集中在「有明确术语」的问题上**，纯口语化的「挂了我该干啥」收益小。

4. **怎么执行**：把三组指标（基线 / hybrid / hybrid+rerank）整理成 §9 的对照表。
   - **你会看到**：一张有 3 行 3 列的指标表。
   - **为什么**：第 14 天的结论第 1 条要用这张表。**没有这张表，「加了 Rerank」不能算完成**。

---

### 第 12 天 · 记忆：窗口留在 Redis，旧对话进向量库

**动手**：`【写代码】`

**学什么**：阶段 3 的 `MessageWindowChatMemory` 默认存在进程内存，重启就没了。今天换两件事：**仓库换成 Redis**（重启后会话还在），以及**长期记忆**——把历史对话做成可检索的片段，下一轮按语义捞回来。短期窗口和长期向量记忆是**两层**，不是二选一。

**为了什么**：这是「记忆设计」在阶段 4 的落点。窗口管「最近说了什么」（便宜、有上限），向量记忆管「很久以前提过什么」（贵、能跨会话）。你做过 Redis 会话，这一套映射很直接。

**今天读**

- 回看阶段 3 `stage-3.md` 第 5–6 天的窗口记忆与仓库分层，**这次读代码而不是读文字**。
- [LangChain4j：Chat Memory](https://docs.langchain4j.dev/tutorials/chat-memory)。只读 `ChatMemoryStore` 与 `MessageWindowChatMemory.builder()` 两节。

**要写的代码**

在 `com.example.demos.stage04.memory`：

| 类                          | 职责                                                                                                |
| --------------------------- | --------------------------------------------------------------------------------------------------- |
| `RedisMemoryConfig`       | 用第 2.1 节的 host/port/password 建 `RedisChatMemoryStore`，**没 RedisJSON 时必须 `storeType(StoreType.STRING)`**；外面套 `MessageWindowChatMemory.builder().maxMessages(10).id(...)` |
| `LongTermMemory`          | `save(conversationId, userText, answerText)`：把一轮问答拼成一段，embed 之后存进**同一个** `EmbeddingStore`，`dept` 设为 `memory` |
| `MemoryRagService`        | 每轮：① 用短期窗口拼历史 ② 用问题在 `dept=memory` 里检索历史 ③ 两者都进提示词 ④ 回答后 `save()` |
| `MemoryController`        | `POST /api/stage/04/memory`，入参 `conversationId`、`question`；`GET /api/stage/04/memory/{id}` 看 Redis 里存了什么 |

记忆仓库的装配（`host/port/password` 从 `@Value` 或 `RedisConnectionConfig` 拿，**不要写死 localhost**）：

```java
RedisChatMemoryStore store = RedisChatMemoryStore.builder()
        .host(host).port(port).password(password)
        .storeType(StoreType.STRING)                     // 路线 B 必须；路线 A 可用默认 JSON
        .prefix("stage04:chat:")                         // 和探测用 key 分开
        .ttl(Duration.ofDays(7).toMillis())              // 生产必须设，否则 Redis 被历史撑满
        .build();

ChatMemory memory = MessageWindowChatMemory.builder()
        .chatMemoryStore(store)
        .maxMessages(10)
        .id(conversationId)
        .build();
```

> 长期记忆和文档**共用一个索引**，靠 `dept=memory` 区分。路线 A 是同一个 Redis 索引；路线 B 是同一个本地 `index.json`。这是有意的简化：学的是「同一套向量检索也能存对话」，不是再造一个库。代价是检索记忆时要带 `dept` 过滤，否则会捞到文档块。

**伪代码**

```text
chat(conversationId, question):
    history   = windowMemory(conversationId).messages()        // 最近 10 条，从 Redis 读
    recalled  = retrieve(question, filter dept == "memory", topK=3)
    prompt    = system(规则) + "【历史对话片段】" + recalled + "【近期对话】" + history + question
    answer    = deepSeek(prompt)
    windowMemory.add(user(question)); windowMemory.add(ai(answer))
    longTerm.save(conversationId, question, answer)            // 再 embed 一次
    return { answer, recalledCount: recalled.size(), windowSize: history.size() }
    # 注意：longTerm.save 也会走同一个 EmbeddingModel，铁律仍然成立
```

**执行计划**

1. **怎么执行**：重启后端。先用新会话问一件「值得记住」的事。
   **执行语句**：

   ```bash
   curl -sS http://localhost:8080/api/stage/04/memory \
     -H 'Content-Type: application/json' \
     -d '{"conversationId":"c1","question":"我们组下季度预算 30 万，主要花在差旅上。"}'
   ```

   - **你会看到**：正常回答；`windowSize` 是 0（还没写回时的口径）或 1。
   - **为什么**：这一句本身没什么特别，但 `longTerm.save` 把它 embed 进了库。

2. **怎么执行**：重启后端（模拟进程重启），再用**同一个** `conversationId` 问一个不点名的问题。
   **执行语句**：

   ```bash
   # 先 Ctrl+C 停掉后端，再 mvn spring-boot:run
   curl -sS http://localhost:8080/api/stage/04/memory \
     -H 'Content-Type: application/json' \
     -d '{"conversationId":"c1","question":"我刚才说预算主要花在哪？"}'
   ```

   - **你会看到**：`windowSize` 不是 0（窗口从 Redis 读回来了），`recalledCount` 至少 1，回答能说出「差旅」。
   - **为什么**：两件事同时验证了——窗口换了仓库所以活着；长期记忆按语义捞回了上一轮，**即使它已经被窗口裁掉**。

3. **怎么执行**：换一个新的 `conversationId`（`c2`）问同一句话。
   - **你会看到**：`windowSize` 是 0，`recalledCount` 仍然是 1（长期记忆**跨会话**可见）。
   - **为什么**：这正是两层记忆的差别。窗口按会话隔离，向量记忆是全局的（按 `dept` / 人过滤是下一步的事）。**如果你不希望跨会话可见，就要在记忆块上带 `conversationId` 元数据并过滤**——这是设计选择，不是 bug。

4. **怎么执行**：看 Redis 里到底存了什么。
   **执行语句**：

   ```bash
   redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning --scan --pattern '*c1*' | head
   curl -sS http://localhost:8080/api/stage/04/memory/c1 | head -c 400
   ```

   - **你会看到**：窗口 key（由 `RedisChatMemoryStore` 命名）+ 记忆块的向量 key。
   - **为什么**：**「记忆」落到存储上就是两个 key 空间**。想清这一点，阶段 6 的会话持久化就不会混。

---

### 第 13 天 · 成本：top-k 与 chunk 长度怎么换算成钱

**动手**：`【调用后端】`（不改代码）

**学什么**：RAG 的账单有三个来源：**入库时的 embedding 调用**（本地模型免费，但要算 CPU 时间）、**每次提问的检索**（免费，但占内存）、**拼进提示词的块**（这是真花钱的部分）。今天把 `promptChars`、`topK`、`chunk` 长度换算成每次提问的 token 数，并算出「一天 1000 次提问」的量级。

**为了什么**：RAG 最常见的浪费是「top-k 设 10，其实 3 条就够」。你在做接口性能优化时算过「一次请求多查一张表值不值」，这是同一道题。**没有成本数字，「效果更好」就无上限**。

**今天读**

- 回看 `notes/llm-basics.md` 的 Token 估算（中文 1 字 ≈ 1.5~2 token）。
- [DeepSeek 定价页](https://api-docs.deepseek.com/zh-cn/quick_start/pricing)。只记输入 / 输出的单价，**不要背**，用的时候查。

**今天要做的事**

1. 用同一句问题，在 `topK = 1 / 3 / 5` 下各打一次 `/query`，记 `promptChars`
2. 填一张成本表（下面给模板）
3. 用 `chunkSize = 300` 与 `600` 各跑一次，比较 `promptChars`

**成本表模板**（抄进 `notes/rag.md` §9 末尾）

| topK | chunkSize | 平均 promptChars | 估算输入 token | 单次输入成本 | 1000 次/天 | recall@3 |
| ---- | --------- | ---------------- | -------------- | ------------ | ---------- | -------- |
| 1    | 300       |                  |                |              |            |          |
| 3    | 300       |                  |                |              |            |          |
| 5    | 300       |                  |                |              |            |          |
| 3    | 600       |                  |                |              |            |          |

> 估算输入 token ≈ `promptChars × 1.6`（中文偏保守）。单次输入成本 = token × 单价 / 1e6。

**执行计划**

1. **怎么执行**：用同一个问题，改 `topK` 打三次。**每打一次把 `promptChars` 抄下来**。
   **执行语句**：

   ```bash
   for k in 1 3 5; do
     curl -sS http://localhost:8080/api/stage/04/query \
       -H 'Content-Type: application/json' \
       -d "{\"question\":\"去上海出差住宿一晚最多报多少，需要什么发票\",\"topK\":$k}" \
       | python3 -c "import sys,json;d=json.load(sys.stdin);print('topK=$k', 'promptChars=', d['promptChars'], 'sources=', len(d['sources']))"
   done
   ```

   - **你会看到**：`promptChars` 随 topK 近似线性增长（每多一条块多约 300 字）。
   - **为什么**：**钱是按块数线性涨的**。topK 从 3 到 5，成本涨 60% 左右，但 recall 只涨几个点（第 10 天有数）——这笔买卖划不划算，要拿指标换。

2. **怎么执行**：把 `IngestService` 的 `chunkSize` 从 300 改成 600，重建索引，再用 `topK=3` 打同一句。
   - **你会看到**：`promptChars` 翻倍，但 `sources` 还是 3 条，答案未必更好。
   - **为什么**：**块大 = 每次塞进提示词的内容多 = 贵**。而且大块会把无关内容一起带进来，反而稀释 prompt。切块大小是个成本旋钮，不只是效果旋钮。

3. **怎么执行**：按上面的表算完，写一句结论：以当前 recall 曲线，你选 topK 几、chunkSize 多少，理由是什么。
   - **你会看到**：一句话结论 + 一张填满的表。
   - **为什么**：这是第 14 天收口的第 2 条。**「我选 topK=3，因为 recall 相对 topK=1 提升最多，而成本只涨 x 倍」** 这种句子，面试和生产评审都能用。

---

### 第 14 天 · 收口：填表、写三句话

**动手**：`【不写代码】`

**学什么**：把 13 天的数字收成三句能指着页面讲出来的结论，并划清阶段 4 和阶段 5 / 6 的边界。

**为了什么**：和阶段 1、2、3 的收口一样——**没有结论表，这个阶段不算完成**。

**今天读**

- 回看 `docs/learning-plan.md` 阶段 4 的 S4-T1～T8，逐条对照「知识完成项」「操作完成项」有没有全勾。
- 回看 `notes/rag.md` 的 §9 对照表，把还空着的行补上。

**今天要做的事**（全在已有页面上，不改代码）

1. `http://localhost:5173/demos/rag/query`：问 3 个不同部门的同一句话，确认过滤生效
2. `http://localhost:5173/demos/rag/eval`：跑一次评估，把三个指标截图/抄下来
3. `http://localhost:5173/demos/rag/memory`：两个会话 ID 交替问，看窗口与召回数
4. 在 `notes/rag.md` §9 末尾写明：**你走的路线 A 还是 B**，以及判定的依据（哪个命令报了什么）

**填三句话**（模板在 `notes/rag.md` 文末，用 §9 表里的原文填空）

```text
1. 检索质量：基线 recall@3 = ___，加混合后 = ___，加 Rerank 后 = ___；
   改进最大的是 ___，因为失败样例里 ___ 类问题最多。
2. 成本取舍：topK 从 1 到 3，recall 从 ___ 涨到 ___，promptChars 从 ___ 涨到 ___；
   我选 topK=___，chunkSize=___。
3. 边界：本阶段窗口记忆在 ___，长期记忆在 ___；文档级权限用 ___ 实现；
   换 embedding 模型必须 ___，否则 ___。MCP 与多 Agent 留到阶段 ___。
```

**验收清单**

- [ ] 四份夹具文档 + 12 条评估问题已入库（`demos/04-rag/`）
- [ ] `stage04` 有：embedding / split / retrieve / query / query-springai / eval / memory 七组接口
- [ ] `POST /api/stage/04/query` 能答对至少 10/12 条评估问题
- [ ] 元数据过滤：`dept=all` 看不到技术部手册，`dept=tech` 能看到
- [ ] 换维度后返回 409，重建后恢复
- [ ] 三组指标（基线 / hybrid / hybrid+rerank）在同一张表里
- [ ] 成本表四行填满
- [ ] 后端重启后 `c1` 会话还能接上（窗口来自 Redis），向量索引也能重新加载
- [ ] `notes/rag.md` §9 有数据、三句话已填
- [ ] `demos/frontend/demos.config.ts` 里 `rag` 的 `status` 已改为 `ready`

---

## 本阶段不要做的事

| 别做                                   | 为什么                                                    | 留到   |
| -------------------------------------- | --------------------------------------------------------- | ------ |
| 接真实公司文档                         | 夹具已经能暴露全部问题；真实文档要处理格式、权限、更新    | 阶段 6 |
| 上 Milvus / Elasticsearch              | 单个内网 Redis（或本地索引文件）足够学机制，分布式向量库是运维问题不是 RAG 问题 | 阶段 6 |
| 微调 embedding 模型                    | 需要训练数据和 GPU，收益远小于改切块与混合检索            | 不安排 |
| 做多轮 query 改写（HyDE / Multi-Query） | LangChain4j 有现成组件，但第 10 天的指标已说明瓶颈在切块  | 阶段 5 |
| 用 Docker 起整套环境                   | Redis 已在内网可直接用，不必再起一套                      | 阶段 6 |

