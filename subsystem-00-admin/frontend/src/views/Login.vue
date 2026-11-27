<template>
  <div class="login-wrap">
    <div class="login-shell">
      <!-- 左：品牌面板 -->
      <section class="brand-panel">
        <span class="blob b1"></span>
        <span class="blob b2"></span>
        <span class="blob b3"></span>

        <div class="brand-top">
          <div class="logo">🛡️</div>
          <span class="badge">v1.0.0</span>
        </div>

        <h1>学校物资管理大系统</h1>
        <p class="tagline">统一平台底座 · 让组织、权限与审计在一个入口运转</p>

        <ul class="feats">
          <li>
            <el-icon :size="18"><Key /></el-icon>
            <div>
              <b>统一身份认证</b>
              <span>跨子系统单点登录，账号一处维护</span>
            </div>
          </li>
          <li>
            <el-icon :size="18"><Lock /></el-icon>
            <div>
              <b>角色权限控制</b>
              <span>按菜单粒度授权，改完即时生效</span>
            </div>
          </li>
          <li>
            <el-icon :size="18"><Tickets /></el-icon>
            <div>
              <b>操作全程留痕</b>
              <span>登录与操作日志可查、可导出</span>
            </div>
          </li>
        </ul>

        <div class="brand-foot">平台管理端 · 数据总览 / 用户 / 权限 / 审计</div>
      </section>

      <!-- 右：登录卡片 -->
      <section class="login-card">
        <div class="card-head">
          <h2>平台管理端</h2>
          <p>请使用统一身份登录后进入管理后台</p>
        </div>

        <!-- 正在跳板提示 -->
        <div v-if="!handoffReady" class="jump">
          <p>正在为你引导到统一登录入口…</p>
          <a class="force-link" href="http://localhost:5179">如果没有自动跳转，请点这里</a>
        </div>
        <div v-else class="jump">
          <p>登录信息已接收，正在进入系统…</p>
          <div class="spinner"></div>
        </div>

        <a v-if="!handoffReady" class="enter-btn" :href="PORTAL">
          <el-icon :size="16"><Right /></el-icon>前往统一登录入口
        </a>

        <p class="foot-tip">首次登录需修改初始密码；账号锁定请联系平台管理员。</p>
      </section>
    </div>

    <el-dialog v-model="pwdDlg" title="首次登录需修改密码" width="420px" :close-on-click-modal="false" :show-close="false">
      <p class="pwd-tip">为保障账号安全，初始密码仅用于首次登录，请设置新密码（至少 6 位）。</p>
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPwd" type="password" show-password placeholder="至少 6 位">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
      </el-form>
      <p class="msg">{{ pwdForm.msg }}</p>
      <template #footer>
        <el-button @click="cancelPwd">稍后再说</el-button>
        <el-button type="primary" :loading="savingPwd" @click="savePwd">确认修改并进入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key, Lock, Tickets, Right } from '@element-plus/icons-vue'
import api from '../api'

const PORTAL = 'http://localhost:5179'
const router = useRouter()
const route = useRoute()
const handoffReady = ref(false)
const pwdDlg = ref(false)
const savingPwd = ref(false)
const pwdForm = reactive({ newPwd: '', confirm: '', msg: '' })
let pending = null

function finish(data) {
  localStorage.setItem('adminToken', data.token)
  localStorage.setItem('adminUser', JSON.stringify(data.user))
  localStorage.setItem('adminMenus', JSON.stringify(data.menus))
  ElMessage.success('欢迎回来，' + data.user.userName)
  const redirect = route.query.redirect
  router.replace(redirect && !redirect.startsWith('/login') ? redirect : '/overview')
}

function enter(data) {
  if (data.needChangePwd) {
    pending = data
    pwdForm.newPwd = ''
    pwdForm.confirm = ''
    pwdForm.msg = ''
    pwdDlg.value = true
    return
  }
  finish(data)
}

async function savePwd() {
  pwdForm.msg = ''
  if (pwdForm.newPwd.length < 6) { pwdForm.msg = '新密码长度不能少于 6 位'; return }
  if (pwdForm.newPwd !== pwdForm.confirm) { pwdForm.msg = '两次输入的密码不一致'; return }
  savingPwd.value = true
  try {
    localStorage.setItem('adminToken', pending.token)
    localStorage.setItem('adminUser', JSON.stringify(pending.user))
    localStorage.setItem('adminMenus', JSON.stringify(pending.menus))
    await api.changePwd({ oldPwd: '123456', newPwd: pwdForm.newPwd })
    pwdDlg.value = false
    ElMessage.success('密码已修改')
    finish(pending)
  } catch (e) {
    pwdForm.msg = e?.msg || '修改失败，请重试'
  } finally {
    savingPwd.value = false
  }
}

function cancelPwd() {
  pwdDlg.value = false
  if (pending) finish(pending)
  else router.replace('/overview')
}

onMounted(() => {
  // 1) 统一入口登录后携带 token 跳转过来（base64 编码）
  const h = route.query.handoff
  if (h) {
    try {
      enter(JSON.parse(decodeURIComponent(escape(atob(h)))))
      return
    } catch (e) {
      ElMessage.warning('登录信息已失效，请重新登录')
    }
  }

  // 2) 没有 handoff：直接跳统一登录入口
  //    无论是否已登录都走 Portal（Portal 会按角色分流）
  window.location.replace(PORTAL)
})
</script>

