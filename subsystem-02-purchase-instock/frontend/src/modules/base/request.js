import { createHttp } from '../../shared/http'

// 子系统① 独立前缀，避免与子系统② 的 /api/v1/base 冲突
// 统一拦截器（Cookie / 401 / 403）见 src/shared/http.js
const request = createHttp({
  baseURL: '/api/v1/basedata',
  timeout: 10000,
  onRequest: (config) => {
    // 演示阶段按当前角色上报操作人，供后端操作日志切面记录
    const isTeacher = localStorage.getItem('demo_role') === 'teacher'
    // HTTP 头不支持中文，按 UTF-8 编码后由后端解码
    config.headers['X-User-Name'] = encodeURIComponent(isTeacher ? '韦森宁' : '系统管理员')
  }
})

export default request
