import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1/basedata',
  timeout: 10000,
  withCredentials: true
})

request.interceptors.request.use(
  (config) => {
    // 鉴权模块接入后在这里统一注入 token
    // 演示阶段按当前角色上报操作人，供后端操作日志切面记录
    const isTeacher = localStorage.getItem('demo_role') === 'teacher'
    // HTTP 头不支持中文，按 UTF-8 编码后由后端解码
    config.headers['X-User-Name'] = encodeURIComponent(isTeacher ? '韦森宁' : '系统管理员')
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.code === 0) {
      return res.data
    }
    ElMessage.error((res && res.msg) || '请求失败')
    return Promise.reject(new Error((res && res.msg) || '请求失败'))
  },
  (error) => {
    const msg = error.response
      ? `请求异常（HTTP ${error.response.status}）`
      : '网络异常，请检查后端服务是否已启动'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
