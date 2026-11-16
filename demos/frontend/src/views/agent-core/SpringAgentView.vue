<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { agentCoreApi, type StepRecord } from '@/api/agent-core'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

/*
 * 第 4 天只验收发货这一句，并和手写循环页并排看。
 * 期望 stopReason=final，steps 里先 getOrder 再 getInventory，库存观察 available 为 10。
 * 查表方法仍是阶段 2 的 @Tool。变的是 HTTP 由 ChatModel 发，循环由本页背后的 for 握住。
 */
const QUESTION = 'ORD-1001 能不能发货？先查订单再查对应库存。'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)

const question = ref(QUESTION)
const maxSteps = ref(4)

const { loading, error, data, elapsedMs, run } = useRunner(() =>
  agentCoreApi.springAi(question.value.trim(), maxSteps.value),
)

const steps = computed<StepRecord[]>(() => data.value?.steps ?? [])

function submit() {
  if (!question.value.trim()) {
    return
  }
  void run()
}

function toolLabel(step: StepRecord) {
  return step.toolName ? step.toolName : '不再点菜'
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <p class="hint">
      发完后打开「手写 Agent Loop」，用同一句再发一次。两边都应有 getOrder、getInventory，stopReason 都是 final。
    </p>
    <el-input
      v-model="question"
      type="textarea"
      :rows="3"
      maxlength="500"
      show-word-limit
    />
    <div class="toolbar">
      <span class="label">最大步数</span>
      <el-input-number v-model="maxSteps" :min="1" :max="8" :disabled="loading" />
      <el-button type="primary" :loading="loading" :disabled="!question.trim()" @click="submit">
        发送
      </el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <template v-if="data">
      <div class="summary">
        <span class="field-label">stopReason</span>
        <el-tag :type="data.stopReason === 'final' ? 'success' : 'warning'" effect="plain">
          {{ data.stopReason }}
        </el-tag>
        <span class="field-label">steps</span>
        <el-tag effect="plain">{{ steps.length }} 条</el-tag>
      </div>
      <p class="field-label">夹具</p>
      <div class="fixture">
        <el-tag v-for="order in data.fixture.orders" :key="order.orderId" effect="plain">
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
        <el-table-column prop="observation" label="观察" min-width="240" />
        <el-table-column label="latencyMs" width="110">
          <template #default="{ row }">{{ row.latencyMs ?? '—' }}</template>
        </el-table-column>
      </el-table>
      <JsonPanel :value="data" />
    </template>
  </el-card>
</template>

<style scoped>
.toolbar,
.summary,
.fixture {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.summary,
.fixture {
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
</style>
