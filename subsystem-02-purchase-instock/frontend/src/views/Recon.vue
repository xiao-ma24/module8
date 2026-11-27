<template>
  <div>
    <el-card class="page-card fill">
      <div class="toolbar">
        <el-select v-model="q.supplierId" placeholder="全部供应商" clearable filterable style="width:220px" @change="load">
          <el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
        </el-select>
        <el-select v-model="q.materialId" placeholder="全部物资" clearable filterable style="width:220px" @change="load">
          <el-option v-for="m in materials" :key="m.materialId"
                     :label="`${m.materialCode} ${m.materialName}`" :value="m.materialId" />
        </el-select>
        <el-date-picker v-model="q.range" type="daterange" value-format="YYYY-MM-DD"
                        start-placeholder="入库开始日期" end-placeholder="入库结束日期"
                        style="width:260px" @change="load" />
        <el-button plain @click="resetQuery">重置</el-button>
        <div class="grow"></div>
        <el-button plain @click="doExport"><el-icon><Download /></el-icon>导出 Excel</el-button>
      </div>

      <el-row :gutter="12" style="margin-bottom:12px">
        <el-col :span="8">
          <div class="stat"><div class="k">入库成本记录</div><div class="v">{{ rows.length }}</div></div>
        </el-col>
        <el-col :span="8">
          <div class="stat"><div class="k">入库总数量</div><div class="v">{{ qty(totalQty) }}</div></div>
        </el-col>
        <el-col :span="8">
          <div class="stat"><div class="k">实际入库金额</div><div class="v accent">{{ money(totalAmount) }}</div></div>
        </el-col>
      </el-row>

      <el-table :data="paged" stripe show-summary :summary-method="summary">
        <el-table-column label="入库日期" width="120"><template #default="{ row: r }">{{ date(r.inDate) }}</template></el-table-column>
        <el-table-column prop="inNo" label="入库单号" width="145" />
        <el-table-column prop="poNo" label="订单号" width="145" />
        <el-table-column prop="supplierName" label="供应商" min-width="170" show-overflow-tooltip />
        <el-table-column prop="materialCode" label="物资编码" width="110" />
        <el-table-column prop="materialName" label="物资名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="入库数量" width="110" align="right">
          <template #default="{ row: r }"><span style="color:#16a34a">{{ qty(r.qty) }}</span></template>
        </el-table-column>
        <el-table-column label="入库单价" width="110" align="right"><template #default="{ row: r }">{{ money(r.inPrice) }}</template></el-table-column>
        <el-table-column label="入库金额" width="110" align="right">
          <template #default="{ row: r }"><b>{{ money(r.inAmount) }}</b></template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="入库仓库" width="110" />
        <el-table-column prop="operatorName" label="经办人" width="90" />
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '../api'
import { money, qty, date } from '../common'
import { exportExcel } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage } from 'element-plus'

const rows = ref([]), suppliers = ref([]), materials = ref([])
const q = reactive({ supplierId: null, materialId: null, range: null })
const { page, pageSize, total, paged, reset } = usePager(rows, 10)

const totalQty = computed(() => rows.value.reduce((s, r) => s + Number(r.qty || 0), 0))
const totalAmount = computed(() => rows.value.reduce((s, r) => s + Number(r.inAmount || 0), 0))

onMounted(async () => {
  suppliers.value = await api.suppliers()
  materials.value = await api.materials()
  load()
})
async function load() {
  rows.value = await api.recon({
    supplierId: q.supplierId || undefined,
    materialId: q.materialId || undefined,
    startDate: q.range?.[0], endDate: q.range?.[1]
  })
  reset()
}
function resetQuery() {
  q.supplierId = null; q.materialId = null; q.range = null
  load()
}
function summary({ columns }) {
  return columns.map((c, i) => {
    if (i === 0) return '合计'
    if (i === 7) return qty(totalQty.value)
    if (i === 9) return money(totalAmount.value)
    return ''
  })
}
const cols = [
  { label: '入库日期', prop: r => date(r.inDate) }, { label: '入库单号', prop: 'inNo' },
  { label: '订单号', prop: 'poNo' }, { label: '供应商', prop: 'supplierName', width: 30 },
  { label: '物资编码', prop: 'materialCode' }, { label: '物资名称', prop: 'materialName', width: 30 },
  { label: '单位', prop: 'unit' }, { label: '入库数量', prop: 'qty' },
  { label: '入库单价', prop: 'inPrice' }, { label: '入库金额', prop: 'inAmount' },
  { label: '入库仓库', prop: 'warehouseName' }, { label: '经办人', prop: 'operatorName' },
]
function doExport() {
  if (!rows.value.length) return ElMessage.warning('没有可导出的数据')
  exportExcel(cols, rows.value, '财务核对-入库依据', '财务核对')
}
</script>

<style scoped>
.stat { background: #f8fafc; border: 1px solid #e8eef8; border-radius: 10px; padding: 12px 16px; }
.stat .k { font-size: 12px; color: #64748b; }
.stat .v { font-size: 22px; font-weight: 700; color: #1e293b; margin-top: 4px; }
.stat .v.accent { color: #2563eb; }
</style>
