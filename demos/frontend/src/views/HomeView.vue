<script setup lang="ts">
import { computed } from 'vue'
import { DEMO_SERVICES } from '../../demos.config'

const readyCount = computed(() => DEMO_SERVICES.filter((d) => d.status === 'ready').length)
</script>

<template>
  <div class="page-head">
    <h2>Demo 控制台</h2>
    <p>
      一个 Vue 前端对接 <strong>demos/backend</strong> 统一后端（端口 8080）。左侧菜单按学习阶段分组，
      各阶段代码在 Java 包 <code>stage00</code>、<code>stage01</code> … 下；当前已接通 {{ readyCount }} 个阶段。
    </p>
  </div>

  <el-row :gutter="16">
    <el-col v-for="demo in DEMO_SERVICES" :key="demo.id" :xs="24" :sm="12" :lg="8">
      <el-card class="card" shadow="hover" @click="$router.push(demo.endpoints[0].route)">
        <div class="card-top">
          <span class="stage">阶段 {{ demo.stage }}</span>
          <el-tag :type="demo.status === 'ready' ? 'success' : 'info'" size="small">
            {{ demo.status === 'ready' ? '可调用' : '待开发' }}
          </el-tag>
        </div>
        <h3>{{ demo.title }}</h3>
        <p>{{ demo.subtitle }}</p>
        <div class="card-foot">
          <span>{{ demo.codeDir }}</span>
          <code>stage {{ demo.stage }}</code>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.card {
  margin-bottom: 16px;
  cursor: pointer;
  border-radius: 12px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.stage {
  font-size: 12px;
  color: #64748b;
}

h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  min-height: 36px;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 14px;
  font-size: 12px;
  color: #94a3b8;
}

code {
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
