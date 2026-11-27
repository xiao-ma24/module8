import request from '@/utils/request'

export function registerPurchase(data) {
  return request.post('/purchase/register', data)
}

export function getPurchaseRecords(params) {
  return request.get('/purchase/records', { params })
}

export function getPendingPurchases() {
  return request.get('/purchase/pending')
}

export function auditPurchase(data) {
  return request.post('/purchase/audit', data)
}
