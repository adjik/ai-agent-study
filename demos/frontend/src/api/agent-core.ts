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
}
