<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { toolUseApi, type ToolCallRecord } from '@/api/tool-use'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

interface QuestionPreset {
  id: string
  label: string
  text: string
  hint: string
}

const PRESETS: QuestionPreset[] = [
  {
    id: 'q1',
    label: 'Q1 订单',
    text: 'ORD-1001 现在什么状态？',
    hint: '应查到 ORD-1001。回答里应有 PAID 或已支付。Spring 的 toolsCalled 里是 getOrder。',
  },
  {
    id: 'q2',
    label: 'Q2 库存',
    text: 'SKU-88 在 WH-A 还有多少库存？',
    hint: '应查库存。可用数量是 10。Spring 的 toolsCalled 里应是 getInventory。',
  },
  {
    id: 'q3',
    label: 'Q3 连查',
    text: 'ORD-1001 能不能发货？先查订单再查对应库存。',
    hint: '同一轮里先查订单再查库存。Spring 的 toolsCalled 应有 getOrder 和 getInventory 两条。',
  },
  {
    id: 'missing',
    label: '查无此单',
    text: 'ORD-9999 什么状态？',
    hint: '单号合法，夹具里没有。回答不要把这笔说成 PAID。Spring 轨迹里 result 对应 not_found。',
  },
  {
    id: 'bad',
    label: '非法单号',
    text: '查订单 ABC 的状态',
    hint: '不是 ORD-数字。模型可以不调工具；若调用，应是 invalid_argument，不要查出 PAID。',
  },
]

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const isLc4j = computed(() => endpoint.value.id === 'chat-lc4j')

const question = ref(PRESETS[0].text)
const activeId = ref(PRESETS[0].id)
const activeHint = computed(
  () => PRESETS.find((item) => item.id === activeId.value)?.hint ?? PRESETS[0].hint,
)

const { loading, error, data, elapsedMs, run } = useRunner(() => {
  const text = question.value.trim()
  return isLc4j.value ? toolUseApi.chatLc4j(text) : toolUseApi.chat(text)
})

const toolsCalled = computed<ToolCallRecord[]>(() => data.value?.toolsCalled ?? [])

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
    <p v-if="isLc4j" class="hint">
      这条走 LangChain4j，读的是同一份订单和库存。响应只有 answer，没有 toolsCalled。
    </p>
    <el-input
      v-model="question"
      type="textarea"
      :rows="3"
      maxlength="500"
      show-word-limit
      placeholder="ORD-1001 现在什么状态？"
    />
    <div class="toolbar">
      <el-button type="primary" :loading="loading" :disabled="!question.trim()" @click="submit">
        {{ isLc4j ? 'POST /chat-lc4j' : 'POST /chat' }}
      </el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms（含 LLM + 工具）</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <template v-if="data">
      <p class="field-label">回答</p>
      <p class="answer">{{ data.answer }}</p>
      <template v-if="!isLc4j">
        <p class="field-label">toolsCalled</p>
        <el-table v-if="toolsCalled.length" :data="toolsCalled" size="small" border>
          <el-table-column prop="name" label="工具" width="140" />
          <el-table-column prop="args" label="参数" min-width="220" />
          <el-table-column label="成功" width="80">
            <template #default="{ row }">{{ row.success ? 'true' : 'false' }}</template>
          </el-table-column>
          <el-table-column prop="elapsedMs" label="耗时 ms" width="100" />
        </el-table>
        <p v-else class="hint">这次没有调用工具。</p>
      </template>
      <JsonPanel :value="data" />
    </template>
  </el-card>
</template>

<style scoped>
.scenarios {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
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
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 14px 0;
}
.elapsed {
  font-size: 12px;
  color: #94a3b8;
}
</style>
