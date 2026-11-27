<template>
  <el-container style="height: 100%">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="26"><Box /></el-icon>
        <div>
          <div class="t1">物资管理大系统</div>
          <div class="t2">采购 · 领用 · 借用 · 盘点 一体化</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router background-color="transparent" text-color="#b6c2e2"
               active-text-color="#fff" class="menu">
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>工作台
        </el-menu-item>
        <template v-for="g in menuGroups" :key="g.key">
          <div class="group-title">{{ g.title }}</div>
          <el-menu-item v-for="m in g.items" :key="m.index" :index="m.index">
            <el-icon><component :is="m.icon" /></el-icon>{{ m.label }}
          </el-menu-item>
        </template>
        <div class="role-tip">{{ ROLE_TIP[user.roleCode] || '全功能视图' }}</div>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏：首页 + 当前页 + 用户 -->
      <el-header class="topbar">
        <div class="tb-left">
          <el-button text class="home-btn" @click="$router.push('/')">
            <el-icon size="17" style="margin-right:5px"><HomeFilled /></el-icon>首页
          </el-button>
          <span class="tb-sep"></span>
          <span class="tb-page">{{ currentLabel }}</span>
        </div>

        <el-dropdown trigger="click" @command="onCommand">
          <div class="user-chip">
            <el-avatar :size="34" style="background:#2563eb">{{ (user.userName || '李').slice(0, 1) }}</el-avatar>
            <div class="uinfo">
              <div class="uname">{{ user.userName || '李思滢' }}</div>
              <div class="urole">{{ user.roleName || '采购员' }}</div>
            </div>
            <el-icon style="color:#94a3b8"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <div class="dd-head">
                <div class="dd-name">{{ user.userName }}</div>
                <div class="dd-sub">{{ user.deptName }} · {{ user.roleName }}</div>
              </div>
              <el-dropdown-item command="profile" divided><el-icon><User /></el-icon>个人中心</el-dropdown-item>
              <el-dropdown-item command="todo"><el-icon><Bell /></el-icon>我的待办</el-dropdown-item>
              <el-dropdown-item command="ai"><el-icon><MagicStick /></el-icon>AI 采购助手</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main style="background:var(--bg); padding:18px 20px">
        <router-view />
      </el-main>
    </el-container>

    <!-- 右下角悬浮 AI 助手（抽屉/弹窗打开时隐藏，避免遮挡底部按钮） -->
    <div class="ai-fab" v-show="!overlayOpen" @click="ai.open = true">
      <el-icon size="22"><MagicStick /></el-icon>
      <span>AI 助手</span>
    </div>

    <el-drawer v-model="ai.open" size="520px" :with-header="false">
      <div class="ai-panel">
        <div class="ai-head">
          <el-icon color="#2563eb" size="20"><MagicStick /></el-icon>
          <b>AI 采购助手</b>
          <span class="sub">{{ aiMode === 'LLM' ? '大模型驱动' : '规则引擎·配置 Key 后接入大模型' }}</span>
        </div>
        <div class="ai-msgs" ref="msgs">
          <div class="msg ai">您好！我是采购助手，可以查库存、给补货建议、查订单进度。试试下面的快捷提问👇</div>
          <div v-for="(m, i) in ai.msgs" :key="i" class="msg" :class="m.role">{{ m.text }}</div>
        </div>
        <div class="ai-quick">
          <el-button size="small" round @click="ask('A4复印纸还有多少库存')">查A4纸库存</el-button>
          <el-button size="small" round @click="ask('给出补货建议')">补货建议</el-button>
          <el-button size="small" round @click="ask('最近订单情况')">订单情况</el-button>
          <el-button size="small" round @click="ask('预算余额还有多少')">预算余额</el-button>
        </div>
        <div class="ai-input">
          <el-input v-model="ai.q" placeholder="输入问题，回车发送" @keyup.enter="send" :disabled="ai.loading">
            <template #append>
              <el-button :loading="ai.loading" @click="send">发送</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { useRole } from '../modules/base/composables/useRole'
import { post } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Odometer, Document, ShoppingCart, Checked, Box, Goods,
  OfficeBuilding, House, Money, DataAnalysis,
  HomeFilled, User, Bell, MagicStick, ArrowDown, SwitchButton,
  Tickets, RefreshLeft, Warning, Files,
} from '@element-plus/icons-vue'
import reqApi from '../modules/req/api'
import borrowApi from '../modules/borrow/api'

const router = useRouter()
const { setRole } = useRole()
const aiMode = ref('规则引擎')
const ai = reactive({ open: false, q: '', loading: false, msgs: [] })
const user = reactive({ userName: '', roleName: '', roleCode: '', deptName: '', userId: 2 })
const msgs = ref(null)
// 任意 Element Plus 抽屉/弹窗打开时隐藏右下角悬浮按钮，避免与底部操作按钮重叠
const overlayOpen = ref(false)
let overlayObserver = null

