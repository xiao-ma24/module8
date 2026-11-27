import request from '../request'

export function getUserPage(params) {
  return request.get('/sys/user/list', { params })
}

export function getUser(id) {
  return request.get(`/sys/user/${id}`)
}

export function registerUser(data) {
  return request.post('/sys/user/register', data)
}

export function assignUserRoles(data) {
  return request.post('/sys/user/assign-role', data)
}

export function createUser(data) {
  return request.post('/sys/user', data)
}

export function updateUser(id, data) {
  return request.put(`/sys/user/${id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/sys/user/${id}`)
}
