<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { helloLlmClient } from '@/api/hello-llm'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

interface StructuredPayload {
  rawJson: string
  parsed: Record<string, unknown> | null
  parseSuccess: boolean
  parseError: string
}

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const api = computed(() => helloLlmClient(demo.value))
const { loading, error, data, elapsedMs, run } = useRunner<StructuredPayload>(() =>
  api.value.structured(),
)
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="run">生成结构化 JSON</el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
      <el-tag v-if="data" :type="data.parseSuccess ? 'success' : 'danger'" size="small">
        {{ data.parseSuccess ? '解析成功' : '解析失败' }}
      </el-tag>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <el-row v-if="data" :gutter="16">
      <el-col :md="12">
        <h4>模型原文</h4>
        <JsonPanel :value="data.rawJson" />
      </el-col>
      <el-col :md="12">
        <h4>Java 解析结果</h4>
        <JsonPanel :value="data.parsed ?? data.parseError" />
      </el-col>
    </el-row>
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
h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #64748b;
}
</style>
