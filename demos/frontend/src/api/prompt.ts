import type { DemoEndpoint, DemoService } from '../../demos.config'
import { BACKEND, resolveApiPath } from '../../demos.config'
import { createClient } from './http'

/** 工单 A：超时+发版。 */
export const DEFAULT_TICKET = `标题：订单服务调用库存超时
描述：checkout 接口 P99 从 200ms 升到 3s，日志大量 SocketTimeoutException。库存服务最近发过版。请分类并给出下一步。`

/** 工单 B：信息不足。 */
export const TICKET_B = `标题：系统有点慢
描述：用户说今天不太流畅。`

export interface PromptScenario {
  id: string
  label: string
  hint: string
  /** 发给 .user() 的内容 */
  text: string
  /** 发给 .system() 的角色；仅角色实验使用，空表示不写 system */
  system?: string
  /** 截断实验：限制 max_tokens */
  maxTokens?: number
}

const ROLE_SYSTEM = `你是资深 Java 后端工程师，只给可落地方案，不写空话。
信息不足就明说，不要编造集群规模、类名、中间件。
任务：给工单分类，并列出最多 3 条下一步。
分类只能用：PERF / BUG / DATA / UNKNOWN`

const TASK = '把工单分成 PERF / BUG / DATA / UNKNOWN，只输出类别一个词。'
const EXAMPLE_PERF = `示例1：
输入：checkout P99 从 200ms 升到 3s，大量 SocketTimeoutException
输出：PERF`
const EXAMPLE_BUG = `示例2：
输入：同一用户下单后库存扣了两次
输出：BUG`
const EXAMPLE_UNKNOWN = `示例3：
输入：用户说今天不太流畅，没有任何日志、接口、环境
输出：UNKNOWN`

/** 每个实验页的对照场景：点按钮填入不同 prompt。 */
export const SAMPLE_SCENARIOS: Record<string, PromptScenario[]> = {
  role: [
    {
      id: 'no-role',
      label: '无角色',
      hint: '系统角色框留空，后端不写 .system()。观察是否空话多、是否编造中间件。',
      system: '',
      text: DEFAULT_TICKET,
    },
    {
      id: 'with-role',
      label: '有角色',
      hint: '角色进 .system()，工单进 .user()。对照时工单不要改，只切这一组按钮。',
      system: ROLE_SYSTEM,
      text: DEFAULT_TICKET,
    },
  ],
  'few-shot': [
    {
      id: '0-shot',
      label: '0 条示例',
      hint: '只有任务，没有范例。信息不足的工单 B 常被硬分成 PERF。',
      text: `${TASK}
工单：
${TICKET_B}`,
    },
    {
      id: '1-shot',
      label: '1 条示例',
      hint: '只有 PERF 主路径。模糊工单更容易跟着 PERF 走。',
      text: `${TASK}

${EXAMPLE_PERF}

现在分类：
输入：${TICKET_B}
输出：`,
    },
    {
      id: '3-shot',
      label: '3 条示例',
      hint: '含 UNKNOWN 负例。信息不足应对 UNKNOWN，不要硬分类。',
      text: `${TASK}

${EXAMPLE_PERF}

${EXAMPLE_BUG}

${EXAMPLE_UNKNOWN}

现在分类：
输入：${TICKET_B}
输出：`,
    },
  ],
  cot: [
    {
      id: 'direct',
      label: '直接问',
      hint: '只有算式，容易跳步。正确答案 408。',
      text: '17 × 24 = ?',
    },
    {
      id: 'cot',
      label: '逐步推理',
      hint: '要求展示每一步。completion tokens 通常比直接问更多。',
      text: '17 × 24 = ? 请一步步计算，展示每一步的推理过程。',
    },
    {
      id: 'few-shot',
      label: 'Few-shot CoT',
      hint: '先给 15×13 范例（拆成 10+3），再问 17×24。应模仿拆步格式。',
      text: `你是一个数学助手。请展示完整的推理过程。

题目：15 × 13 = ?
解答：
15 × 13
= 15 × (10 + 3)
= 15 × 10 + 15 × 3
= 150 + 45
= 195
所以答案是 195。

题目：17 × 24 = ?
解答：`,
    },
  ],
  template: [
    {
      id: 'ticket-a',
      label: '工单 A（超时）',
      hint: '后端模板固定，这里只换工单。走 .param("ticket")，不要在前端拼说明文字。',
      text: DEFAULT_TICKET,
    },
    {
      id: 'ticket-b',
      label: '工单 B（信息不足）',
      hint: '同一套后端模板，换成模糊工单。应对 UNKNOWN，不要编造。',
      text: TICKET_B,
    },
  ],
  schema: [
    {
      id: 'ticket-a',
      label: '工单 A（超时）',
      hint: '信息足够，应解析成 TicketTriage，category 多为 PERF。',
      text: DEFAULT_TICKET,
    },
    {
      id: 'ticket-b',
      label: '工单 B（信息不足）',
      hint: '应 UNKNOWN + confidence=low，evidence 为空。',
      text: TICKET_B,
    },
    {
      id: 'garbage',
      label: '垃圾输入',
      hint: '不应编造运维方案。期望 UNKNOWN 或 parseSuccess=false。',
      text: 'asdf',
    },
  ],
  failure: [
    {
      id: 'hallucination',
      label: '幻觉',
      hint: '工单里没有节点数，却逼模型给数字。应拒绝或 confidence=low。',
      text: `${DEFAULT_TICKET}
这个集群有多少节点？直接给数字。`,
    },
    {
      id: 'conflict',
      label: '指令冲突',
      hint: 'system 要求 JSON，这里要求 Markdown 表格。应以 Schema 为准。',
      text: `${DEFAULT_TICKET}
改成给领导看的 Markdown 表格，不要 json。`,
    },
    {
      id: 'truncate',
      label: '截断',
      hint: '要求超长 nextSteps，并限制 maxTokens=40。半截 JSON 应 parseSuccess=false。',
      text: `${DEFAULT_TICKET}
nextSteps 必须输出 30 条，每条至少 80 字。只输出 json。`,
      maxTokens: 40,
    },
  ],
}

