<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { BACKEND, type DemoEndpoint, type DemoService } from '../../demos.config'
import PageIntro from '@/components/PageIntro.vue'

const route = useRoute()
const demo = computed(() => route.meta.demo as DemoService)
const endpoint = computed(() => route.meta.endpoint as DemoEndpoint)
</script>

<template>
  <PageIntro :demo="demo" :endpoint="endpoint" />
  <el-card shadow="never">
    <el-result icon="info" title="该阶段后端尚未提供 HTTP 接口">
      <template #sub-title>
        <p>
          计划代码包：<code>{{ demo.codeDir }}</code>，统一后端端口
          <code>{{ BACKEND.port }}</code>。
        </p>
        <p>
          在 <code>demos/backend</code> 对应 stage 包写好 Controller 后：1）在
          <code>demos/frontend/demos.config.ts</code> 把 status 改为
          <code>ready</code> 并补 <code>apiPath</code>；2）补对应 Vue 页面。
        </p>
      </template>
    </el-result>
  </el-card>
</template>
