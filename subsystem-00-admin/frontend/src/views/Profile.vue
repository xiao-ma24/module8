<template>
  <div class="profile" v-loading="loading">
    <!-- 账号名片 -->
    <el-card shadow="never" class="card banner">
      <div class="id-card">
        <div class="avatar">{{ (info.userName || 'A').slice(0, 1) }}</div>
        <div class="who">
          <div class="line">
            <h2>{{ info.userName || '—' }}</h2>
            <el-tag size="small" effect="dark" type="primary">{{ info.roleName || '未分配角色' }}</el-tag>
            <el-tag size="small" :type="info.userStatus === 1 ? 'success' : 'danger'" effect="plain">
              {{ info.userStatus === 1 ? '在职启用' : '已停用' }}
            </el-tag>
          </div>
          <p class="muted">
            工号 {{ empty(info.userNo) }} ｜ {{ empty(info.deptName) }} ｜ {{ empty(info.title) }}
          </p>
          <p class="muted">手机 {{ empty(info.phone) }} ｜ 邮箱 {{ empty(info.email) }}</p>
        </div>
        <div class="right">
          <div class="kv"><span>最近登录</span><b>{{ dt(info.lastLoginAt) }}</b></div>
          <div class="kv"><span>入职/建档</span><b>{{ date(info.createdAt) }}</b></div>
        </div>
      </div>
    </el-card>

    <!-- 指标卡 -->
    <el-row :gutter="12" class="stats">
      <el-col :xs="12" :md="6"><div class="stat-card blue"><div class="label">累计操作</div><div class="num">{{ stats.myOps || 0 }}</div><div class="foot">本人全部审计记录</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card green"><div class="label">今日操作</div><div class="num">{{ stats.todayOps || 0 }}</div><div class="foot">当天平台操作次数</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card orange"><div class="label">近 7 天</div><div class="num">{{ stats.weekOps || 0 }}</div><div class="foot">周内操作趋势</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card purple"><div class="label">平台资产</div><div class="num">{{ (stats.dictItems || 0) + (stats.configs || 0) }}</div><div class="foot"> {{ stats.dictItems || 0 }} 字典项 / {{ stats.configs || 0 }} 参数</div></div></el-col>
    </el-row>

    <el-card shadow="never" class="card fill">
      <el-tabs v-model="tab">
        <el-tab-pane name="todo" :label="`我的待办 (${(data.todos || []).length})`">
          <div v-if="!(data.todos || []).length" class="empty">
            <el-icon :size="34" color="#94a3b8"><CircleCheck /></el-icon>
            <p>暂无待办，平台数据健康</p>
          </div>
          <div v-else class="todo-list">
            <div v-for="(t, i) in data.todos" :key="i" class="todo" :class="t.level">
              <el-icon :size="20">
                <WarningFilled v-if="t.level === 'danger'" />
                <Warning v-else-if="t.level === 'warning'" />
                <InfoFilled v-else />
              </el-icon>
              <div class="t-main">
                <b>{{ t.type }}</b>
                <p>{{ t.desc }}</p>
              </div>
              <el-button size="small" type="primary" plain @click="$router.push(t.path)">去处理</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="logs" :label="`操作记录 (${(data.logs || []).length})`">
          <el-timeline v-if="(data.logs || []).length" class="timeline">
            <el-timeline-item v-for="l in data.logs" :key="l.id" :timestamp="dt(l.createdAt)" placement="top"
                              :type="l.result === 1 ? 'primary' : 'danger'">
              <div class="log">
                <el-tag size="small" effect="plain">{{ l.module }}</el-tag>
                <b>{{ l.action }}</b>
                <span class="muted">{{ l.target }}</span>
                <span class="muted">{{ l.detail }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="empty"><p>暂无操作记录</p></div>
        </el-tab-pane>

        <el-tab-pane name="security" label="账号与安全">
          <el-descriptions :column="2" border class="desc">
            <el-descriptions-item label="登录账号">{{ empty(info.userNo) }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ empty(info.userName) }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">{{ empty(info.deptName) }}</el-descriptions-item>
            <el-descriptions-item label="岗位角色">{{ empty(info.roleName) }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ empty(info.phone) }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ empty(info.email) }}</el-descriptions-item>
            <el-descriptions-item label="最近登录时间">{{ dt(info.lastLoginAt) }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag size="small" :type="info.userStatus === 1 ? 'success' : 'danger'" effect="plain">
                {{ info.userStatus === 1 ? '启用' : '停用' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-alert class="alert" type="info" :closable="false" show-icon
                    title="账号信息由平台管理端统一维护"
                    description="修改姓名、手机号、邮箱、部门或角色，请前往「用户管理」编辑本人档案；忘记密码请联系另一位管理员在「用户管理 → 重置密码」处理，重置后初始密码为 123456。" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { CircleCheck, InfoFilled, Warning, WarningFilled } from '@element-plus/icons-vue'
import api from '../api'
import { date, dt, empty } from '../common'

const loading = ref(false)
const data = ref({})
const info = reactive({})
const stats = reactive({})
const tab = ref('todo')

async function load() {
  loading.value = true
  try {
    const d = await api.profile()
    data.value = d
    Object.keys(info).forEach(k => delete info[k])
    Object.keys(stats).forEach(k => delete stats[k])
    Object.assign(info, d.info || {})
    Object.assign(stats, d.stats || {})
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile { display: flex; flex-direction: column; gap: 12px; }
.card { border-radius: 12px; }
.banner { background: linear-gradient(135deg, #f8fbff 0%, #eef4ff 100%); }
.id-card { display: flex; align-items: center; gap: 18px; }
.avatar {
  width: 68px; height: 68px; border-radius: 18px; flex: none;
  background: linear-gradient(135deg, #2563eb, #7c3aed); color: #fff;
  display: grid; place-items: center; font-size: 28px; font-weight: 600;
}
.who { flex: 1; }
.who .line { display: flex; align-items: center; gap: 8px; }
.who h2 { margin: 0; font-size: 20px; color: #0f172a; }
.muted { color: #64748b; font-size: 13px; margin: 4px 0 0; }
.right { text-align: right; display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
.kv { font-size: 12px; color: #94a3b8; }
.kv b { color: #334155; margin-left: 6px; font-weight: 500; }
.stats .stat-card .label { font-size: 13px; opacity: .92; }
.stat-card.blue { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.stat-card.green { background: linear-gradient(135deg, #22c55e, #15803d); }
.stat-card.orange { background: linear-gradient(135deg, #fb923c, #c2410c); }
.stat-card.purple { background: linear-gradient(135deg, #a855f7, #6d28d9); }
.fill { flex: 1; min-height: 380px; }
.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 60px 0; color: #94a3b8; font-size: 13px; }
.todo-list { display: flex; flex-direction: column; gap: 10px; }
.todo {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px;
  border-radius: 10px; border: 1px solid #eef1f6; background: #fbfcfe;
}
.todo .t-main { flex: 1; }
.todo b { font-size: 14px; color: #0f172a; }
.todo p { margin: 2px 0 0; font-size: 12px; color: #64748b; }
.todo.warning { border-color: #fde68a; background: #fffbeb; }
.todo.danger { border-color: #fecaca; background: #fef2f2; }
.todo.warning .el-icon { color: #d97706; }
.todo.danger .el-icon { color: #dc2626; }
.todo.info .el-icon { color: #2563eb; }
.timeline { padding: 6px 4px 0; }
.log { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 13px; color: #334155; }
.tip { margin-bottom: 10px; }
.desc { margin-bottom: 12px; }
.alert { margin-top: 4px; }
</style>
