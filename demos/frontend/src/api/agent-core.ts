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

export const agentCoreApi = {
  /** Vite 把 /api 代理到 8080，这里写的是代理后面的路径。 */
  loop: (question: string, maxSteps: number) =>
    http.post<LoopResponse>('/stage/03/loop', { question, maxSteps }).then((response) => response.data),
}
