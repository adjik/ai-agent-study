# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目性质

这是一个 **AI Agent 学习工作区**（不是可部署的软件项目）。用户是一名资深 Java Web 后端工程师（Spring Boot / Dubbo / MySQL / Redis），正在从零系统学习 AI Agent 开发。当前处于**阶段 2（Function Calling / Tool Use）资料已就绪、代码实验待做**；阶段 0 已完成；阶段 1 资料已按三层约定拆完。

学习路线：**Java 生态为主**（Spring AI 2.0 + LangChain4j + Spring AI Alibaba，直接在 Spring Boot 技术栈上构建 Agent），Python 生态为辅（用于快速实验、阅读社区教程时理解概念）。两条腿走路，但 Java 为主线，80% 投入 Java，20% 了解 Python。

## 环境

- **Java（主线）**: JDK 21（Spring AI 2.0 硬性要求），Spring Boot 4.x，Maven 构建。默认 JAVA_HOME 保持 JDK8，IDE 内部指定 SDK，终端按需 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`
- **Python（辅助）**: 3.12.13（Homebrew 安装），虚拟环境位于 `venv/`。激活方式：`source venv/bin/activate`。仅用于快速实验和阅读 Python 教程时跑示例，不要求产出
- **LLM API**: 使用 DeepSeek API（OpenAI 兼容格式），API Key 存放在环境变量 `DEEPSEEK_API_KEY`（不硬编码、不入库）

## 目录结构与含义

```
ai-agent-study/
├── docs/                  # 学习计划（learning-plan.md）+ 阶段详案（stage-1.md / stage-2.md）+ 资料清单（resources.md）
├── notes/                 # 按主题的学习笔记（llm-basics.md / prompt-engineering.md / tool-use.md）
├── demos/                 # 练手代码，按阶段递增（Java 为主线产出）
│   ├── backend/           # 统一 Java 后端（单端口 8080），按 stage00～stage06 包区分各阶段代码
│   ├── frontend/          # 统一 Vue 控制台（/api 代理到 backend）
│   ├── 00-env/            # 阶段 0：hello_llm.py（Python 对照）+ stage00 Java 在 backend
│   ├── 01-hello-llm/      # 阶段 1：提示词工程（Python 对照 + stage01 Java 在 backend）
│   ├── 02-tool-use/       # 阶段 2：Function Calling（stage02 在 backend）
│   ├── 03-agent-core/     # 阶段 3：Agent 框架（stage03 在 backend）
│   ├── 04-rag/            # 阶段 4：RAG（stage04 在 backend）
│   ├── 05-multi-agent/    # 阶段 5：MCP / 多 Agent（stage05 在 backend）
│   └── 06-project/        # 阶段 6：综合落地（stage06 在 backend）
├── resources/             # 下载的资料、模型配置等
└── venv/                  # Python 虚拟环境（辅助用）
```

## 关键约定

1. **各阶段学习目录**（`00-env/`～`06-project/`）放 README 与 Python 对照脚本；**Java 代码统一在 `demos/backend`**，按 `stageNN` 包区分，单 Maven 工程（Spring Boot 4.x + JDK 21）
2. **notes/ 下写全必知点和怎么做**——知识拆成 `K*`（每节：必知 / 怎么做 / 指定资料，勾「已读完」）。操作拆成 `D*-O*` 放 `docs/stage-N.md`（给步骤、可粘贴例子、判定标准、博客链接，不下开放题）。总计划 `S{N}-T*` 打勾 = 对应 D + K 全部完成。
3. **API Key 一律环境变量**，`.env` 文件不入库（已在 .gitignore 排除），Spring Boot 中通过 `application.yml` 的 `${DEEPSEEK_API_KEY}` 引用
4. **学习进度记录**在 README.md 和 docs/learning-plan.md 中，实际进度定期回写更新
5. **demos 下各目录目前主要是占位**；`00-env/` 有 Java/Python 产出，`01-hello-llm/` 与 `02-tool-use/` 有实验规范 README（Java 按手册天数写进 `backend`），`frontend/` 是统一 Vue 控制台

## 已完成的产出

- `demos/backend/` — 统一 Spring Boot 后端（:8080），`stage00` 含 4 个 LLM Demo + 测试接口
- `demos/00-env/hello_llm.py` — 4 个 Demo：简单问答、系统提示词+多轮对话、结构化 JSON 输出、流式输出。使用 OpenAI SDK 连接 DeepSeek API（验证 API 可用性）
- `demos/frontend/` — Vue 3 控制台：一个前端通过 /api 代理调用统一后端（阶段 0 已接通）
- `notes/llm-basics.md` — LLM 基础认知笔记（Token、上下文窗口、温度、幻觉、指令遵循）
- `notes/prompt-engineering.md` — 阶段 1 知识 K1–K20（必知 / 怎么做 / 资料）+ 文末对照表
- `docs/learning-plan.md` — 6 阶段任务 S*-T*；阶段 2 整合在 stage-2.md
- `docs/stage-1.md` — 阶段 1 操作手册（与 prompt-engineering 分两文件）
- `docs/stage-2.md` — 阶段 2 **整合学习文档**（必知 K*、D0–D10、写代码日历、示例效果、§9）
- `docs/resources.md` — Java 生态框架选型对比、课程、开源仓库、书籍汇总 + 阶段 1 / 阶段 2 专题资料
- `demos/01-hello-llm/README.md` — 六个 prompt 实验的接口与目录约定
- `demos/02-tool-use/README.md` — Tool Use 实验规范（Java 在 backend/stage02，代码待按 D1 起实现）

## 用户背景（影响代码解释和类比方式）

- 资深 Java Web 后端，B 端大型项目交付经验
- 技术 Stack：JDK8 / Spring Boot / MyBatis Plus / MySQL / Dubbo / ZooKeeper / Redis
- 擅长：分布式系统设计、缓存/数据库建模、微服务治理、工程化交付
- 正在学习的：LLM API、Prompt 工程、Agent 编排、RAG、评估与可观测（**Java 主线实现**，Python 仅辅助理解概念）
- **解释概念时优先用 Java 后端经验做类比**（如 Agent 状态管理 ↔ 分布式会话、RAG ↔ 缓存+索引、MCP ↔ Dubbo/OpenAPI、Agent 评估 ↔ 测试体系/SLA）

## 学习原则

- 每阶段必须有可运行产出，代码留在 demos/ 对应目录。Java 是主力产出语言，Python 仅限快速实验
- 先手写循环再用框架（如阶段 3 先用纯 Java HTTP Client + Jackson 手写 Agent Loop，再学 Spring AI / LangChain4j）
- 从阶段 2 起尽量往企业级场景靠（Tool 封装真实业务接口、RAG 用真实业务文档、对接 MySQL/Redis）
- 改一次测一次，用 JUnit 测试集 + 评估数据说话而非感觉
- **Java/Spring 生态是主角，Python 是配角**——解释概念时优先用 Java 后端经验做类比，写代码时默认用 Java
- 阶段笔记每节写清必知、怎么做、指定资料；每天按 `docs/stage-N.md` 的 `D*-O*` 执行（给例子和链接，不问怎么做）；`S{N}-T*` 打勾当且仅当对应 K 与 D 都完成。博客优先于长课程 / 书
