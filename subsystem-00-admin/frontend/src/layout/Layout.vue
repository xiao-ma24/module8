<template>
  <el-container class="app">
    <el-aside :width="collapse ? '64px' : '224px'" class="aside">
      <div class="brand" :class="{ mini: collapse }">
        <el-icon :size="22"><School /></el-icon>
        <div v-show="!collapse" class="brand-txt">
          <div class="t1">平台管理端</div>
          <div class="t2">组织 · 权限 · 审计</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" :collapse="collapse" router class="menu" background-color="transparent"
               text-color="#b9c7e6" active-text-color="#ffffff" :collapse-transition="false">
        <template v-for="m in menus" :key="m.menuId">
          <el-sub-menu v-if="m.children && m.children.length" :index="'g' + m.menuId">
            <template #title>
              <el-icon><component :is="iconOf(m.icon)" /></el-icon>
              <span>{{ m.menuName }}</span>
            </template>
            <el-menu-item v-for="c in m.children" :key="c.menuId" :index="c.path">
              <el-icon><component :is="iconOf(c.icon)" /></el-icon>
              <span>{{ c.menuName }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="m.path">
            <el-icon><component :is="iconOf(m.icon)" /></el-icon>
            <span>{{ m.menuName }}</span>
          </el-menu-item>
        </template>
      </el-menu>
      <div class="env" v-show="!collapse">
        <el-tag size="small" type="success" effect="dark">v1.0.0</el-tag>
        <span>接口 8080</span>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="left">
          <el-button text class="icon-btn" :icon="collapse ? Expand : Fold" @click="collapse = !collapse" />
          <el-button text class="home-btn" :icon="HomeFilled" @click="$router.push('/overview')">首页</el-button>
          <span class="tb-sep"></span>
          <span class="crumb">{{ currentTitle }}</span>
        </div>
        <div class="right">
          <el-tooltip content="平台共 "><el-tag type="info" effect="plain" size="small">{{ orgTip }}</el-tag></el-tooltip>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user">
              <div class="avatar">{{ (user.userName || 'A').slice(0, 1) }}</div>
              <div class="meta">
                <div class="name">{{ user.userName || '管理员' }}</div>
                <div class="role">{{ user.roleName || '平台管理员' }}</div>
              </div>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item command="todo" :icon="Bell">我的待办<el-badge v-if="todoCount" :value="todoCount" class="dot" /></el-dropdown-item>
                <el-dropdown-item command="ai" :icon="MagicStick">AI 运维助手</el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <component :is="Component" @refresh-user="loadMe" />
        </router-view>
      </el-main>
    </el-container>

    <!-- AI 运维助手 -->
    <el-button class="ai-fab" type="primary" circle :icon="MagicStick" @click="aiOpen = true" />
    <el-drawer v-model="aiOpen" title="AI 运维助手" size="420px">
      <div class="ai-box">
        <div class="ai-tip">可问我：用户与账号数量、登录失败排查、部门/角色权限、字典与参数、审计日志——回答基于平台实时数据。</div>
        <div class="chat">
          <div v-for="(m, i) in chat" :key="i" :class="['msg', m.role]">{{ m.text }}</div>
        </div>
        <div class="quick-q">
          <el-tag v-for="q in quickQuestions" :key="q" size="small" effect="plain" @click="ask(q)">{{ q }}</el-tag>
        </div>
        <div class="input">
          <el-input v-model="question" placeholder="输入问题，回车发送" @keyup.enter="ask()" />
          <el-button type="primary" @click="ask()">发送</el-button>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled, ArrowDown, Fold, Expand, User, Bell, SwitchButton, MagicStick, School
} from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const collapse = ref(false)
const menus = ref(JSON.parse(localStorage.getItem('adminMenus') || '[]'))
const user = reactive(JSON.parse(localStorage.getItem('adminUser') || '{}'))
const orgTip = ref('组织与权限中心')
const todoCount = ref(0)

// 顶栏当前页名称：优先取菜单数据里的名称，回退到路由 meta.title
const currentTitle = computed(() => {
  const path = router.currentRoute.value.path
  const find = (list = []) => {
    for (const m of list) {
      if (m.path === path) return m.menuName
      const hit = find(m.children)
      if (hit) return hit
    }
    return ''
  }
  return find(menus.value) || router.currentRoute.value.meta.title || '数据总览'
})