// 角色权限矩阵（对齐开发文档/数据字典）：
// PURCHASER=采购任务+到货登记+供应商+补货建议；CHECKER=到货验收+异常登记；
// STORE_KEEPER=入库+库存台账+库存查询；ADMIN=全功能
const ALL_MENUS = [
  { index: '/', icon: Odometer, label: '工作台' },
  { index: '/requests', icon: Document, label: '采购申请' },
  { index: '/orders', icon: ShoppingCart, label: '采购订单' },
  { index: '/acceptances', icon: Checked, label: '到货验收' },
  { index: '/inbounds', icon: Box, label: '入库管理' },
  { index: '/suppliers', icon: OfficeBuilding, label: '供应商管理' },
  { index: '/warehouses', icon: House, label: '仓库管理' },
  { index: '/recon', icon: Money, label: '财务核对' },
  { index: '/report', icon: DataAnalysis, label: '统计分析' },
]
const ROLE_MENUS = {
  APPLICANT: ['/', '/requests'],
  DEPT_HEAD: ['/', '/requests'],
  PURCHASER: ['/', '/requests', '/orders', '/acceptances', '/suppliers', '/report'],
  CHECKER: ['/', '/acceptances'],
  STORE_KEEPER: ['/', '/inbounds', '/warehouses'],
  STOCKTAKER: ['/'],
  STUDENT: ['/', '/requests'],
  TEACHER: ['/', '/requests'],
}
const ROLE_TIP = {
  APPLICANT: '申请人·提交采购申请',
  DEPT_HEAD: '部门负责人·审批采购申请',
  PURCHASER: '采购员·采购与到货登记',
  CHECKER: '验收人·质量数量核验',
  STOCKTAKER: '盘点员·盘点核实与库存预警',
  STORE_KEEPER: '库管员·出入库与台账',
  STUDENT: '学生·提交采购申请',
  TEACHER: '教职工·提交采购申请',
}
const visibleMenus = computed(() => {
  const allow = ROLE_MENUS[user.roleCode]
  return allow ? ALL_MENUS.filter(m => allow.includes(m.index)) : ALL_MENUS
})


const REQ_MENUS = [
  { index: '/req/orders', icon: Document, label: '领用申请', roles: ['APPLICANT', 'STUDENT', 'DEPT_HEAD', 'STORE_KEEPER', 'ADMIN'] },
  { index: '/req/approve', icon: Checked, label: '领用审批', roles: ['STORE_KEEPER'] },
  { index: '/req/outbounds', icon: Box, label: '领用出库', roles: ['STORE_KEEPER'] },
  { index: '/req/stocks', icon: Goods, label: '领用库存', roles: ['APPLICANT', 'STUDENT', 'DEPT_HEAD', 'STORE_KEEPER', 'ADMIN'] },
]
const BORROW_MENUS = [
  { index: '/borrow/orders', icon: Tickets, label: '借用申请', roles: ['APPLICANT', 'STUDENT', 'DEPT_HEAD', 'STORE_KEEPER', 'ADMIN'] },
  { index: '/borrow/approve', icon: Checked, label: '借用审批', roles: ['STORE_KEEPER'] },
  { index: '/borrow/lends', icon: Box, label: '借出登记', roles: ['STORE_KEEPER'] },
  { index: '/borrow/returns', icon: RefreshLeft, label: '归还与逾期', roles: ['APPLICANT', 'STUDENT', 'DEPT_HEAD', 'STORE_KEEPER', 'ADMIN'] },
  { index: '/borrow/losses', icon: Warning, label: '报损处理', roles: ['STORE_KEEPER'] },
  { index: '/borrow/stocks', icon: Goods, label: '借用库存', roles: ['APPLICANT', 'STUDENT', 'DEPT_HEAD', 'STORE_KEEPER', 'ADMIN'] },
]
const ST_MENUS = [
  { index: '/stocktake/warning', icon: Warning, label: '库存预警', roles: ['STORE_KEEPER', 'ADMIN'] },
  { index: '/stocktake/orders', icon: Files, label: '盘点任务', roles: ['STOCKTAKER', 'ADMIN'] },
  { index: '/stocktake/replenish', icon: ShoppingCart, label: '补货计划', roles: ['STOCKTAKER', 'ADMIN'] },
]
const BASE_MENUS = [
  { index: '/materials', icon: Goods, label: '物资目录', roles: ['ADMIN', 'PURCHASER', 'CHECKER', 'STORE_KEEPER', 'STOCKTAKER'] },
  { index: '/base/purchase', icon: Tickets, label: '个人发票登记', roles: ['ADMIN', 'APPLICANT', 'TEACHER', 'DEPT_HEAD'] },
  { index: '/base/approval', icon: Checked, label: '发票审核', roles: ['ADMIN', 'STORE_KEEPER'] },
]
const menuGroups = computed(() => {
  const role = user.roleCode
  const allow = ROLE_MENUS[role]
  const purchase = (allow ? ALL_MENUS.filter(m => allow.includes(m.index)) : ALL_MENUS).filter(m => m.index !== '/')
  const lend = [...REQ_MENUS, ...BORROW_MENUS].filter(m => !role || m.roles.includes(role))
  return [
    { key: 'purchase', title: '采购管理', items: purchase },
    { key: 'lend', title: '领用与借用', items: lend },
    { key: 'stocktake', title: '盘点预警', items: ST_MENUS.filter(m => !role || m.roles.includes(role)) },
    { key: 'base', title: '基础数据与审批', items: BASE_MENUS.filter(m => !role || m.roles.includes(role)) },
  ].filter(g => g.items.length)
})