<style scoped>
.login-wrap {
  min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 32px 24px;
  background:
    radial-gradient(circle at 12% 8%, #e6efff 0, transparent 46%),
    radial-gradient(circle at 92% 94%, #e9f1ff 0, transparent 48%),
    linear-gradient(180deg, #f7faff, #eef3fb);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; color: var(--text-1);
}
.login-shell {
  width: 100%; max-width: 1040px; display: grid; grid-template-columns: 1.1fr 440px;
  background: #fff; border-radius: 22px; overflow: hidden;
  border: 1px solid var(--line); box-shadow: 0 26px 64px rgba(15, 30, 61, .14);
}

/* ---------- 左侧品牌面板 ---------- */
.brand-panel {
  position: relative; overflow: hidden; padding: 42px 44px;
  background: linear-gradient(150deg, #0c1a33 0%, #14305f 55%, #1a3a75 100%);
  color: #fff; display: flex; flex-direction: column;
}
.brand-panel .blob {
  position: absolute; border-radius: 50%; pointer-events: none; filter: blur(2px);
}
.brand-panel .b1 {
  width: 300px; height: 300px; top: -110px; right: -80px;
  background: radial-gradient(circle, rgba(96, 165, 250, .34), transparent 68%);
}
.brand-panel .b2 {
  width: 260px; height: 260px; bottom: -110px; left: -70px;
  background: radial-gradient(circle, rgba(59, 130, 246, .30), transparent 70%);
}
.brand-panel .b3 {
  width: 190px; height: 190px; top: 46%; right: 8%;
  background: radial-gradient(circle, rgba(125, 211, 252, .18), transparent 70%);
}
.brand-top { display: flex; align-items: center; gap: 10px; position: relative; }
.brand-top .logo {
  width: 42px; height: 42px; border-radius: 12px; display: grid; place-items: center;
  font-size: 20px; background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  box-shadow: 0 8px 20px rgba(37, 99, 235, .45);
}
.brand-top .badge {
  font-size: 11px; color: #b9cdf5; padding: 3px 9px; border-radius: 20px;
  background: rgba(255, 255, 255, .10); border: 1px solid rgba(255, 255, 255, .14);
}
.brand-panel h1 {
  position: relative; margin: 30px 0 10px; font-size: 25px; letter-spacing: .6px; line-height: 1.35;
}
.brand-panel .tagline {
  position: relative; margin: 0; font-size: 13px; line-height: 1.8; color: #9fb4dc; max-width: 380px;
}
.feats { position: relative; list-style: none; margin: 30px 0 0; padding: 0; display: grid; gap: 14px; }
.feats li {
  display: flex; align-items: flex-start; gap: 12px; padding: 11px 14px; border-radius: 12px;
  background: rgba(255, 255, 255, .05); border: 1px solid rgba(255, 255, 255, .08);
}
.feats li .el-icon {
  width: 32px; height: 32px; flex: none; border-radius: 9px; color: #dbeafe;
  background: rgba(96, 165, 250, .18);
}
.feats b { display: block; font-size: 13px; font-weight: 600; margin-bottom: 3px; }
.feats span { display: block; font-size: 11.5px; color: #9fb4dc; line-height: 1.6; }
.brand-foot {
  position: relative; margin-top: auto; padding-top: 26px; font-size: 11px; color: #7f95c0; letter-spacing: .3px;
}

/* ---------- 右侧登录卡片 ---------- */
.login-card {
  padding: 46px 42px 36px; display: flex; flex-direction: column; justify-content: center; background: #fff;
}
.card-head h2 { margin: 0 0 8px; font-size: 21px; font-weight: 700; letter-spacing: .4px; }
.card-head p { margin: 0; font-size: 12.5px; color: var(--text-3); line-height: 1.7; }
.jump {
  margin-top: 26px; padding: 18px 16px; text-align: center;
  background: var(--primary-soft); border: 1px solid #dbe7fd; border-radius: 12px;
}
.jump p { margin: 0 0 8px; font-size: 13px; color: #1e3a8a; }
.jump p:last-child { margin-bottom: 0; }
.force-link { font-size: 12.5px; color: var(--primary); text-decoration: none; }
.force-link:hover { text-decoration: underline; }
.spinner {
  width: 26px; height: 26px; border: 3px solid #dbe7fd; border-top-color: var(--primary);
  border-radius: 50%; margin: 6px auto 0; animation: spin .8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.enter-btn {
  margin-top: 22px; height: 44px; display: flex; align-items: center; justify-content: center; gap: 6px;
  border-radius: 10px; font-size: 14px; font-weight: 600; color: #fff; text-decoration: none;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 8px 20px rgba(37, 99, 235, .30); transition: .18s;
}
.enter-btn:hover {
  background: linear-gradient(135deg, #4b8dfa, #2f6ff0);
  transform: translateY(-1px); box-shadow: 0 10px 24px rgba(37, 99, 235, .38);
}
.foot-tip { margin: 18px 0 0; font-size: 11.5px; color: var(--text-3); text-align: center; line-height: 1.7; }
.msg { min-height: 17px; font-size: 12px; color: #dc2626; margin-top: 8px; }
.pwd-tip { font-size: 12.5px; color: #64748b; margin: 0 0 14px; line-height: 1.7; }

/* 窄屏：上下堆叠 */
@media (max-width: 900px) {
  .login-wrap { padding: 20px 16px; }
  .login-shell { grid-template-columns: 1fr; max-width: 460px; }
  .brand-panel { padding: 28px 26px; }
  .brand-panel h1 { margin: 20px 0 8px; font-size: 21px; }
  .feats { margin-top: 20px; gap: 10px; }
  .brand-foot { display: none; }
  .login-card { padding: 30px 26px 26px; }
}
</style>
