<template>
  <div class="page-card fill" v-loading="loading">
    <div class="head">
      <div>
        <h3>部门月度额度</h3>
        <p class="sub">额度来源：财务系统预算表 budget（部门年度额度 ÷ 12，只读）· 出库时累加本月已用额度</p>
      </div>
      <div class="ops">
        <el-button :icon="Download" @click="exportXls">导出 Excel</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="k">核定部门</div>
        <div class="v">{{ rows.length }} <span>个</span></div>
      </div>
      <div class="stat">
        <div class="k">月度额度合计</div>
        <div class="v">{{ money(sumQuota) }}</div>
      </div>
      <div class="stat">
        <div class="k">本月已用合计</div>
        <div class="v used">{{ money(sumUsed) }}</div>
      </div>
      <div class="stat">
        <div class="k">整体执行率</div>
        <div class="v">{{ sumQuota ? Math.round(sumUsed / sumQuota * 100) : 0 }}<span>%</span></div>
      </div>
    </div>

    <div class="toolbar">
      <el-date-picker v-model="period" type="month" value-format="YYYYMM" format="YYYY 年 MM 月"
                      :clearable="false" style="width: 150px" @change="load" />
      <el-input v-model="keyword" placeholder="搜索部门名称 / 编码" clearable style="width: 240px"
                :prefix-icon="Search" @input="load" />
      <div class="grow"></div>
      <span class="total">共 {{ total }} 个部门</span>
    </div>

    <el-table :data="paged" height="100%">
      <el-table-column label="部门" min-width="170">
        <template #default="{ row: r }">
          <div class="dept">
            <span class="ico"><el-icon><OfficeBuilding /></el-icon></span>
            <div>
              <div class="nm">{{ r.deptName }}</div>
              <div class="cd">{{ r.deptCode || '未设置编码' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="年度预算" width="130" align="right">
        <template #default="{ row: r }">{{ r.annualBudget == null ? '—' : money(r.annualBudget) }}</template>
      </el-table-column>
      <el-table-column label="月度额度" width="130" align="right">
        <template #default="{ row: r }">{{ money(r.monthQuota) }}</template>
      </el-table-column>
      <el-table-column label="本月已用" width="130" align="right">
        <template #default="{ row: r }">
          <span :class="{ over: Number(r.usedAmount) > Number(r.monthQuota) && Number(r.monthQuota) > 0 }">
            {{ money(r.usedAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="剩余额度" width="130" align="right">
        <template #default="{ row: r }">{{ money(remain(r)) }}</template>
      </el-table-column>
      <el-table-column label="执行率" min-width="200">
        <template #default="{ row: r }">
          <div class="quota">
            <el-progress :percentage="pct(r)" :stroke-width="8" :color="pct(r) > 90 ? '#ef4444' : '#2563eb'" />
            <span class="q-txt">{{ pct(r) }}%</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="额度来源" width="110" align="center">
        <template #default>
          <el-tag size="small" type="info" effect="plain">财务预算</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近同步" width="160">
        <template #default="{ row: r }">{{ r.updateTime ? String(r.updateTime).replace('T', ' ').slice(0, 19) : '—' }}</template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
                     :total="total" layout="total, sizes, prev, pager, next" background />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, OfficeBuilding } from '@element-plus/icons-vue'
import api from '../api'
import { usePager } from '../composables/usePager'
import { exportExcel } from '../utils/export'
import { money } from '../common'

const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const period = ref(new Date().toISOString().slice(0, 7).replace('-', ''))

const filtered = computed(() => rows.value)
const { page, pageSize, total, paged, reset } = usePager(filtered)
const sumQuota = computed(() => rows.value.reduce((s, r) => s + Number(r.monthQuota || 0), 0))
const sumUsed = computed(() => rows.value.reduce((s, r) => s + Number(r.usedAmount || 0), 0))

function remain(r) {
  return Number(r.monthQuota || 0) - Number(r.usedAmount || 0)
}

function pct(r) {
  const q = Number(r.monthQuota || 0)
  if (!q) return 0
  return Math.min(100, Math.round((Number(r.usedAmount || 0) / q) * 100))
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.deptQuotas({ period: period.value, keyword: keyword.value || undefined })
    reset()
  } finally {
    loading.value = false
  }
}

function exportXls() {
  const data = filtered.value.map(r => [r.deptCode || '', r.deptName, r.annualBudget || 0, r.monthQuota || 0, r.usedAmount || 0,
    remain(r), pct(r) + '%', r.updateTime || ''])
  exportExcel(['部门编码', '部门名称', '年度预算', '月度额度', '本月已用', '剩余额度', '执行率', '最近同步'],
    data, `部门月度额度-${period.value}`, '部门月度额度')
  ElMessage.success('已导出部门月度额度')
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 12px; }
.head h3 { margin: 0 0 4px; font-size: 17px; color: #0f172a; }
.head .sub { margin: 0; font-size: 12px; color: #94a3b8; }
.ops { display: flex; gap: 8px; }
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 14px; }
.stat { background: linear-gradient(135deg, #f8fbff, #eef5ff); border: 1px solid #e2ebfa; border-radius: 12px; padding: 12px 14px; }
.stat .k { font-size: 12px; color: #64748b; }
.stat .v { margin-top: 6px; font-size: 20px; font-weight: 700; color: #1d4ed8; }
.stat .v.used { color: #ea580c; }
.stat .v span { font-size: 12px; font-weight: 500; color: #94a3b8; margin-left: 2px; }
.total { font-size: 12px; color: #94a3b8; }
.dept { display: flex; align-items: center; gap: 10px; }
.dept .ico { width: 30px; height: 30px; border-radius: 8px; background: #eef4ff; color: #2563eb; display: grid; place-items: center; }
.dept .nm { font-size: 13.5px; color: #0f172a; }
.dept .cd { font-size: 11px; color: #94a3b8; }
.quota { display: flex; align-items: center; gap: 10px; }
.quota :deep(.el-progress) { flex: 1; }
.q-txt { font-size: 11.5px; color: #64748b; white-space: nowrap; }
.over { color: #ef4444; font-weight: 600; }
</style>
