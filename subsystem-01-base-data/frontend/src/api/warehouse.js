import request from '@/utils/request'

export function getWarehousePage(params) {
  return request.get('/warehouses', { params })
}
