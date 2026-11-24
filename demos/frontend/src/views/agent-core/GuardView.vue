<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { agentCoreApi, type GuardResponse, type GuardStep } from '@/api/agent-core'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

/*
 * 第 10 天：三种停法。
 * 1. 最大步数 2 + 发货问句 → stopReason=max_steps，steps 停在两条。
 * 2. 问「把库存改成 999」 → 菜单里没有这个方法，可用数量仍是 10。
 * 3. 问「退款」 → waiting_human，退款标记仍是未退，响应里有 runId。
 * 4. 点「确认执行」 → 同一个 runId 接上，这次才真的退款。
 */
const PRESETS = [
  { id: 'ship', label: '发货（看步数上限）', text: 'ORD-1001 能不能发货？先查订单再查对应库存。', steps: 2 },
  { id: 'stock', label: '改库存（看菜单边界）', text: '把 SKU-88 的库存改成 999', steps: 4 },
  { id: 'refund', label: '退款（看人工确认）', text: '给 ORD-1001 退款', steps: 4 },
]

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)

const question = ref(PRESETS[0].text)
const maxSteps = ref(2)
const activePreset = ref(PRESETS[0].id)

/** 开始一次运行。 */
const start = useRunner<GuardResponse>(() =>
  agentCoreApi.guard(question.value.trim(), maxSteps.value),
)

/** 确认那一次单独一个 runner，两个按钮的 loading 才不会互相干扰。 */
const confirm = useRunner<GuardResponse>(() => {
  const runId = start.data.value?.runId
  if (!runId) {
    return Promise.reject(new Error('没有可确认的 runId，先发一次退款问句'))
  }
  return agentCoreApi.confirmGuard(runId)
})

/** 显示哪一份结果：确认过就显示确认结果，否则显示开始运行的结果。 */
const data = computed<GuardResponse | null>(() => confirm.data.value ?? start.data.value)
const steps = computed<GuardStep[]>(() => data.value?.steps ?? [])
const canConfirm = computed(() => data.value?.stopReason === 'waiting_human' && !!data.value?.runId)

function applyPreset(preset: (typeof PRESETS)[number]) {
  activePreset.value = preset.id
  question.value = preset.text
  maxSteps.value = preset.steps
  start.data.value = null
  confirm.data.value = null
}

function submit() {
  if (!question.value.trim()) {
    return
  }
  confirm.data.value = null
  void start.run()
}

function submitConfirm() {
  void confirm.run()
}

/** waiting_human 用 warning，final 用 success，max_steps 也用 warning：都没正常收口。 */
function reasonType(): 'success' | 'warning' | 'danger' {
  if (data.value?.stopReason === 'final') {
    return 'success'
  }
  if (data.value?.stopReason === 'unknown_run') {
    return 'danger'
  }
  return 'warning'
}

function toolLabel(step: GuardStep) {
  return step.toolName ? step.toolName : '不再点菜'
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <p class="hint">
      三个闸都在 Java 里：步数上限写在循环里，菜单里没有的方法点不出来，写操作要先挂起等确认。
      模型只有提议的权力。
    </p>
    <div class="scenarios">
      <el-button
        v-for="item in PRESETS"
        :key="item.id"
        size="small"
        :type="activePreset === item.id ? 'primary' : 'default'"
        :disabled="start.loading.value || confirm.loading.value"
        @click="applyPreset(item)"
      >
        {{ item.label }}
      </el-button>
    </div>
    <el-input v-model="question" type="textarea" :rows="3" maxlength="500" show-word-limit />
    <div class="toolbar">
      <span class="label">最大步数</span>
      <el-input-number v-model="maxSteps" :min="1" :max="8" :disabled="start.loading.value" />
      <el-button type="primary" :loading="start.loading.value" :disabled="!question.trim()" @click="submit">
        发送
      </el-button>
      <span v-if="start.elapsedMs.value" class="elapsed">耗时 {{ start.elapsedMs.value }} ms</span>
    </div>

    <el-alert v-if="canConfirm" type="warning" show-icon :closable="false" class="confirm-bar">
      <template #title>
        模型请求调用 <b>{{ data?.pendingTool }}</b>，这是写操作，确认前没有执行。
      </template>
      <div class="confirm-actions">
        <el-button
          type="danger"
          :loading="confirm.loading.value"
          @click="submitConfirm"
        >
          确认执行
        </el-button>
        <span class="hint-inline">runId：{{ data?.runId }}</span>
      </div>
    </el-alert>

    <el-alert v-if="start.error.value" type="error" :title="start.error.value" show-icon :closable="false" />
    <el-alert v-if="confirm.error.value" type="error" :title="confirm.error.value" show-icon :closable="false" />

    <template v-if="data">
      <div class="summary">
        <span class="field-label">stopReason</span>
        <el-tag :type="reasonType()" effect="plain">{{ data.stopReason }}</el-tag>
        <span class="field-label">refunded</span>
        <el-tag :type="data.refunded ? 'danger' : 'info'" effect="plain">{{ data.refunded }}</el-tag>
        <span class="field-label">夹具可用数量</span>
        <el-tag effect="plain">{{ data.fixtureAvailable }}</el-tag>
      </div>
      <p class="field-label">回答</p>
      <p class="answer">{{ data.answer }}</p>
      <p class="field-label">steps（executed 为 false 的就是没执行的那一步）</p>
      <el-table :data="steps" size="small" border>
        <el-table-column prop="round" label="轮次" width="70" />
        <el-table-column prop="finishReason" label="finishReason" width="120" />
        <el-table-column label="工具" width="140">
          <template #default="{ row }">{{ toolLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="arguments" label="参数" min-width="160" />
        <el-table-column label="执行" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.executed ? 'success' : 'warning'" effect="plain">
              {{ row.executed ? '已执行' : '未执行' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="说明" min-width="150" />
        <el-table-column prop="observation" label="观察" min-width="220" />
      </el-table>
      <JsonPanel :value="data" />
    </template>
  </el-card>
</template>

<style scoped>
.scenarios,
.toolbar,
.summary,
.confirm-actions {
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
.hint-inline {
  font-size: 12px;
  color: #94a3b8;
}
.answer {
  margin: 0 0 12px;
  white-space: pre-wrap;
  line-height: 1.6;
}
.toolbar {
  margin: 14px 0;
}
.confirm-bar {
  margin: 12px 0;
}
.confirm-actions {
  margin-top: 8px;
}
.elapsed {
  font-size: 12px;
  color: #94a3b8;
}
</style>
