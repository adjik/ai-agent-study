<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { helloLlmClient } from '@/api/hello-llm'
import { formatError } from '@/api/http'
import PageIntro from '@/components/PageIntro.vue'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const api = computed(() => helloLlmClient(demo.value))

const loading = ref(false)
const error = ref('')
const text = ref('')
const elapsedMs = ref(0)
let abort: AbortController | null = null

async function run() {
  abort?.abort()
  abort = new AbortController()
  loading.value = true
  error.value = ''
  text.value = ''
  const started = performance.now()
  try {
    await api.value.stream((chunk) => {
      text.value += chunk
    }, abort.signal)
  } catch (e) {
    if ((e as Error).name !== 'AbortError') {
      error.value = formatError(e)
    }
  } finally {
    elapsedMs.value = Math.round(performance.now() - started)
    loading.value = false
  }
}

function stop() {
  abort?.abort()
  loading.value = false
}

onBeforeUnmount(stop)
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="run">开始流式输出</el-button>
      <el-button :disabled="!loading" @click="stop">停止</el-button>
      <span v-if="elapsedMs" class="elapsed">{{ elapsedMs }} ms · {{ text.length }} 字</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <pre class="json-block stream">{{ text || (loading ? '等待首个 token…' : '点击按钮后，模型会逐段推送。') }}</pre>
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
.stream {
  min-height: 220px;
}
</style>
