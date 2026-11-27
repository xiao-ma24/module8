<template>
  <div class="page-card fill" v-loading="loading">
    <div class="head">
      <div>
        <h3>登录日志</h3>
        <p class="sub">记录每次登录的账号、IP、设备与结果，异常登录可据此追溯</p>
      </div>
      <div class="ops">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button :icon="Download" @click="exportXls">导出 Excel</el-button>
        <el-button :icon="Document" @click="exportPdf">导出 PDF</el-button>
      </div>
    </div>

    <el-row :gutter="12" class="stats">
      <el-col :xs="12" :md="6"><div class="stat-card blue"><div class="label">登录总数</div><div class="num">{{ rows.length }}</div><div class="foot">含成功与失败</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card green"><div class="label">登录成功</div><div class="num">{{ countOk }}</div><div class="foot">正常进入平台</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card red"><div class="label">登录失败</div><div class="num">{{ countFail }}</div><div class="foot">需关注异常来源</div></div></el-col>
      <el-col :xs="12" :md="6"><div class="stat-card purple"><div class="label">涉及账号</div><div class="num">{{ accounts }}</div><div class="foot">去重后的工号数</div></div></el-col>
    </el-row>

    <el-tabs v-model="tab">
      <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name" :label="`${t.label} (${countOf(t.name)})`" />
    </el-tabs>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索工号 / 姓名 / IP" clearable style="width: 240px" :prefix-icon="Search" />
      <el-select v-model="device" placeholder="全部设备" clearable style="width: 150px">
        <el-option v-for="d in devices" :key="d" :label="d" :value="d" />
      </el-select>
      <div class="grow"></div>
      <span class="total">共 {{ total }} 条</span>
    </div>

    <el-table :data="paged" height="100%">
      <el-table-column prop="createdAt" label="登录时间" width="160">
        <template #default="{ row: r }">{{ dt(r.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="userNo" label="工号" width="110" />
      <el-table-column prop="userName" label="姓名" width="110">
        <template #default="{ row: r }">{{ empty(r.userName) }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP 地址" width="140" />
      <el-table-column prop="device" label="设备 / 浏览器" min-width="200">
        <template #default="{ row: r }">{{ empty(r.device) }}</template>
      </el-table-column>
      <el-table-column label="结果" width="100">
        <template #default="{ row: r }">
          <el-tag size="small" :type="r.result === 1 ? 'success' : 'danger'" effect="plain">
            {{ r.result === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="说明" min-width="200">
        <template #default="{ row: r }">{{ empty(r.message) }}</template>
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
import { Search, Download, Document, Refresh } from '@element-plus/icons-vue'
import api from '../api'
import { usePager } from '../composables/usePager'
import { exportExcel, printPdf } from '../utils/export'
import { dt, empty } from '../common'

const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const device = ref('')
const tab = ref('all')

const tabs = [{ name: 'all', label: '全部' }, { name: 'ok', label: '成功' }, { name: 'fail', label: '失败' }]

const countOk = computed(() => rows.value.filter(r => r.result === 1).length)
const countFail = computed(() => rows.value.filter(r => r.result !== 1).length)
const accounts = computed(() => new Set(rows.value.map(r => r.userNo).filter(Boolean)).size)
const devices = computed(() => [...new Set(rows.value.map(r => r.device).filter(Boolean))])

const filtered = computed(() => {
  let list = rows.value
  if (device.value) list = list.filter(r => r.device === device.value)
  if (tab.value === 'ok') list = list.filter(r => r.result === 1)
  if (tab.value === 'fail') list = list.filter(r => r.result !== 1)
  return list
})
const { page, pageSize, total, paged, reset } = usePager(filtered)

function countOf(name) {
  if (name === 'ok') return countOk.value
  if (name === 'fail') return countFail.value
  return rows.value.length
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.loginLogs({ keyword: keyword.value || undefined })
    reset()
  } finally {
    loading.value = false
  }
}

const COLS = [
  ['登录时间', r => dt(r.createdAt)], ['工号', r => r.userNo || ''], ['姓名', r => r.userName || ''],
  ['IP', r => r.ip || ''], ['设备', r => r.device || ''], ['结果', r => (r.result === 1 ? '成功' : '失败')],
  ['说明', r => r.message || '']
]

function exportXls() {
  exportExcel(COLS.map(c => c[0]), filtered.value.map(r => COLS.map(c => c[1](r))), '登录日志', '登录日志')
  ElMessage.success('已导出登录日志')
}

function exportPdf() {
  printPdf('登录日志', COLS.map(c => c[0]), filtered.value.map(r => COLS.map(c => c[1](r))), `共 ${filtered.value.length} 条`)
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 12px; }
.head h3 { margin: 0 0 4px; font-size: 17px; color: #0f172a; }
.head .sub { margin: 0; font-size: 12px; color: #94a3b8; }
.ops { display: flex; gap: 8px; }
.stats { margin-bottom: 10px; }
.stat-card {
  padding: 16px 18px; border-radius: 14px; color: #fff;
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
  box-shadow: 0 8px 22px rgba(37,99,235,.18);
  min-height: 92px; overflow: hidden;
}
.stat-card .label { font-size: 13px; opacity: .92; margin-bottom: 4px; line-height: 1.35;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.stat-card .num { font-size: 26px; font-weight: 700; letter-spacing: .5px; line-height: 1.1; }
.stat-card .foot { font-size: 11px; opacity: .80; margin-top: 5px; line-height: 1.3;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.total { font-size: 12px; color: #94a3b8; }
</style>
