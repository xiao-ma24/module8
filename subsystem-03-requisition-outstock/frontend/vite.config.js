import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5183,
    proxy: {
      '/api': { target: 'http://localhost:8083', changeOrigin: true }
    }
  }
})
