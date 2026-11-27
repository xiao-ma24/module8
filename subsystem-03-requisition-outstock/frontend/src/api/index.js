import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'req_token'
const USER_KEY = 'req_user'

export const getToken = () => localStorage.getItem(TOKEN_KEY) || ''
export const getUser = () => {
  try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null') } catch { return null }
}
export const setLogin = (token, user) => {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}
export const clearLogin = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/** 当前角色是否在允许列表内（ADMIN 不自动放行，与后端口径一致） */
export const hasRole = (...roles) => {
  const u = getUser()
  return !!u && roles.includes(u.roleCode)
}

const http = axios.create({ baseURL: '/api/v1', timeout: 20000 })
http.interceptors.request.use(cfg => {
  const t = getToken()
  if (t) cfg.headers['X-Token'] = t
  return cfg
})
http.interceptors.response.use(
  r => {
    const body = r.data
    if (body && body.code === 401) {
      clearLogin()
      if (location.hash.indexOf('/login') < 0) location.hash = '#/login'
      ElMessage.error('登录已失效，请重新登录')
      return Promise.reject(body)
    }
    if (body && body.code !== 0) {
      ElMessage.error(body.msg || '操作失败')
      return Promise.reject(body)
    }
    return body.data
  },
  e => {
    ElMessage.error(e.response?.data?.msg || e.message || '网络异常')
    return Promise.reject(e)
  }
)

const get = (url, params) => http.get(url, { params })
const post = (url, data, params) => http.post(url, data, { params })

export const api = {
  login: (userNo, password) => post('/requisition/auth/login', { userNo, password }),
  logout: () => post('/requisition/auth/logout'),

  dashboard: () => get('/requisition/dashboard'),
  stats: () => get('/requisition/stats'),

  materials: keyword => get('/requisition/meta/materials', { keyword }),
  warehouses: () => get('/requisition/meta/warehouses'),
  stocks: keyword => get('/requisition/meta/stocks', { keyword }),
  batches: (materialId, warehouseId) => get('/requisition/meta/batches', { materialId, warehouseId }),

  orders: (status, keyword) => get('/requisition/orders', { status, keyword }),
  pendingApprove: () => get('/requisition/orders/pending-approve'),
  order: id => get(`/requisition/orders/${id}`),
  orderLogs: id => get(`/requisition/orders/${id}/logs`),
  createOrder: d => post('/requisition/orders', d),
  submitOrder: id => post(`/requisition/orders/${id}/submit`),
  approveOrder: (id, d) => post(`/requisition/orders/${id}/approve`, d),
  withdrawOrder: id => post(`/requisition/orders/${id}/withdraw`),
  receiveOrder: id => post(`/requisition/orders/${id}/receive`),

  outbounds: keyword => get('/requisition/outbounds', { keyword }),
  pendingOutbound: keyword => get('/requisition/outbounds/pending', { keyword }),
  confirmOutbound: d => post('/requisition/outbounds/confirm', d)
}

export default api
