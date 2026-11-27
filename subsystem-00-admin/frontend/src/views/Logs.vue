<template>
  <div class="page-card fill" v-loading="loading">
    <div class="head">
      <div>
        <h3>操作日志</h3>
        <p class="sub">记录「谁在什么模块对哪个对象做了什么」，可直接导出交审计</p>
      </div>
      <div class="ops">
        <el-button :icon="Download" @click="exportXls">导出 Excel</el-button>
        <el-button :icon="Document" @click="exportPdf">导出 PDF</el-button>
      </div>
    </div>

    <el-row :gutter="12" class="stats">
      <el-col :xs="12" :md="6"><div class="stat-card"><div class="label">累计操作</div><div class="num">{{ stats.auditTotal || 0 }}</div><div class="foot">全部模块</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card"><div class="label">今日操作</div><div class="num">{{ stats.todayOps || 0 }}</div><div class="foot">当天新增记录</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card"><div class="label">登录失败</div><div class="num">{{ stats.loginFail || 0 }}</div><div class="foot">需关注异常</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card"><div class="label">覆盖模块</div><div class="num">{{ (stats.modules || []).length }}</div><div class="foot">用户/角色/字典/参数…</div></div></el-col>
    </el-row>

    <el-tabs v-model="tab">
      <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name" :label="`${t.label} (${countOf(t.name)})`" />
    </el-tabs>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索操作人 / 对象 / 详情" clearable style="width: 240px" :prefix-icon="Search" />
      <el-select v-model="module" placeholder="全部模块" clearable style="width: 150px">
        <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
      </el-select>
      <div class="grow"></div>
      <span class="total">共 {{ total }} 条</span>
    </div>

    <el-table :data="paged" height="100%">
      <el-table-column type="expand">
        <template #default="{ row: r }">
          <div class="snapshot" v-if="r.beforeValue || r.afterValue">
            <div class="snap-item">
              <div class="snap-label">变更前值</div>
              <div class="snap-body before">{{ empty(r.beforeValue) }}</div>
            </div>
            <div class="snap-item">
              <div class="snap-label">变更后值</div>
              <div class="snap-body after">{{ empty(r.afterValue) }}</div>
            </div>
          </div>
          <div v-else class="snapshot"><span class="none">该操作无结构化变更快照，详情见「详情」列</span></div>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="160">
        <template #default="{ row: r }">{{ dt(r.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="userName" label="操作人" width="110" />
      <el-table-column label="模块" width="110">
        <template #default="{ row: r }"><el-tag size="small" effect="plain">{{ r.module }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="action" label="动作" width="100">
        <template #default="{ row: r }">
          <el-tag size="small" :type="actionType(r.action)" effect="light">{{ r.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="target" label="操作对象" min-width="170" />
      <el-table-column prop="detail" label="详情" min-width="220">
        <template #default="{ row: r }">{{ empty(r.detail) }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column label="结果" width="90">
        <template #default="{ row: r }">
          <el-tag size="small" :type="r.result === 1 ? 'success' : 'danger'" effect="plain">
            {{ r.result === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
                     :total="total" layout="total, sizes, prev, pager, next, jumper" background />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, Document } from '@element-plus/icons-vue'
import api from '../api'
import { usePager } from '../composables/usePager'
import { exportExcel, printPdf } from '../utils/export'
import { dt, empty } from '../common'

const loading = ref(false)
const rows = ref([])
const stats = ref({})
const keyword = ref('')
const module = ref('')
const tab = ref('all')

const tabs = [{ name: 'all', label: '全部' }, { name: 'ok', label: '成功' }, { name: 'fail', label: '失败' }]

const modules = computed(() => [...new Set(rows.value.map(r => r.module).filter(Boolean))])
const filtered = computed(() => {
  let list = rows.value
  if (module.value) list = list.filter(r => r.module === module.value)
  if (tab.value === 'ok') list = list.filter(r => r.result === 1)
  if (tab.value === 'fail') list = list.filter(r => r.result !== 1)
  return list
})
const { page, pageSize, total, paged, reset } = usePager(filtered)

function countOf(name) {
  if (name === 'ok') return rows.value.filter(r => r.result === 1).length
  if (name === 'fail') return rows.value.filter(r => r.result !== 1).length
  return rows.value.length
}

function actionType(a) {
  if (!a) return 'info'
  if (a.includes('删除') || a.includes('停用')) return 'danger'
  if (a.includes('新增')) return 'success'
  if (a.includes('修改') || a.includes('改值') || a.includes('重置')) return 'warning'
  return 'primary'
}

async function load() {
  loading.value = true
  try {
    const [l, s] = await Promise.all([
      api.auditLogs({ keyword: keyword.value || undefined }),
      api.logStats()
    ])
    rows.value = l
    stats.value = s
    reset()
  } finally {
    loading.value = false
  }
}

const COLS = [
  ['时间', r => dt(r.createdAt)], ['操作人', r => r.userName], ['模块', r => r.module], ['动作', r => r.action],
  ['操作对象', r => r.target || ''], ['详情', r => r.detail || ''],
  ['变更前值', r => r.beforeValue || ''], ['变更后值', r => r.afterValue || ''],
  ['IP', r => r.ip || ''],
  ['结果', r => (r.result === 1 ? '成功' : '失败')]
]

function exportXls() {
  exportExcel(COLS.map(c => c[0]), filtered.value.map(r => COLS.map(c => c[1](r))), '操作日志', '操作日志')
  ElMessage.success('已导出操作日志')
}

function exportPdf() {
  printPdf('操作日志', COLS.map(c => c[0]), filtered.value.map(r => COLS.map(c => c[1](r))), `共 ${filtered.value.length} 条`)
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 12px; }
.head h3 { margin: 0 0 4px; font-size: 17px; color: #0f172a; }
.head .sub { margin: 0; font-size: 12px; color: #94a3b8; }
.ops { display: flex; gap: 8px; }
.stats { margin-bottom: 10px; }

/* 蓝白渐变统计卡（抄 Overview/LoginLogs，解决 stat-card 未定义导致的白底问题） */
.stat-card {
  padding: 16px 18px; border-radius: 14px; color: #fff;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  box-shadow: 0 8px 22px rgba(37,99,235,.18);
  transition: transform .18s, box-shadow .18s; min-height: 92px;
  overflow: hidden;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(37,99,235,.28); }
.stat-card .label { font-size: 13px; opacity: .92; margin-bottom: 4px; line-height: 1.35;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat-card .num { font-size: 26px; font-weight: 700; letter-spacing: .5px; line-height: 1.1; }
.stat-card .foot { font-size: 11px; opacity: .80; margin-top: 5px; line-height: 1.3;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.total { font-size: 12px; color: #94a3b8; }
.snapshot { display: flex; gap: 16px; padding: 4px 12px; }
.snap-item { flex: 1; min-width: 0; }
.snap-label { font-size: 12px; color: #64748b; margin-bottom: 4px; }
.snap-body { background: #f8fafc; border: 1px solid #e8eef8; border-radius: 6px; padding: 8px 10px; font-size: 12px; color: #1e293b; word-break: break-all; white-space: pre-wrap; }
.snap-body.before { border-left: 3px solid #f59e0b; }
.snap-body.after { border-left: 3px solid #10b981; }
.none { font-size: 12px; color: #94a3b8; }
</style>
