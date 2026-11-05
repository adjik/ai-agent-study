# 阶段 0：环境搭建

本目录保留 **Python 对照脚本**；Java 代码已迁入统一后端 `demos/backend` 的 `stage00` 包。

## 内容

| 文件/目录 | 说明 |
|-----------|------|
| `hello_llm.py` | Python 版 4 个 Demo，验证 DeepSeek API |
| `../backend/stage00/` | Java 版（Spring AI），与 Python 对照 |

## 启动 Java（统一后端）

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd demos/backend
mvn spring-boot:run
```

接口示例：

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/stage/00/demo/hello
# 旧路径仍可用：/api/demo/hello、/api/test/hello?msg=...
```

前端控制台：`cd demos/frontend && npm run dev`

> 原 `hello-llm-java/` 独立工程已合并进 `demos/backend`，不再单独维护。
