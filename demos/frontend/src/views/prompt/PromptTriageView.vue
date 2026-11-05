<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import {
  DEFAULT_TICKET,
  isTicketTriageError,
  promptClient,
  TICKET_B,
  type TicketTriage,
  type TicketTriageResult,
} from '@/api/prompt'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const api = computed(() => promptClient(demo.value))
const ticket = ref(DEFAULT_TICKET)

const { loading, error, data, elapsedMs, run } = useRunner<TicketTriageResult>(() =>
  api.value.triageTest(ticket.value.trim()),
)

const triage = computed<TicketTriage | null>(() => {
  if (!data.value || isTicketTriageError(data.value)) {
    return null
  }
  return data.value
})

const parseFailed = computed(() => data.value !== null && isTicketTriageError(data.value))

function submit() {
  if (!ticket.value.trim()) {
    return
  }
  void run()
}

function useDefaultTicket() {
  ticket.value = DEFAULT_TICKET
}

function useTicketB() {
  ticket.value = TICKET_B
}

function useGarbage() {
  ticket.value = 'asdf'
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <p class="hint">
      调用 <code>GET /api/test/prompt/triage?ticket=</code>，后端用
      <code>ChatClient.entity(TicketTriage.class)</code> 做结构化分类。可对照「结构化 Schema」实验看差异。
    </p>
    <el-input
      v-model="ticket"
      type="textarea"
      :rows="6"
      maxlength="4000"
      show-word-limit
      placeholder="粘贴工单正文"
    />
    <div class="toolbar">
      <el-button type="primary" :loading="loading" :disabled="!ticket.trim()" @click="submit">
        调用分类接口
      </el-button>
      <el-button :disabled="loading" @click="useDefaultTicket">工单 A（超时）</el-button>
      <el-button :disabled="loading" @click="useTicketB">工单 B（信息不足）</el-button>
      <el-button :disabled="loading" @click="useGarbage">垃圾输入</el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
      <el-tag v-if="triage" type="success" size="small">解析成功</el-tag>
      <el-tag v-if="parseFailed" type="danger" size="small">解析失败</el-tag>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <template v-if="triage">
      <el-descriptions :column="2" border size="small" class="summary">
        <el-descriptions-item label="分类">{{ triage.category }}</el-descriptions-item>
        <el-descriptions-item label="严重度">{{ triage.severity }}</el-descriptions-item>
        <el-descriptions-item label="置信度">{{ triage.confidence }}</el-descriptions-item>
        <el-descriptions-item label="依据">{{ triage.evidence || '—' }}</el-descriptions-item>
      </el-descriptions>
      <h4 v-if="triage.nextSteps?.length">下一步</h4>
      <ul v-if="triage.nextSteps?.length" class="steps">
        <li v-for="(step, i) in triage.nextSteps" :key="i">{{ step }}</li>
      </ul>
    </template>
    <el-alert
      v-if="parseFailed && isTicketTriageError(data!)"
      type="warning"
      :title="data!.error"
      show-icon
      :closable="false"
      class="parse-alert"
    />
    <JsonPanel v-if="data !== null" :value="data" class="raw-json" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 14px 0;
  flex-wrap: wrap;
}

.elapsed {
  font-size: 12px;
  color: #94a3b8;
}

.summary {
  margin-bottom: 16px;
}

h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #64748b;
}

.steps {
  margin: 0 0 16px;
  padding-left: 20px;
  font-size: 14px;
  line-height: 1.6;
}

.parse-alert {
  margin-bottom: 12px;
}

.raw-json {
  margin-top: 16px;
}
</style>