const aiOpen = ref(false)
const aiLoading = ref(false)
const question = ref('')
const chat = ref([{ role: 'ai', text: '你好，我是平台运维助手。可以帮你定位登录异常、解释参数含义、提示权限配置风险。' }])
const quickQuestions = ['有用户登录失败怎么办', '新增部门要注意什么', '字典和参数有什么区别', '怎样给新同事开权限']

function iconOf(name) {
  return name || 'Menu'
}

async function loadMe() {
  try {
    const d = await api.me()
    Object.assign(user, d.user)
    menus.value = d.menus
    localStorage.setItem('adminUser', JSON.stringify(d.user))
    localStorage.setItem('adminMenus', JSON.stringify(d.menus))
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadTodo() {
  try {
    const p = await api.profile(user.userId)
    todoCount.value = (p.todos || []).length
    orgTip.value = `在职 ${p.stats?.myOps ?? 0} 次操作 / ${p.todos?.length ?? 0} 项待办`
  } catch (e) { /* ignore */ }
}

function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'todo') router.push('/profile?tab=todo')
  else if (cmd === 'ai') aiOpen.value = true
  else if (cmd === 'logout') logout()
}

async function logout() {
  await ElMessageBox.confirm('确定退出平台管理端吗？', '退出登录', { type: 'warning' })
  try { await api.logout() } catch (e) { /* ignore */ }
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminUser')
  localStorage.removeItem('adminMenus')
  ElMessage.success('已退出登录')
  router.replace('/login')
}

function ask(q) {
  const text = q || question.value.trim()
  if (!text || aiLoading.value) return
  chat.value.push({ role: 'me', text })
  question.value = ''
  chat.value.push({ role: 'ai', text: '正在分析平台实时数据…' })
  const idx = chat.value.length - 1
  aiLoading.value = true
  api.aiChat(text)
    .then(res => { chat.value[idx].text = res?.answer || '暂时没有答案，请换个问法。' })
    .catch(() => { chat.value[idx].text = answer(text) })
    .finally(() => { aiLoading.value = false })
}

function answer(t) {
  if (t.includes('登录失败') || t.includes('登录异常') || t.includes('锁定')) {
    return '先看「安全审计 → 登录日志」筛选失败记录：①账号不存在→核对工号是否已建档；②密码错误且连续 5 次→账号锁定，可在用户管理里重置密码后重新登录；③账号已停用→在用户管理中启用。'
  }
  if (t.includes('部门')) {
    return '新增部门需要：部门编码唯一、指定负责人、填写月度预算额度。若部门下还有成员将无法删除，建议先做人员调岗。'
  }
  if (t.includes('字典') && t.includes('参数')) {
    return '字典用于「枚举值」——如物资分类、单据状态、付款方式，供各子系统下拉框和标签使用；参数用于「开关与阈值」——如初始密码、自动审批金额、AI 开关。改字典影响展示，改参数影响业务规则。'
  }
  if (t.includes('权限') || t.includes('开权限')) {
    return '流程：①用户管理里新建账号并选择角色；②若角色不合适，在角色权限里给该角色勾选菜单；③让用户用初始密码 123456 登录并修改。权限按菜单粒度授权，改完立即生效（重新登录后菜单刷新）。'
  }
  if (t.includes('审计') || t.includes('日志')) {
    return '操作日志记录「谁在什么模块对哪个对象做了什么」，可直接导出 Excel 交审计；登录日志记录成功/失败、IP 与设备。默认保留 180 天，可在系统参数 audit.retainDays 调整。'
  }
  if (t.includes('参数')) {
    return '系统参数是平台级开关，例如 purchase.autoApproveAmount（小额免审批上限）、sys.user.initPassword（新用户初始密码）、ai.enabled（是否对全员开放 AI 助手）。内置参数不可删除，只能改值。'
  }
  return '我可以帮你排查账号与登录问题、说明权限模型、解释字典与参数差异、提示审计合规要点。也可以直接点上面的快捷问题。'
}

