<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { DemoEndpoint, DemoService } from '../../../demos.config'
import { agentCoreApi, type MemoryMessage } from '@/api/agent-core'
import { useRunner } from '@/composables/useRunner'
import PageIntro from '@/components/PageIntro.vue'
import JsonPanel from '@/components/JsonPanel.vue'

/*
 * 第 7 天：同一句问题换成 LangChain4j 走。
 * 工具循环在 AiServices 内部，页面看不到 steps，只有 answer 和 memoryMessages。
 * 1. 窗口 20：先问状态，再问「刚才那张单」——第二句没有单号，靠记忆里的 ORD-1001。
 * 2. 窗口 2：发货这一问会写入用户句、工具调用、工具结果和最终回答，条数超过 2。
 *    裁掉带工具调用的模型消息时，紧跟的工具结果一并裁掉，库存 10 就不会进下一次提示。
 */
const PRESETS = [
  { id: 'status', label: '查状态', text: 'ORD-1001 现在什么状态？' },
  { id: 'follow', label: '刚才那张单', text: '刚才那张单是付过款了吗？' },
  { id: 'ship', label: '发货', text: 'ORD-1001 能不能发货？先查订单再查对应库存。' },
  { id: 'stock', label: '刚才查到的库存', text: '刚才查到的可用库存是多少？' },
]

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)

const question = ref(PRESETS[0].text)
const maxMessages = ref(20)
const conversationId = ref(crypto.randomUUID())
const activeId = ref(PRESETS[0].id)

const { loading, error, data, elapsedMs, run } = useRunner(() =>
  agentCoreApi.langchain4j({
    question: question.value.trim(),
    conversationId: conversationId.value.trim(),
    maxMessages: maxMessages.value,
  }),
)

const memoryMessages = computed<MemoryMessage[]>(() => data.value?.memoryMessages ?? [])

/**
 * 换会话 ID 而不是清空服务端记忆：旧 id 下的消息仍留在进程里，
 * 只是页面不再拿它去查。清掉上一次结果，避免把旧会话的窗口误认为新会话的。
 */
function newSession() {
  conversationId.value = crypto.randomUUID()
  data.value = null
  error.value = ''
}

function applyPreset(preset: (typeof PRESETS)[number]) {
  activeId.value = preset.id
  question.value = preset.text
}

/*
 * 发送前不改会话 ID。同一会话的第二句必须沿用上一次响应里的那个值，
 * 否则「刚才那张单」就没有单号可指。
 */
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
      循环在 AiServices 内部，没有 steps。第一句之后不要点「新会话」，
      第二句的「刚才」才有单号可用。把窗口改成 2，再发发货那句和追问，库存就会从提示里消失。
    </p>
    <div class="scenarios">
      <el-button
        v-for="item in PRESETS"
        :key="item.id"
        size="small"
        :type="activeId === item.id ? 'primary' : 'default'"
        :disabled="loading"
        @click="applyPreset(item)"
      >
        {{ item.label }}
      </el-button>
    </div>
    <el-input v-model="question" type="textarea" :rows="3" maxlength="500" show-word-limit />
    <div class="toolbar">
      <span class="label">会话 ID</span>
      <el-input v-model="conversationId" class="id-input" :disabled="loading" />
      <el-button :disabled="loading" @click="newSession">新会话</el-button>
      <span class="label">窗口条数</span>
      <el-input-number v-model="maxMessages" :min="1" :max="40" :disabled="loading" />
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
        <span class="field-label">conversationId</span>
        <el-tag effect="plain">{{ data.conversationId }}</el-tag>
        <span class="field-label">memoryMessages</span>
        <el-tag effect="plain">{{ memoryMessages.length }} 条</el-tag>
      </div>
      <p class="field-label">memoryMessages</p>
      <el-table v-if="memoryMessages.length" :data="memoryMessages" size="small" border>
        <el-table-column prop="role" label="角色" width="120" />
        <el-table-column prop="text" label="内容" min-width="280" />
      </el-table>
      <p v-else class="hint">这个会话 ID 在当前进程里没有消息。</p>
      <p class="field-label">回答</p>
      <p class="answer">{{ data.answer }}</p>
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
.id-input {
  width: 360px;
}
</style>
