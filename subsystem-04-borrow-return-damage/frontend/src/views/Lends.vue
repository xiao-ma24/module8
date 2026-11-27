<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">借出登记</div>
        <div class="d">借出只减少可用量、在库总量不变；批次按先到期先出选择</div>
      </div>
      
    </div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane name="pending" :label="`待借出 (${pending.length})`" />
        <el-tab-pane name="done" :label="`借出记录 (${outs.length})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索单号 / 物资 / 借用人" clearable style="width:260px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="grow"></div>
        <el-tag type="info" effect="plain">借出只减可用量，在库总量不变</el-tag>
        <el-button plain @click="doExport"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="refresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>

      <el-table v-if="tab === 'pending'" :data="paged" stripe v-loading="loading">
        <el-table-column prop="borrowNo" label="借用单号" width="150" />
        <el-table-column prop="materialName" label="物资" min-width="170" show-overflow-tooltip />
        <el-table-column prop="borrowerName" label="借用人" width="95" />
        <el-table-column prop="deptName" label="部门" width="120" show-overflow-tooltip />
        <el-table-column label="借用数量" width="110" align="right">
          <template #default="{ row: r }">{{ num(r.borrowQty) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column label="可借库存" width="110" align="right">
          <template #default="{ row: r }">
            <span :style="{ color: Number(r.availableQty) < Number(r.borrowQty) ? '#dc2626' : '#16a34a' }">
              {{ num(r.availableQty) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="预计归还" width="120">
          <template #default="{ row: r }">{{ date(r.dueDate) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row: r }">
            <el-button type="primary" size="small" @click="openLend(r)">借出</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="paged" stripe>
        <el-table-column prop="outNo" label="出库单号" width="150" />
        <el-table-column prop="borrowNo" label="借用单号" width="150" />
        <el-table-column prop="materialName" label="物资" min-width="150" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次" width="170" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column label="数量" width="100" align="right">
          <template #default="{ row: r }">{{ num(r.outQty) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column prop="keeperName" label="经办人" width="95" />
        <el-table-column label="借出时间" width="140">
          <template #default="{ row: r }">{{ dt(r.outTime) }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filtered.length"
                :description="tab === 'pending' ? '暂无待借出单据' : '暂无借出记录'" :image-size="70" />

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>

    <el-dialog v-model="dlg.open" title="借出登记" width="640px">
      <el-descriptions v-if="dlg.row" :column="2" border size="small">
        <el-descriptions-item label="借用单号">{{ dlg.row.borrowNo }}</el-descriptions-item>
        <el-descriptions-item label="借用人">{{ dlg.row.borrowerName }}</el-descriptions-item>
        <el-descriptions-item label="物资">{{ dlg.row.materialName }}</el-descriptions-item>
        <el-descriptions-item label="借用数量">{{ num(dlg.row.borrowQty) }} {{ dlg.row.unit }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ dlg.row.deptName }}</el-descriptions-item>
        <el-descriptions-item label="预计归还">{{ date(dlg.row.dueDate) }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="dlg.form" label-width="100px" style="margin-top:16px">
        <!-- 批次由物资决定，仓库仅用于筛选；默认「全部仓库」避免选错仓库后列表为空 -->
        <el-form-item label="出库批次" required>
          <el-select v-model="dlg.form.batchKey" placeholder="按先到期先出选择批次（可跨仓库）"
                     style="width:100%" :no-data-text="'该物资暂无可借批次（详见下方说明）'">
            <el-option v-for="b in filteredBatches" :key="`${b.warehouseId}::${b.batchNo}`"
                       :value="`${b.warehouseId}::${b.batchNo}`"
                       :label="`${b.warehouseName} · ${b.batchNo}（可用 ${num(b.qtyAvailable)}${b.expireDate ? '，到期 ' + date(b.expireDate) : ''}）`">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span>{{ b.warehouseName }} · {{ b.batchNo }}</span>
                <span style="color:#16a34a;font-size:12px">
                  可用 {{ num(b.qtyAvailable) }}{{ b.expireDate ? ' · 到期 ' + date(b.expireDate) : '' }}
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="warehouseOptions.length > 1" label="仓库筛选">
          <el-select v-model="dlg.form.warehouseId" placeholder="全部仓库" clearable style="width:240px">
            <el-option v-for="w in warehouseOptions" :key="w.warehouseId" :value="w.warehouseId"
                       :label="`${w.warehouseName}（可用 ${num(w.qty)}）`" />
          </el-select>
          <span style="font-size:12px;color:#94a3b8;margin-left:10px">只列该物资有可用批次的仓库</span>
        </el-form-item>
        <el-form-item v-else-if="warehouseOptions.length === 1" label="库存仓库">
          <el-tag type="info" effect="plain">
            {{ warehouseOptions[0].warehouseName }}（可用 {{ num(warehouseOptions[0].qty) }}）
          </el-tag>
          <span style="font-size:12px;color:#94a3b8;margin-left:10px">该物资仅此仓库有可用批次</span>
        </el-form-item>

        <el-alert v-if="!batches.length" type="warning" :closable="false" show-icon
                  title="该物资当前没有可借批次"
                  :description="emptyReason" />

        <el-alert v-else-if="selectedBatch && Number(selectedBatch.qtyAvailable) < Number(dlg.row?.borrowQty)"
                  type="warning" :closable="false" show-icon
                  :title="`所选批次可用 ${num(selectedBatch.qtyAvailable)}，不足本单借用数量 ${num(dlg.row?.borrowQty)}`"
                  description="请更换批次，或先由采购/入库补足库存后再办理借出" />

        <el-alert v-else-if="selectedBatch" type="success" :closable="false" show-icon
                  :title="`将借出 ${num(dlg.row?.borrowQty)} ${dlg.row?.unit}：${selectedBatch.warehouseName} / ${selectedBatch.batchNo}`"
                  description="借出后在库总量不变、可用量减少；完好归还时恢复可用量。" />
      </el-form>

      <template #footer>
        <el-button @click="dlg.open = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selectedBatch" @click="doLend">确认借出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import api from '../api'
import { num, date, dt } from '../common'
import { usePager } from '../composables/usePager'
import { exportExcel } from '../utils/export'

const tab = ref('pending')
const kw = ref('')
const loading = ref(false)
const pending = ref([])
const outs = ref([])
const warehouses = ref([])
const batches = ref([])
const dlg = reactive({ open: false, row: null, form: {} })
const saving = ref(false)

const filtered = computed(() => (tab.value === 'pending' ? pending.value : outs.value).filter(r => {
  if (!kw.value) return true
  return [r.borrowNo, r.outNo, r.materialName, r.borrowerName].filter(Boolean).join(' ').includes(kw.value)
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)

/** 仓库只作筛选：选了就过滤，没选则展示该物资在所有实体仓的可用批次 */
/** 只列出该物资实际有可用批次的仓库（避免选中无库存仓库导致批次列表为空） */
const warehouseOptions = computed(() => {
  const map = new Map()
  for (const b of batches.value) {
    const cur = map.get(b.warehouseId) || { warehouseId: b.warehouseId, warehouseName: b.warehouseName, qty: 0 }
    cur.qty += Number(b.qtyAvailable || 0)
    map.set(b.warehouseId, cur)
  }
  return [...map.values()]
})

const filteredBatches = computed(() => {
  const wid = dlg.form.warehouseId
  return wid ? batches.value.filter(b => b.warehouseId === wid) : batches.value
})
const selectedBatch = computed(() => {
  if (!dlg.form.batchKey) return null
  const [wid, ...rest] = dlg.form.batchKey.split('::')
  const bn = rest.join('::')
  return batches.value.find(b => String(b.warehouseId) === wid && b.batchNo === bn) || null
})
const emptyReason = computed(() => {
  if (!dlg.row) return ''
  if (dlg.form.warehouseId) return '所选仓库没有该物资的可用批次，可把「仓库筛选」清空查看其它仓库。'
  return '可能原因：尚未验收入库、批次已过期被冻结，或该物资只存在于个人保管仓（不参与公共借用）。可到「库存查询」核对。'
})

onMounted(refresh)
async function refresh() {
  loading.value = true
  try {
    pending.value = await api.pendingLend()
    outs.value = await api.outbounds()
  } finally { loading.value = false }
  reset()
}

async function openLend(r) {
  dlg.row = r
  dlg.form = { warehouseId: null, batchKey: '' }
  // 关键：不传 warehouseId，拉该物资在所有实体仓的可用批次（后端已按先到期先出排序）
  batches.value = (await api.batches(r.materialId)).batches || []
  if (batches.value.length) {
    const first = batches.value[0]
    dlg.form.batchKey = `${first.warehouseId}::${first.batchNo}`
  }
  dlg.open = true
}

async function doLend() {
  const b = selectedBatch.value
  if (!b) return ElMessage.warning('请选择借出批次')
  saving.value = true
  try {
    await api.lend({ borrowId: dlg.row.borrowId, warehouseId: b.warehouseId, batchNo: b.batchNo })
    ElMessage.success(`已从「${b.warehouseName} / ${b.batchNo}」借出，可用量已扣减`)
    dlg.open = false
    refresh()
  } finally { saving.value = false }
}

const cols = [
  { label: '出库单号', prop: 'outNo', width: 22 },
  { label: '借用单号', prop: 'borrowNo', width: 22 },
  { label: '物资名称', prop: 'materialName', width: 26 },
  { label: '批次号', prop: 'batchNo', width: 22 },
  { label: '仓库', prop: 'warehouseName', width: 16 },
  { label: '数量', prop: r => num(r.outQty) },
  { label: '经办人', prop: 'keeperName' },
  { label: '借出时间', prop: r => dt(r.outTime) },
]
function doExport() {
  if (!outs.value.length) return ElMessage.warning('没有可导出的数据')
  exportExcel(cols, outs.value, '借用出库记录', '借出')
}
</script>
