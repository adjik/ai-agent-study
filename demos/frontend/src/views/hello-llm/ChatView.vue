<script setup lang="ts">
import { computed, ref } from 'vue'
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
const msg = ref('用 Java 工程师能听懂的话解释什么是 Token')
const { loading, error, data, elapsedMs, run } = useRunner<unknown>(() => api.value.chat(msg.value.trim()))

function submit() {
  if (!msg.value.trim()) {
    return
  }
  void run()
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <el-input
      v-model="msg"
      type="textarea"
      :rows="4"
      maxlength="2000"
      show-word-limit
      placeholder="输入问题，回车不提交，点按钮调用 /api/test/hello"
    />
    <div class="toolbar">
      <el-button type="primary" :loading="loading" :disabled="!msg.trim()" @click="submit">
        发送
      </el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <JsonPanel v-if="data !== null" :value="data" />
  </el-card>
</template>

<style scoped>
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
