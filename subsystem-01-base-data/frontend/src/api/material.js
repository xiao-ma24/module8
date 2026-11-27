import request from '@/utils/request'

export function getMaterialPage(params = {}) {
  const { categoryId, ...rest } = params
  return request.get('/materials', { params: rest })
}

export function createMaterial(data) {
  return request.post('/materials', data)
}

export function updateMaterial(id, data) {
  return request.put(`/materials/${id}`, data)
}

export function deleteMaterial(id) {
  return request.delete(`/materials/${id}`)
}
