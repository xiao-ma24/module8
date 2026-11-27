import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5182,
    proxy: {
      // 合并系统：统一入口 5182，按接口前缀分流到各业务服务
      // 统一身份认证走管理员端 8080；采购验收与入库 8082；领用出库 8083；借用归还 8084；盘点预警与补货 8085
      '/api/v1/auth': { target: 'http://localhost:8080', changeOrigin: true },
      '/api/v1/requisition': { target: 'http://localhost:8083', changeOrigin: true },
      '/api/v1/borrow': { target: 'http://localhost:8084', changeOrigin: true },
      '/api/v1/stocktake': { target: 'http://localhost:8085', changeOrigin: true },
      // ① 基础数据统一使用 /api/v1/basedata 前缀（子系统① context-path），不再做重写
      '/api/v1/basedata': { target: 'http://localhost:8081', changeOrigin: true },
      '/api': { target: 'http://localhost:8082', changeOrigin: true }
    }
  }
})
