<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { toolUseApi } from '@/api/tool-use'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const { loading, error, data, elapsedMs, run } = useRunner(() => toolUseApi.tools())
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <p class="hint">
      不调 LLM。列出 Spring 侧注册的工具名，应同时有 getOrder 和 getInventory。
    </p>
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="run">加载 /tools</el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <JsonPanel v-if="data !== null" :value="data" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 13px;
}
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
</style>
