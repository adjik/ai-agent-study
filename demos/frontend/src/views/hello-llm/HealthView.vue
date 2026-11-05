<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { helloLlmClient, type HealthPayload } from '@/api/hello-llm'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const api = computed(() => helloLlmClient(demo.value))
const { loading, error, data, elapsedMs, run } = useRunner<HealthPayload>(() => api.value.health())

onMounted(run)
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="run">重新检查</el-button>
      <span v-if="elapsedMs" class="elapsed">{{ elapsedMs }} ms</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="block" />
    <template v-if="data">
      <el-descriptions :column="2" border class="block">
        <el-descriptions-item label="状态">{{ data.status }}</el-descriptions-item>
        <el-descriptions-item label="Provider">{{ data.llmProvider }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ data.model }}</el-descriptions-item>
        <el-descriptions-item label="API Key">
          {{ data.apiKeyConfigured ? data.apiKeyPreview : '未配置' }}
        </el-descriptions-item>
        <el-descriptions-item label="Base URL" :span="2">{{ data.baseUrl }}</el-descriptions-item>
      </el-descriptions>
      <JsonPanel :value="data" />
    </template>
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
.block {
  margin-bottom: 14px;
}
</style>