onMounted(() => {
  loadMe()
  loadTodo()
})
</script>

<style scoped>
.app { height: 100vh; }
.aside {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width .2s;
  overflow: hidden;
  position: relative;
}
.aside::before {
  content: ''; position: absolute; top: -90px; left: -70px; width: 240px; height: 240px;
  border-radius: 50%; pointer-events: none;
  background: radial-gradient(circle, rgba(96, 165, 250, .28), transparent 70%);
}
.brand {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 16px;
  color: #fff;
  position: relative;
  border-bottom: 1px solid rgba(255, 255, 255, .07);
}
.brand > .el-icon {
  width: 38px; height: 38px; flex: none;
  border-radius: 11px;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  box-shadow: 0 6px 16px rgba(37, 99, 235, .45);
}
.brand.mini { justify-content: center; padding: 0; }
.brand-txt { min-width: 0; }
.brand-txt .t1 { font-size: 15px; font-weight: 700; letter-spacing: .3px; line-height: 1.25; }
.brand-txt .t2 { font-size: 10.5px; color: #8ea2cf; margin-top: 3px; }
.menu { border-right: none; flex: 1; overflow-y: auto; padding: 8px 10px; position: relative; }
.menu.el-menu--collapse { padding: 8px 6px; }
.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  border-radius: 9px; margin: 3px 0; height: 44px; font-size: 13.5px;
  transition: background .18s, color .18s;
}
.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) { background: rgba(255, 255, 255, .08) !important; color: #fff; }
.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  color: #fff; box-shadow: 0 6px 16px rgba(37, 99, 235, .42);
}
.menu :deep(.el-sub-menu .el-menu-item) { height: 40px; font-size: 13px; }
.env {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #8ea2cf;
  border-top: 1px solid rgba(255, 255, 255, .07);
  position: relative;
}
.header {
  height: 64px;
  background: rgba(255, 255, 255, .88);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header .left { display: flex; align-items: center; gap: 12px; }
.icon-btn { color: #64748b; }
.home-btn { font-size: 14.5px; font-weight: 600; color: #334155; border-radius: 8px; }
.home-btn:hover { background: #f1f6ff; color: var(--primary); }
.tb-sep { width: 1px; height: 18px; background: var(--line); }
.crumb { color: var(--text-1); font-size: 14px; font-weight: 600; letter-spacing: .2px; }
.header .right { display: flex; align-items: center; gap: 14px; }
.user {
  display: flex; align-items: center; gap: 10px; cursor: pointer;
  padding: 5px 10px; border: 1px solid transparent; border-radius: 10px; transition: .18s;
}
.user:hover { background: #f4f8ff; border-color: var(--line); }
.avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 4px 12px rgba(37, 99, 235, .28);
  color: #fff; display: grid; place-items: center; font-size: 14px;
}
.meta .name { font-size: 13px; font-weight: 600; color: var(--text-1); line-height: 1.2; }
.meta .role { font-size: 11px; color: var(--text-3); }
.arrow { color: var(--text-3); }
.dot { margin-left: 6px; }
.main { background: var(--bg); padding: 18px 20px; overflow-y: auto; }
.ai-fab { position: fixed; right: 28px; bottom: 28px; width: 52px; height: 52px; box-shadow: 0 10px 24px rgba(37, 99, 235, .4); }
.ai-box { display: flex; flex-direction: column; height: 100%; }
.ai-tip { font-size: 12px; color: #94a3b8; margin-bottom: 10px; }
.chat { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding-right: 4px; }
.msg { padding: 10px 12px; border-radius: 12px; font-size: 13px; line-height: 1.7; max-width: 92%; }
.msg.ai { background: var(--primary-soft); color: #1e3a8a; align-self: flex-start; border-bottom-left-radius: 3px; }
.msg.me {
  background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff;
  align-self: flex-end; border-bottom-right-radius: 3px;
}
.quick-q { display: flex; flex-wrap: wrap; gap: 6px; margin: 10px 0; }
.quick-q .el-tag { cursor: pointer; }
.input { display: flex; gap: 8px; }
</style>
