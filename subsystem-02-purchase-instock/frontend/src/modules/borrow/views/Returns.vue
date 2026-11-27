<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">归还验收</div>
        <div class="d">完好归还恢复可用量；损坏或缺失生成报损单并跳转报损处理，库管员一键核销扣减库存</div>
      </div>
      
    </div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane name="borrowed" :label="`在借清单 (${borrowed.length})`" />
        <el-tab-pane name="overdue" :label="`逾期清单 (${overdue.length})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索单号 / 物资 / 借用人" clearable style="width:260px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="grow"></div>
        <el-tag v-if="tab === 'overdue'" type="danger" effect="plain">按逾期天数倒序 · 需催还</el-tag>
        <el-tag v-else type="success" effect="plain">完好归还恢复可用量</el-tag>
        <el-button plain @click="doExport"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="refresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>

      <el-table :data="paged" stripe v-loading="loading" :row-class-name="rowClass">
        <el-table-column prop="borrowNo" label="借用单号" width="150" />
        <el-table-column prop="materialName" label="物资" min-width="170" show-overflow-tooltip />
        <el-table-column prop="borrowerName" label="借用人" width="95" />
        <el-table-column prop="deptName" label="部门" width="120" show-overflow-tooltip />
        <el-table-column label="借用 / 已还" width="115" align="right">
          <template #default="{ row: r }">
            {{ num(r.borrowQty) }} <span style="color:#94a3b8">/ {{ num(r.returnedQty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已核销" width="90" align="right">
          <template #default="{ row: r }">{{ num(r.writtenOffQty) }}</template>
        </el-table-column>
        <el-table-column label="预计归还" width="145">
          <template #default="{ row: r }">
            {{ date(r.dueDate) }}
            <el-tag v-if="r.overdue" size="small" type="danger" effect="dark" style="margin-left:4px">
              逾期{{ r.overdueDays }}天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row: r }">
            <el-tag :type="BORROW_TAG[r.borrowStatus]" size="small" effect="plain">{{ BORROW_STATUS[r.borrowStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row: r }">
            <el-button v-if="canReturn" type="primary" size="small" @click="openReturn(r)">归还验收</el-button>
            <span v-else style="color:#cbd5e1">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filtered.length"
                :description="tab === 'overdue' ? '没有逾期未还的物资' : '暂无在借记录'" :image-size="70" />

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>

    <el-dialog v-model="dlg.open" title="归还验收（支持分次归还）" width="620px">
      <el-descriptions v-if="dlg.row" :column="2" border size="small">
        <el-descriptions-item label="借用单号">{{ dlg.row.borrowNo }}</el-descriptions-item>
        <el-descriptions-item label="借用人">{{ dlg.row.borrowerName }}</el-descriptions-item>
        <el-descriptions-item label="物资">{{ dlg.row.materialName }}</el-descriptions-item>
        <el-descriptions-item label="批次">{{ dlg.row.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="借用数量">{{ num(dlg.row.borrowQty) }}</el-descriptions-item>
        <el-descriptions-item label="未归还数量">
          <b style="color:#dc2626">{{ num(remaining) }}</b>
        </el-descriptions-item>
      </el-descriptions>

      <el-form :model="dlg.form" label-width="110px" style="margin-top:16px">
        <el-form-item label="本次归还数量" required>
          <el-input-number v-model="dlg.form.returnQty" :min="0.001" :max="remaining" :precision="3" :step="1"
                           style="width:170px" />
        </el-form-item>
        <el-form-item label="验收结果" required>
          <el-radio-group v-model="dlg.form.acceptResult">
            <el-radio-button :value="1">完好入库</el-radio-button>
            <el-radio-button :value="2">损坏核销</el-radio-button>
            <el-radio-button :value="3">缺失核销</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="情况说明" :required="dlg.form.acceptResult !== 1">
          <el-input v-model="dlg.form.remark" type="textarea" :rows="2"
                    :placeholder="dlg.form.acceptResult === 1 ? '选填' : '损坏/缺失必须填写，将作为报损原因'" />
        </el-form-item>
        <el-alert v-if="dlg.form.acceptResult !== 1" type="warning" :closable="false" show-icon
                  title="损坏/缺失将生成报损单并跳转报损处理，库管员一键核销后立即扣减在库总量" />
      </el-form>

      <template #footer>
        <el-button @click="dlg.open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doReturn">确认验收</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import api, { getUser } from '../api'
import { BORROW_STATUS, BORROW_TAG, num, date } from '../common'
import { usePager } from '../../../composables/usePager'
import { exportExcel } from '../../../utils/export'

const route = useRoute()
const router = useRouter()
const user = getUser()
const canReturn = computed(() => user?.roleCode === 'STORE_KEEPER')

const tab = ref(route.query.tab === 'overdue' ? 'overdue' : 'borrowed')
const kw = ref('')
const loading = ref(false)
const borrowed = ref([])
const overdue = ref([])
const dlg = reactive({ open: false, row: null, form: {} })
const saving = ref(false)

const filtered = computed(() => (tab.value === 'overdue' ? overdue.value : borrowed.value).filter(r => {
  if (!kw.value) return true
  return [r.borrowNo, r.materialName, r.borrowerName].filter(Boolean).join(' ').includes(kw.value)
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const remaining = computed(() => {
  if (!dlg.row) return 0
  return Math.max(0, Number(dlg.row.borrowQty || 0) - Number(dlg.row.returnedQty || 0) - Number(dlg.row.writtenOffQty || 0))
})
const rowClass = ({ row }) => (row.overdue ? 'low-row' : '')

onMounted(refresh)
async function refresh() {
  loading.value = true
  try {
    overdue.value = await api.overdue()
    borrowed.value = await api.borrowed()
  } finally { loading.value = false }
  reset()
}

function openReturn(r) {
  dlg.row = r
  dlg.form = { returnQty: 1, acceptResult: 1, remark: '' }
  dlg.form.returnQty = Math.max(0.001, Number(r.borrowQty) - Number(r.returnedQty) - Number(r.writtenOffQty))
  dlg.open = true
}
async function doReturn() {
  if (!dlg.form.returnQty || dlg.form.returnQty <= 0) return ElMessage.warning('归还数量必须大于 0')
  if (dlg.form.acceptResult !== 1 && !dlg.form.remark) return ElMessage.warning('损坏或缺失必须填写情况说明')
  saving.value = true
  try {
    const r = await api.doReturn({
      borrowId: dlg.row.borrowId, returnQty: dlg.form.returnQty,
      acceptResult: dlg.form.acceptResult, remark: dlg.form.remark
    })
    ElMessage.success(r.result || '归还已登记')
    dlg.open = false
    if (dlg.form.acceptResult !== 1) {
      // 损坏/缺失已转报损单，直接跳到报损处理由库管员一键处理
      router.push('/borrow/losses')
    } else {
      refresh()
    }
  } finally { saving.value = false }
}

const cols = [
  { label: '借用单号', prop: 'borrowNo', width: 22 },
  { label: '物资名称', prop: 'materialName', width: 26 },
  { label: '借用人', prop: 'borrowerName' },
  { label: '部门', prop: 'deptName', width: 18 },
  { label: '借用数量', prop: r => num(r.borrowQty) },
  { label: '已归还', prop: r => num(r.returnedQty) },
  { label: '已核销', prop: r => num(r.writtenOffQty) },
  { label: '预计归还', prop: r => date(r.dueDate) },
  { label: '逾期天数', prop: r => (r.overdue ? r.overdueDays : 0) },
  { label: '状态', prop: r => BORROW_STATUS[r.borrowStatus] },
]
function doExport() {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  exportExcel(cols, filtered.value, tab.value === 'overdue' ? '逾期未还清单' : '在借清单', '借用')
}
</script>
