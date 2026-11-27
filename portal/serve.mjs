// 统一登录入口（HTTP）
// 静态托管 portal/index.html，并把 /api 反向代理到 8080
import http from 'node:http'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const PORT = 5179
const API_HOST = '127.0.0.1'
const API_PORT = 8080

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) {
    const proxy = http.request({
      host: API_HOST,
      port: API_PORT,
      path: req.url,
      method: req.method,
      headers: { ...req.headers, host: `${API_HOST}:${API_PORT}` }
    }, up => {
      res.writeHead(up.statusCode || 502, up.headers)
      up.pipe(res)
    })
    proxy.on('error', () => {
      res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' })
      res.end(JSON.stringify({ code: 502, msg: '认证服务未启动（8080）' }))
    })
    req.pipe(proxy)
    return
  }

  const file = req.url === '/' || req.url.startsWith('/?') ? 'index.html' : req.url.slice(1)
  const full = path.join(__dirname, file)
  if (!full.startsWith(__dirname) || !fs.existsSync(full) || fs.statSync(full).isDirectory()) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
    res.end('Not Found')
    return
  }
  const type = full.endsWith('.html') ? 'text/html; charset=utf-8' : 'application/octet-stream'
  // 禁止缓存，避免浏览器仍展示旧版登录页
  res.writeHead(200, { 'Content-Type': type, 'Cache-Control': 'no-store, must-revalidate' })
  fs.createReadStream(full).pipe(res)
})

server.listen(PORT, () => {
  console.log(`统一登录入口：http://localhost:${PORT}`)
})
