import { BACKEND } from '../../demos.config'
import { createClient } from './http'

const http = createClient(BACKEND.proxyPrefix)

/**
 * 与后端 StepRecord 对齐。
 * toolName / arguments / observation 在收口轮是 null：模型没有点菜，这三个字段没有值。
 * observation 是查表 JSON 字符串，页面直接展示原文，方便对 PAID 和 available:10。
 */
export interface StepRecord {
  /** 第几次请求模型，从 1 开始 */
  round: number
  /** 点菜轮是 tool_calls；收口轮是厂商的 finish_reason，通常是 stop */
  finishReason: string
  toolName: string | null
  /** 模型参数原文，例如 {"orderId":"ORD-1001"} */
  arguments: string | null
  /**
   * Java 查表 JSON。PAID 是命中；error=not_found 是格式合法但没有这行；
   * error=invalid_argument 是进表之前被挡住。
   */
  observation: string | null
  /** 这次查表的毫秒数。收口轮没有查表，为 null */
  latencyMs: number | null
}

export interface FixtureOrder {
  orderId: string
  userId: string
  sku: string
  qty: number
  status: string
  warehouse: string
}

export interface FixtureInventory {
  sku: string
  warehouse: string
  available: number
}

/** 这一次响应时的内存夹具，用来对照观察，不是模型写的。 */
export interface FixtureSummary {
  orders: FixtureOrder[]
  inventory: FixtureInventory[]
}

/**
 * stopReason 是整次循环的结束原因，不是某一轮的 finishReason。
 * final：最后一轮没有 tool_calls。max_steps：步数用尽时模型还想点菜。
 */
export interface LoopResponse {
  answer: string
  stopReason: string
  steps: StepRecord[]
  fixture: FixtureSummary
}

/** 记忆仓库里的一条。role=tool 时 text 里含工具名和观察。 */
export interface MemoryMessage {
  role: string
  text: string
}

/**
 * Spring AI 循环的响应。memoryRepository 是仓库类名，本阶段是堆里的实现。
 * trigger 只在巡检里出现，值为 schedule。
 */
export interface SpringAgentResponse extends LoopResponse {
  conversationId: string
  memoryMessages: MemoryMessage[]
  memoryRepository: string
  trigger: string | null
}

export interface SpringAgentBody {
  question: string
  maxSteps: number
  maxMessages: number
  rememberToolTrace: boolean
  conversationId: string
}

/**
 * LangChain4j 循环的响应。工具轨迹留在框架内部，页面只能看到回答和记忆窗口。
 */
export interface Lc4jAgentResponse {
  answer: string
  conversationId: string
  memoryMessages: MemoryMessage[]
}

export interface Lc4jAgentBody {
  question: string
  conversationId: string
  maxMessages: number
}

export type PlanningStrategy = 'react' | 'plan' | 'reflect'

/** 查表之前写下的清单。参数可能还是 null，执行时才补。 */
export interface PlanStep {
  tool: string
  arguments: string
}

/**
 * 规划对照的响应。哪个字段有值就说明用的是哪种策略：
 * plan 只有「先计划再执行」有值；draft / review 只有「做完再评」有值。
 */
export interface PlanningResponse {
  strategy: PlanningStrategy
  answer: string
  stopReason: string
  steps: StepRecord[]
  plan: PlanStep[] | null
  draft: string | null
  review: string | null
  fixture: FixtureSummary
}

/**
 * 固定工作流的响应。
 * nodes / branch 是「下一步由 Java 定」的证据：nodes 按顺序列出走过的节点。
 */
export interface GraphResponse {
  /** 本次实际查的单号：从问句抽出，抽不到是兜底值 ORD-1001 */
  orderId: string
  nodes: string[]
  branch: string
  answer: string
  /** 夹具里的真实可用数量。传了 availableOverride 也不会变；订单没查到时为 0 */
  fixtureAvailable: number
  /** 两个查询节点各自看到的 JSON */
  observations: StepRecord[]
}

export const agentCoreApi = {
  /** Vite 把 /api 代理到 8080，这里写的是代理后面的路径。 */
  loop: (question: string, maxSteps: number) =>
    http.post<LoopResponse>('/stage/03/loop', { question, maxSteps }).then((response) => response.data),

  springAi: (body: SpringAgentBody) =>
    http.post<SpringAgentResponse>('/stage/03/spring-ai', body).then((response) => response.data),

  /** 不传问句。服务端用写死的发货句和 conversationId=patrol。 */
  patrol: () =>
    http.post<SpringAgentResponse>('/stage/03/spring-ai/patrol').then((response) => response.data),

  /** 同一份 Store、同一句问题，工具循环由 AiServices 跑。 */
  langchain4j: (body: Lc4jAgentBody) =>
    http.post<Lc4jAgentResponse>('/stage/03/langchain4j', body).then((response) => response.data),

  /** 同一句问题，strategy 决定下一步由谁定：模型每轮选、先写清单、还是先出草稿再检查。 */
  plan: (question: string, strategy: PlanningStrategy, maxSteps: number) =>
    http
      .post<PlanningResponse>('/stage/03/plan', { question, strategy, maxSteps })
      .then((response) => response.data),

  /**
   * 固定工作流。availableOverride 只影响这一次的条件边，不改库存夹具；
   * 不传（undefined）时请求体里不会出现这个字段，后端按读夹具处理。
   */
  graph: (question: string, availableOverride?: number) =>
    http
      .post<GraphResponse>('/stage/03/graph', { question, availableOverride })
      .then((response) => response.data),
}
