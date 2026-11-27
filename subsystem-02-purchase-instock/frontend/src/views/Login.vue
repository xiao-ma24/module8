<template>
  <div class="login-wrap">
    <span class="orb o1"></span><span class="orb o2"></span>
    <div class="login-card">
      <div class="head">
        <div class="logo">📦</div>
        <div>
          <h1>学校物资管理大系统</h1>
          <p class="sub">采购验收与入库 · 统一身份认证</p>
        </div>
      </div>
      <div class="jump">
        <div class="bar"><span></span></div>
        <p>{{ handoffReady ? '登录信息已接收，正在进入系统…' : '正在为你引导到统一登录入口…' }}</p>
        <a class="force-link" href="http://localhost:5179">未自动跳转？点这里进入统一登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const PORTAL = 'http://localhost:5179'
const route = useRoute()
const router = useRouter()
const handoffReady = ref(false)

function enter(uid) {
  localStorage.setItem('mockUserId', String(uid))
  router.replace('/')
}

onMounted(() => {
  // 统一入口登录后携带 uid 跳转过来
  if (route.query.skip === '1') {
    const uid = Number(route.query.uid) || 2
    enter(uid)
    return
  }

  // 没有 handoff：直接跳统一登录入口
  window.location.replace(PORTAL)
})
</script>

<style scoped>
.login-wrap {
  min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px;
  position: relative; overflow: hidden;
  background: linear-gradient(160deg, #eef4ff 0%, #f6f9fe 45%, #e7f0ff 100%);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; color: #1e293b;
}
.orb { position: absolute; border-radius: 50%; filter: blur(2px); pointer-events: none; }
.o1 { width: 460px; height: 460px; top: -160px; left: -120px;
  background: radial-gradient(circle, rgba(59,130,246,.20), transparent 68%); }
.o2 { width: 520px; height: 520px; right: -180px; bottom: -220px;
  background: radial-gradient(circle, rgba(99,102,241,.16), transparent 68%); }
.login-card {
  width: 440px; max-width: 100%; background: rgba(255,255,255,.92);
  backdrop-filter: blur(14px); border: 1px solid rgba(255,255,255,.9);
  border-radius: 22px; padding: 30px 28px 26px;
  box-shadow: 0 26px 60px rgba(30,64,175,.14); position: relative;
}
.head { display: flex; align-items: center; gap: 13px; }
.logo {
  width: 46px; height: 46px; border-radius: 13px; display: grid; place-items: center;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8); font-size: 21px;
  box-shadow: 0 8px 20px rgba(37,99,235,.35); flex: none;
}
h1 { font-size: 19px; margin: 0; letter-spacing: .5px; }
.sub { font-size: 12px; color: #94a3b8; margin: 4px 0 0; }
.jump { text-align: center; padding: 26px 0 6px; }
.bar { height: 4px; border-radius: 4px; background: #e8eefb; overflow: hidden; margin-bottom: 16px; }
.bar span { display: block; width: 42%; height: 100%; border-radius: 4px;
  background: linear-gradient(90deg, #60a5fa, #2563eb); animation: slide 1.1s ease-in-out infinite; }
@keyframes slide { 0% { transform: translateX(-110%); } 100% { transform: translateX(250%); } }
.jump p { margin: 0 0 12px; font-size: 13.5px; color: #475569; }
.force-link { font-size: 12.5px; color: #2563eb; text-decoration: none; }
.force-link:hover { text-decoration: underline; }
</style>
