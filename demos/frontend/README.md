# AI Agent Demo Console

一个 Vue 前端，对接 **demos/backend** 统一 Java 后端（单端口 8080）。左侧按学习阶段菜单切换，各阶段代码在后端 `stage00` / `stage01` / … 包下。

## 怎么跑

```bash
# 终端 1：统一后端
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd demos/backend
mvn spring-boot:run

# 终端 2：前端
cd demos/frontend
npm install
npm run dev
```

浏览器打开 [http://localhost:5173](http://localhost:5173)。

未启动后端时，页面会提示连不上，菜单仍可用。

## 架构

```
浏览器 :5173  →  Vite 代理 /api  →  demos/backend :8080
                                      ├── stage00/  （阶段 0，已实现）
                                      ├── stage01/  （阶段 1，待实现）
                                      └── ...
```

路由表写在 `demos.config.ts`。新增阶段：在后端 `stageNN` 包实现接口 → 在本文件登记 `apiPath` → 改 `status` 为 `ready` 并补 View。

## 当前可调用

- **阶段 0**：健康检查、简单问答、多轮对话、结构化输出、K10 成绩提取、流式 SSE、自由问答、一键跑全部

其余阶段是占位页，等对应 `stageNN` 接口落地。
