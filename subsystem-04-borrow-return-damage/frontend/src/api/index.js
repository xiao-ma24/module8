import axios from 'axios'
import { ElMessage } from 'element-plus'

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
