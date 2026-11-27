<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">领用申请</div>
        <div class="d">提交时校验可用库存与部门月度额度；审批通过后由库管员按先到期先出出库</div>
      </div>
      <el-button v-if="canApply" type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新建领用申请</el-button>
    </div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name"
                     :label="`${t.label} (${countOf(t.name)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索单号 / 物资 / 申请人" clearable style="width:260px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="scopeStatus" placeholder="全部状态" clearable style="width:130px" @change="reset">
          <el-option v-for="(v, k) in REQ_STATUS" :key="k" :label="v" :value="Number(k)" />
        </el-select>
        <div class="grow"></div>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
        <el-button v-if="canApply" type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增领用申请</el-button>
      </div>

      <el-table :data="paged" stripe v-loading="loading">
        <el-table-column prop="reqNo" label="领用单号" width="150" />
        <el-table-column label="物资" min-width="180" show-overflow-tooltip>
          <template #default="{ row: r }">
            {{ r.materialName }}
            <el-tag v-if="r.isControlled === 1" size="small" type="danger" effect="plain" style="margin-left:6px">管控</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="deptName" label="申请部门" width="130" show-overflow-tooltip />
        <el-table-column label="申请 / 核准" width="120" align="right">
          <template #default="{ row: r }">
            {{ num(r.qtyApply) }} <span style="color:#94a3b8">/ {{ r.qtyApprove ? num(r.qtyApprove) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row: r }">{{ money(r.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row: r }">
            <el-tag :type="REQ_TAG[r.reqStatus]" size="small" effect="plain">{{ REQ_STATUS[r.reqStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="140">
          <template #default="{ row: r }">{{ dt(r.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row: r }">
            <el-link type="primary" @click="openDetail(r)">详情</el-link>
            <el-link v-if="isOwner(r) && r.reqStatus === 1" type="warning" style="margin-left:10px" @click="withdraw(r)">撤回</el-link>
            <el-link v-if="isOwner(r) && r.reqStatus === 5" type="success" style="margin-left:10px" @click="receive(r)">签收</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <!-- 新增申请 -->
    <el-dialog v-model="create.open" title="新增领用申请" width="620px">
      <el-form :model="create.form" label-width="105px">
        <el-form-item label="领用物资" required>
          <el-select v-model="create.form.materialId" filterable remote :remote-method="searchMaterials"
                     placeholder="输入名称或编码检索（仅低值易耗品）" style="width:100%" @change="onMaterial">
            <el-option v-for="m in materials" :key="m.materialId" :value="m.materialId"
                       :label="`${m.materialName}（${m.materialCode}）`">
              <span>{{ m.materialName }}</span>
              <span style="float:right;color:#94a3b8">{{ money(m.refPrice) }}/{{ m.unit }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="可领库存" v-if="create.form.materialId">
          <el-tag type="success" effect="plain">{{ num(available) }} {{ currentMaterial?.unit }}</el-tag>
          <span style="font-size:12px;color:#94a3b8;margin-left:8px">审批通过后按先到期先出出库</span>
        </el-form-item>
        <el-form-item label="申请数量" required>
          <el-input-number v-model="create.form.qtyApply" :min="0.001" :precision="3" :step="1" style="width:170px" />
          <span style="font-size:12px;color:#94a3b8;margin-left:10px" v-if="currentMaterial">
            预估 {{ money(create.form.qtyApply * currentMaterial.refPrice) }}（单价 {{ money(currentMaterial.refPrice) }}）
          </span>
        </el-form-item>
        <el-alert v-if="currentMaterial?.isControlled === 1" type="warning" :closable="false" show-icon
                  title="该物资为管控物资，审批时须填写加签意见" />
      </el-form>
      <template #footer>
        <el-button @click="create.open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detail.open" title="领用单详情" width="680px">
      <template v-if="detail.row">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="领用单号">{{ detail.row.reqNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="REQ_TAG[detail.row.reqStatus]" size="small">{{ REQ_STATUS[detail.row.reqStatus] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.row.applicantName }}（{{ detail.row.applicantNo }}）</el-descriptions-item>
          <el-descriptions-item label="申请部门">{{ detail.row.deptName }}</el-descriptions-item>
          <el-descriptions-item label="物资">{{ detail.row.materialName }}</el-descriptions-item>
          <el-descriptions-item label="物资编码">{{ detail.row.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="申请数量">{{ num(detail.row.qtyApply) }} {{ detail.row.unit }}</el-descriptions-item>
          <el-descriptions-item label="核准数量">{{ detail.row.qtyApprove ? num(detail.row.qtyApprove) : '—' }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ money(detail.row.amount) }}</el-descriptions-item>
          <el-descriptions-item label="出库单号">{{ detail.row.outNo || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider style="margin:16px 0 12px">审批留痕</el-divider>
        <el-timeline v-if="detail.logs.length">
          <el-timeline-item v-for="l in detail.logs" :key="l.recordId" :timestamp="dt(l.approveTime)"
                            :type="l.approveResult === 1 ? 'success' : 'danger'">
            <b>{{ l.approverName }}</b> {{ l.approveResult === 1 ? '通过' : '驳回' }}
            <div style="font-size:12px;color:#94a3b8" v-if="l.opinion">意见：{{ l.opinion }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无审批记录" :image-size="60" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download, Printer } from '@element-plus/icons-vue'
import api, { getUser } from '../api'
import { REQ_STATUS, REQ_TAG, money, num, dt } from '../common'
import { usePager } from '../composables/usePager'
import { exportExcel, printPdf } from '../utils/export'

const user = getUser()
const canApply = computed(() => ['APPLICANT', 'STUDENT', 'DEPT_HEAD'].includes(user?.roleCode))
const isOwner = r => r.applicantId === user?.userId

const list = ref([])
const loading = ref(false)
const tab = ref('all')
const kw = ref('')
const scopeStatus = ref(null)

const tabs = [
  { name: 'all', label: '全部' },
  { name: '1', label: '待审批' },
  { name: '3', label: '已批准' },
  { name: '5', label: '已出库' },
  { name: '6', label: '已完成' },
  { name: '4', label: '已驳回' },
]
const filtered = computed(() => list.value.filter(r => {
  const tabOk = tab.value === 'all' || r.reqStatus === +tab.value
  const statusOk = scopeStatus.value == null || r.reqStatus === scopeStatus.value
  const kwOk = !kw.value || (r.reqNo || '').includes(kw.value) || (r.materialName || '').includes(kw.value)
    || (r.applicantName || '').includes(kw.value)
  return tabOk && statusOk && kwOk
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const countOf = n => n === 'all' ? list.value.length : list.value.filter(r => r.reqStatus === +n).length

const materials = ref([])
const available = ref(0)
const create = reactive({ open: false, form: {} })
const detail = reactive({ open: false, row: null, logs: [] })
const saving = ref(false)
const currentMaterial = computed(() => materials.value.find(m => m.materialId === create.form.materialId))

onMounted(refresh)
async function refresh() {
  loading.value = true
  try { list.value = await api.orders() } finally { loading.value = false }
}

async function searchMaterials(k) { materials.value = await api.materials(k || undefined) }
async function onMaterial(id) { available.value = Number((await api.batches(id)).available || 0) }

function openCreate() {
  create.form = { materialId: null, qtyApply: 1 }
  available.value = 0
  searchMaterials('')
  create.open = true
}
async function doCreate() {
  if (!create.form.materialId) return ElMessage.warning('请选择领用物资')
  if (!create.form.qtyApply || create.form.qtyApply <= 0) return ElMessage.warning('申请数量必须大于 0')
  saving.value = true
  try {
    await api.createOrder({ materialId: create.form.materialId, qtyApply: create.form.qtyApply, submit: true })
    ElMessage.success('已提交，等待库管员审批')
    create.open = false
    refresh()
  } finally { saving.value = false }
}

async function openDetail(r) {
  detail.row = await api.order(r.reqId)
  detail.logs = await api.orderLogs(r.reqId)
  detail.open = true
}
async function withdraw(r) {
  await ElMessageBox.confirm(`确认撤回领用单 ${r.reqNo}？`, '撤回', { type: 'warning' })
  await api.withdrawOrder(r.reqId)
  ElMessage.success('已撤回')
  refresh()
}
async function receive(r) {
  await api.receiveOrder(r.reqId)
  ElMessage.success('已签收，单据完成')
  refresh()
}

const cols = [
  { label: '领用单号', prop: 'reqNo', width: 22 },
  { label: '物资名称', prop: 'materialName', width: 26 },
  { label: '申请人', prop: 'applicantName' },
  { label: '申请部门', prop: 'deptName', width: 18 },
  { label: '申请数量', prop: r => num(r.qtyApply) },
  { label: '核准数量', prop: r => (r.qtyApprove ? num(r.qtyApprove) : '') },
  { label: '金额(元)', prop: r => Number(r.amount || 0).toFixed(2) },
  { label: '状态', prop: r => REQ_STATUS[r.reqStatus] },
  { label: '申请时间', prop: r => dt(r.createdAt) },
]
function doExport(type) {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, filtered.value, '领用申请清单', '领用')
  else printPdf('领用申请清单', cols, filtered.value, '学校物资管理系统 · 子系统③ 领用')
}
</script>
