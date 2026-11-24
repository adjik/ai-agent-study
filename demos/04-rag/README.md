# 阶段 4：RAG 与记忆 —— 数据与评估夹具

Java 代码不在这里，在统一后端 `demos/backend/src/main/java/com/example/demos/stage04`（端口 8080）。
每天做什么、怎么验收：见 [docs/stage-4.md](../../docs/stage-4.md)。知识点：见 [notes/rag.md](../../notes/rag.md)。

## 目录

| 路径 | 用途 |
| --- | --- |
| `data/` | 四份虚构公司制度（差旅、请假、运维、合同），RAG 的文档夹具 |
| `eval/questions.jsonl` | 12 条评估问题：问题 + 期望命中文件 + 期望关键词 |

## 元数据约定

四份文档按部门分级，第 7 天用 `dept` 做检索前过滤：

| 文件 | `dept` | 谁能看 |
| --- | --- | --- |
| `travel-expense.md` | `all` | 全员 |
| `leave-policy.md` | `all` | 全员 |
| `ops-runbook.md` | `tech` | 技术部 + 全员可见部分 |
| `contract-review.md` | `legal` | 法务部 + 全员可见部分 |

过滤写法是 `isIn("all", dept)`——**公共 + 本部门**，不是 `isEqualTo(dept)`。

## 评估问题怎么用

`questions.jsonl` 每行一个 JSON：

```json
{"id":1,"question":"去上海出差住宿一晚最多报多少","expectedFile":"travel-expense.md","expectedKeyword":"600"}
```

一条问题算「命中」，当且仅当前 k 条里有任意一条同时满足：

1. `file` 等于 `expectedFile`
2. 块原文包含 `expectedKeyword`

`expectedKeyword` 必须是文档里的**原文片段**（改文档时同步改这里，否则指标失真）。

## Redis 连接

用你已有的内网 Redis，**不在本机装服务**。项目根目录 `.env`（不入库）：

```properties
REDIS_HOST=10.0.0.12
REDIS_PORT=6379
REDIS_PASSWORD=你的密码
REDIS_DB=0
```

`application.properties` 只引用变量名。两条路按第 5 天的探测结果决定：

| 路线 | 判定 | 向量存哪 | 记忆存哪 |
| --- | --- | --- | --- |
| A | `FT.CREATE` 与 `JSON.SET` 都 OK | Redis（`RedisEmbeddingStore`，索引 `rag-idx`，前缀 `rag:`） | Redis（`StoreType.JSON`） |
| B | 任一命令 unknown | `vector-data/index.json`（`InMemoryEmbeddingStore` + `serializeToFile`） | Redis（`StoreType.STRING`） |

## 重建索引的固定动作

改了切块参数、换了 embedding 模型、或改了文档之后：

```bash
# 路线 A
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning FT.DROPINDEX rag-idx DD
curl -sS -X POST http://localhost:8080/api/stage/04/ingest

# 路线 B
rm -f demos/04-rag/vector-data/index.json
curl -sS -X POST http://localhost:8080/api/stage/04/ingest
```

**不重建就会命中旧向量**，第 9 天专门演练这个故障。`vector-data/` 已在 `.gitignore`，不会入库。
