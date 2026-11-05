# AI Agent Demos — 统一后端

各学习阶段的 Java 代码集中在一个 Spring Boot 服务里，按 `stage00` / `stage01` / … 包区分。只需启动一次（端口 **8080**），前端 `demos/frontend` 通过 `/api` 代理调用。

## 快速开始

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd demos/backend
mvn spring-boot:run
```

在项目根目录 `.env` 或环境变量中配置 `DEEPSEEK_API_KEY`。

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/stage/00/demo/hello
```

配合前端控制台：

```bash
cd demos/frontend && npm run dev
# 浏览器 http://localhost:5173
```

## 目录结构

```
demos/backend/
├── pom.xml
└── src/main/java/com/example/demos/
    ├── DemosApplication.java          # 启动类
    ├── common/                        # 各阶段共享
    │   ├── config/                    # DeepSeek、CORS、.env、异常处理
    │   └── web/                       # /api/health、/api/stages
    ├── stage00/                       # 阶段 0：LLM 入门（已实现）
    │   ├── api/                       # DTO / record
    │   ├── controller/                # REST 接口
    │   └── service/                   # 业务逻辑
    ├── stage01/                       # 阶段 1：提示词工程
    ├── stage02/                       # 阶段 2：Tool Use（按 docs/stage-2.md D1 起实现）
    └── ...
```

## API 约定

| 用途 | 路径模式 | 示例 |
|------|----------|------|
| 健康检查 | `/api/health` | 全局 |
| 阶段索引 | `/api/stages` | 列出各阶段状态 |
| 阶段 N 接口 | `/api/stage/{NN}/...` | `/api/stage/00/demo/hello` |

阶段 0 同时保留旧路径（学习笔记兼容）：`/api/demo/*`、`/api/test/*`。

## 新增阶段

1. 在 `com.example.demos.stageNN` 下建 `controller` / `service` / `api`
2. Controller 的 `@RequestMapping` 使用 `/api/stage/NN/...`
3. 更新 `demos/frontend/demos.config.ts`：登记 `apiPath`、改 `status` 为 `ready`、补 Vue 页面

## 技术栈

| 项目 | 版本 |
|------|------|
| JDK | 21 |
| Spring Boot | 4.0.0 |
| Spring AI | 2.0.0 |
| LLM | DeepSeek（OpenAI 兼容接口） |
