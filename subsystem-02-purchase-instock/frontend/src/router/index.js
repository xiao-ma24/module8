import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录', public: true } },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'requests', name: 'requests', component: () => import('../views/PurchaseRequest.vue'), meta: { title: '采购申请' } },
      { path: 'orders', name: 'orders', component: () => import('../views/PurchaseOrder.vue'), meta: { title: '采购订单' } },
      { path: 'acceptances', name: 'acceptances', component: () => import('../views/Acceptance.vue'), meta: { title: '到货验收' } },
      { path: 'inbounds', name: 'inbounds', component: () => import('../views/Inbound.vue'), meta: { title: '入库管理' } },
      { path: 'materials', name: 'materials', component: () => import('../views/Material.vue'), meta: { title: '物资档案' } },
      { path: 'suppliers', name: 'suppliers', component: () => import('../views/Supplier.vue'), meta: { title: '供应商管理' } },
      { path: 'warehouses', name: 'warehouses', component: () => import('../views/Warehouse.vue'), meta: { title: '仓库管理' } },
      { path: 'recon', name: 'recon', component: () => import('../views/Recon.vue'), meta: { title: '财务核对' } },
      { path: 'report', name: 'report', component: () => import('../views/Report.vue'), meta: { title: '统计分析' } },
      { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },

      // ---- ③ 领用出库（并入同一系统） ----
      { path: 'req/orders', name: 'req-orders', component: () => import('../modules/req/views/Requisitions.vue'), meta: { title: '领用申请' } },
      { path: 'req/approve', name: 'req-approve', component: () => import('../modules/req/views/Approvals.vue'), meta: { title: '领用审批' } },
      { path: 'req/outbounds', name: 'req-outbounds', component: () => import('../modules/req/views/Outbounds.vue'), meta: { title: '领用出库' } },
      { path: 'req/stocks', name: 'req-stocks', component: () => import('../modules/req/views/Stocks.vue'), meta: { title: '领用库存查询' } },

      // ---- ④ 借用归还报损（并入同一系统） ----
      { path: 'borrow/orders', name: 'borrow-orders', component: () => import('../modules/borrow/views/Borrows.vue'), meta: { title: '借用申请' } },
      { path: 'borrow/approve', name: 'borrow-approve', component: () => import('../modules/borrow/views/Approvals.vue'), meta: { title: '借用审批' } },
      { path: 'borrow/lends', name: 'borrow-lends', component: () => import('../modules/borrow/views/Lends.vue'), meta: { title: '借出登记' } },
      { path: 'borrow/returns', name: 'borrow-returns', component: () => import('../modules/borrow/views/Returns.vue'), meta: { title: '归还与逾期' } },
      { path: 'borrow/losses', name: 'borrow-losses', component: () => import('../modules/borrow/views/Losses.vue'), meta: { title: '报损处理' } },
      { path: 'borrow/stocks', name: 'borrow-stocks', component: () => import('../modules/borrow/views/Stocks.vue'), meta: { title: '借用库存查询' } },

      // ---- ⑤ 盘点预警与补货（并入同一系统） ----
      { path: 'stocktake/warning', name: 'stocktake-warning', component: () => import('../modules/stocktake/views/Warning.vue'), meta: { title: '库存预警' } },
      { path: 'stocktake/orders', name: 'stocktake-orders', component: () => import('../modules/stocktake/views/Stocktake.vue'), meta: { title: '盘点任务' } },
      { path: 'stocktake/replenish', name: 'stocktake-replenish', component: () => import('../modules/stocktake/views/Replenish.vue'), meta: { title: '补货计划' } },
      { path: 'stocktake/overview', name: 'stocktake-overview', component: () => import('../modules/stocktake/views/Dashboard.vue'), meta: { title: '盘点工作台' } },

      // ---- ① 基础数据与审批（并入同一系统） ----
      // 物资档案与物资目录已合并：统一由物资档案页（/materials）承载，旧地址透传交接身份
      { path: 'base/material', redirect: to => ({ path: '/materials', query: to.query }) },
      { path: 'base/purchase', name: 'base-purchase', component: () => import('../modules/base/views/purchase/index.vue'), meta: { title: '个人发票登记' } },
      { path: 'base/approval', name: 'base-approval', component: () => import('../modules/base/views/approval/index.vue'), meta: { title: '发票审核' } },
      { path: 'base/user', name: 'base-user', component: () => import('../modules/base/views/user/index.vue'), meta: { title: '用户与权限' } },
      { path: 'base/log', name: 'base-log', component: () => import('../modules/base/views/log/index.vue'), meta: { title: '操作日志' } },
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async to => {
  if (to.meta.public) return true
  // 平台端（5180）卡片跳转时携带 uid，等价于统一登录后的身份交接
  if (to.query.uid) localStorage.setItem('mockUserId', String(to.query.uid))

  // 统一登录：每次进入都以服务端身份为准，覆盖本地旧缓存，避免切换账号后仍显示上一个人
  try {
    const res = await fetch('/api/v1/profile', { credentials: 'same-origin' })
    if (res.ok) {
      const body = await res.json()
      const info = body?.data?.info
      if (info && info.userId != null) {
        localStorage.setItem('mockUserId', String(info.userId))
        localStorage.setItem('req_token', String(info.userId))
        localStorage.setItem('borrow_token', String(info.userId))
      }
    } else if (res.status === 401 || res.status === 403) {
      localStorage.removeItem('mockUserId')
      return { path: '/login' }
    }
  } catch {
    // 认证服务不可用时保留本地状态，由接口层给出提示
  }

  if (!localStorage.getItem('mockUserId')) return { path: '/login' }
  if (to.path.startsWith('/req/') && !localStorage.getItem('req_token')) localStorage.setItem('req_token', localStorage.getItem('mockUserId'))
  if (to.path.startsWith('/borrow/') && !localStorage.getItem('borrow_token')) localStorage.setItem('borrow_token', localStorage.getItem('mockUserId'))
  return true
})

export default router
