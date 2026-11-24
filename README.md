# AI Agent 学习工作区

> 从资深 Java Web 后端工程师到 AI Agent 开发者
> 创建日期：2026-08-01 | 环境：macOS

## 目标

系统学习 AI Agent 开发，从零基础到能够独立交付生产可用的 Agent 应用，并最终与自身的 Java/B 端业务工程经验结合，走"AI Agent × 企业级应用"的差异化路线。

## 个人背景

- 资深 Java Web 后端工程师，大型 B 端项目交付经验
- 技术栈：JDK8 / Spring Boot / MyBatis Plus / MySQL / Dubbo / ZooKeeper / Redis
- 已有优势：分布式系统设计、缓存/数据库建模、微服务治理、大型系统交付工程化
- 待补短板：Python、LLM API、Prompt 工程、Agent 编排、RAG、评估与可观测

## 目录结构

```
ai-agent-study/
├── README.md              # 本文件：工作区总览
├── docs/                  # 学习计划、资料清单、阶段详案
│   ├── learning-plan.md   # 分阶段学习计划（任务 S{N}-T* + 完成判据）
│   ├── stage-1.md         # 阶段 1 每天操作 D*-O*
│   ├── stage-2.md         # 阶段 2 整合学习（必知+操作+§9+写代码日历）
│   ├── stage-3.md         # 阶段 3：Agent 核心 10 天操作
│   ├── stage-4.md         # 阶段 4：RAG 与记忆 14 天操作
│   └── resources.md       # 学习资料汇总（课程/书籍/仓库/链接）
├── notes/                 # 学习笔记（知识 K*，每节有必知 / 怎么做 / 资料）
│   ├── llm-basics.md      # 阶段 0：Token / 窗口 / 温度 / 幻觉
│   ├── prompt-engineering.md # 阶段 1：K1–K20
│   ├── tool-use.md        # 阶段 2：已合并至 docs/stage-2.md（跳转页）
│   └── rag.md             # 阶段 4：K1–K28 + §9 对照表
├── demos/                 # 练手 demo，按阶段递增编号
│   ├── backend/           # 统一 Java 后端（:8080），stage00～stage06 分包
│   ├── frontend/          # 统一 Vue 控制台（/api 代理到 backend）
│   ├── 00-env/            # 阶段0：环境搭建（Python 对照 + stage00 Java）
│   ├── 01-hello-llm/      # 阶段1：提示词工程
│   ├── 02-tool-use/       # 阶段2：Function Calling
│   ├── 03-agent-core/     # 阶段3：Agent 框架
│   ├── 04-rag/            # 阶段4：RAG（data 夹具 + eval 评估集 + README）
│   ├── 05-multi-agent/    # 阶段5：多 Agent / MCP
│   └── 06-project/        # 阶段6：综合落地项目
└── resources/             # 下载的资料、源码、模型配置等
```

## 学习主线（速览，详见 docs/learning-plan.md）

1. **环境层**：环境搭建、API Key 注册、第一个 LLM 调用（1 周）
2. **基础层**：LLM 原理与提示词工程（1-2 周）——详见 [docs/stage-1.md](docs/stage-1.md)
3. **工具层**：Function Calling / Tool Use（2-3 周）——详见 [docs/stage-2.md](docs/stage-2.md)
4. **Agent 层**：手写循环 + Spring AI / LangChain4j（3-4 周）
5. **能力层**：RAG、记忆（3-4 周）
6. **工程层**：MCP、多 Agent、评估与可观测（2-3 周）
7. **落地层**：结合 B 端业务场景的综合项目（持续）

## 使用约定

- 每个 demo 独立成目录，自带 README 说明
- notes/ 下按主题写笔记：每节 K 写清必知、怎么做、指定资料；操作日程在 `docs/stage-N.md`（步骤 + 例子 + 判定标准）
- 一项任务完成 = learning-plan 里该 S{N}-T 列出的操作项 + 知识项全部打勾（约定见 learning-plan「三层约定」）
- 涉及 API Key 的配置一律放环境变量或本地 .env（不入库）
- 学习计划按周迭代，实际进度定期回写更新

## 进度记录

- [x] 阶段 0：环境准备 —— JDK21 / Python / DeepSeek / Spring AI 第一个调用（`demos/00-env/`）
- [ ] 阶段 1：提示词工程 —— 资料已就绪；按 [stage-1.md](docs/stage-1.md) 打勾
- [ ] 阶段 2：Function Calling / Tool Use —— 资料已就绪、代码待做；从 [stage-2.md D0](docs/stage-2.md) 直连 API 看 `tool_calls`
- [ ] 阶段 3：Agent 核心概念与框架
- [ ] 阶段 4：RAG 与记忆 —— 资料已就绪（14 天）；操作见 [stage-4.md](docs/stage-4.md)，知识见 [rag.md](notes/rag.md)
- [ ] 阶段 5：MCP / 多 Agent / 工程化
- [ ] 阶段 6：综合落地项目
