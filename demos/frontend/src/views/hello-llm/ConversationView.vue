<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { helloLlmClient } from '@/api/hello-llm'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

interface Round {
  question: string
  answer: string
}

interface ConversationPayload {
  round1: Round
  round2: Round
}

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const api = computed(() => helloLlmClient(demo.value))
const { loading, error, data, elapsedMs, run } = useRunner<ConversationPayload>(() =>
  api.value.conversation(),
)
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="run">跑两轮对话</el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms · 会调用两次 LLM</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <el-timeline v-if="data">
      <el-timeline-item timestamp="第 1 轮" placement="top">
        <p class="q">Q: {{ data.round1.question }}</p>
        <p class="a">{{ data.round1.answer }}</p>
      </el-timeline-item>
      <el-timeline-item timestamp="第 2 轮" placement="top">
        <p class="q">Q: {{ data.round2.question }}</p>
        <p class="a">{{ data.round2.answer }}</p>
      </el-timeline-item>
    </el-timeline>
    <JsonPanel v-if="data" :value="data" />
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.elapsed {
  font-size: 12px;
  color: #94a3b8;
}
.q {
  margin: 0 0 8px;
  color: #64748b;
  font-size: 13px;
}
.a {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.65;
}
</style>
