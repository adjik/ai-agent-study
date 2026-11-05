import { BACKEND } from '../../demos.config'
import { createClient } from './http'

const http = createClient(BACKEND.proxyPrefix)

const STORE_PATH = '/stage/02/store'
const TOOLS_PATH = '/stage/02/tools'
const CHAT_PATH = '/stage/02/chat'

export interface ToolChatResponse {
  answer: string
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
}

export const toolUseApi = {
  tools: () => http.get<ToolListResponse>(TOOLS_PATH).then((r) => r.data),

  store: () => http.get<ToolStoreResponse>(STORE_PATH).then((r) => r.data),

  chat: (question: string) =>
    http.post<ToolChatResponse>(CHAT_PATH, { question }).then((r) => r.data),
}
