import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api/v1/stocktake', timeout: 20000 })
http.interceptors.response.use(
  r => {
    if (r.data && r.data.code !== 0) { ElMessage.error(r.data.msg || '操作失败'); return Promise.reject(r.data) }
    return r.data.data
  },
  e => { ElMessage.error(e.response?.data?.msg || e.message || '网络异常'); return Promise.reject(e) }
)

export const get = (url, params) => http.get(url, { params })
export const post = (url, data) => http.post(url, data)
export const del = (url) => http.delete(url)

/** 后端 meta 接口返回的是数据库列名，这里统一转成驼峰，页面只用一种字段风格 */
const camel = rows => (rows || []).map(r => Object.fromEntries(
  Object.entries(r).map(([k, v]) => [k.replace(/_([a-z])/g, (_, c) => c.toUpperCase()), v])
))

/** 子系统⑤ 业务 API（与 docs/api/api-05.md 一一对应） */
export const api = {
  // 基础数据（合并到子系统① 后改为 /api/v1/base/**）
  materials: () => get('/meta/materials').then(camel),
  warehouses: () => get('/meta/warehouses').then(camel),
  suppliers: () => get('/meta/suppliers').then(camel),
  users: () => get('/meta/users').then(camel),
  depts: () => get('/meta/depts').then(camel),

  // 盘点单
  orders: params => get('/orders', params),
  order: id => get(`/orders/${id}`),
  createOrder: d => post('/orders', d),
  startOrder: (id, operatorId) => post(`/orders/${id}/start?operatorId=${operatorId || 1}`),
  entryItem: (id, d) => post(`/orders/${id}/items`, d),
  finishOrder: (id, d) => post(`/orders/${id}/finish`, d),

  // 预警
  warnings: params => get('/warnings', params),
  warningSummary: () => get('/warnings/summary'),
  scanWarnings: () => post('/warnings/scan'),
  rules: () => get('/rules'),
  saveRule: d => post('/rules', d),
  deleteRule: id => del(`/rules/${id}`),

  // 补货与采购计划
  suggestions: params => get('/replenish/suggestions', params),
  replenishSummary: () => get('/replenish/summary'),
  createPlans: d => post('/replenish/plans', d),
  plans: params => get('/plans', params),
  submitPlans: d => post('/plans/submit', d),
  approvePlans: d => post('/plans/approve', d),
}

export default api
