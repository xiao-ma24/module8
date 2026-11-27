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
      { path: 'orders', name: 'orders', component: () => import('../views/Requisitions.vue'), meta: { title: '领用申请' } },
      { path: 'approve', name: 'approve', component: () => import('../views/Approvals.vue'), meta: { title: '待我审批', roles: ['DEPT_HEAD'] } },
      { path: 'outbounds', name: 'outbounds', component: () => import('../views/Outbounds.vue'), meta: { title: '出库办理' } },
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