const currentLabel = computed(() => {
  const p = router.currentRoute.value.path
  if (p === '/profile') return '个人中心'
  const all = [...ALL_MENUS, ...REQ_MENUS, ...BORROW_MENUS, ...ST_MENUS, ...BASE_MENUS]
  const m = all.find(x => x.index === p)
  return m ? m.label : '工作台'
})

onMounted(async () => {
  try {
    const p = await api.profile(Number(localStorage.getItem('mockUserId') || 2))
    Object.assign(user, p.info)
    // 供 ① 基础数据页面识别当前人员与演示视角
    localStorage.setItem('demo_user_no', p.info.userNo || '')
    setRole(['APPLICANT', 'TEACHER'].includes(p.info.roleCode) ? 'teacher' : 'admin')
    // 已有登录态但缺少领用/借用令牌时自动换发，保证一次登录即可访问全部业务
    const uid = Number(localStorage.getItem('mockUserId') || user.userId || 2)
    if (!localStorage.getItem('req_token')) {
      try {
        const r = await post('/requisition/auth/handoff', { userId: uid })
        localStorage.setItem('req_token', r.token)
        localStorage.setItem('req_user', JSON.stringify(r.user))
      } catch {}
    }
    if (!localStorage.getItem('borrow_token')) {
      try {
        const b = await post('/borrow/auth/handoff', { userId: uid })
        localStorage.setItem('borrow_token', b.token)
        localStorage.setItem('borrow_user', JSON.stringify(b.user))
      } catch {}
    }
    // 仅当当前页属于采购菜单且角色无权时才回退，避免把领用/借用页面误跳走
    const allow = ROLE_MENUS[user.roleCode]
    const cur = router.currentRoute.value.path
    const purchasePaths = ALL_MENUS.map(m => m.index)
    if (allow && purchasePaths.includes(cur) && !allow.includes(cur)) {
      router.replace(allow[0])
    }
  } catch {}
  try { const s = await api.aiStatus(); aiMode.value = s.llmEnabled ? 'LLM' : '规则引擎' } catch {}
  const syncOverlay = () => { overlayOpen.value = !!document.querySelector('.el-overlay') }
  overlayObserver = new MutationObserver(syncOverlay)
  overlayObserver.observe(document.body, { childList: true, subtree: true })
  syncOverlay()
})

onUnmounted(() => { if (overlayObserver) overlayObserver.disconnect() })

function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'todo') router.push('/profile?tab=todo')
  else if (cmd === 'ai') ai.open = true
  else if (cmd === 'logout') {
    ElMessageBox.confirm('退出后需重新登录，确认退出？', '退出登录', { type: 'warning' })
      .then(async () => {
        try { await reqApi.logout() } catch {}
        try { await borrowApi.logout() } catch {}
        localStorage.removeItem('mockUserId')
        localStorage.removeItem('req_token')
        localStorage.removeItem('req_user')
        localStorage.removeItem('borrow_token')
        localStorage.removeItem('borrow_user')
        router.replace('/login')
      })
      .catch(() => {})
  }
}

async function ask(q) { ai.q = q; send() }

async function send() {
  const q = ai.q.trim()
  if (!q || ai.loading) return
  ai.msgs.push({ role: 'me', text: q })
  ai.q = ''
  ai.loading = true
  scroll()
  try {
    const r = await api.aiChat(q)
    ai.msgs.push({ role: 'ai', text: r.answer })
    aiMode.value = r.mode === 'LLM' ? 'LLM' : '规则引擎'
  } catch {
    ai.msgs.push({ role: 'ai', text: '出错了，请稍后再试' })
  }
  ai.loading = false
  scroll()
}
function scroll() { nextTick(() => { if (msgs.value) msgs.value.scrollTop = msgs.value.scrollHeight }) }
</script>

