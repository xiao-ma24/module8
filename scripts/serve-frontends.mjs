#!/usr/bin/env node
// 零依赖静态站点 + 接口反代：没有 Nginx 时用它把 6 份前端产物跑起来
//   node scripts/serve-frontends.mjs
// 端口：5173(01) 5180(00) 5182(02) 5183(03) 5184(04) 5185(05)
// 可用 FE_PORT_OFFSET=1000 整体偏移端口（本机已被占用时做冒烟验证用）
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const DIST = path.join(ROOT, 'dist');
const API = process.env.API_HOST || '127.0.0.1';
const OFFSET = Number(process.env.FE_PORT_OFFSET || 0);

const SITES = [
  { port: 5173, dir: 'frontend-m01', name: '01 基础数据与审批' },
  { port: 5180, dir: 'frontend-m00', name: '00 平台管理端' },
  { port: 5182, dir: 'frontend', name: '02 统一业务大系统（采购/领用/借用/盘点）' },
  { port: 5183, dir: 'frontend-m03', name: '03 领用申请审批出库' },
  { port: 5184, dir: 'frontend-m04', name: '04 借用归还报损' },
  { port: 5185, dir: 'frontend-m05', name: '05 盘点预警与补货' },
];

const BACKENDS = [
  ['/api/v1/auth', 8080], ['/api/v1/admin', 8080],
  ['/api/v1/basedata', 8081],
  ['/api/v1/base', 8082], ['/api/v1/purchase', 8082], ['/api/v1/profile', 8082],
  ['/api/v1/requisition', 8083],
  ['/api/v1/borrow', 8084],
  ['/api/v1/stocktake', 8085],
];

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg',
  '.gif': 'image/gif', '.ico': 'image/x-icon', '.woff': 'font/woff', '.woff2': 'font/woff2',
  '.ttf': 'font/ttf', '.map': 'application/json',
};

function proxy(req, res, port) {
  const up = http.request({
    host: API, port, path: req.url, method: req.method,
    headers: { ...req.headers, host: `${API}:${port}` },
  }, (r) => {
    res.writeHead(r.statusCode || 502, r.headers);
    r.pipe(res);
  });
  up.on('error', (e) => {
    if (!res.headersSent) res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ code: 502, message: `后端 :${port} 不可用：${e.message}` }));
  });
  req.pipe(up);
}

function resolveFile(base, url) {
  const target = path.join(base, decodeURIComponent(url.split('?')[0]));
  try {
    if (fs.statSync(target).isFile()) return target;
  } catch { /* 目录或不存在，回退 index.html */ }
  return path.join(base, 'index.html');
}

for (const site of SITES) {
  const base = path.join(DIST, site.dir);
  if (!fs.existsSync(path.join(base, 'index.html'))) {
    console.error(`[跳过] ${site.dir} 不存在，请先执行 scripts/build-all.sh --with-frontend`);
    continue;
  }
  const port = site.port + OFFSET;
  http.createServer((req, res) => {
    const url = req.url.split('?')[0];
    const hit = BACKENDS.find(([p]) => url === p || url.startsWith(p + '/'));
    if (hit) return proxy(req, res, hit[1]);
    const file = resolveFile(base, req.url);
    res.writeHead(200, {
      'Content-Type': MIME[path.extname(file)] || 'application/octet-stream',
      'Cache-Control': path.extname(file) === '.html' ? 'no-store' : 'public, max-age=31536000',
    });
    fs.createReadStream(file).pipe(res);
  }).listen(port, '0.0.0.0', () => {
    console.log(`  ${site.name}  ->  http://localhost:${port}/`);
  });
}

console.log(`\n静态目录：${DIST}\n后端地址：${API}:8080~8085（可用 API_HOST 覆盖）\nCtrl+C 停止`);
