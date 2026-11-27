import request from '../request'

export function getRoleList(params) {
  return request.get('/sys/role/list', { params })
}