<style scoped>
.sidebar { background: var(--sidebar-bg); position: relative; overflow: hidden; display: flex; flex-direction: column; }
.sidebar::before {
  content: ''; position: absolute; top: -90px; left: -70px; width: 240px; height: 240px;
  border-radius: 50%; pointer-events: none;
  background: radial-gradient(circle, rgba(96,165,250,.28), transparent 70%);
}
.logo { display: flex; gap: 11px; align-items: center; padding: 20px 18px; color: #fff; position: relative; }
.logo > .el-icon {
  width: 38px; height: 38px; border-radius: 11px; flex: none;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  box-shadow: 0 6px 16px rgba(37,99,235,.45);
}
.logo .t1 { font-size: 16px; font-weight: 700; letter-spacing: .3px; }
.logo .t2 { font-size: 10.5px; color: #8ea2cf; margin-top: 3px; }
.menu { border: none; padding: 6px 10px; position: relative; flex: 1; overflow-y: auto; overflow-x: hidden; }
.menu::-webkit-scrollbar { width: 4px; }
.menu::-webkit-scrollbar-thumb { background: rgba(255,255,255,.16); border-radius: 4px; }
.menu::-webkit-scrollbar-track { background: transparent; }
.group-title { margin: 12px 10px 4px; font-size: 11px; letter-spacing: 1px; color: #7d90bd; }
.role-tip { margin: 14px 8px 6px; padding: 8px 10px; font-size: 11px; color: #93a5d1;
  background: rgba(255,255,255,.06); border: 1px solid rgba(255,255,255,.06);
  border-radius: 8px; text-align: center; }
.menu .el-menu-item { border-radius: 9px; margin: 3px 0; height: 45px; font-size: 13.5px;
  color: #b9c7e6; transition: .18s; }
.menu .el-menu-item:hover { background: rgba(255,255,255,.08) !important; color: #fff; }
.menu .el-menu-item.is-active {
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  color: #fff; box-shadow: 0 6px 16px rgba(37,99,235,.42);
}
.topbar { display: flex; align-items: center; justify-content: space-between; height: 64px;
  padding: 0 20px; background: rgba(255,255,255,.88); backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line); position: sticky; top: 0; z-index: 100; }
.tb-left { display: flex; align-items: center; gap: 12px; }
.tb-sep { width: 1px; height: 18px; background: var(--line); }
.tb-page { font-size: 14px; font-weight: 600; color: var(--text-1); letter-spacing: .2px; }
.home-btn { font-size: 14.5px; font-weight: 600; color: #334155; border-radius: 8px; }
.home-btn:hover { background: #f1f6ff; color: var(--primary); }
.user-chip { display: flex; align-items: center; gap: 9px; cursor: pointer; padding: 5px 10px;
  border: 1px solid transparent; border-radius: 10px; transition: .18s; }
.user-chip:hover { background: #f4f8ff; border-color: var(--line); }
.uinfo .uname { font-size: 13px; font-weight: 600; color: var(--text-1); line-height: 1.2; }
.uinfo .urole { font-size: 11px; color: var(--text-3); }
.dd-head { padding: 8px 16px 6px; }
.dd-name { font-size: 13px; font-weight: 600; color: var(--text-1); }
.dd-sub { font-size: 11px; color: var(--text-3); }
.ai-fab { position: fixed; right: 26px; bottom: 26px; z-index: 2000; display: flex; align-items: center; gap: 6px;
  background: linear-gradient(135deg, #2563eb, #7c3aed); color: #fff; padding: 12px 18px;
  border-radius: 26px; cursor: pointer; box-shadow: 0 8px 22px rgba(37,99,235,.42); font-size: 13px;
  transition: transform .18s; }
.ai-fab:hover { transform: translateY(-2px) scale(1.03); }
.ai-panel { display: flex; flex-direction: column; height: 100%; }
.ai-head { display: flex; align-items: center; gap: 8px; padding-bottom: 12px; border-bottom: 1px solid var(--line); }
.ai-head .sub { font-size: 11px; color: var(--text-3); margin-left: auto; }
.ai-msgs { flex: 1; overflow-y: auto; padding: 12px 2px; }
.msg { max-width: 86%; padding: 10px 12px; border-radius: 12px; margin-bottom: 10px; font-size: 13px;
  line-height: 1.7; white-space: pre-wrap; }
.msg.ai { background: var(--primary-soft); color: #1e3a8a; border-bottom-left-radius: 3px; }
.msg.me { background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; margin-left: auto; border-bottom-right-radius: 3px; }
.ai-quick { display: flex; flex-wrap: wrap; gap: 6px; padding: 8px 0; }
.ai-quick .el-button { border-color: #bcd3ff; color: var(--primary); }
</style>
