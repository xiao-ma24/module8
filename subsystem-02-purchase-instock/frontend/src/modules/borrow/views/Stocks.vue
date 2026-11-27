<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">库存查询</div>
        <div class="d">公共库存按批次展示；个人保管库存在入库模块查看</div>
      </div>
      
    </div>
    <el-card class="page-card fill">
      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索物资名称 / 编码" clearable style="width:260px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-checkbox v-model="lowOnly" @change="reset">仅看低于安全库存</el-checkbox>
        <div class="grow"></div>
        <el-tag type="info" effect="plain">公共库存 · 按批次</el-tag>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="refresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>

      <el-table :data="paged" stripe v-loading="loading" :row-class-name="rowClass">
        <el-table-column prop="materialCode" label="物资编码" width="120" />
        <el-table-column prop="materialName" label="物资名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="130" />
        <el-table-column prop="batchNo" label="批次号" width="170" show-overflow-tooltip />
        <el-table-column label="在库总量" width="110" align="right">
          <template #default="{ row: r }">{{ num(r.qtyOnHand) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column label="可用量" width="110" align="right">
          <template #default="{ row: r }">
            <b :style="{ color: Number(r.qtyAvailable) > 0 ? '#16a34a' : '#dc2626' }">{{ num(r.qtyAvailable) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="预占量" width="100" align="right">
          <template #default="{ row: r }">{{ num(r.qtyLocked) }}</template>
        </el-table-column>
        <el-table-column label="到期日期" width="120">
          <template #default="{ row: r }">
            <span :style="{ color: isExpiring(r.expireDate) ? '#dc2626' : '' }">
              {{ r.expireDate ? date(r.expireDate) : '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="入账日期" width="120">
          <template #default="{ row: r }">{{ r.inboundDate ? date(r.inboundDate) : '—' }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filtered.length" description="暂无库存数据" :image-size="70" />

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import api from '../api'
import { num, date } from '../common'
import { usePager } from '../../../composables/usePager'
import { exportExcel } from '../../../utils/export'

const list = ref([])
const loading = ref(false)
const kw = ref('')
const lowOnly = ref(false)

const filtered = computed(() => list.value.filter(r => {
  const kwOk = !kw.value || (r.materialName || '').includes(kw.value) || (r.materialCode || '').includes(kw.value)
  const lowOk = !lowOnly.value || Number(r.qtyAvailable) <= 0
  return kwOk && lowOk
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)

onMounted(refresh)
async function refresh() {
  loading.value = true
  try { list.value = await api.stocks() } finally { loading.value = false }
  reset()
}
const isExpiring = d => {
  if (!d) return false
  const diff = (new Date(d) - Date.now()) / 86400000
  return diff <= 30
}
const rowClass = ({ row }) => (Number(row.qtyAvailable) <= 0 ? 'low-row' : '')

const cols = [
  { label: '物资编码', prop: 'materialCode', width: 16 },
  { label: '物资名称', prop: 'materialName', width: 26 },
  { label: '仓库', prop: 'warehouseName', width: 16 },
  { label: '批次号', prop: 'batchNo', width: 22 },
  { label: '在库总量', prop: r => num(r.qtyOnHand) },
  { label: '可用量', prop: r => num(r.qtyAvailable) },
  { label: '预占量', prop: r => num(r.qtyLocked) },
  { label: '到期日期', prop: r => (r.expireDate ? date(r.expireDate) : '') },
]
function doExport() {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  exportExcel(cols, filtered.value, '库存台账', '库存')
}
</script>
