import request from '../request'

export function getWarehousePage(params) {
  return request.get('/warehouses', { params })
}
