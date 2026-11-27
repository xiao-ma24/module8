import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'stocktake', name: 'stocktake', component: () => import('../views/Stocktake.vue'), meta: { title: '盘点任务' } },
      { path: 'warning', name: 'warning', component: () => import('../views/Warning.vue'), meta: { title: '库存预警' } },
      { path: 'replenish', name: 'replenish', component: () => import('../views/Replenish.vue'), meta: { title: '补货计划' } },
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.afterEach(to => {
  document.title = `${to.meta.title || '盘点补货'} · 学校物资管理系统`
})

export default router
