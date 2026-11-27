import request from '@/utils/request'

export function getRoleList(params) {
  return request.get('/sys/role/list', { params })
}
