import request from '@/utils/request'

export function getDeptList(params) {
  return request.get('/sys/dept/list', { params })
}

export function getDept(id) {
  return request.get(`/sys/dept/${id}`)
}

export function createDept(data) {
  return request.post('/sys/dept', data)
}

export function updateDept(id, data) {
  return request.put(`/sys/dept/${id}`, data)
}

export function deleteDept(id) {
  return request.delete(`/sys/dept/${id}`)
}
