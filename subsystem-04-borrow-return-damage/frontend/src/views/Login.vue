<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="head">
        <div class="logo">🔧</div>
        <h1>学校物资管理大系统</h1>
      </div>
      <p class="sub">子系统④ 借用归还报损</p>

      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="工号 / 学号">
          <el-input v-model="form.userNo" size="large" placeholder="如 T0006" autocomplete="username">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" size="large" show-password
                    placeholder="演示账号初始密码 123456" @keyup.enter="submit">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%;margin-top:4px" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <el-divider style="margin:20px 0 14px"><span class="divider-txt">演示账号（密码 123456）</span></el-divider>
      <div class="demo-list">
        <div class="demo-row" v-for="d in demos" :key="d.no" @click="fill(d.no)">
          <span class="tag">{{ d.no }}</span>
          <span class="nm">{{ d.name }}</span>
          <span class="rl">{{ d.role }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api, { setLogin } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ userNo: '', password: '' })
const demos = [
  { no: 'T0006', name: '张老师', role: '借用人 · 申请借用' },
  { no: 'T0001', name: '王主任', role: '部门负责人 · 审批' },
  { no: 'T0003', name: '刘志豪', role: '库管员 · 借出与核销' },
  { no: 'T0005', name: '韦森宁', role: '管理员 · 只读查看' },
]

function fill(no) { form.userNo = no; form.password = '123456' }

async function submit() {
  if (!form.userNo || !form.password) return
  loading.value = true
  try {
    const data = await api.login(form.userNo.trim(), form.password)
    setLogin(data.token, data.user)
    router.replace('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px;
  background: radial-gradient(circle at 50% 0%, #ffffff 0, #f1f3fa 45%, #e6eaf5 100%);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; color: #1f2937;
}
.login-card {
  width: 430px; max-width: 100%; background: #fff; border-radius: 22px;
  padding: 30px 28px 24px; box-shadow: 0 24px 60px rgba(51, 65, 121, .16);
}
.head { display: flex; align-items: center; gap: 12px; }
.logo {
  width: 44px; height: 44px; border-radius: 12px; display: grid; place-items: center;
  background: linear-gradient(135deg, #2563eb, #1e40af); font-size: 20px;
}
h1 { font-size: 19px; margin: 0; letter-spacing: .5px; }
.sub { font-size: 12px; color: #94a3b8; margin: 4px 0 22px 56px; }
.divider-txt { font-size: 11.5px; color: #94a3b8; }
.demo-list { display: flex; flex-direction: column; gap: 6px; }
.demo-row {
  display: flex; align-items: center; gap: 10px; padding: 7px 10px; border-radius: 8px;
  cursor: pointer; transition: background .16s;
}
.demo-row:hover { background: #f4f7fd; }
.demo-row .tag {
  font-size: 11px; color: #2563eb; background: #eff6ff; padding: 2px 8px; border-radius: 6px;
  font-weight: 600; min-width: 52px; text-align: center;
}
.demo-row .nm { font-size: 12.5px; color: #334155; font-weight: 600; }
.demo-row .rl { font-size: 11.5px; color: #94a3b8; margin-left: auto; }
</style>
