<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { agentCoreApi, type PlanStep, type PlanningResponse, type StepRecord } from '@/api/agent-core'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

type Strategy = 'react' | 'plan' | 'reflect'

interface StrategyOption {
  id: Strategy
  label: string
  hint: string
}

/*
 * 第 8 天：同一句问句，只换策略，看「下一步由谁决定」。
 * react：没有 plan，steps 一轮一轮长出来，库存工具是因为上一轮看到了订单才出现。
 * plan：先有 plan（此时还没有观察），Java 照清单查表，最后才写句子。
 * reflect：draft 通常比 answer 长；review 里能看到「过长」或「通过」；查表仍是那两次。
 */
const STRATEGIES: StrategyOption[] = [
  {
    id: 'react',
    label: '边想边做',
    hint: '响应里没有 plan。steps 先出现 getOrder，看到观察后下一轮才出现 getInventory。',
  },
  {
    id: 'plan',
    label: '先计划再执行',
    hint: '先有 plan，两项工具名是 getOrder、getInventory，此时还没有观察。随后 steps 才补上 PAID 和 10。',
  },
  {
    id: 'reflect',
    label: '做完再评',
    hint: 'draft 通常比 answer 长。review 里能看到过长或通过。最终 answer 含 ORD-1001，查表仍只是那两次。',
  },
]

const PRESETS = [
  { id: 'ship', label: '发货', text: 'ORD-1001 能不能发货？先查订单再查对应库存。' },
  { id: 'status', label: '只问状态', text: 'ORD-1001 现在什么状态？' },
]

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)

const question = ref(PRESETS[0].text)
const strategy = ref<Strategy>('react')
const maxSteps = ref(4)
const activePreset = ref(PRESETS[0].id)

/** 提示随策略切换，避免拿「先计划再执行」的预期去读 react 的结果。 */
const activeHint = computed(
  () => STRATEGIES.find((item) => item.id === strategy.value)?.hint ?? STRATEGIES[0].hint,
)

/*
 * 每次点击都等三种策略各自跑完才返回：plan 是三次模型调用，reflect 是两次，
 * 所以同一个问句在不同策略下耗时差别明显，页面上要留着 elapsedMs 作对照。
 */
const { loading, error, data, elapsedMs, run } = useRunner<PlanningResponse>(() =>
  agentCoreApi.plan(question.value.trim(), strategy.value, maxSteps.value),
)

const steps = computed<StepRecord[]>(() => data.value?.steps ?? [])
/*
 * react 与 reflect 的 plan 是 null，表格不该渲染。Vue 模板里直接读数据对象的字段，
 * 用这个 computed 把 null 收成空数组，模板的 v-if 就只需要判长度。
 */
const plan = computed<PlanStep[]>(() => data.value?.plan ?? [])

function applyPreset(preset: (typeof PRESETS)[number]) {
  activePreset.value = preset.id
  question.value = preset.text
}

function submit() {
  if (!question.value.trim()) {
    return
  }
  void run()
}

/** 收口行没有工具名。写成「不再点菜」才能和上一行的具体函数名区分开。 */
function toolLabel(step: StepRecord) {
  return step.toolName ? step.toolName : '不再点菜'
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <div class="scenarios">
      <span class="label">问法</span>
      <el-button
        v-for="item in PRESETS"
        :key="item.id"
        size="small"
        :type="activePreset === item.id ? 'primary' : 'default'"
        :disabled="loading"
        @click="applyPreset(item)"
      >
        {{ item.label }}
      </el-button>
    </div>
    <el-input v-model="question" type="textarea" :rows="3" maxlength="500" show-word-limit />
    <div class="toolbar">
      <span class="label">策略</span>
      <el-radio-group v-model="strategy" :disabled="loading">
        <el-radio-button v-for="item in STRATEGIES" :key="item.id" :value="item.id">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
      <span class="label">最大步数</span>
      <el-input-number v-model="maxSteps" :min="1" :max="8" :disabled="loading" />
      <el-button type="primary" :loading="loading" :disabled="!question.trim()" @click="submit">
        发送
      </el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
    </div>
    <p class="hint">{{ activeHint }}</p>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <template v-if="data">
      <div class="summary">
        <span class="field-label">strategy</span>
        <el-tag effect="plain">{{ data.strategy }}</el-tag>
        <span class="field-label">stopReason</span>
        <el-tag :type="data.stopReason === 'final' ? 'success' : 'warning'" effect="plain">
          {{ data.stopReason }}
        </el-tag>
        <span class="field-label">steps</span>
        <el-tag effect="plain">{{ steps.length }} 条</el-tag>
      </div>

      <template v-if="plan.length">
        <p class="field-label">plan（查表之前写下的清单）</p>
        <el-table :data="plan" size="small" border>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="tool" label="工具" width="160" />
          <el-table-column prop="arguments" label="参数（此时还没有观察可参考）" min-width="280" />
        </el-table>
        <p class="hint">清单里 sku/warehouse 可能是 null：写清单时还没查过订单，执行时才用上一步观察补上。</p>
      </template>

      <template v-if="data.strategy === 'reflect'">
        <p class="field-label">draft（初稿）</p>
        <p class="answer">{{ data.draft }}</p>
        <p class="field-label">review（检查意见）</p>
        <p class="answer">{{ data.review }}</p>
      </template>

      <p class="field-label">回答</p>
      <p class="answer">{{ data.answer }}</p>

      <p class="field-label">steps（查表轨迹）</p>
      <el-table :data="steps" size="small" border>
        <el-table-column prop="round" label="轮次" width="70" />
        <el-table-column prop="finishReason" label="finishReason" width="130" />
        <el-table-column label="工具" width="140">
          <template #default="{ row }">{{ toolLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="arguments" label="参数" min-width="180" />
        <el-table-column prop="observation" label="观察" min-width="260" />
        <el-table-column label="latencyMs" width="110">
          <template #default="{ row }">{{ row.latencyMs ?? '—' }}</template>
        </el-table-column>
      </el-table>
      <JsonPanel :value="data" />
    </template>
  </el-card>
</template>

<style scoped>
.scenarios,
.toolbar,
.summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.scenarios {
  margin-bottom: 12px;
}
.summary {
  margin-bottom: 8px;
}
.label,
.field-label {
  font-size: 13px;
  color: #475569;
}
.field-label {
  margin: 8px 0;
  font-weight: 600;
}
.hint {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 13px;
}
.answer {
  margin: 0 0 12px;
  white-space: pre-wrap;
  line-height: 1.6;
}
.toolbar {
  margin: 14px 0;
}
.elapsed {
  font-size: 12px;
  color: #94a3b8;
}
</style>
