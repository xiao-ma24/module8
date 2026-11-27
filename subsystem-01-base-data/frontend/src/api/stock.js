import request from '@/utils/request'

export function getStockList(params) {
  return request.get('/stocks', { params })
}

export function changeStock(data) {
  return request.post('/stocks/change', data)
}
