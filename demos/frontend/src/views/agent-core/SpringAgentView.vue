<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { agentCoreApi, type MemoryMessage, type StepRecord } from '@/api/agent-core'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

/*
 * 第 6 天看两件事：
 * 1. memoryRepository 是内存仓库的类名。记下 conversationId 后重启后端，再发「刚才那张单」，
 *    同一个 id 的 memoryMessages 是空的，回答不知道刚才是哪张单。
 * 2. 「跑一次巡检」不读输入框。steps 里仍有 getOrder 和 getInventory，trigger=schedule。
 */
const PRESETS = [
  { id: 'status', label: '查状态', text: 'ORD-1001 现在什么状态？' },
  { id: 'follow', label: '刚才那张单', text: '刚才那张单是什么状态？' },
  { id: 'ship', label: '发货', text: 'ORD-1001 能不能发货？先查订单再查对应库存。' },
]

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)

const question = ref(PRESETS[0].text)
const maxSteps = ref(4)
const maxMessages = ref(20)
const rememberToolTrace = ref(false)
const conversationId = ref(crypto.randomUUID())
const activeId = ref(PRESETS[0].id)

const { loading, error, data, elapsedMs, run } = useRunner(() => {
  if (patrolMode.value) {
    return agentCoreApi.patrol()
  }
  return agentCoreApi.springAi({
    question: question.value.trim(),
    maxSteps: maxSteps.value,
    maxMessages: maxMessages.value,
    rememberToolTrace: rememberToolTrace.value,
    conversationId: conversationId.value.trim(),
  })
})

const patrolMode = ref(false)
const steps = computed<StepRecord[]>(() => data.value?.steps ?? [])
const memoryMessages = computed<MemoryMessage[]>(() => data.value?.memoryMessages ?? [])

function newSession() {
  conversationId.value = crypto.randomUUID()
  data.value = null
  error.value = ''
}

function applyPreset(preset: (typeof PRESETS)[number]) {
  activeId.value = preset.id
  question.value = preset.text
}

function submit() {
  if (!question.value.trim()) {
    return
  }
  patrolMode.value = false
  void run()
}

function patrol() {
  patrolMode.value = true
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
      窗口和「记住工具来回」决定留下什么。memoryRepository 是仓库类名，存在进程堆里。重启后再用同一个会话 ID，记忆是空的。
    </p>
    <div class="scenarios">
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
    <el-input v-model="question" type="textarea" :rows="3" maxlength="500" show-word-limit />
    <div class="toolbar">
      <span class="label">会话 ID</span>
      <el-input v-model="conversationId" class="id-input" :disabled="loading" />
      <el-button :disabled="loading" @click="newSession">新会话</el-button>
    </div>
    <div class="toolbar">
      <span class="label">最大步数</span>
      <el-input-number v-model="maxSteps" :min="1" :max="8" :disabled="loading" />
      <span class="label">窗口条数</span>
      <el-input-number v-model="maxMessages" :min="1" :max="40" :disabled="loading" />
      <el-switch v-model="rememberToolTrace" :disabled="loading" active-text="记住工具来回" />
    </div>
    <div class="toolbar">
      <el-button type="primary" :loading="loading && !patrolMode" :disabled="!question.trim()" @click="submit">
        发送
      </el-button>
      <el-button :loading="loading && patrolMode" :disabled="loading && !patrolMode" @click="patrol">
        跑一次巡检
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
        <span class="field-label">trigger</span>
        <el-tag effect="plain">{{ data.trigger ?? '—' }}</el-tag>
        <span class="field-label">conversationId</span>
        <el-tag effect="plain">{{ data.conversationId }}</el-tag>
      </div>
      <p class="field-label">memoryRepository</p>
      <p class="answer">{{ data.memoryRepository }}</p>
      <p class="field-label">memoryMessages</p>
      <el-table v-if="memoryMessages.length" :data="memoryMessages" size="small" border>
        <el-table-column prop="role" label="角色" width="120" />
        <el-table-column prop="text" label="内容" min-width="280" />
      </el-table>
      <p v-else class="hint">这个会话 ID 在当前进程里没有消息。</p>
      <p class="field-label">回答</p>
      <p class="answer">{{ data.answer }}</p>
      <p class="field-label">steps</p>
      <el-table :data="steps" size="small" border>
        <el-table-column prop="round" label="轮次" width="70" />
        <el-table-column prop="finishReason" label="finishReason" width="130" />
        <el-table-column label="工具" width="140">
          <template #default="{ row }">{{ toolLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="arguments" label="参数" min-width="160" />
        <el-table-column prop="observation" label="观察" min-width="220" />
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
.id-input {
  width: 360px;
}
</style>
