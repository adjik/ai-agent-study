import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { DEMO_SERVICES, type DemoView } from '../../demos.config'
import AppLayout from '@/layouts/AppLayout.vue'
import HomeView from '@/views/HomeView.vue'
import PlaceholderView from '@/views/PlaceholderView.vue'
import HealthView from '@/views/hello-llm/HealthView.vue'
import HelloView from '@/views/hello-llm/HelloView.vue'
import ConversationView from '@/views/hello-llm/ConversationView.vue'
import StructuredView from '@/views/hello-llm/StructuredView.vue'
import StudentScoresView from '@/views/hello-llm/StudentScoresView.vue'
import StreamingView from '@/views/hello-llm/StreamingView.vue'
import ChatView from '@/views/hello-llm/ChatView.vue'
import AllView from '@/views/hello-llm/AllView.vue'
import PromptExperimentView from '@/views/prompt/PromptExperimentView.vue'
import PromptTriageView from '@/views/prompt/PromptTriageView.vue'
import ToolStoreView from '@/views/tool-use/ToolStoreView.vue'
import ToolToolsView from '@/views/tool-use/ToolToolsView.vue'
import ToolChatView from '@/views/tool-use/ToolChatView.vue'
import type { Component } from 'vue'

const viewMap: Record<DemoView, Component> = {
  health: HealthView,
  hello: HelloView,
  conversation: ConversationView,
  structured: StructuredView,
  studentScores: StudentScoresView,
  streaming: StreamingView,
  chat: ChatView,
  all: AllView,
  promptExperiment: PromptExperimentView,
  promptTriage: PromptTriageView,
  toolStore: ToolStoreView,
  toolTools: ToolToolsView,
  toolChat: ToolChatView,
  placeholder: PlaceholderView,
}

const demoRoutes: RouteRecordRaw[] = DEMO_SERVICES.flatMap((demo) =>
  demo.endpoints.map((endpoint) => ({
    path: endpoint.route.replace(/^\//, ''),
    name: `${demo.id}-${endpoint.id}`,
    component: viewMap[endpoint.view],
    meta: { demo, endpoint, title: `${demo.title} / ${endpoint.title}` },
  })),
)

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', name: 'home', component: HomeView, meta: { title: '总览' } },
        ...demoRoutes,
      ],
    },
  ],
})

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) ?? 'Demo Console'
  document.title = `${title} · AI Agent`
})

export default router
