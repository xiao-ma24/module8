import fs from 'node:fs';

const BASE = 'http://127.0.0.1:5182';
const AUTH = 'http://127.0.0.1:18080';
const CDP = 'http://127.0.0.1:9222';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const ROLES = [
  { account: 'T0005', name: '韦森宁', role: 'ADMIN', routes: ['/', '/requests', '/orders', '/acceptances', '/inbounds', '/suppliers', '/warehouses', '/recon', '/report', '/materials', '/base/purchase', '/base/approval', '/req/orders', '/req/stocks', '/borrow/orders', '/borrow/returns', '/borrow/stocks', '/stocktake/warning', '/stocktake/orders', '/stocktake/replenish'] },
  { account: 'T0002', name: '李思滢', role: 'PURCHASER', routes: ['/', '/requests', '/orders', '/acceptances', '/suppliers', '/report', '/materials'] },
  { account: 'T0007', name: '韦森聍', role: 'CHECKER', routes: ['/', '/acceptances', '/materials'] },
  { account: 'T0003', name: '刘志豪', role: 'STORE_KEEPER', routes: ['/', '/inbounds', '/warehouses', '/materials', '/stocktake/warning', '/req/orders', '/req/approve', '/req/outbounds', '/req/stocks', '/borrow/orders', '/borrow/approve', '/borrow/lends', '/borrow/returns', '/borrow/losses', '/borrow/stocks', '/base/approval'] },
  { account: 'T0004', name: '金日勋', role: 'STOCKTAKER', routes: ['/', '/stocktake/orders', '/stocktake/replenish', '/materials'] },
  { account: 'T0006', name: '张老师', role: 'APPLICANT', routes: ['/', '/requests', '/req/orders', '/req/stocks', '/borrow/orders', '/borrow/returns', '/borrow/stocks', '/base/purchase'] },
  { account: 'T0001', name: '王主任', role: 'DEPT_HEAD', routes: ['/', '/requests', '/req/orders', '/req/stocks', '/borrow/orders', '/borrow/returns', '/base/purchase'] }
];

async function connect() {
  const created = await (await fetch(`${CDP}/json/new?about:blank`, { method: 'PUT' })).json();
  const ws = new WebSocket(created.webSocketDebuggerUrl);
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
  let id = 0;
  const pending = new Map();
  const events = [];
  ws.onmessage = (e) => {
    const m = JSON.parse(e.data);
    if (m.id && pending.has(m.id)) { const h = pending.get(m.id); pending.delete(m.id); h(m); }
    else if (m.method) events.push(m);
  };
  const send = (method, params = {}) => new Promise((res, rej) => {
    const i = ++id;
    pending.set(i, (m) => (m.error ? rej(new Error(JSON.stringify(m.error))) : res(m.result)));
    ws.send(JSON.stringify({ id: i, method, params }));
  });
  const evalJs = async (expression) => {
    const r = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.text);
    return r.result.value;
  };
  await send('Page.enable');
  await send('Runtime.enable');
  await send('Network.enable');
  return { send, evalJs, ws, events, targetId: created.id };
}

async function goto(api, url, wait = 1000) {
  await api.send('Page.navigate', { url });
  for (let i = 0; i < 60; i++) {
    await sleep(200);
    try { if ((await api.evalJs('document.readyState')) === 'complete') break; } catch {}
  }
  await sleep(wait);
}

const report = [];
for (const role of ROLES) {
  const api = await connect();
  await api.send('Network.clearBrowserCookies');
  await goto(api, `${AUTH}/`, 500);
  const login = await api.evalJs(`localStorage.clear(); fetch('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({login_name:'${role.account}',password:'Campus@123'})}).then(r=>r.status)`);
  api.events.length = 0;
  await goto(api, `${BASE}/`, 500);

  let chip = '';
  for (let i = 0; i < 80; i++) {
    chip = await api.evalJs(`(document.querySelector('.user-chip')||{}).innerText||''`);
    if (chip.includes(role.name)) break;
    await sleep(300);
  }

  const bad = [];
  for (const route of role.routes) {
    api.events.length = 0;
    await goto(api, BASE + route, 1200);
    for (const e of api.events) {
      if (e.method === 'Network.responseReceived') {
        const { status, url } = e.params.response;
        if (status >= 400 && !url.includes('favicon')) bad.push(`${route} -> ${status} ${url}`);
      }
    }
  }

  const shot = await api.send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(`C:/Users/MECHREVO/Documents/Codex/2026-11-09/w-xia/outputs/integration/m08/final2-${role.role}.png`, Buffer.from(shot.data, 'base64'));

  report.push({ role: role.role, account: role.account, login, chip: chip.replace(/\n/g, ' '), ok: chip.includes(role.name), pages: role.routes.length, bad });
  api.ws.close();
  await fetch(`${CDP}/json/close/${api.targetId}`);
}

console.log(JSON.stringify(report, null, 2));
