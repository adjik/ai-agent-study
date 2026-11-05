import axios, { type AxiosInstance } from 'axios'

const clients = new Map<string, AxiosInstance>()

export function createClient(proxyPrefix: string): AxiosInstance {
  const cached = clients.get(proxyPrefix)
  if (cached) {
    return cached
  }
  const instance = axios.create({
    baseURL: proxyPrefix,
    timeout: 180_000,
  })
  clients.set(proxyPrefix, instance)
  return instance
}

export function formatError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string; error?: string } | undefined
    if (data?.message) {
      return data.message
    }
    if (data?.error) {
      return data.error
    }
    if (error.code === 'ECONNABORTED') {
      return '请求超时。LLM 调用可能较慢，或后端未启动。'
    }
    if (error.code === 'ERR_NETWORK' || error.message.includes('Network Error')) {
      return '无法连接后端。请确认 demos/backend 已启动（mvn spring-boot:run，端口 8080）。'
    }
    return error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return String(error)
}

export async function readSseStream(
  url: string,
  onChunk: (text: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const response = await fetch(url, {
    headers: { Accept: 'text/event-stream' },
    signal,
  })
  if (!response.ok) {
    throw new Error(`流式接口返回 ${response.status}`)
  }
  if (!response.body) {
    throw new Error('浏览器未拿到响应流')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split(/\r?\n/)
    buffer = lines.pop() ?? ''
    for (const line of lines) {
      if (line.startsWith('data:')) {
        onChunk(line.slice(5).trimStart())
      } else if (line && !line.startsWith(':') && !line.startsWith('event:') && !line.startsWith('id:')) {
        onChunk(line)
      }
    }
  }
  if (buffer) {
    if (buffer.startsWith('data:')) {
      onChunk(buffer.slice(5).trimStart())
    } else {
      onChunk(buffer)
    }
  }
}
