<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { helloLlmClient } from '@/api/hello-llm'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

interface HelloPayload {
  question: string
  answer: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
}

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const api = computed(() => helloLlmClient(demo.value))
const { loading, error, data, elapsedMs, run } = useRunner<HelloPayload>(() => api.value.hello())
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="run">调用 LLM</el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <template v-if="data">
      <el-descriptions :column="1" border class="block">
        <el-descriptions-item label="问题">{{ data.question }}</el-descriptions-item>
        <el-descriptions-item label="回答">{{ data.answer }}</el-descriptions-item>
      </el-descriptions>
      <el-space class="block">
        <el-tag>prompt {{ data.promptTokens }}</el-tag>
        <el-tag type="success">completion {{ data.completionTokens }}</el-tag>
        <el-tag type="warning">total {{ data.totalTokens }}</el-tag>
      </el-space>
      <JsonPanel :value="data" />
    </template>
  </el-card>
</template>

<style scoped>
.toolbar,
.block {
  margin-bottom: 14px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.elapsed {
  font-size: 12px;
  color: #94a3b8;
}
</style>
