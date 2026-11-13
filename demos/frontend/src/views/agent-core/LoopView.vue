<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { agentCoreApi, type StepRecord } from '@/api/agent-core'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

interface QuestionPreset {
  id: string
  label: string
  text: string
  hint: string
}

/*
 * 手册第 2 天的两句，用来看同一份夹具在不同问法下轮次不同。
 * 一问一轮：模型只缺订单状态。期望 steps 共 2 条——第 1 条 getOrder 且观察含 PAID，第 2 条不再点菜。
 *   若这里也出现 getInventory，说明系统提示没把「只问状态就停」说清楚。
 * 一问多轮：发货要两段事实。期望至少 3 条——getOrder、getInventory（观察 available 为 10）、最后一条不再点菜。
 *   第 2 条的 sku/warehouse 应来自第 1 条观察（SKU-88、WH-A），问句里没有这两个值。
 *   若只有一条 getOrder 就停，先看工具说明是否写了「发货还要查库存」。
 */
const PRESETS: QuestionPreset[] = [
  {
    id: 'status',
    label: '一问一轮',
    text: 'ORD-1001 现在什么状态？',
    hint: 'answer 里应是已支付。stopReason 为 final。steps 长度为 2：第 1 步 finishReason=tool_calls、toolName=getOrder、参数含 ORD-1001、observation 含 PAID；第 2 步不再点菜。',
  },
  {
    id: 'ship',
    label: '一问多轮',
    text: 'ORD-1001 能不能发货？先查订单再查对应库存。',
    hint: 'steps 至少 3 条。前两条 toolName 依次是 getOrder、getInventory，库存观察里可用数量是 10。最后一条不再点菜，stopReason 为 final。',
  },
  {
    id: 'missing',
    label: '查无此单',
    text: 'ORD-9999 什么状态？',
    hint: '单号符合 ORD-数字，所以会查表。观察是 not_found，并且这一步有 latencyMs。回答不要把这笔说成已支付。夹具里仍然有 ORD-1001 的 PAID。',
  },
  {
    id: 'bad',
    label: '格式不对',
    text: '查订单 ABC 的状态',
    hint: 'ABC 不是 ORD-数字。要么没有工具步骤，要么观察是 invalid_argument，并且没有拿 ABC 去对订单表。回答里不出现夹具中的 PAID。',
  },
]

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)

const question = ref(PRESETS[0].text)
/** 与后端默认一致。发货 3 轮，4 留余量；输入框上限 8，和 Controller 的校验相同。 */
const maxSteps = ref(4)
const activeId = ref(PRESETS[0].id)
const activeHint = computed(
  () => PRESETS.find((item) => item.id === activeId.value)?.hint ?? PRESETS[0].hint,
)

/*
 * 一次点击等整段循环结束才返回，不是按轮次流式推送。
 * elapsedMs 含每一轮的模型耗时加查表，所以会比阶段 2 单次 call 更长。
 */
const { loading, error, data, elapsedMs, run } = useRunner(() =>
  agentCoreApi.loop(question.value.trim(), maxSteps.value),
)

const steps = computed<StepRecord[]>(() => data.value?.steps ?? [])

/** 换问法时清掉上一次的 steps，避免拿「问状态」的两行去对「发货」的预期。 */
function applyPreset(preset: QuestionPreset) {
  activeId.value = preset.id
  question.value = preset.text
  data.value = null
  error.value = ''
}

function submit() {
  if (!question.value.trim()) {
    return
  }
  void run()
}

/**
 * 收口轮 toolName 为 null。表格若直接渲染空单元格，看不出这一步是「模型决定停」，
 * 所以写成「不再点菜」。同一行的 finishReason 仍是 stop，两者一起看：
 * stop 是厂商这一轮的结束原因，不再点菜是我们给空工具名的读法。
 */
function toolLabel(step: StepRecord) {
  return step.toolName ? step.toolName : '不再点菜'
}

/**
 * 从观察 JSON 里认出第 3 天的两种错误。
 * not_found：查过表，业务未命中。invalid_argument：格式拦截，查表没有发生。
 * 命中时返回空串，表格只展示原始 JSON。
 */
