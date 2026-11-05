<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import {
  helloLlmClient,
  isStudentScoresError,
  type StudentExamRecord,
  type StudentScoresResult,
} from '@/api/hello-llm'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

const DEFAULT_TEXT =
  '小明是一个五年级的小学生，本次考试语文成绩98，数学成绩100，科学99'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const api = computed(() => helloLlmClient(demo.value))
const text = ref(DEFAULT_TEXT)

const { loading, error, data, elapsedMs, run } = useRunner<StudentScoresResult>(() =>
  api.value.studentScores(text.value.trim() || undefined),
)

const record = computed<StudentExamRecord | null>(() => {
  if (!data.value || isStudentScoresError(data.value)) {
    return null
  }
  return data.value
})

const parseFailed = computed(() => data.value !== null && isStudentScoresError(data.value))

function submit() {
  void run()
}

function useDefault() {
  text.value = DEFAULT_TEXT
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <p class="hint">
      从自然语言描述中提取学生姓名、年级与各科分数。不传 <code>text</code> 时后端使用默认示例（小明五年级）。
    </p>
    <el-input
      v-model="text"
      type="textarea"
      :rows="4"
      maxlength="2000"
      show-word-limit
      placeholder="输入成绩描述，或留空使用默认示例"
    />
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="submit">提取成绩</el-button>
      <el-button :disabled="loading" @click="useDefault">恢复默认示例</el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
      <el-tag v-if="record" type="success" size="small">解析成功</el-tag>
      <el-tag v-if="parseFailed" type="danger" size="small">解析失败</el-tag>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <template v-if="record">
      <el-descriptions :column="2" border size="small" class="summary">
        <el-descriptions-item label="学生姓名">{{ record.studentName }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ record.gradeLevel }}</el-descriptions-item>
      </el-descriptions>
      <h4>各科分数</h4>
      <el-table :data="record.subjectScores" size="small" stripe>
        <el-table-column prop="subject" label="科目" min-width="120" />
        <el-table-column prop="score" label="分数" width="100" />
      </el-table>
    </template>
    <el-alert
      v-if="parseFailed && isStudentScoresError(data!)"
      type="warning"
      :title="data!.error"
      show-icon
      :closable="false"
      class="parse-alert"
    />
    <JsonPanel v-if="data !== null" :value="data" class="raw-json" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
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

.summary {
  margin-bottom: 16px;
}

h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #64748b;
}

.parse-alert {
  margin-bottom: 12px;
}

.raw-json {
  margin-top: 16px;
}
</style>
