import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api/v1', timeout: 20000 })

http.interceptors.request.use(cfg => {
  const token = localStorage.getItem('adminToken')
  if (token) cfg.headers['X-Token'] = token
  return cfg
})

http.interceptors.response.use(
  r => {
    const body = r.data
    if (body && body.code !== 0) {
      ElMessage.error(body.msg || '操作失败')
      return Promise.reject(body)
    }
    return body.data
  },
  e => {
    const body = e.response?.data
    if (body?.code === 401 || e.response?.status === 401) {
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUser')
      if (location.hash !== '#/login') location.href = '#/login'
      ElMessage.warning('登录已失效，请重新登录')
      return Promise.reject(e)
    }
    ElMessage.error(body?.msg || e.message || '网络异常')
    return Promise.reject(e)
  }
)

export const get = (url, params) => http.get(url, { params })
export const post = (url, data) => http.post(url, data)
export const put = (url, data) => http.put(url, data)
export const del = url => http.delete(url)

export const api = {
  // 认证
  login: d => post('/auth/login', d),
  me: () => get('/auth/me'),
  logout: () => post('/auth/logout'),
  changePwd: d => post('/auth/change-pwd', d),

  // 数据总览
  overview: () => get('/admin/overview'),

  // 用户
  users: q => get('/admin/users', q),
  createUser: d => post('/admin/users', d),
  updateUser: d => put('/admin/users', d),
  deleteUser: id => del(`/admin/users/${id}`),
  setUserStatus: (id, status, reason) =>
    post(`/admin/users/${id}/status?status=${status}${reason ? '&reason=' + encodeURIComponent(reason) : ''}`),
  activateUser: (id, reason) =>
    post(`/admin/users/${id}/activate${reason ? '?reason=' + encodeURIComponent(reason) : ''}`),
  disableUser: (id, reason) =>
    post(`/admin/users/${id}/disable${reason ? '?reason=' + encodeURIComponent(reason) : ''}`),
  checkUser: q => get('/admin/users/check', q),
  teacherOptions: q => get('/admin/users/teacher-options', q),
  syncUsers: () => post('/admin/users/sync'),
  resetPwd: id => post(`/admin/users/${id}/reset-pwd`),
  importUsers: rows => post('/admin/users/import', rows),

  // 角色与菜单
  roles: q => get('/admin/roles', q),
  createRole: d => post('/admin/roles', d),
  updateRole: d => put('/admin/roles', d),
  deleteRole: id => del(`/admin/roles/${id}`),
  grantMenus: (id, ids) => post(`/admin/roles/${id}/menus`, ids),
  menuTree: () => get('/admin/roles/menus'),

  // 部门
  depts: q => get('/admin/depts', q),
  createDept: d => post('/admin/depts', d),
  updateDept: d => put('/admin/depts', d),
  deleteDept: id => del(`/admin/depts/${id}`),

  // 部门月度额度（只读：来源财务预算表 budget）
  deptQuotas: q => get('/admin/dept-quotas', q),

  // 字典
  dictTypes: q => get('/admin/dicts/types', q),
  saveDictType: d => post('/admin/dicts/types', d),
  deleteDictType: t => del(`/admin/dicts/types/${t}`),
  dictItems: q => get('/admin/dicts/items', q),
  saveDictItem: d => post('/admin/dicts/items', d),
  deleteDictItem: id => del(`/admin/dicts/items/${id}`),
  allDicts: () => get('/admin/dicts/all'),

  // 参数
  configs: q => get('/admin/configs', q),
  createConfig: d => post('/admin/configs', d),
  updateConfig: d => put('/admin/configs', d),
  updateConfigValue: (id, value) => put(`/admin/configs/${id}/value`, { value }),
  deleteConfig: id => del(`/admin/configs/${id}`),

  // 日志
  auditLogs: q => get('/admin/logs', q),
  loginLogs: q => get('/admin/login-logs', q),
  logStats: () => get('/admin/log-stats'),

  // 个人中心
  profile: userId => get('/admin/profile', { userId }),

  // AI 运维助手
  aiChat: question => post('/admin/ai/chat', { question }),
  aiStatus: () => get('/admin/ai/status')
}

export default api
