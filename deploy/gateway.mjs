/**
 * 本地开发网关（生产用 deploy/nginx.conf）
 *
 * 作用：托管前端静态文件；对 /api/ 请求先向认证服务核验 Cookie，
 *      再注入 X-DBA-USER-ID / X-DBA-ROLE 后按路径转发到 8080~8085。
 *
 * 启动：
 *   SITE=/opt/module8/frontend PORT=5182 \
 *   AUTH_SERVICE_URL=http://127.0.0.1:18080 \
 *   AUTH_INTERNAL_SECRET=<内部密钥> node deploy/gateway.mjs
 */
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';

const SITE = process.env.SITE;
const PORT = Number(process.env.PORT || 5182);
const AUTH = process.env.AUTH_SERVICE_URL || 'http://127.0.0.1:18080';
const SECRET = process.env.AUTH_INTERNAL_SECRET || 'local-dev-secret';

// 与 Nginx 部署时的 location 分发保持一致：一个前端，多个后端
const ROUTES = [
  { prefix: '/api/v1/requisition/', target: 'http://127.0.0.1:8083' },
  { prefix: '/api/v1/borrow/', target: 'http://127.0.0.1:8084' },
  { prefix: '/api/v1/stocktake/', target: 'http://127.0.0.1:8085' },
  { prefix: '/api/v1/basedata/', target: 'http://127.0.0.1:8081' },
  { prefix: '/api/v1/admin/', target: 'http://127.0.0.1:8080' },
  { prefix: '/api/v1/base/', target: 'http://127.0.0.1:8082' },
  { prefix: '/api/v1/purchase/', target: 'http://127.0.0.1:8082' },
  { prefix: '/api/v1/profile', target: 'http://127.0.0.1:8082' },
  { prefix: '/api/v1/auth/', target: 'http://127.0.0.1:8080' }
];

function resolveTarget(url) {
  for (const route of ROUTES) {
    if (url.startsWith(route.prefix)) return new URL(route.target);
  }
  return new URL('http://127.0.0.1:8082');
}

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json'
};

function cookies(header) {
  const out = {};
  if (!header) return out;
  for (const part of header.split(';')) {
    const i = part.indexOf('=');
    if (i > 0) out[part.slice(0, i).trim()] = part.slice(i + 1).trim();
  }
  return out;
}

const verifyCache = new Map();

function cacheGet(token) {
  const item = verifyCache.get(token);
  if (!item) return null;
  if (item.expires < Date.now()) { verifyCache.delete(token); return null; }
  return item.user;
}

function cacheSet(token, user) {
  if (verifyCache.size > 5000) verifyCache.clear();
  verifyCache.set(token, { user, expires: Date.now() + 10000 });
}

function json(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json;charset=UTF-8' });
  res.end(JSON.stringify(body));
}

async function proxyApi(req, res) {
  const token = cookies(req.headers.cookie)['CAMPUS_SESSION'];
  if (!token) return json(res, 401, { error: 'unauthenticated', message: '未登录或登录已过期' });

  let user = cacheGet(token);
  try {
    if (user) return proxyWithUser(req, res, user);
    const response = await fetch(`${AUTH}/api/auth/internal/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Internal-Secret': SECRET },
      body: JSON.stringify({ token })
    });
    if (response.status !== 200) return json(res, 401, { error: 'unauthenticated', message: '凭证无效' });
    user = await response.json();
    cacheSet(token, user);
  } catch {
    return json(res, 503, { error: 'auth_unavailable', message: '统一认证服务不可用' });
  }

  return proxyWithUser(req, res, user);
}

function proxyWithUser(req, res, user) {
  const target = resolveTarget(req.url);
  const headers = { ...req.headers };
  for (const name of Object.keys(headers)) {
    if (name.toLowerCase().startsWith('x-dba-')) delete headers[name];
  }
  headers['x-dba-user-id'] = user.user_id;
  headers['x-dba-role'] = user.role;
  headers['x-dba-business-id'] = user.business_id || '';
  headers.host = target.host;

  const upstream = http.request({
    hostname: target.hostname,
    port: target.port,
    path: req.url,
    method: req.method,
    headers
  }, (response) => {
    if (response.statusCode >= 400) {
      const line = `${new Date().toISOString()} ${response.statusCode} role=${user.role} user=${user.user_id} ${req.method} ${req.url}\n`;
      try { fs.appendFileSync(path.join(process.cwd(), 'access-403.log'), line); } catch {}
    }
    res.writeHead(response.statusCode, response.headers);
    response.pipe(res);
  });
  upstream.on('error', () => json(res, 502, { error: 'bad_gateway', target: target.origin }));
  req.pipe(upstream);
}

function serveStatic(req, res) {
  const urlPath = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  let file = path.join(SITE, urlPath);
  if (!file.startsWith(SITE)) return json(res, 403, { error: 'forbidden' });
  if (urlPath === '/' || !path.extname(urlPath)) {
    file = path.join(SITE, 'index.html');
  }
  fs.readFile(file, (err, data) => {
    if (err) return json(res, 404, { error: 'not_found', path: urlPath });
    res.writeHead(200, { 'Content-Type': MIME[path.extname(file)] || 'application/octet-stream' });
    res.end(data);
  });
}

http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) return proxyApi(req, res);
  return serveStatic(req, res);
}).listen(PORT, '127.0.0.1', () => {
  console.log(`site ${PORT} root=${SITE}, api routed by prefix`);
});
