<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { agentCoreApi, type GraphResponse, type StepRecord } from '@/api/agent-core'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

/*
 * 第 9 天：下一跳写在边上。
 * 1. 只问状态、「可用数量」留空 —— nodes 里仍然有 queryInventory，因为那条边不看问句。
 * 2. 拿同一句去「规划对照」的边想边做 —— steps 通常只有 getOrder。这一对就是今天的主题。
 * 3. 回来把可用数量填 0 —— branch 翻到 cannotShip，而 fixtureAvailable 仍是夹具里的数。
 * 4. 换单号：ORD-1002 已取消、ORD-1003 库存 0、ORD-1009 不存在，三种都落在 cannotShip，
 *    但 observations 里的观察不同（CANCELLED / available=0 / not_found）。
 */
const PRESETS = [
  { id: 'status', label: '只问状态', text: 'ORD-1001 现在什么状态？' },
  { id: 'ship', label: '问发货', text: 'ORD-1001 能不能发货？先查订单再查对应库存。' },
  { id: 'cancelled', label: '已取消', text: 'ORD-1002 能不能发货？' },
  { id: 'no-stock', label: '库存为 0', text: 'ORD-1003 能不能发货？' },
  { id: 'missing', label: '单号不存在', text: 'ORD-1009 能不能发货？' },
]

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)

const question = ref(PRESETS[0].text)
const activePreset = ref(PRESETS[0].id)
/** 页面上是「留空」。用 null 表示没填，提交时转成 undefined，请求体里就不会出现这个字段。 */
const override = ref<number | null>(null)

const { loading, error, data, elapsedMs, run } = useRunner<GraphResponse>(() =>
  agentCoreApi.graph(question.value.trim(), override.value ?? undefined),
)

const nodes = computed<string[]>(() => data.value?.nodes ?? [])
const observations = computed<StepRecord[]>(() => data.value?.observations ?? [])

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

/** 条件边的结果直接决定标签颜色：能不能发，一眼看出来。 */
function branchType(): 'success' | 'warning' {
  return data.value?.branch === 'canShip' ? 'success' : 'warning'
}
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <p class="hint">
      这张图里没有 tool_calls：查订单 → 查库存 → 按条件选文案，都是 addEdge 和
      addConditionalEdges 写死的。问句只决定查哪张单，不决定走哪条边；模型只在最后一个节点里写那句话。
    </p>
    <div class="scenarios">
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
      <span class="label">本次判断用的可用数量</span>
      <el-input-number
        v-model="override"
        :min="0"
        :max="9999"
        placeholder="留空则读夹具"
        :disabled="loading"
      />
      <span class="hint-inline">留空 = 读夹具；填 0 = 演练「库存为 0」，夹具不会被改</span>
    </div>
    <div class="toolbar">
      <el-button type="primary" :loading="loading" :disabled="!question.trim()" @click="submit">
        发送
      </el-button>
      <span v-if="elapsedMs" class="elapsed">耗时 {{ elapsedMs }} ms</span>
    </div>
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" />
    <template v-if="data">
      <div class="summary">
        <span class="field-label">orderId</span>
        <el-tag effect="plain">{{ data.orderId }}</el-tag>
        <span class="field-label">branch</span>
        <el-tag :type="branchType()" effect="plain">{{ data.branch }}</el-tag>
        <span class="field-label">fixtureAvailable</span>
        <el-tag effect="plain">{{ data.fixtureAvailable }}</el-tag>
      </div>
      <p class="field-label">nodes（走过的节点，按顺序）</p>
      <div class="chain">
        <template v-for="(node, index) in nodes" :key="`${node}-${index}`">
          <el-tag effect="plain">{{ node }}</el-tag>
          <span v-if="index < nodes.length - 1" class="arrow">→</span>
        </template>
      </div>
      <p class="field-label">回答</p>
      <p class="answer">{{ data.answer }}</p>
      <p class="field-label">observations（两个查询节点看到的 JSON）</p>
      <el-table :data="observations" size="small" border>
        <el-table-column prop="round" label="序号" width="70" />
        <el-table-column prop="toolName" label="工具" width="140" />
        <el-table-column prop="arguments" label="参数" min-width="190" />
        <el-table-column prop="observation" label="观察" min-width="270" />
      </el-table>
      <JsonPanel :value="data" />
    </template>
  </el-card>
</template>

<style scoped>
.scenarios,
.toolbar,
.summary,
.chain {
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
.hint-inline {
  font-size: 12px;
  color: #94a3b8;
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
.arrow {
  color: #94a3b8;
}
</style>
