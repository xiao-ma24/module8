import { createHttp } from '../../shared/http'

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

// 统一请求封装：X-Token 注入、401 跳登录、403 提示见 src/shared/http.js
const http = createHttp({ baseURL: '/api/v1', tokenKey: TOKEN_KEY, userKey: USER_KEY })

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
