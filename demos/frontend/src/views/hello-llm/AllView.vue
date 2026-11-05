<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { helloLlmClient } from '@/api/hello-llm'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const api = computed(() => helloLlmClient(demo.value))
const { loading, error, data, elapsedMs, run } = useRunner<Record<string, unknown>>(() => api.value.all())
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="run">运行全部 Demo</el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms · 不含流式接口</span>
    </div>
    <el-alert
      v-if="!data && !error"
      type="warning"
      title="会连续调用多次 DeepSeek，耗时和费用都比单接口高。"
      show-icon
      :closable="false"
      class="block"
    />
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <JsonPanel v-if="data" :value="data" />
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
