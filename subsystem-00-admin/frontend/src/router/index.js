import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: () => import('../layout/Layout.vue'),
    redirect: '/overview',
    children: [
      { path: 'overview', name: 'overview', component: () => import('../views/Overview.vue'), meta: { title: '数据总览' } },
      { path: 'users', name: 'users', component: () => import('../views/Users.vue'), meta: { title: '用户管理' } },
      { path: 'roles', name: 'roles', component: () => import('../views/Roles.vue'), meta: { title: '角色权限' } },
      { path: 'depts', name: 'depts', component: () => import('../views/Depts.vue'), meta: { title: '部门管理' } },
      { path: 'dept-quotas', name: 'deptQuotas', component: () => import('../views/DeptQuotas.vue'), meta: { title: '部门月度额度' } },
      { path: 'dicts', name: 'dicts', component: () => import('../views/Dicts.vue'), meta: { title: '数据字典' } },
      { path: 'configs', name: 'configs', component: () => import('../views/Configs.vue'), meta: { title: '系统参数' } },
      { path: 'logs', name: 'logs', component: () => import('../views/Logs.vue'), meta: { title: '操作日志' } },
      { path: 'login-logs', name: 'loginLogs', component: () => import('../views/LoginLogs.vue'), meta: { title: '登录日志' } },
      { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/overview' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach(to => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '平台管理端'
  if (to.meta.public) return true
  if (!localStorage.getItem('adminToken')) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
