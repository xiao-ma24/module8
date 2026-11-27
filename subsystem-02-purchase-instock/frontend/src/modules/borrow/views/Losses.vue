<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">报损处理</div>
        <div class="d">归还损坏或缺失自动生成报损单，库管员一键报损处理并立即扣减库存</div>
      </div>
    </div>

    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane name="all" :label="`全部 (${list.length})`" />
        <el-tab-pane name="0" :label="`待审批 (${countOf(0)})`" />
        <el-tab-pane name="1" :label="`已通过 (${countOf(1)})`" />
        <el-tab-pane name="2" :label="`已驳回 (${countOf(2)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索报损单号 / 物资 / 借用单号" clearable style="width:280px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="grow"></div>
        <el-tag type="info" effect="plain">核销才扣在库量 · executed 防重复</el-tag>
        <el-button plain @click="doExport"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="refresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>

      <el-table :data="paged" stripe v-loading="loading">
        <el-table-column prop="lossNo" label="报损单号" width="150" />
        <el-table-column prop="borrowNo" label="来源借用单" width="150" />
        <el-table-column prop="materialName" label="物资" min-width="150" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次" width="140" show-overflow-tooltip />
        <el-table-column label="来源" width="110" align="center">
          <template #default="{ row: r }">
            <el-tag size="small" effect="plain" :type="r.sourceType === 1 ? 'warning' : 'info'">
              {{ LOSS_SOURCE[r.sourceType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报损数量" width="100" align="right">
          <template #default="{ row: r }">{{ num(r.lossQty) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column label="验收结论" width="105" align="center">
          <template #default="{ row: r }">
            <el-tag v-if="r.conditionCode" size="small" type="danger" effect="plain">{{ CONDITION[r.conditionCode] }}</el-tag>
            <span v-else style="color:#cbd5e1">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="报损原因" min-width="130" show-overflow-tooltip />
        <el-table-column label="审批" width="90" align="center">
          <template #default="{ row: r }">
            <el-tag :type="LOSS_STATUS_TAG[r.approveStatus]" size="small" effect="plain">{{ LOSS_STATUS[r.approveStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处置结论" width="110" align="center">
          <template #default="{ row: r }">{{ r.disposeResult ? DISPOSE[r.disposeResult] : '—' }}</template>
        </el-table-column>
        <el-table-column label="核销" width="90" align="center">
          <template #default="{ row: r }">
            <el-tag v-if="r.executed === 1" type="success" size="small" effect="plain">已核销</el-tag>
            <el-tag v-else type="info" size="small" effect="plain">未核销</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row: r }">
            <el-button v-if="canApprove(r)" type="primary" size="small" @click="openDlg(r)">审批</el-button>
            <el-button v-else-if="canExec(r)" type="danger" size="small" :loading="saving && actingId === r.lossId"
                       @click="doLoss(r)">报损处理</el-button>
            <el-button v-else link type="primary" @click="openDlg(r)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filtered.length" description="暂无报损单" :image-size="70" />

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>

    <!-- 报损审批与核销（对齐报告图3-42：同一弹窗内完成审批，库管员在同一弹窗核销） -->
    <el-dialog v-model="dlg.open" title="报损审批与核销" width="620px">
      <template v-if="dlg.row">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报损单号">{{ dlg.row.lossNo }}</el-descriptions-item>
          <el-descriptions-item label="借用人">{{ dlg.row.borrowerName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="物资">{{ dlg.row.materialName }}</el-descriptions-item>
          <el-descriptions-item label="批次">{{ dlg.row.batchNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="报损数量">{{ num(dlg.row.lossQty) }} {{ dlg.row.unit }}</el-descriptions-item>
          <el-descriptions-item label="来源单据">{{ dlg.row.borrowNo || LOSS_SOURCE[dlg.row.sourceType] }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-width="100px" style="margin-top:16px">
          <el-form-item label="报损原因">
            <el-input :model-value="reasonText" type="textarea" :rows="2" readonly />
          </el-form-item>

          <!-- 审批区：部门负责人 + 待审批 -->
          <template v-if="mode === 'approve'">
            <el-form-item label="审批结论">
              <el-radio-group v-model="form.result">
                <el-radio :value="1">通过</el-radio>
                <el-radio :value="2">驳回</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.result === 1" label="处置结论" required>
              <el-select v-model="form.disposeResult" style="width:100%">
                <el-option v-for="(v, k) in DISPOSE" :key="k" :label="v" :value="Number(k)" />
              </el-select>
              <div v-if="form.disposeResult === 1" style="font-size:12px;color:#94a3b8;margin-top:6px">
                选择「折价赔偿」时，按「报损数量 × 参考单价」计算赔偿金额，作为推送财务系统的依据
              </div>
            </el-form-item>
            <el-form-item label="审批意见" :required="form.result === 2">
              <el-input v-model="form.opinion" type="textarea" :rows="3"
                        :placeholder="form.result === 2 ? '驳回必须填写意见' : '选填'" />
            </el-form-item>
          </template>

          <!-- 只读：库管员直接处理走列表按钮，不再填写审批表单 -->
          <template v-else>
            <el-form-item label="审批状态">
              <el-tag :type="LOSS_STATUS_TAG[dlg.row.approveStatus]">{{ LOSS_STATUS[dlg.row.approveStatus] }}</el-tag>
              <el-tag v-if="dlg.row.executed === 1" type="success" style="margin-left:6px">已核销</el-tag>
            </el-form-item>
            <el-form-item label="处置结论">
              {{ dlg.row.disposeResult ? DISPOSE[dlg.row.disposeResult] : '—' }}
            </el-form-item>
          </template>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="dlg.open = false">取消</el-button>
        <el-button v-if="mode === 'approve'" type="primary" :loading="saving" @click="doApprove">提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import api, { getUser } from '../api'
import { LOSS_STATUS, LOSS_STATUS_TAG, LOSS_SOURCE, DISPOSE, CONDITION, num } from '../common'
import { usePager } from '../../../composables/usePager'
import { exportExcel } from '../../../utils/export'

const user = getUser()
const isHead = computed(() => ['DEPT_HEAD', 'ADMIN'].includes(user?.roleCode))
const isKeeper = computed(() => user?.roleCode === 'STORE_KEEPER')

const list = ref([])
const loading = ref(false)
const tab = ref('all')
const kw = ref('')
const saving = ref(false)
const actingId = ref(null)
const dlg = reactive({ open: false, row: null })
const form = reactive({ result: 1, disposeResult: 1, opinion: '' })

const filtered = computed(() => list.value.filter(r => {
  const tabOk = tab.value === 'all' || r.approveStatus === +tab.value
  const kwOk = !kw.value || [r.lossNo, r.materialName, r.borrowNo].filter(Boolean).join(' ').includes(kw.value)
  return tabOk && kwOk
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const countOf = n => list.value.filter(r => r.approveStatus === n).length

const canApprove = r => isHead.value && r.approveStatus === 0
const canExec = r => isKeeper.value && r.approveStatus !== 2 && r.executed === 0
const mode = computed(() => {
  if (!dlg.row) return 'view'
  if (canApprove(dlg.row)) return 'approve'
  return 'view'
})
const reasonText = computed(() => {
  if (!dlg.row) return ''
  const c = dlg.row.conditionCode
  return c ? `${dlg.row.reason}（验收结论：${CONDITION[c]}）` : dlg.row.reason
})

onMounted(refresh)
async function refresh() {
  loading.value = true
  try { list.value = await api.losses() } finally { loading.value = false }
}
function openDlg(r) {
  dlg.row = r
  form.result = 1
  form.disposeResult = r.disposeResult || 1
  form.opinion = ''
  dlg.open = true
}
async function doApprove() {
  saving.value = true
  try {
    await api.approveLoss(dlg.row.lossId, {
      result: form.result,
      disposeResult: form.result === 1 ? form.disposeResult : null,
      opinion: form.opinion
    })
    ElMessage.success('审批已提交')
    dlg.open = false
    refresh()
  } finally { saving.value = false }
}
/** 库管员一键报损处理：跳过审批表单，确认后直接核销并扣减库存 */
async function doLoss(r) {
  try {
    await ElMessageBox.confirm(
      `确认对「${r.materialName}」报损 ${num(r.lossQty)} ${r.unit}（批次 ${r.batchNo || '—'}）？确认后立即扣减库存，不可撤销。`,
      '报损处理确认',
      { type: 'warning', confirmButtonText: '确认报损', cancelButtonText: '取消' }
    )
  } catch { return }
  actingId.value = r.lossId
  saving.value = true
  try {
    const res = await api.executeLoss(r.lossId, { disposeResult: r.disposeResult || 3 })
    ElMessage.success(res.compensateAmount ? `报损完成，赔偿金额 ¥${res.compensateAmount}` : '报损完成，库存已扣减')
    refresh()
  } finally {
    saving.value = false
    actingId.value = null
  }
}

const cols = [
  { label: '报损单号', prop: 'lossNo', width: 22 },
  { label: '来源借用单', prop: 'borrowNo', width: 22 },
  { label: '物资名称', prop: 'materialName', width: 26 },
  { label: '批次号', prop: 'batchNo', width: 20 },
  { label: '报损数量', prop: r => num(r.lossQty) },
  { label: '验收结论', prop: r => (r.conditionCode ? CONDITION[r.conditionCode] : '') },
  { label: '报损原因', prop: 'reason', width: 26 },
  { label: '审批状态', prop: r => LOSS_STATUS[r.approveStatus] },
  { label: '处置结论', prop: r => (r.disposeResult ? DISPOSE[r.disposeResult] : '') },
  { label: '是否核销', prop: r => (r.executed === 1 ? '已核销' : '未核销') },
]
function doExport() {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  exportExcel(cols, filtered.value, '报损处置清单', '报损')
}
</script>
