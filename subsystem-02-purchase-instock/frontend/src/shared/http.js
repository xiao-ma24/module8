import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 全项目唯一的请求封装。
 * 统一处理：携带 Cookie、注入 X-Token、业务码校验、401 跳登录、403 无权限提示。
 * 各业务模块只允许通过 createHttp 生成实例，禁止再直接 axios.create。
 */
export function createHttp({ baseURL = '/api/v1', timeout = 20000, tokenKey, userKey, onRequest } = {}) {
  const http = axios.create({ baseURL, timeout, withCredentials: true })

  http.interceptors.request.use(
    (config) => {
      if (tokenKey) {
        const token = localStorage.getItem(tokenKey)
        if (token) config.headers['X-Token'] = token
      }
      if (onRequest) onRequest(config)
      return config
    },
    (error) => Promise.reject(error)
  )

  const unauthorized = () => {
    if (tokenKey) localStorage.removeItem(tokenKey)
    if (userKey) localStorage.removeItem(userKey)
    if (location.pathname !== '/login') location.href = '/login'
    ElMessage.error('登录已失效，请重新登录')
  }

  http.interceptors.response.use(
    (response) => {
      const body = response.data
      if (body && body.code === 401) {
        unauthorized()
        return Promise.reject(body)
      }
      if (body && body.code !== 0) {
        ElMessage.error(body.msg || '操作失败')
        return Promise.reject(body)
      }
      return body && typeof body === 'object' && 'data' in body ? body.data : body
    },
    (error) => {
      const status = error.response?.status
      if (status === 401) {
        unauthorized()
        return Promise.reject(error)
      }
      if (status === 403) {
        ElMessage.error(error.response?.data?.msg || '当前角色无权执行该操作')
        return Promise.reject(error)
      }
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
      return Promise.reject(error)
    }
  )

  return http
}

/** 默认实例（/api/v1），供无需额外配置的模块直接使用 */
const defaultHttp = createHttp()

export const get = (url, params) => defaultHttp.get(url, { params })
export const post = (url, data, config) => defaultHttp.post(url, data, config)
export const put = (url, data) => defaultHttp.put(url, data)
export const del = (url) => defaultHttp.delete(url)

export default defaultHttp
