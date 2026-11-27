import request from '../request'

export function getCategoryTree(params) {
  return request.get('/categories/tree', { params })
}

export function getCategoryDetail(id) {
  return request.get(`/categories/${id}`)
}

export function createCategory(data) {
  return request.post('/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}
