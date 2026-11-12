<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { TabsPaneContext } from 'element-plus'
import { DEMO_SERVICES, findDemoByRoute } from '../../demos.config'

type OpenTab = {
  path: string
  title: string
  closable: boolean
}

const router = useRouter()
const route = useRoute()
const currentDemo = computed(() => findDemoByRoute(route.path))
const HOME_PATH = '/'

const openedTabs = ref<OpenTab[]>([])
const activeTabPath = ref(HOME_PATH)

const resolveTabTitle = () => {
  const routeTitle = route.meta.title as string | undefined
  return routeTitle ?? '未命名页面'
}

const ensureTabOpened = () => {
  const path = route.path
  const title = resolveTabTitle()
  const currentIndex = openedTabs.value.findIndex((tab) => tab.path === path)
  const closable = path !== HOME_PATH

  if (currentIndex >= 0) {
    openedTabs.value[currentIndex] = { path, title, closable }
  } else {
    openedTabs.value.push({ path, title, closable })
  }

  activeTabPath.value = path
}

watch(
  () => route.fullPath,
  () => {
    ensureTabOpened()
  },
  { immediate: true },
)

const onTabClick = (tabName: string | number) => {
  const path = String(tabName)
  if (path !== route.path) {
    router.push(path)
  }
}

const handleTabClick = (tab: TabsPaneContext) => {
  if (tab.paneName !== undefined) {
    onTabClick(tab.paneName)
  }
}

const onTabRemove = (targetPath: string | number) => {
  const path = String(targetPath)
  const currentIndex = openedTabs.value.findIndex((tab) => tab.path === path)
  if (currentIndex < 0) {
    return
  }

  openedTabs.value.splice(currentIndex, 1)
  if (route.path !== path) {
    return
  }

  const fallbackTab =
    openedTabs.value[currentIndex] ?? openedTabs.value[currentIndex - 1] ?? openedTabs.value[0]
  const fallbackPath = fallbackTab?.path ?? HOME_PATH
  router.push(fallbackPath)
}
</script>

<template>
  <el-container class="shell">
    <el-aside width="248px" class="aside">
      <div class="brand">
        <div class="brand-mark">AI</div>
        <div>
          <div class="brand-title">Agent Console</div>
          <div class="brand-sub">一个前端 · 一个后端</div>
        </div>
      </div>

      <el-menu
        :default-active="route.path"
        :default-openeds="['hello-llm', 'prompt', 'tool-use', 'agent-core']"
        router
        background-color="#0f172a"
        text-color="#94a3b8"
        active-text-color="#38bdf8"
      >
        <el-menu-item index="/">总览</el-menu-item>
        <el-sub-menu v-for="demo in DEMO_SERVICES" :key="demo.id" :index="demo.id">
          <template #title>
            <span class="menu-stage">{{ demo.stage }}</span>
            <span>{{ demo.title }}</span>
          </template>
          <el-menu-item v-for="ep in demo.endpoints" :key="ep.id" :index="ep.route">
            {{ ep.title }}
            <el-tag
              v-if="demo.status === 'planned'"
              size="small"
              type="info"
              class="planned-tag"
              effect="plain"
            >
              待开发
            </el-tag>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header" height="56px">
        <div class="header-left">
          <template v-if="currentDemo">
            <span class="crumb">阶段 {{ currentDemo.stage }}</span>
            <span class="sep">/</span>
            <span>{{ currentDemo.title }}</span>
          </template>
          <template v-else>学习工作区总览</template>
        </div>
        <div v-if="currentDemo" class="header-right">
          <el-tag :type="currentDemo.status === 'ready' ? 'success' : 'info'" size="small">
            {{ currentDemo.status === 'ready' ? '可调用' : '占位' }}
          </el-tag>
          <code>stage {{ currentDemo.stage }}</code>
        </div>
      </el-header>
      <el-main class="main">
        <el-tabs
          v-model="activeTabPath"
          type="card"
          class="page-tabs"
          @tab-click="handleTabClick"
          @tab-remove="onTabRemove"
        >
          <el-tab-pane
            v-for="tab in openedTabs"
            :key="tab.path"
            :name="tab.path"
            :label="tab.title"
            :closable="tab.closable"
          />
        </el-tabs>

        <router-view v-slot="{ Component, route: currentRoute }">
          <keep-alive>
            <component :is="Component" :key="currentRoute.path" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.shell {
  height: 100%;
}

.aside {
  background: var(--console-sidebar);
  color: var(--console-sidebar-text);
  overflow: auto;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0ea5e9, #6366f1);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-title {
  color: #f8fafc;
  font-weight: 650;
  font-size: 14px;
}

.brand-sub {
  color: #64748b;
  font-size: 12px;
  margin-top: 2px;
}

.menu-stage {
  display: inline-block;
  min-width: 22px;
  margin-right: 6px;
  font-variant-numeric: tabular-nums;
  color: #64748b;
}

.planned-tag {
  margin-left: 6px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 22px;
}

.header-left {
  font-size: 14px;
  color: #334155;
}

.crumb {
  color: #64748b;
}

.sep {
  margin: 0 8px;
  color: #cbd5e1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #64748b;
}

.header-right code {
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

.main {
  padding: 22px 24px 32px;
}

.page-tabs {
  margin-bottom: 14px;
}
</style>
