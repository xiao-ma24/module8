<template>
  <el-container style="height: 100%">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="26"><Tickets /></el-icon>
        <div>
          <div class="t1">物资领用管理</div>
          <div class="t2">领用申请审批出库流程</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router background-color="transparent" text-color="#b6c2e2"
               active-text-color="#fff" class="menu">
        <template v-for="m in visibleMenus" :key="m.index">
          <el-menu-item :index="m.index"><el-icon><component :is="m.icon" /></el-icon>{{ m.label }}</el-menu-item>
        </template>
        <div class="role-tip">{{ ROLE_TIP[user.roleCode] || '全功能视图' }}</div>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <el-button text class="home-btn" @click="$router.push('/')">
          <el-icon size="18" style="margin-right:6px"><HomeFilled /></el-icon>首页
        </el-button>

        <el-dropdown trigger="click" @command="onCommand">
          <div class="user-chip">
            <el-avatar :size="34" style="background:#2563eb">{{ (user.userName || '刘').slice(0, 1) }}</el-avatar>
            <div class="uinfo">
              <div class="uname">{{ user.userName }}</div>
              <div class="urole">{{ user.roleName }}</div>
            </div>
            <el-icon style="color:#94a3b8"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <div class="dd-head">
                <div class="dd-name">{{ user.userName }}（{{ user.userNo }}）</div>
                <div class="dd-sub">{{ user.deptName }} · {{ user.roleName }}</div>
              </div>
              <el-dropdown-item command="dashboard" divided><el-icon><Odometer /></el-icon>我的工作台</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main style="background:#f2f5fa; padding:16px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Odometer, Document, Checked, Box, Goods, HomeFilled, ArrowDown, SwitchButton, Tickets } from '@element-plus/icons-vue'
import api, { getUser, clearLogin } from '../api'

const router = useRouter()
const user = reactive({ userId: null, userName: '', userNo: '', roleName: '', roleCode: '', deptName: '' })

// 角色菜单矩阵（与后端 @RequireRoles 一致；ADMIN 只读）
const ALL_MENUS = [
  { index: '/', icon: Odometer, label: '工作台' },
  { index: '/orders', icon: Document, label: '领用申请' },
  { index: '/approve', icon: Checked, label: '待我审批' },
  { index: '/outbounds', icon: Box, label: '出库办理' },
  { index: '/stocks', icon: Goods, label: '库存查询' },
]
const ROLE_MENUS = {
  APPLICANT: ['/', '/orders', '/stocks'],
  STUDENT: ['/', '/orders'],
  DEPT_HEAD: ['/', '/orders', '/stocks'],
  STORE_KEEPER: ['/', '/orders', '/outbounds', '/stocks'],
  ADMIN: ['/', '/orders', '/outbounds', '/stocks'],
}
const ROLE_TIP = {
  APPLICANT: '申请人 · 提交领用与签收',
  STUDENT: '学生 · 提交领用申请',
  DEPT_HEAD: '部门负责人 · 提交领用申请',
  STORE_KEEPER: '库管员 · 拣货出库',
  ADMIN: '管理员 · 只读全量数据',
}
const visibleMenus = computed(() => {
  const allow = ROLE_MENUS[user.roleCode]
  return allow ? ALL_MENUS.filter(m => allow.includes(m.index)) : ALL_MENUS
})

onMounted(() => {
  const u = getUser()
  if (u) Object.assign(user, u)
  try {
    const allow = ROLE_MENUS[user.roleCode]
    if (allow && !allow.includes(router.currentRoute.value.path)) router.replace(allow[0])
  } catch { /* 忽略 */ }
})

function onCommand(cmd) {
  if (cmd === 'dashboard') router.push('/')
  else if (cmd === 'logout') {
    ElMessageBox.confirm('退出后需重新登录，确认退出？', '退出登录', { type: 'warning' })
      .then(async () => {
        try { await api.logout() } catch { /* 忽略 */ }
        clearLogin()
        router.replace('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.sidebar { background: var(--sidebar-bg); }
.logo { display: flex; gap: 10px; align-items: center; padding: 18px 16px; color: #fff; }
.logo .t1 { font-size: 17px; font-weight: 700; }
.logo .t2 { font-size: 11px; color: #8ea2cf; margin-top: 2px; }
.menu { border: none; padding: 8px; }
.role-tip { margin: 14px 12px 4px; padding: 8px 10px; font-size: 11px; color: #93a5d1;
  background: rgba(255,255,255,.06); border-radius: 6px; text-align: center; }
.menu .el-menu-item { border-radius: 8px; margin: 2px 0; height: 46px; }
.menu .el-menu-item.is-active { background: var(--primary) !important; }
.topbar { display: flex; align-items: center; justify-content: space-between; background: #fff;
          box-shadow: 0 1px 4px rgba(15,30,61,.06); height: 64px; }
.home-btn { font-size: 15px; font-weight: 600; color: #1f2937; }
.user-chip { display: flex; align-items: center; gap: 9px; cursor: pointer; padding: 5px 10px; border-radius: 10px; }
.user-chip:hover { background: #f4f7fd; }
.uinfo .uname { font-size: 13px; font-weight: 600; color: #1f2937; line-height: 1.2; }
.uinfo .urole { font-size: 11px; color: #94a3b8; }
.dd-head { padding: 8px 16px 6px; }
.dd-name { font-size: 13px; font-weight: 600; color: #1f2937; }
.dd-sub { font-size: 11px; color: #94a3b8; }
</style>
