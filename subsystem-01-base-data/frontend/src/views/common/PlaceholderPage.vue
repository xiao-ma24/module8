<template>
  <el-card shadow="never" class="placeholder-card">
    <template #header>
      <span class="card-title">{{ pageTitle }}</span>
    </template>
    <el-empty :description="pageDesc">
      <el-tag type="info" effect="plain">页面开发中</el-tag>
    </el-empty>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const pageTitle = computed(() => route.meta.title || '页面')
const pageDesc = computed(() => route.meta.desc || '该页面功能开发中')
</script>

<style scoped>
.placeholder-card {
  min-height: calc(100vh - 92px);
}

.card-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
}
</style>
