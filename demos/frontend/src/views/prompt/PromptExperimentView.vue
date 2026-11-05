<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import {
  callPromptEndpoint,
  scenariosOf,
  type PromptScenario,
} from '@/api/prompt'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
const scenarios = computed(() => scenariosOf(endpoint.value.id))
const isRole = computed(() => endpoint.value.id === 'role')
const isFailure = computed(() => endpoint.value.id === 'failure')
const activeId = ref('')
const system = ref('')
const ticket = ref('')
const maxTokens = ref<number | undefined>(undefined)

const activeScenario = computed(() =>
  scenarios.value.find((s) => s.id === activeId.value),
)

const { loading, error, data, elapsedMs, run } = useRunner<unknown>(() =>
  callPromptEndpoint(demo.value, endpoint.value.id, ticket.value.trim(), {
    system: isRole.value ? system.value : undefined,
    maxTokens: isFailure.value ? maxTokens.value : undefined,
  }),
)

const activeHint = computed(() => {
  return activeScenario.value?.hint ?? '点上方按钮切换对照场景，再调用接口。'
})

function applyScenario(scenario: PromptScenario) {
  activeId.value = scenario.id
  ticket.value = scenario.text
  system.value = scenario.system ?? ''
  maxTokens.value = scenario.maxTokens
  data.value = null
  error.value = ''
}

watch(
  () => endpoint.value.id,
  () => {
    const first = scenarios.value[0]
    if (first) {
      applyScenario(first)
    }
  },
  { immediate: true },
)

function submit() {
  if (!ticket.value.trim()) {
    return
  }
  void run()
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="scenarios">
      <span class="label">对照场景</span>
      <el-button
        v-for="item in scenarios"
        :key="item.id"
        size="small"
        :type="activeId === item.id ? 'primary' : 'default'"
        :disabled="loading"
        @click="applyScenario(item)"
      >
        {{ item.label }}
      </el-button>
    </div>
    <p class="hint">{{ activeHint }}</p>
    <template v-if="isRole">
      <p class="field-label">系统角色（.system）</p>
      <el-input
        v-model="system"
        type="textarea"
        :rows="5"
        maxlength="2000"
        show-word-limit
        placeholder="留空 = 不写 system（无角色）；有内容则进 ChatClient.system()"
      />
      <p class="field-label">工单内容（.user）</p>
    </template>
    <p v-if="isFailure" class="field-label">maxTokens（可改，留空=不限制）</p>
    <el-input-number
      v-if="isFailure"
      v-model="maxTokens"
      :min="1"
      :max="4000"
      :step="10"
      :controls-position="'right'"
      class="tokens-input"
      placeholder="例如 40"
    />
    <el-input
      v-model="ticket"
      type="textarea"
      :rows="isRole ? 6 : 12"
      maxlength="4000"
      show-word-limit
      placeholder="点上方按钮填入示例，也可自己改"
    />
    <div class="toolbar">
      <el-button type="primary" :loading="loading" :disabled="!ticket.trim()" @click="submit">
        调用实验接口
      </el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <JsonPanel v-if="data !== null" :value="data" />
  </el-card>
</template>

<style scoped>
.scenarios {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.label {
  font-size: 13px;
  color: #64748b;
  margin-right: 4px;
}

.hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.field-label {
  margin: 12px 0 6px;
  font-size: 13px;
  color: #475569;
}

.tokens-input {
  margin-bottom: 8px;
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
