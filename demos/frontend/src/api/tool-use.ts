import { BACKEND } from '../../demos.config'
import { createClient } from './http'

const http = createClient(BACKEND.proxyPrefix)

const STORE_PATH = '/stage/02/store'
const TOOLS_PATH = '/stage/02/tools'
const CHAT_PATH = '/stage/02/chat'
const CHAT_LC4J_PATH = '/stage/02/chat-lc4j'

export interface ToolCallRecord {
  name: string
  args: string
  success: boolean
  elapsedMs: number
}

export interface ToolChatResponse {
  answer: string
  toolsCalled?: ToolCallRecord[]
}

export interface ToolListResponse {
  tools: string[]
}

export interface ToolStoreResponse {
  orders: Record<
    string,
    {
      orderId: string
      userId: string
      sku: string
      qty: number
      status: string
      warehouse: string
    }
  >
  inventory: Record<
    string,
    {
      sku: string
      warehouse: string
      available: number
    }
  >
}

export const toolUseApi = {
  tools: () => http.get<ToolListResponse>(TOOLS_PATH).then((r) => r.data),

  store: () => http.get<ToolStoreResponse>(STORE_PATH).then((r) => r.data),

  chat: (question: string) =>
    http.post<ToolChatResponse>(CHAT_PATH, { question }).then((r) => r.data),

  chatLc4j: (question: string) =>
    http.post<ToolChatResponse>(CHAT_LC4J_PATH, { question }).then((r) => r.data),
}