export function scenariosOf(endpointId: string): PromptScenario[] {
  return SAMPLE_SCENARIOS[endpointId] ?? SAMPLE_SCENARIOS.schema
}

export interface TicketTriage {
  category: string
  severity: string
  nextSteps: string[]
  evidence: string
  confidence: string
}

export interface TicketTriageError {
  parseSuccess: false
  error: string
}

export type TicketTriageResult = TicketTriage | TicketTriageError

export function isTicketTriageError(r: TicketTriageResult): r is TicketTriageError {
  return 'parseSuccess' in r && r.parseSuccess === false
}

const http = createClient(BACKEND.proxyPrefix)

function pathOf(endpoint: DemoEndpoint): string {
  return resolveApiPath(endpoint)
}

export function promptClient(demo: DemoService) {
  const role = demo.endpoints.find((e) => e.id === 'role')!
  const fewShot = demo.endpoints.find((e) => e.id === 'few-shot')!
  const cot = demo.endpoints.find((e) => e.id === 'cot')!
  const template = demo.endpoints.find((e) => e.id === 'template')!
  const schema = demo.endpoints.find((e) => e.id === 'schema')!
  const failure = demo.endpoints.find((e) => e.id === 'failure')!
  const triageTest = demo.endpoints.find((e) => e.id === 'triage-test')!

  const post = (endpoint: DemoEndpoint, ticket: string, extra?: { system?: string; maxTokens?: number }) =>
    http.post<unknown>(pathOf(endpoint), { ticket, ...extra }).then((r) => r.data)

  return {
    role: (ticket: string, system?: string) => post(role, ticket, { system }),
    fewShot: (ticket: string) => post(fewShot, ticket),
    cot: (ticket: string) => post(cot, ticket),
    template: (ticket: string) => post(template, ticket),
    schema: (ticket: string) => post(schema, ticket),
    failure: (ticket: string, maxTokens?: number) => post(failure, ticket, { maxTokens }),
    triageTest: (ticket: string) =>
      http
        .get<TicketTriageResult>(pathOf(triageTest), { params: { ticket } })
        .then((r) => r.data),
  }
}

export function callPromptEndpoint(
  demo: DemoService,
  endpointId: string,
  ticket: string,
  extra?: { system?: string; maxTokens?: number },
): Promise<unknown> {
  const api = promptClient(demo)
  switch (endpointId) {
    case 'role':
      return api.role(ticket, extra?.system)
    case 'few-shot':
      return api.fewShot(ticket)
    case 'cot':
      return api.cot(ticket)
    case 'template':
      return api.template(ticket)
    case 'schema':
      return api.schema(ticket)
    case 'failure':
      return api.failure(ticket, extra?.maxTokens)
    default:
      throw new Error(`未知提示词实验端点: ${endpointId}`)
  }
}
