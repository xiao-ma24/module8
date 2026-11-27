<template>
  <div>
    <el-card class="page-card fill">
      <div class="toolbar">
        <b style="font-size:15px">统计分析</b>
        <el-tag effect="plain" size="small">数据实时取自业务单据</el-tag>
        <div class="grow"></div>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出当前报表</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
      </div>

      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane label="采购 / 入库趋势" name="trend">
          <div ref="chartEl" style="height:250px"></div>
          <el-table :data="paged" size="small" stripe>
            <el-table-column prop="month" label="月份" width="120" />
            <el-table-column label="采购金额" align="right"><template #default="{ row: r }">{{ money(r.order) }}</template></el-table-column>
            <el-table-column label="入库金额" align="right"><template #default="{ row: r }">{{ money(r.inbound) }}</template></el-table-column>
            <el-table-column label="入库占采购比" align="right">
              <template #default="{ row: r }">{{ r.order ? ((r.inbound / r.order) * 100).toFixed(1) + '%' : '—' }}</template>
            </el-table-column>
            <el-table-column label="环比" align="right">
              <template #default="{ row: r }">
                <span :style="{ color: r.mom >= 0 ? '#16a34a' : '#ef4444' }">{{ r.mom == null ? '—' : (r.mom >= 0 ? '▲ ' : '▼ ') + Math.abs(r.mom).toFixed(1) + '%' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="供应商采购排行" name="supplier">
          <el-table :data="paged" size="small" stripe>
            <el-table-column type="index" label="排名" width="70" />
            <el-table-column prop="name" label="供应商" min-width="240" show-overflow-tooltip />
            <el-table-column label="采购金额" align="right" width="160"><template #default="{ row: r }">{{ money(r.value) }}</template></el-table-column>
            <el-table-column label="占比" width="110" align="right">
              <template #default="{ row: r }">{{ (r.value / supplierTotal * 100).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column label="集中度提示" min-width="200">
              <template #default="{ row: r }">
                <el-tag v-if="r.value / supplierTotal > 0.4" type="warning" size="small">占比过高，建议引入备选供应商</el-tag>
                <span v-else style="color:#94a3b8">合理</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="物资类别结构" name="category">
          <el-table :data="paged" size="small" stripe>
            <el-table-column prop="name" label="物资类别" width="200" />
            <el-table-column label="采购金额" align="right"><template #default="{ row: r }">{{ money(r.value) }}</template></el-table-column>
            <el-table-column label="占比" width="120" align="right">
              <template #default="{ row: r }">{{ (r.value / catTotal * 100).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column label="建议" min-width="260">
              <template #default="{ row: r }">
                <span style="color:#64748b">{{ advise(r) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="预算执行情况" name="budget">
          <el-table :data="paged" size="small" stripe>
            <el-table-column prop="budgetNo" label="预算编号" width="110" />
            <el-table-column prop="subjectName" label="预算科目" min-width="160" />
            <el-table-column label="可用余额" align="right" width="150"><template #default="{ row: r }">{{ money(r.balance) }}</template></el-table-column>
            <el-table-column label="已冻结" align="right" width="130"><template #default="{ row: r }">{{ money(r.frozen) }}</template></el-table-column>
            <el-table-column label="额度合计" align="right" width="150">
              <template #default="{ row: r }">{{ money(Number(r.balance) + Number(r.frozen)) }}</template>
            </el-table-column>
            <el-table-column label="冻结占比" width="200">
              <template #default="{ row: r }">
                <el-progress :percentage="pct(r)" :stroke-width="8" :status="pct(r) > 60 ? 'warning' : undefined" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '../api'
import { money } from '../common'
import { exportExcel, printPdf } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage } from 'element-plus'

const tab = ref('trend')
const trend = ref([]), suppliers = ref([]), category = ref([]), budgets = ref([])
const chartEl = ref()

const withMom = computed(() => trend.value.map((r, i) => ({
  ...r,
  mom: i === 0 || !trend.value[i - 1].order ? null : ((r.order - trend.value[i - 1].order) / trend.value[i - 1].order) * 100
})))
const current = computed(() => tab.value === 'trend' ? withMom.value
  : tab.value === 'supplier' ? suppliers.value
  : tab.value === 'category' ? category.value : budgets.value)
const { page, pageSize, total, paged, reset } = usePager(current, 10)

const supplierTotal = computed(() => suppliers.value.reduce((s, r) => s + r.value, 0) || 1)
const catTotal = computed(() => category.value.reduce((s, r) => s + r.value, 0) || 1)
const pct = r => {
  const sum = Number(r.balance) + Number(r.frozen)
  return sum ? Math.round((Number(r.frozen) / sum) * 100) : 0
}
const advise = r => {
  const share = r.value / catTotal.value
  if (share > 0.45) return '该类别占采购额近半，建议做年度框架协议采购以压价'
  if (r.name === '实验器材') return '实验器材单价高、需资质，建议固定 2 家供应商并做效期管理'
  return '占比正常，可按安全库存做周期性补货'
}

onMounted(async () => {
  const s = await api.summary()
  trend.value = s.trend || []
  suppliers.value = s.supplierTop || []
  category.value = s.category || []
  budgets.value = await api.budgets()
  await nextTick()
  drawChart()
})

function drawChart() {
  if (!chartEl.value) return
  const c = echarts.init(chartEl.value)
  c.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['采购金额', '入库金额'], top: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trend.value.map(t => t.month) },
    yAxis: { type: 'value', axisLabel: { formatter: v => v / 1000 + 'k' } },
    series: [
      { name: '采购金额', type: 'bar', barWidth: 18, itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] }, data: trend.value.map(t => t.order) },
      { name: '入库金额', type: 'line', smooth: true, symbolSize: 7, itemStyle: { color: '#16a34a' }, data: trend.value.map(t => t.inbound) },
    ]
  })
}

const colsMap = {
  trend: [{ label: '月份', prop: 'month' }, { label: '采购金额', prop: r => Number(r.order).toFixed(2) },
          { label: '入库金额', prop: r => Number(r.inbound).toFixed(2) }],
  supplier: [{ label: '供应商', prop: 'name', width: 30 }, { label: '采购金额', prop: r => Number(r.value).toFixed(2) }],
  category: [{ label: '物资类别', prop: 'name' }, { label: '采购金额', prop: r => Number(r.value).toFixed(2) }],
  budget: [{ label: '预算编号', prop: 'budgetNo' }, { label: '预算科目', prop: 'subjectName', width: 22 },
           { label: '可用余额', prop: r => Number(r.balance).toFixed(2) }, { label: '已冻结', prop: r => Number(r.frozen).toFixed(2) }],
}
const titles = { trend: '采购与入库月度趋势', supplier: '供应商采购排行', category: '物资类别结构', budget: '预算执行情况' }
function doExport(type) {
  const cols = colsMap[tab.value], rows = current.value
  if (!rows.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, rows, titles[tab.value], '统计分析')
  else printPdf(titles[tab.value], cols, rows, '学校物资采购管理系统')
}
</script>
