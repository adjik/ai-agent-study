/**
 * Demo 注册表 = 前端的「路由表」。
 *
 * 统一后端：demos/backend（单端口 8080），各阶段代码在 Java 包 stage00 / stage01 / … 下。
 * 前端通过 Vite 代理 /api → localhost:8080，按 apiPath 调用对应阶段接口。
 *
 * 新增阶段时：
 * 1. 在 backend 下新增 stageNN 包与 Controller
 * 2. 在本文件 DEMO_SERVICES 里加菜单与 apiPath
 * 3. 把 status 改为 ready，并补对应 View
 */

export type DemoStatus = 'ready' | 'planned'
export type DemoView =
  | 'health'
  | 'hello'
  | 'conversation'
  | 'structured'
  | 'studentScores'
  | 'streaming'
  | 'chat'
  | 'all'
  | 'promptExperiment'
  | 'promptTriage'
  | 'toolStore'
  | 'toolTools'
  | 'toolChat'
  | 'agentLoop'
  | 'placeholder'

export interface DemoEndpoint {
  id: string
  title: string
  description: string
  /** Vue 路由，如 /demos/hello-llm/hello */
  route: string
  method: 'GET' | 'POST'
  /** 后端路径，如 /api/stage/00/demo/hello */
  apiPath?: string
  view: DemoView
  /**
   * 对照 docs/learning-plan.md（S*-T*）、docs/stage-1.md（Day · D*-O*）、notes/（K*）。
   * 前端 PageIntro 会展示，后端同名实验的 JavaDoc 与之对齐。
   */
  learning?: {
    plan: string[]
    stageDoc: string
    notes: string[]
  }
}

export interface DemoService {
  id: string
  stage: string
  title: string
  subtitle: string
  /** 后端代码包目录：demos/backend/src/.../stageNN */
  codeDir: string
  status: DemoStatus
  healthPath?: string
  endpoints: DemoEndpoint[]
}

/** 统一后端（单服务） */
export const BACKEND = {
  port: 8080,
  /** Vite 开发代理前缀，转发到 localhost:8080 */
  proxyPrefix: '/api',
  projectDir: 'demos/backend',
} as const

