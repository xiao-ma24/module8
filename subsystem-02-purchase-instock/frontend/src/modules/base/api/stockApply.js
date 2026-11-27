import request from '../request'

export function submitStockApply(data) {
  return request.post('/stock-apply/submit', data)
}

export function auditStockApply(data) {
  return request.post('/stock-apply/audit', data)
}

export function getStockApplyList(params) {
  return request.get('/stock-apply/list', { params })
}
