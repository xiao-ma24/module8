<template>
  <div class="app-layout">
    <aside class="app-aside">
      <div class="brand">
        <div class="brand-logo">
          <el-icon :size="20"><Box /></el-icon>
        </div>
        <div class="brand-text">
          <span class="brand-name">学校物资管理</span>
          <span class="brand-sub">统一业务工作台</span>
        </div>
      </div>

      <el-menu
        class="side-menu"
        mode="vertical"
        router
        background-color="transparent"
        text-color="#b9c7e6"
        active-text-color="#ffffff"
        :default-active="activeMenu"
        :default-openeds="['basic']"
      >
        <el-menu-item-group v-if="isAdmin" title="平台管理">
          <el-menu-item index="/base/user">用户管理</el-menu-item>
          <el-menu-item index="/base/log">
            <span>操作监控</span>
            <el-badge :value="logTotal" :max="99" class="menu-badge" />
          </el-menu-item>
        </el-menu-item-group>

        <el-menu-item-group title="统一业务工作台">
          <el-sub-menu index="basic">
            <template #title>基础数据与审批</template>
            <el-menu-item :index="isAdmin ? '/base/material' : '/base/catalog'">
              {{ isAdmin ? '物资档案' : '物资目录' }}
            </el-menu-item>
            <el-menu-item index="/base/purchase">个人发票登记</el-menu-item>
            <el-menu-item v-if="isAdmin" index="/base/approval">审批中心</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/purchase">采购验收与入库</el-menu-item>
          <el-menu-item index="/issue">领用申请与出库</el-menu-item>
          <el-menu-item index="/borrow">借用归还与报损</el-menu-item>
          <el-menu-item index="/inventory">盘点预警与补货</el-menu-item>
        </el-menu-item-group>
      </el-menu>

      <div class="side-foot">{{ isAdmin ? '管理员视图 · 全功能' : '教职工视图 · 业务办理' }}</div>
    </aside>

    <div class="app-body">
      <header class="app-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="pageMeta.group">{{ pageMeta.group }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageMeta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-select
            v-model="role"
            class="role-switch"
            size="default"
            @change="handleRoleChange"
          >
            <el-option
              v-for="item in roleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <div class="user-chip">
            <el-avatar :size="34" class="avatar">韦</el-avatar>
            <div class="uinfo">
              <div class="uname">韦森宁</div>
              <div class="urole">{{ isAdmin ? '管理员' : '教职工' }}</div>
            </div>
            <el-button text class="logout-btn" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </header>

      <main class="app-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useRole } from '@/composables/useRole'
import { getLogPage } from '@/api/sysLog'

const route = useRoute()
const router = useRouter()

const { role, roleOptions, isAdmin, setRole } = useRole()

/** 菜单高亮只看路径，避免带查询参数（如 ?tab=invoice）时失焦 */
const activeMenu = computed(() => route.path)

/** 平台管理内各页面的面包屑 */
const pageMeta = computed(() => {
  if (route.path === '/base/user') {
    return {
      title: route.query.tab === 'dept' ? '部门管理' : '用户与权限管理',
      group: '平台管理'
    }
  }
  if (route.path === '/base/log') {
    return { title: '操作监控', group: '平台管理' }
  }
  if (['/base/material', '/base/catalog',
       '/base/purchase', '/base/approval'].includes(route.path)) {
    return { title: route.meta.title, group: '基础数据与审批' }
  }
  return { title: route.meta.title, group: '统一业务工作台' }
})

/** 操作监控角标：日志总条数 */
const logTotal = ref(0)

async function loadLogTotal() {
  if (!isAdmin.value) {
    logTotal.value = 0
    return
  }
  try {
    const data = await getLogPage({ pageNum: 1, pageSize: 1 })
    logTotal.value = data.total || 0
  } catch (e) {
    logTotal.value = 0
  }
}

watch(isAdmin, loadLogTotal)
onMounted(loadLogTotal)

/** 每个角色可访问的页面前缀与默认落地页 */
const roleHome = { admin: '/base/material', teacher: '/base/purchase' }
const roleAccess = {
  admin: [
    '/base/user',
    '/base/log',
    '/base/material',
    '/base/purchase',
    '/base/approval',
    '/purchase',
    '/issue',
    '/borrow',
    '/inventory'
  ],
  teacher: [
    '/base/catalog',
    '/base/purchase',
    '/purchase',
    '/issue',
    '/borrow',
    '/inventory'
  ]
}

function handleRoleChange(value) {
  setRole(value)
  const allowed = roleAccess[value] || []
  if (!allowed.some((path) => route.path.startsWith(path))) {
    router.push(roleHome[value])
  }
}

function handleLogout() {
  ElMessage.info('退出登录功能待接入')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

/* ========== 侧边栏：深蓝渐变 ========== */
.app-aside {
  display: flex;
  flex-direction: column;
  width: 220px;
  flex-shrink: 0;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow: hidden auto;
  background: var(--sidebar-bg);
}

.app-aside::before {
  content: '';
  position: absolute;
  top: -90px;
  left: -70px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  pointer-events: none;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.28), transparent 70%);
}

.brand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 18px;
  color: #fff;
  flex-shrink: 0;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex: none;
  border-radius: 11px;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.45);
  color: #fff;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.brand-sub {
  margin-top: 3px;
  font-size: 10.5px;
  color: #8ea2cf;
}

.side-menu {
  position: relative;
  flex: 1;
  border-right: none;
  padding: 6px 10px;
}

.side-menu :deep(.el-menu-item-group__title) {
  padding: 12px 10px 6px;
  font-size: 11px;
  color: #7f93c1;
  letter-spacing: 1px;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 45px;
  line-height: 45px;
  font-size: 13.5px;
  color: #b9c7e6;
  margin: 3px 0;
  border-radius: 9px;
  transition: 0.18s;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  color: #fff;
  font-weight: 600;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.42);
}

.side-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff;
  font-weight: 600;
}

.side-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.side-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: 0;
  padding-left: 42px !important;
}

.menu-badge {
  margin-left: auto;
}

.menu-badge :deep(.el-badge__content) {
  background-color: #ef4444;
  border: none;
}

.side-foot {
  position: relative;
  margin: 14px 18px 18px;
  padding: 8px 10px;
  font-size: 11px;
  color: #93a5d1;
  text-align: center;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  flex-shrink: 0;
}

/* ========== 顶栏 ========== */
.app-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  height: 64px;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.header-left :deep(.el-breadcrumb__inner) {
  font-size: 13.5px;
  color: var(--text-2);
  font-weight: 500;
}

.header-left :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-1);
  font-weight: 600;
}

.header-left :deep(.el-breadcrumb__separator) {
  color: var(--text-3);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.role-switch {
  width: 132px;
}

.role-switch :deep(.el-select__wrapper) {
  background: #f7faff;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  transition: 0.18s;
}

.user-chip:hover {
  background: #f4f8ff;
  border-color: var(--line);
}

.avatar {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.uinfo {
  line-height: 1.2;
}

.uname {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
}

.urole {
  font-size: 11px;
  color: var(--text-3);
}

.logout-btn {
  margin-left: 4px;
  font-size: 12.5px;
  color: var(--text-2);
}

.app-main {
  flex: 1;
  padding: 18px 20px;
  min-width: 0;
}
</style>
