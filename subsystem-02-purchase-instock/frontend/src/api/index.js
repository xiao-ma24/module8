import { createHttp } from '../shared/http'

// 统一请求封装：Cookie 携带、401 跳登录、403 提示见 src/shared/http.js
const http = createHttp({ baseURL: '/api/v1' })

export const get = (url, params) => http.get(url, { params })
export const post = (url, data, config) => http.post(url, data, config)
export const put = (url, data) => http.put(url, data)
export const del = (url) => http.delete(url)

// 业务 API
export const api = {
  materials: (kw, cat) => get('/base/materials', { keyword: kw, categoryCode: cat }),
  suppliers: () => get('/base/suppliers'),
  warehouses: () => get('/base/warehouses'),
  users: (role) => get('/base/users', { roleCode: role }),
  depts: () => get('/base/depts'),
  budgets: () => get('/base/budgets'),
  stocks: (kw, low) => get('/base/stocks', { keyword: kw, lowOnly: low }),

  requests: () => get('/purchase/requests'),
  pendingRequests: () => get('/purchase/requests/pending'),
  request: id => get(`/purchase/requests/${id}`),
  requestItems: id => get(`/purchase/requests/${id}/items`),
  requestLogs: id => get(`/purchase/requests/${id}/logs`),
  createRequest: d => post('/purchase/requests', d),
  submitRequest: id => post(`/purchase/requests/${id}/submit`),
  approveRequest: (id, params, confirms) => post(`/purchase/requests/${id}/approve`, confirms || [], { params }),

  orders: () => get('/purchase/orders'),
  receivableOrders: () => get('/purchase/orders/receivable'),
  order: id => get(`/purchase/orders/${id}`),
  orderItems: id => get(`/purchase/orders/${id}/items`),
  toOrder: (prId, d) => post(`/purchase/orders/from-request/${prId}`, d),
  confirmOrder: id => post(`/purchase/orders/${id}/confirm`),
  cancelOrder: id => post(`/purchase/orders/${id}/cancel`),

  acceptances: () => get('/purchase/acceptances'),
  acceptance: id => get(`/purchase/acceptances/${id}`),
  acptItems: id => get(`/purchase/acceptances/${id}/items`),
  createAcpt: d => post('/purchase/acceptances', d),
  completeAcpt: (id, d) => post(`/purchase/acceptances/${id}/complete`, d),
  acptReport: (id, d) => post(`/purchase/acceptances/${id}/report`, d || {}),
  returns: () => get('/purchase/acceptances/returns'),
  finishReturn: (aiId, handler) => post(`/purchase/acceptances/returns/${aiId}/finish`, null, { params: { handler } }),
  recon: params => get('/purchase/finance/recon', params),

  inbounds: () => get('/purchase/inbounds'),
  inbound: id => get(`/purchase/inbounds/${id}`),
  inItems: id => get(`/purchase/inbounds/${id}/items`),
  inExtras: (id, d) => post(`/purchase/inbounds/${id}/extras`, d),
  confirmInbound: id => post(`/purchase/inbounds/${id}/confirm`),
  fillVoucher: (id, d) => post(`/purchase/inbounds/${id}/voucher`, d),
  expiring: () => get('/purchase/inbounds/expiring'),

  summary: () => get('/purchase/dashboard/summary'),
  profile: userId => get('/profile', { userId }),
  aiChat: q => post('/purchase/ai/chat', { question: q }),
  aiAdvice: () => get('/purchase/ai/replenish-advice'),
  aiStatus: () => get('/purchase/ai/status'),

  // 基础数据增删改（页面内直接调用）
  post, put, del,
}

export default api
