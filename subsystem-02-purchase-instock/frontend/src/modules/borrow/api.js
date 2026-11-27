import { createHttp } from '../../shared/http'

const TOKEN_KEY = 'borrow_token'
const USER_KEY = 'borrow_user'

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

// 统一请求封装：X-Token 注入、401 跳登录、403 提示见 src/shared/http.js
const http = createHttp({ baseURL: '/api/v1', tokenKey: TOKEN_KEY, userKey: USER_KEY })

const get = (url, params) => http.get(url, { params })
const post = (url, data, params) => http.post(url, data, { params })

export const api = {
  login: (userNo, password) => post('/borrow/auth/login', { userNo, password }),
  logout: () => post('/borrow/auth/logout'),
  dashboard: () => get('/borrow/dashboard'),

  materials: keyword => get('/borrow/meta/materials', { keyword }),
  warehouses: () => get('/borrow/meta/warehouses'),
  stocks: keyword => get('/borrow/meta/stocks', { keyword }),
  batches: (materialId, warehouseId) => get('/borrow/meta/batches', { materialId, warehouseId }),

  orders: (status, keyword) => get('/borrow/orders', { status, keyword }),
  pendingApprove: () => get('/borrow/orders/pending-approve'),
  order: id => get(`/borrow/orders/${id}`),
  orderItems: id => get(`/borrow/orders/${id}/items`),
  orderLogs: id => get(`/borrow/orders/${id}/logs`),
  createOrder: d => post('/borrow/orders', d),
  approveOrder: (id, d) => post(`/borrow/orders/${id}/approve`, d),

  pendingLend: keyword => get('/borrow/lends/pending', { keyword }),
  lend: d => post('/borrow/lends', d),
  outbounds: keyword => get('/borrow/outbounds', { keyword }),

  borrowed: keyword => get('/borrow/returns/borrowed', { keyword }),
  overdue: keyword => get('/borrow/returns/overdue', { keyword }),
  doReturn: d => post('/borrow/returns', d),

  losses: (status, sourceType, keyword) => get('/borrow/losses', { status, sourceType, keyword }),
  lossLogs: id => get(`/borrow/losses/${id}/logs`),
  approveLoss: (id, d) => post(`/borrow/losses/${id}/approve`, d),
  executeLoss: (id, d) => post(`/borrow/losses/${id}/execute`, d)
}

export default api
