import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import { getToken } from '../api'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'orders', name: 'orders', component: () => import('../views/Borrows.vue'), meta: { title: '借用申请' } },
      { path: 'approve', name: 'approve', component: () => import('../views/Approvals.vue'), meta: { title: '待我审批', roles: ['DEPT_HEAD'] } },
      { path: 'lends', name: 'lends', component: () => import('../views/Lends.vue'), meta: { title: '借出登记' } },
      { path: 'returns', name: 'returns', component: () => import('../views/Returns.vue'), meta: { title: '归还与逾期' } },
      { path: 'losses', name: 'losses', component: () => import('../views/Losses.vue'), meta: { title: '报损处理' } },
      { path: 'stocks', name: 'stocks', component: () => import('../views/Stocks.vue'), meta: { title: '库存查询' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  if (to.meta.public) return true
  if (!getToken()) return { path: '/login' }
  return true
})

export default router
