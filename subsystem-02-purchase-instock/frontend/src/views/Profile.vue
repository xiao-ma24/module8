<template>
  <div>
    <el-row :gutter="14">
      <!-- 账号名片 -->
      <el-col :span="7">
        <el-card class="page-card fill" style="height:100%">
          <div class="me">
            <el-avatar :size="76" style="background:linear-gradient(135deg,#2563eb,#7c3aed);font-size:30px">
              {{ (info.userName || '李').slice(0, 1) }}
            </el-avatar>
            <div class="me-name">{{ info.userName }}</div>
            <div class="me-sub">工号 {{ info.userNo }}　·　{{ info.deptName }}</div>
            <div class="me-tags">
              <el-tag effect="dark" :type="roleTag">{{ info.roleName }}</el-tag>
              <el-tag effect="plain" :type="info.userStatus === 1 ? 'success' : 'danger'" style="margin-left:6px">
                {{ info.userStatus === 1 ? '在职' : '停用' }}
              </el-tag>
            </div>
          </div>
          <el-divider />
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="所属部门">{{ info.deptName }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">{{ info.userStatus === 1 ? '正常' : '停用' }}</el-descriptions-item>
            <el-descriptions-item label="开通日期">{{ info.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">由师资系统同步</el-descriptions-item>
            <el-descriptions-item label="数据权限">{{ scope }}</el-descriptions-item>
            <el-descriptions-item label="本次登录">{{ now }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 工作量 + 待办 + 记录 -->
      <el-col :span="17">
        <el-card class="page-card fill">
          <div class="toolbar">
            <b style="font-size:15px">我的工作台</b>
            <el-tag size="small" effect="plain">{{ viewName }}视角</el-tag>
            <div class="grow"></div>
            <el-button size="small" plain @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
          </div>

          <el-row :gutter="12">
            <el-col :span="6" v-for="(s, i) in stats" :key="s.label">
              <div class="mini" :style="{ background: colors[i % colors.length] }">
                <div class="mini-label">{{ s.label }}</div>
                <div class="mini-num">{{ s.value }}<span class="unit">{{ s.unit }}</span></div>
                <div class="mini-note">{{ s.note }}</div>
              </div>
            </el-col>
          </el-row>

          <el-tabs v-model="tab" style="margin-top:16px">
            <el-tab-pane :label="`我的待办 (${todos.length})`" name="todo">
              <el-table :data="todos" size="small" stripe height="330" empty-text="暂无待办，全部处理完了">
                <el-table-column label="类型" width="130">
                  <template #default="{ row: r }"><el-tag :type="r.tag" effect="dark" size="small">{{ r.type }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="no" label="单据号" width="150" />
                <el-table-column prop="desc" label="说明" show-overflow-tooltip />
                <el-table-column label="操作" width="90">
                  <template #default="{ row: r }">
                    <el-link type="primary" @click="$router.push(r.path)">去处理</el-link>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="操作记录" name="log">
              <div style="height:330px; overflow-y:auto; padding:6px 4px">
                <el-timeline>
                  <el-timeline-item v-for="(l, i) in logs" :key="i" :timestamp="l.time" placement="top"
                                    :type="l.action === '驳回' ? 'danger' : 'primary'">
                    <div class="log-line"><b>{{ l.who }}</b>　{{ l.action }}<span class="log-detail">{{ l.detail }}</span></div>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </el-tab-pane>

            <el-tab-pane label="账号与安全" name="safe">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="登录账号">{{ info.userNo }}</el-descriptions-item>
                <el-descriptions-item label="角色"> {{ info.roleName }}</el-descriptions-item>
                <el-descriptions-item label="认证方式">统一身份认证（师资系统）</el-descriptions-item>
                <el-descriptions-item label="最近登录 IP">10.12.33.108</el-descriptions-item>
                <el-descriptions-item label="密码策略" :span="2">90 天更换一次，连续 5 次错误锁定 30 分钟</el-descriptions-item>
                <el-descriptions-item label="操作留痕" :span="2">所有单据操作写入 approve_log / stock_flow，可追溯</el-descriptions-item>
              </el-descriptions>
              <el-alert type="info" :closable="false" style="margin-top:12px"
                        title="课程演示环境未接入统一认证，实际部署时与师资系统 SSO 对接，本页数据按当前登录人过滤" />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const info = reactive({})
const stats = ref([])
const todos = ref([])
const logs = ref([])
const tab = ref(route.query.tab || 'todo')
const now = new Date().toLocaleString('zh-CN')

const colors = [
  'linear-gradient(135deg,#2563eb,#60a5fa)',
  'linear-gradient(135deg,#0d9488,#2dd4bf)',
  'linear-gradient(135deg,#7c3aed,#a78bfa)',
  'linear-gradient(135deg,#d97706,#fbbf24)',
  'linear-gradient(135deg,#16a34a,#4ade80)',
]
const roleTag = computed(() => info.roleCode === 'STORE_KEEPER' ? 'warning' : 'primary')
const viewName = computed(() => info.roleCode === 'STORE_KEEPER' ? '仓储' : '采购')
const scope = computed(() => info.roleCode === 'STORE_KEEPER' ? '本仓库入库与库存' : '采购全流程单据')

onMounted(load)
async function load() {
  const p = await api.profile(Number(localStorage.getItem('mockUserId') || 2))
  Object.assign(info, p.info)
  stats.value = (p.info.roleCode === 'STORE_KEEPER' ? p.storeStats : p.buyStats) || []
  todos.value = p.todos || []
  logs.value = p.logs || []
}
</script>

<style scoped>
.me { text-align: center; padding: 8px 0 4px; }
.me-name { font-size: 18px; font-weight: 700; margin-top: 10px; color: #1f2937; }
.me-sub { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.me-tags { margin-top: 10px; }
.lbl { font-size: 12px; color: #94a3b8; margin-bottom: 6px; }
.mini { border-radius: 12px; padding: 14px 16px; color: #fff; }
.mini-label { font-size: 12px; opacity: .92; }
.mini-num { font-size: 24px; font-weight: 700; margin: 4px 0 2px; }
.mini-num .unit { font-size: 12px; font-weight: 400; margin-left: 3px; opacity: .9; }
.mini-note { font-size: 11px; opacity: .85; }
.log-line { font-size: 13px; color: #334155; }
.log-detail { color: #94a3b8; margin-left: 8px; }
</style>
