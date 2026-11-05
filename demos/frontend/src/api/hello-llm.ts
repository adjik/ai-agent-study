import type { DemoEndpoint, DemoService } from '../../demos.config'
import { BACKEND, resolveApiPath } from '../../demos.config'
import { createClient, readSseStream } from './http'

export interface HealthPayload {
  status: string
  llmProvider: string
  baseUrl: string
  model: string
  apiKeyConfigured: boolean
  apiKeyPreview: string
}

export interface SubjectScore {
  subject: string
  score: number
}

export interface StudentExamRecord {
  studentName: string
  gradeLevel: string
  subjectScores: SubjectScore[]
}

export interface StudentScoresError {
  parseSuccess: false
  error: string
  inputText: string
}

export type StudentScoresResult = StudentExamRecord | StudentScoresError

export function isStudentScoresError(r: StudentScoresResult): r is StudentScoresError {
  return 'parseSuccess' in r && r.parseSuccess === false
}

const http = createClient(BACKEND.proxyPrefix)

function pathOf(endpoint: DemoEndpoint): string {
  return resolveApiPath(endpoint)
}

export function helloLlmClient(demo: DemoService) {
  const healthPath = demo.healthPath ?? '/health'
  const hello = demo.endpoints.find((e) => e.id === 'hello')!
  const conversation = demo.endpoints.find((e) => e.id === 'conversation')!
  const structured = demo.endpoints.find((e) => e.id === 'structured')!
  const all = demo.endpoints.find((e) => e.id === 'all')!
  const chat = demo.endpoints.find((e) => e.id === 'chat')!
  const studentScores = demo.endpoints.find((e) => e.id === 'student-scores')!
  const streaming = demo.endpoints.find((e) => e.id === 'streaming')!

  return {
    health: () => http.get<HealthPayload>(healthPath).then((r) => r.data),
    hello: () => http.get(pathOf(hello)).then((r) => r.data),
    conversation: () => http.get(pathOf(conversation)).then((r) => r.data),
    structured: () => http.get(pathOf(structured)).then((r) => r.data),
    all: () => http.get(pathOf(all)).then((r) => r.data),
    chat: (msg: string) =>
      http.get<string>(pathOf(chat), { params: { msg } }).then((r) => r.data),
    studentScores: (text?: string) =>
      http
        .get<StudentScoresResult>(pathOf(studentScores), {
          params: text?.trim() ? { text } : undefined,
        })
        .then((r) => r.data),
    stream: (onChunk: (text: string) => void, signal?: AbortSignal) =>
      readSseStream(`${BACKEND.proxyPrefix}${pathOf(streaming)}`, onChunk, signal),
  }
}
