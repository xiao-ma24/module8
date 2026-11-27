<template>
  <el-container style="height: 100%">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="26"><Files /></el-icon>
        <div>
          <div class="t1">物资盘点补货</div>
          <div class="t2">盘点预警与补货计划</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router background-color="transparent" text-color="#b6c2e2"
               active-text-color="#fff" class="menu">
        <el-menu-item v-for="m in menus" :key="m.index" :index="m.index">
          <el-icon><component :is="m.icon" /></el-icon>{{ m.label }}
        </el-menu-item>
        <div class="role-tip">库管员 · 金日勋<br />盘点 / 预警 / 补货计划</div>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div class="tb-left">
          <el-button text class="home-btn" @click="$router.push('/')">
            <el-icon size="16" style="margin-right:5px"><HomeFilled /></el-icon>首页
          </el-button>
          <span class="tb-sep"></span>
          <span class="tb-page">{{ $route.meta.title }}</span>
        </div>

        <el-dropdown trigger="click">
          <div class="user-chip">
            <el-avatar :size="32" style="background:#2563eb">金</el-avatar>
            <div class="uinfo">
              <div class="uname">金日勋</div>
              <div class="urole">库管员 · 盘点人</div>
            </div>
            <el-icon style="color:#94a3b8"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>子系统⑤ 盘点预警与补货</el-dropdown-item>
              <el-dropdown-item disabled>端口 8085 · /api/v1/stocktake</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main style="background: var(--bg); padding: 18px 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { Odometer, Files, Warning, ShoppingCart, HomeFilled, ArrowDown } from '@element-plus/icons-vue'

const menus = [
  { index: '/', icon: Odometer, label: '工作台' },
  { index: '/stocktake', icon: Files, label: '盘点任务' },
  { index: '/warning', icon: Warning, label: '库存预警' },
  { index: '/replenish', icon: ShoppingCart, label: '补货计划' },
]
</script>
