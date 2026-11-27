<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">出库办理</div>
        <div class="d">按核准数量出库：选择批次后扣减在库与可用量，并累加部门月度已用额度</div>
      </div>
      
    </div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane name="pending" :label="`待出库 (${pending.length})`" />
        <el-tab-pane name="done" :label="`出库记录 (${outs.length})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索单号 / 物资 / 申请人" clearable style="width:260px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="grow"></div>
        <el-tag v-if="tab === 'pending'" type="warning" effect="plain">按「先到期先出」选批</el-tag>
        <el-button plain @click="doExport"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="refresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>

      <el-table v-if="tab === 'pending'" :data="paged" stripe v-loading="loading">
        <el-table-column prop="reqNo" label="领用单号" width="150" />
        <el-table-column prop="materialName" label="物资" min-width="170" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="部门" width="130" show-overflow-tooltip />
        <el-table-column label="核准数量" width="110" align="right">
          <template #default="{ row: r }">{{ num(r.qtyApprove || r.qtyApply) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column label="可领库存" width="110" align="right">
          <template #default="{ row: r }">
            <span :style="{ color: Number(r.availableQty) < Number(r.qtyApprove || r.qtyApply) ? '#dc2626' : '#16a34a' }">
              {{ num(r.availableQty) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row: r }">{{ money(r.amount) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row: r }">
            <el-button type="primary" size="small" @click="openOut(r)">出库</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="paged" stripe>
        <el-table-column prop="outNo" label="出库单号" width="150" />
        <el-table-column prop="reqNo" label="领用单号" width="150" />
        <el-table-column prop="materialName" label="物资" min-width="150" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次" width="170" show-overflow-tooltip />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column label="实发数量" width="110" align="right">
          <template #default="{ row: r }">{{ num(r.outQty) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column prop="keeperName" label="经办人" width="100" />
        <el-table-column label="出库时间" width="140">
          <template #default="{ row: r }">{{ dt(r.outTime) }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filtered.length"
                :description="tab === 'pending' ? '暂无待出库单据' : '暂无出库记录'" :image-size="70" />

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>

    <el-dialog v-model="dlg.open" title="出库确认" width="640px">
      <el-descriptions v-if="dlg.row" :column="2" border size="small">
        <el-descriptions-item label="领用单号">{{ dlg.row.reqNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ dlg.row.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="物资">{{ dlg.row.materialName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ dlg.row.deptName }}</el-descriptions-item>
        <el-descriptions-item label="核准数量">{{ num(dlg.row.qtyApprove || dlg.row.qtyApply) }} {{ dlg.row.unit }}</el-descriptions-item>
        <el-descriptions-item label="可领库存">{{ num(dlg.row.availableQty) }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="dlg.form" label-width="100px" style="margin-top:16px">
        <!-- 批次由物资决定，仓库仅用于筛选；默认「全部仓库」避免选错仓库后列表为空 -->
        <el-form-item label="出库批次" required>
          <el-select v-model="dlg.form.batchKey" placeholder="按先到期先出选择批次（可跨仓库）"
                     style="width:100%" no-data-text="该物资暂无可领批次（详见下方说明）">
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
        <el-form-item label="实发数量" required>
          <el-input-number v-model="dlg.form.outQty" :min="0.001" :precision="3" :step="1"
                           :max="maxOutQty" style="width:170px" />
          <span style="font-size:12px;color:#94a3b8;margin-left:10px">
            可小于核准数量（部分出库）{{ selectedBatch ? `，本批次可用 ${num(selectedBatch.qtyAvailable)}` : '' }}
          </span>
        </el-form-item>

        <el-alert v-if="!batches.length" type="warning" :closable="false" show-icon
                  title="该物资当前没有可领批次" :description="emptyReason" />
        <el-alert v-else-if="selectedBatch && Number(selectedBatch.qtyAvailable) < approvedQty"
                  type="warning" :closable="false" show-icon
                  :title="`所选批次可用 ${num(selectedBatch.qtyAvailable)}，少于核准数量 ${num(approvedQty)}`"
                  description="可改为部分出库（实发数量不超过批次可用量），或先补足库存后再出库" />
      </el-form>

      <template #footer>
        <el-button @click="dlg.open = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selectedBatch" @click="doOut">确认出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import api from '../api'
import { money, num, dt, date } from '../common'
import { usePager } from '../../../composables/usePager'
import { exportExcel } from '../../../utils/export'

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
  return [r.reqNo, r.outNo, r.materialName, r.applicantName].filter(Boolean).join(' ').includes(kw.value)
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)

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
const approvedQty = computed(() => Number(dlg.row?.qtyApprove || dlg.row?.qtyApply || 0))
const maxOutQty = computed(() => {
  const byBatch = selectedBatch.value ? Number(selectedBatch.value.qtyAvailable) : approvedQty.value
  return Math.max(0.001, Math.min(approvedQty.value || byBatch, byBatch))
})
const emptyReason = computed(() => {
  if (!dlg.row) return ''
  if (dlg.form.warehouseId) return '所选仓库没有该物资的可用批次，可把「仓库筛选」清空查看其它仓库。'
  return '可能原因：尚未验收入库、批次已过期被冻结，或该物资只存在于个人保管仓（不参与公共领用）。可到「库存查询」核对。'
})

onMounted(refresh)
async function refresh() {
  loading.value = true
  try {
    pending.value = await api.pendingOutbound()
    outs.value = await api.outbounds()
  } finally { loading.value = false }
  reset()
}

async function openOut(r) {
  dlg.row = r
  dlg.form = { warehouseId: null, batchKey: '', outQty: Number(r.qtyApprove || r.qtyApply) }
  // 关键：不传 warehouseId，拉该物资在所有实体仓的可用批次（后端已按先到期先出排序）
  batches.value = (await api.batches(r.materialId)).batches || []
  if (batches.value.length) {
    const first = batches.value[0]
    dlg.form.batchKey = `${first.warehouseId}::${first.batchNo}`
    dlg.form.outQty = Math.min(Number(r.qtyApprove || r.qtyApply), Number(first.qtyAvailable))
  }
  dlg.open = true
}

async function doOut() {
  const b = selectedBatch.value
  if (!b) return ElMessage.warning('请选择出库批次')
  if (Number(dlg.form.outQty) > Number(b.qtyAvailable))
    return ElMessage.warning(`实发数量不能超过该批次可用量 ${num(b.qtyAvailable)}`)
  saving.value = true
  try {
    await api.confirmOutbound({
      reqId: dlg.row.reqId, warehouseId: b.warehouseId,
      batchNo: b.batchNo, outQty: dlg.form.outQty
    })
    ElMessage.success(`已从「${b.warehouseName} / ${b.batchNo}」出库，库存已扣减`)
    dlg.open = false
    refresh()
  } finally { saving.value = false }
}

const cols = [
  { label: '出库单号', prop: 'outNo', width: 22 },
  { label: '领用单号', prop: 'reqNo', width: 22 },
  { label: '物资名称', prop: 'materialName', width: 26 },
  { label: '批次号', prop: 'batchNo', width: 22 },
  { label: '仓库', prop: 'warehouseName', width: 16 },
  { label: '实发数量', prop: r => num(r.outQty) },
  { label: '经办人', prop: 'keeperName' },
  { label: '出库时间', prop: r => dt(r.outTime) },
]
function doExport() {
  if (!outs.value.length) return ElMessage.warning('没有可导出的数据')
  exportExcel(cols, outs.value, '领用出库记录', '出库')
}
</script>
