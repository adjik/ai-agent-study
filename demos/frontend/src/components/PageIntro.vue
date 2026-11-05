<script setup lang="ts">
import { computed } from 'vue'
import { BACKEND, resolveApiPath, type DemoEndpoint, type DemoService } from '../../demos.config'

const props = defineProps<{
  demo: DemoService
  endpoint: DemoEndpoint
}>()

const apiPath = computed(() =>
  props.endpoint.apiPath ? resolveApiPath(props.endpoint) : undefined,
)

const fullUrl = computed(() =>
  apiPath.value ? `http://127.0.0.1:${BACKEND.port}/api${apiPath.value}` : undefined,
)
</script>

<template>
  <div class="page-head">
    <h2>{{ endpoint.title }}</h2>
    <p>{{ endpoint.description }}</p>
    <div class="meta-row">
      <el-tag size="small" effect="plain">{{ endpoint.method }}</el-tag>
      <el-tag v-if="apiPath" size="small" type="info" effect="plain">
        {{ BACKEND.proxyPrefix }}{{ apiPath }}
      </el-tag>
      <el-tag size="small" type="warning" effect="plain">{{ demo.codeDir }}</el-tag>
    </div>
    <div v-if="endpoint.learning" class="learning">
      <p class="learning-title">学习对照</p>
      <p v-if="endpoint.learning.plan.length" class="learning-line">
        <span class="learning-label">计划</span>
        {{ endpoint.learning.plan.join(' · ') }}
      </p>
      <p class="learning-line">
        <span class="learning-label">手册</span>
        {{ endpoint.learning.stageDoc }}
      </p>
      <p v-if="endpoint.learning.notes.length" class="learning-line">
        <span class="learning-label">笔记</span>
        {{ endpoint.learning.notes.join(' · ') }}
      </p>
    </div>
    <p v-if="fullUrl" class="direct">
      直连后端：<code>{{ fullUrl }}</code>
    </p>
  </div>
</template>

<style scoped>
.direct {
  margin: 10px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.learning {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.learning-title {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.learning-line {
  margin: 0 0 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.learning-line:last-child {
  margin-bottom: 0;
}

.learning-label {
  display: inline-block;
  min-width: 2.5em;
  margin-right: 6px;
  color: #94a3b8;
}

code {
  color: #475569;
}
</style>