export const DEMO_SERVICES: DemoService[] = [
  {
    id: 'hello-llm',
    stage: '00',
    title: '环境与 LLM 入门',
    subtitle: 'Spring AI ChatClient 调 DeepSeek',
    codeDir: 'demos/backend/src/main/java/com/example/demos/stage00',
    status: 'ready',
    healthPath: '/health',
    endpoints: [
      {
        id: 'health',
        title: '服务状态',
        description: '检查统一后端是否启动，以及 DeepSeek API Key 是否已配置。',
        route: '/demos/hello-llm/health',
        method: 'GET',
        apiPath: '/health',
        view: 'health',
        learning: {
          plan: ['阶段 0 环境准备'],
          stageDoc: '阶段 0 · 启动 8080 冒烟',
          notes: ['llm-basics.md'],
        },
      },
      {
        id: 'hello',
        title: '简单问答',
        description: '最基础的一次 LLM 调用：固定问题「用一句话介绍什么是 AI Agent」。',
        route: '/demos/hello-llm/hello',
        method: 'GET',
        apiPath: '/stage/00/demo/hello',
        view: 'hello',
        learning: {
          plan: ['阶段 0 产出'],
          stageDoc: '阶段 0 · ChatClient 首次调用',
          notes: ['llm-basics.md · Token / 温度'],
        },
      },
      {
        id: 'conversation',
        title: '多轮对话',
        description: '系统提示词（角色设定）+ 两轮 user 消息，观察上下文如何影响第二轮回答。',
        route: '/demos/hello-llm/conversation',
        method: 'GET',
        apiPath: '/stage/00/demo/conversation',
        view: 'conversation',
        learning: {
          plan: ['阶段 0 产出', 'S1-T3（概念预习）'],
          stageDoc: '阶段 0 · system + 多轮 user',
          notes: ['K3 上下文是第一等公民', 'K11 system/history/user（预习）'],
        },
      },
      {
        id: 'structured',
        title: '结构化输出',
        description: '约束 LLM 输出 JSON，再解析为 Java Map。类比：用 DTO 约束接口出参。',
        route: '/demos/hello-llm/structured',
        method: 'GET',
        apiPath: '/stage/00/demo/structured',
        view: 'structured',
        learning: {
          plan: ['阶段 0 产出', 'S1-T5（对照基线）'],
          stageDoc: '阶段 0 · 手工 ObjectMapper 解析',
          notes: ['K10 结构化输出（对照：尚未 .entity）'],
        },
      },
      {
        id: 'student-scores',
        title: 'K10 成绩提取',
        description:
          'K10 示例：从自然语言提取学生姓名、年级与各科分数，走 /api/stage/00/test/student-scores，.entity(StudentExamRecord) 反序列化。默认示例：小明五年级，语文98、数学100、科学99。',
        route: '/demos/hello-llm/student-scores',
        method: 'GET',
        apiPath: '/stage/00/test/student-scores',
        view: 'studentScores',
        learning: {
          plan: ['S1-T5'],
          stageDoc: 'Day 4 · D4-O*（.entity 预习）',
          notes: ['K10 结构化输出 + DeepSeek JSON 三条'],
        },
      },
      {
        id: 'streaming',
        title: '流式输出',
        description: 'SSE 逐 token 推送。类比：WebFlux Flux / 分块响应。',
        route: '/demos/hello-llm/streaming',
        method: 'GET',
        apiPath: '/stage/00/demo/streaming',
        view: 'streaming',
        learning: {
          plan: ['阶段 0 产出'],
          stageDoc: '阶段 0 · SSE 流式',
          notes: ['llm-basics.md · 流式响应'],
        },
      },
      {
        id: 'chat',
        title: '自由问答',
        description: '自己输入问题，走 /api/stage/00/test/hello?msg= 做即时对话。',
        route: '/demos/hello-llm/chat',
        method: 'GET',
        apiPath: '/stage/00/test/hello',
        view: 'chat',
        learning: {
          plan: ['S1-T2'],
          stageDoc: 'Day 1–4 · D1~D4-O*（手工粘贴 prompt，不改 Java）',
          notes: ['K4', 'K5', 'K6', 'K7', 'K8', 'K9', 'K10'],
        },
      },
      {
        id: 'all',
        title: '一键跑全部',
        description: '顺序调用 hello / conversation / structured（不含流式），汇总返回。会打多次 LLM，稍慢。',
        route: '/demos/hello-llm/all',
        method: 'GET',
        apiPath: '/stage/00/demo/all',
        view: 'all',
        learning: {
          plan: ['阶段 0 产出'],
          stageDoc: '阶段 0 · 冒烟汇总',
          notes: ['llm-basics.md'],
        },
      },
    ],
  },
  {
    id: 'prompt',
    stage: '01',
    title: '提示词工程',
    subtitle: '角色 / Few-shot / CoT / JSON Schema',
    codeDir: 'demos/backend/src/main/java/com/example/demos/stage01',
    status: 'ready',
    healthPath: '/health',
    endpoints: [
      {
        id: 'role',
        title: '角色设定',
        description:
          '两个输入框：系统角色进 .system()，工单进 .user()。无角色时角色框留空。对照时工单不要改。',
        route: '/demos/prompt/role',
        method: 'POST',
        apiPath: '/stage/01/prompt/role',
        view: 'promptExperiment',
        learning: {
          plan: ['S1-T2 提示词五件套'],
          stageDoc: 'Day 1 · D1-O*（坏/好 prompt 对照）',
          notes: ['K4 提示词是工程', 'K5 五件套骨架', 'K6 角色设定', 'K7 指令清晰化'],
        },
      },
      {
        id: 'few-shot',
        title: 'Few-shot 示例',
        description: '前端输入完整 prompt。默认 3-shot（含 UNKNOWN 负例）；删掉示例即 0-shot。',
        route: '/demos/prompt/few-shot',
        method: 'POST',
        apiPath: '/stage/01/prompt/few-shot',
        view: 'promptExperiment',
        learning: {
          plan: ['S1-T2 提示词五件套'],
          stageDoc: 'Day 2 · D2-O*（0-shot vs 3-shot）',
          notes: ['K8 Few-shot：主路径 + 边界 + 负例'],
        },
      },
      {
        id: 'cot',
        title: '思维链 CoT',
        description: '前端输入完整题目。默认带逐步推理；对照时可改成只发 17 × 24 = ?。',
        route: '/demos/prompt/cot',
        method: 'POST',
        apiPath: '/stage/01/prompt/cot',
        view: 'promptExperiment',
        learning: {
          plan: ['S1-T2 提示词五件套'],
          stageDoc: 'Day 3 · D3-O*（直接答 vs CoT）',
          notes: ['K9 CoT：多步推理与 token 代价'],
        },
      },
      {
        id: 'template',
        title: 'PromptTemplate',
        description: '模板写在后端，前端只换工单 A/B。工单走 .param("ticket")，禁止字符串拼接。',
        route: '/demos/prompt/template',
        method: 'POST',
        apiPath: '/stage/01/prompt/template',
        view: 'promptExperiment',
        learning: {
          plan: ['S1-T3 上下文工程', 'S1-T6 Java 实战'],
          stageDoc: 'Day 5 · D5-O3（.param 与模板分离）',
          notes: ['K11 system/history/user', 'K13 PromptTemplate · .param'],
        },
      },
      {
        id: 'schema',
        title: '结构化 Schema',
        description: 'ChatClient.entity(TicketTriage.class)，对照阶段 0 手工解析 JSON。',
        route: '/demos/prompt/schema',
        method: 'POST',
        apiPath: '/stage/01/prompt/schema',
        view: 'promptExperiment',
        learning: {
          plan: ['S1-T2', 'S1-T5 结构化输出', 'S1-T6 Java 实战'],
          stageDoc: 'Day 4 · D4-O*；Day 5 · D5-O3~O5',
          notes: ['K10 结构化输出', 'K11 system 放契约', 'K18 .entity(TicketTriage)'],
        },
      },
      {
        id: 'failure',
        title: '失败模式',
        description: '前端输入带「坏指令」的工单：幻觉问节点数 / 只要表格 / 要求 30 条超长 nextSteps。',
        route: '/demos/prompt/failure',
        method: 'POST',
        apiPath: '/stage/01/prompt/failure',
        view: 'promptExperiment',
        learning: {
          plan: ['S1-T4 常见失败模式'],
          stageDoc: 'Day 6 · D6-O1~O3（三次独立请求）',
          notes: ['K14 幻觉', 'K15 截断', 'K16 指令冲突', 'K17 指令忽略'],
        },
      },
      {
        id: 'triage-test',
        title: '工单分类测试',
        description:
          'PromptTestController：GET ?ticket=，ChatClient.entity(TicketTriage.class) 结构化分类，与 schema 实验对照。',
        route: '/demos/prompt/triage-test',
        method: 'GET',
        apiPath: '/test/prompt/triage',
        view: 'promptTriage',
        learning: {
          plan: ['S1-T3', 'S1-T5', 'S1-T6'],
          stageDoc: 'Day 5 · D5-O3~O5（阶段 1 写代码里程碑）',
          notes: ['K11 system 放角色/边界', 'K13 .param("ticket")', 'K18 .entity() 失败兜底'],
        },
      },
    ],
  },
  {
    id: 'tool-use',
    stage: '02',
    title: 'Function Calling',
    subtitle: 'Spring AI @Tool / LangChain4j',
    codeDir: 'demos/backend/src/main/java/com/example/demos/stage02',
    status: 'ready',
    healthPath: '/health',
    endpoints: [
      {
        id: 'tools',
        title: '已注册工具列表',
        description: 'GET /api/stage/02/tools，不调 LLM。应看到 getOrder 和 getInventory。',
        route: '/demos/tool-use/tools',
        method: 'GET',
        apiPath: '/stage/02/tools',
        view: 'toolTools',
        learning: {
          plan: ['S2-T2'],
          stageDoc: 'D1 · 对照 K6',
          notes: ['K6 没 .tools() 则菜单空、模型易瞎编'],
        },
      },
      {
        id: 'store',
        title: '内存订单夹具',
        description: 'GET /api/stage/02/store，不调 LLM。订单 ORD-1001 为 PAID，库存 SKU-88 / WH-A 为 10。',
        route: '/demos/tool-use/store',
        method: 'GET',
        apiPath: '/stage/02/store',
        view: 'toolStore',
        learning: {
          plan: ['S2-T2'],
          stageDoc: 'D1 · D1-O4',
          notes: ['tool-use · K1 对比：PAID 来自 Java Store，不是模型瞎编'],
        },
      },
      {
        id: 'chat',
        title: 'Spring AI 对话',
        description:
          'POST /api/stage/02/chat。Spring AI 自动跑工具循环，响应带 answer 和 toolsCalled。',
        route: '/demos/tool-use/chat',
        method: 'POST',
        apiPath: '/stage/02/chat',
        view: 'toolChat',
        learning: {
          plan: ['S2-T2', 'S2-T4', 'S2-T6'],
          stageDoc: 'D1～D5 · Q1 / Q2 / Q3 / not_found / invalid_argument',
          notes: ['K5 @Tool', 'K6 .tools()', 'K10 toolsCalled', 'K12 入参校验'],
        },
      },
      {
        id: 'chat-lc4j',
        title: 'LangChain4j 对照',
        description:
          'POST /api/stage/02/chat-lc4j。同一份 Store、同一句问题，由 AiServices 跑工具循环。响应只有 answer。',
        route: '/demos/tool-use/chat-lc4j',
        method: 'POST',
        apiPath: '/stage/02/chat-lc4j',
        view: 'toolChat',
        learning: {
          plan: ['S2-T3', 'S2-T5'],
          stageDoc: 'D6 · /chat-lc4j；D7 与 Spring 对照',
          notes: ['K14 LC4j @Tool 与 Spring @Tool 分两个类', 'K15 AiServices'],
        },
      },
    ],
  },
  {
    id: 'agent-core',
    stage: '03',
    title: 'Agent 核心',
    subtitle: '手写 Loop、记忆、规划、固定图',
    codeDir: 'demos/backend/src/main/java/com/example/demos/stage03',
    status: 'ready',
    healthPath: '/health',
    endpoints: [
      {
        id: 'loop',
        title: '手写 Agent Loop',
        description:
          'POST /api/stage/03/loop。JDK HttpClient 自己跑 while：有 tool_calls 就查表并把观察发回去，不再点菜或到达 maxSteps 就停。响应含 answer、stopReason、steps。',
        route: '/demos/agent-core/loop',
        method: 'POST',
        apiPath: '/stage/03/loop',
        view: 'agentLoop',
        learning: {
          plan: ['阶段 3 · Agent Loop'],
          stageDoc: '第 2 天 · 一问一轮、一问多轮',
          notes: ['finish_reason=tool_calls', 'role=tool 把观察发回', 'ReAct 先动作再看观察'],
        },
      },
      planned('agent-core', 'spring-ai', 'Spring AI Agent', '/stage/03/spring-ai'),
      planned('agent-core', 'langchain4j', 'LangChain4j Agent', '/stage/03/langchain4j'),
      planned('agent-core', 'plan', '规划对照', '/stage/03/plan'),
      planned('agent-core', 'graph', '固定工作流', '/stage/03/graph'),
      planned('agent-core', 'guard', '停止与边界', '/stage/03/guard'),
    ],
  },
  {
    id: 'rag',
    stage: '04',
    title: 'RAG 与记忆',
    subtitle: 'LangChain4j + Redis / pgvector',
    codeDir: 'demos/backend/src/main/java/com/example/demos/stage04',
    status: 'planned',
    endpoints: [
      planned('rag', 'ingest', '文档入库', '/stage/04/ingest'),
      planned('rag', 'query', '检索问答', '/stage/04/query'),
      planned('rag', 'memory', '对话记忆', '/stage/04/memory'),
    ],
  },
  {
    id: 'multi-agent',
    stage: '05',
    title: 'MCP / 多 Agent',
    subtitle: 'Spring AI Alibaba Supervisor',
    codeDir: 'demos/backend/src/main/java/com/example/demos/stage05',
    status: 'planned',
    endpoints: [
      planned('multi-agent', 'mcp', 'MCP Server', '/stage/05/mcp'),
      planned('multi-agent', 'supervisor', 'Supervisor 协作', '/stage/05/supervisor'),
      planned('multi-agent', 'eval', '评估与追踪', '/stage/05/eval'),
    ],
  },
  {
    id: 'project',
    stage: '06',
    title: '综合落地项目',
    subtitle: '可交付的 Spring Boot Agent',
    codeDir: 'demos/backend/src/main/java/com/example/demos/stage06',
    status: 'planned',
    endpoints: [
      planned('project', 'console', '业务控制台', '/stage/06/console'),
    ],
  },
]

function planned(
  demoId: string,
  id: string,
  title: string,
  futureApiPath: string,
): DemoEndpoint {
  return {
    id,
    title,
    description: `阶段尚未提供 HTTP 接口。在 backend 的 stage 包实现后，把 ${demoId} 的 status 改为 ready，apiPath 填 ${futureApiPath}，并补页面。`,
    route: `/demos/${demoId}/${id}`,
    method: 'GET',
    view: 'placeholder',
  }
}

export function findDemoById(id: string): DemoService | undefined {
  return DEMO_SERVICES.find((d) => d.id === id)
}

export function findDemoByRoute(path: string): DemoService | undefined {
  return DEMO_SERVICES.find(
    (d) => path === `/demos/${d.id}` || path.startsWith(`/demos/${d.id}/`),
  )
}

export function resolveApiPath(endpoint: DemoEndpoint): string {
  if (!endpoint.apiPath) {
    throw new Error(`端点 ${endpoint.id} 尚未配置 apiPath`)
  }
  return endpoint.apiPath.startsWith('/') ? endpoint.apiPath : `/${endpoint.apiPath}`
}
