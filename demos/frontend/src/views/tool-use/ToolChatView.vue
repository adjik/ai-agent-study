<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { toolUseApi } from '@/api/tool-use'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)

const question = ref('ORD-1001 现在什么状态？')
const { loading, error, data, elapsedMs, run } = useRunner(() =>
  toolUseApi.chat(question.value.trim()),
)

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
    <p class="hint">
      Q1 夹具。后端 Spring AI 注册 @Tool getOrder 并自动跑工具循环，answer 应提到已支付或 PAID。
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
        POST /chat
      </el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms（含 LLM + 工具）</span>
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
  margin: 14px 0;
}
.elapsed {
  font-size: 12px;
  color: #94a3b8;
}
</style>