function observationKind(step: StepRecord): '' | 'not_found' | 'invalid_argument' {
  if (!step.observation) {
    return ''
  }
  if (step.observation.includes('"invalid_argument"')) {
    return 'invalid_argument'
  }
  if (step.observation.includes('"not_found"')) {
    return 'not_found'
  }
  return ''
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="scenarios">
      <span class="label">手册问法</span>
      <el-button
        v-for="item in PRESETS"
        :key="item.id"
        size="small"
        :type="activeId === item.id ? 'primary' : 'default'"
        :disabled="loading"
        @click="applyPreset(item)"
      >
        {{ item.label }}
      </el-button>
    </div>
    <p class="hint">{{ activeHint }}</p>
    <el-input
      v-model="question"
      type="textarea"
      :rows="3"
      maxlength="500"
      show-word-limit
      placeholder="ORD-1001 现在什么状态？"
    />
    <div class="toolbar">
      <span class="label">最大步数</span>
      <el-input-number v-model="maxSteps" :min="1" :max="8" :disabled="loading" />
      <el-button type="primary" :loading="loading" :disabled="!question.trim()" @click="submit">
        发送
      </el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms（含每一轮模型调用）</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <template v-if="data">
      <div class="summary">
        <span class="field-label">stopReason</span>
        <!-- final 是正常收口；max_steps 表示步数用尽，回答可能还不是业务结论 -->
        <el-tag :type="data.stopReason === 'final' ? 'success' : 'warning'" effect="plain">
          {{ data.stopReason }}
        </el-tag>
        <span class="field-label">steps</span>
        <el-tag effect="plain">{{ steps.length }} 条</el-tag>
      </div>
      <p class="field-label">夹具</p>
      <p class="hint">
        这是本次响应里的内存快照，用来对照观察。ORD-1001 应为 PAID，SKU-88 / WH-A 的可用数量应为 10。
      </p>
      <div class="fixture">
        <el-tag
          v-for="order in data.fixture.orders"
          :key="order.orderId"
          effect="plain"
        >
          {{ order.orderId }} {{ order.status }}
        </el-tag>
        <el-tag
          v-for="row in data.fixture.inventory"
          :key="row.sku + row.warehouse"
          type="success"
          effect="plain"
        >
          {{ row.sku }} / {{ row.warehouse }} 可用 {{ row.available }}
        </el-tag>
      </div>
      <p class="field-label">回答</p>
      <p class="answer">{{ data.answer }}</p>
      <p class="field-label">steps</p>
      <el-table :data="steps" size="small" border>
        <el-table-column prop="round" label="轮次" width="70" />
        <el-table-column prop="finishReason" label="finishReason" width="130" />
        <el-table-column label="工具" width="140">
          <template #default="{ row }">{{ toolLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="arguments" label="参数" min-width="180" />
        <el-table-column label="观察" min-width="280">
          <template #default="{ row }">
            <el-tag
              v-if="observationKind(row)"
              size="small"
              :type="observationKind(row) === 'invalid_argument' ? 'warning' : 'info'"
              effect="plain"
              class="obs-tag"
            >
              {{ observationKind(row) }}
            </el-tag>
            <span>{{ row.observation }}</span>
          </template>
        </el-table-column>
        <el-table-column label="latencyMs" width="110">
          <template #default="{ row }">{{ row.latencyMs ?? '—' }}</template>
        </el-table-column>
      </el-table>
      <JsonPanel :value="data" />
    </template>
  </el-card>
</template>

<style scoped>
.scenarios,
.toolbar,
.summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.scenarios {
  margin-bottom: 12px;
}
.summary {
  margin-bottom: 8px;
}
.label,
.field-label {
  font-size: 13px;
  color: #475569;
}
.field-label {
  margin: 8px 0;
  font-weight: 600;
}
.hint {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 13px;
}
.answer {
  margin: 0 0 12px;
  white-space: pre-wrap;
  line-height: 1.6;
}
.toolbar {
  margin: 14px 0;
}
.elapsed {
  font-size: 12px;
  color: #94a3b8;
}
.fixture {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.obs-tag {
  margin-right: 8px;
}
</style>
