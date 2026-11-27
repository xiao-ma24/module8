<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">借用申请</div>
        <div class="d">仅低值耐用品可借用；借出只减可用量，不改变在库总量</div>
      </div>
      <el-button v-if="canApply" type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新建借用申请</el-button>
    </div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name"
                     :label="`${t.label} (${countOf(t.name)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索单号 / 物资 / 借用人" clearable style="width:260px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-checkbox v-model="overdueOnly" @change="reset">仅看逾期</el-checkbox>
        <div class="grow"></div>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
        <el-button v-if="canApply" type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增借用申请</el-button>
      </div>

      <el-table :data="paged" stripe v-loading="loading">
        <el-table-column prop="borrowNo" label="借用单号" width="150" />
        <el-table-column prop="materialName" label="物资" min-width="170" show-overflow-tooltip />
        <el-table-column prop="borrowerName" label="借用人" width="95" />
        <el-table-column prop="deptName" label="部门" width="120" show-overflow-tooltip />
        <el-table-column label="借用数量" width="100" align="right">
          <template #default="{ row: r }">{{ num(r.borrowQty) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column label="已还 / 核销" width="115" align="right">
          <template #default="{ row: r }">
            <span style="color:#16a34a">{{ num(r.returnedQty) }}</span>
            <span style="color:#94a3b8"> / {{ num(r.writtenOffQty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预计归还" width="140">
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
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row: r }">
            <el-link type="primary" @click="openDetail(r)">详情</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <!-- 新增 -->
    <el-dialog v-model="create.open" title="新增借用申请" width="620px">
      <el-form :model="create.form" label-width="110px">
        <el-form-item label="借用物资" required>
          <el-select v-model="create.form.materialId" filterable remote :remote-method="searchMaterials"
                     placeholder="输入名称或编码检索（仅低值耐用品）" style="width:100%" @change="onMaterial">
            <el-option v-for="m in materials" :key="m.materialId" :value="m.materialId"
                       :label="`${m.materialName}（${m.materialCode}）`">
              <span>{{ m.materialName }}</span>
              <span style="float:right;color:#94a3b8">{{ money(m.refPrice) }}/{{ m.unit }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="可借数量" v-if="create.form.materialId">
          <el-tag type="success" effect="plain">{{ num(available) }} {{ currentMaterial?.unit }}</el-tag>
          <span style="font-size:12px;color:#94a3b8;margin-left:8px">借出只减少可用量，不减少在库总量</span>
        </el-form-item>
        <el-form-item label="借用数量" required>
          <el-input-number v-model="create.form.borrowQty" :min="0.001" :precision="3" :step="1" style="width:160px" />
        </el-form-item>
        <el-form-item label="预计归还日期" required>
          <el-date-picker v-model="create.form.dueDate" type="date" value-format="YYYY-MM-DD"
                          placeholder="选择日期" style="width:200px"
                          :disabled-date="d => d.getTime() < Date.now() - 86400000" />
          <span style="font-size:12px;color:#94a3b8;margin-left:10px">到期未还将进入逾期清单</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="create.open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detail.open" title="借用单详情" width="720px">
      <template v-if="detail.row">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="借用单号">{{ detail.row.borrowNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="BORROW_TAG[detail.row.borrowStatus]" size="small">{{ BORROW_STATUS[detail.row.borrowStatus] }}</el-tag>
            <el-tag v-if="detail.row.overdue" type="danger" effect="dark" size="small" style="margin-left:6px">
              逾期 {{ detail.row.overdueDays }} 天
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="借用人">{{ detail.row.borrowerName }}（{{ detail.row.borrowerNo }}）</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detail.row.deptName }}</el-descriptions-item>
          <el-descriptions-item label="物资">{{ detail.row.materialName }}</el-descriptions-item>
          <el-descriptions-item label="借用数量">{{ num(detail.row.borrowQty) }} {{ detail.row.unit }}</el-descriptions-item>
          <el-descriptions-item label="已完好归还 / 已核销">
            {{ num(detail.row.returnedQty) }} / {{ num(detail.row.writtenOffQty) }}
          </el-descriptions-item>
          <el-descriptions-item label="归还状态">
            {{ detail.row.conditionCode ? CONDITION[detail.row.conditionCode] : '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="借出仓库 / 批次">
            {{ detail.row.warehouseName || '—' }} / {{ detail.row.batchNo || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="预计归还日期">{{ date(detail.row.dueDate) }}</el-descriptions-item>
          <el-descriptions-item label="出库单号">{{ detail.row.outNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="最近归还时间">{{ dt(detail.row.returnTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider style="margin:16px 0 12px">分次归还记录</el-divider>
        <el-table :data="detail.items" size="small" border>
          <el-table-column prop="itemNo" label="归还流水" width="150" />
          <el-table-column label="数量" width="90" align="right">
            <template #default="{ row: r }">{{ num(r.returnQty) }}</template>
          </el-table-column>
          <el-table-column label="验收结果" width="110" align="center">
            <template #default="{ row: r }">
              <el-tag size="small" :type="r.acceptResult === 1 ? 'success' : 'danger'" effect="plain">
                {{ ACCEPT[r.acceptResult] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="keeperName" label="经办人" width="90" />
          <el-table-column label="时间" width="140">
            <template #default="{ row: r }">{{ dt(r.returnTime) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="!detail.items.length" description="暂无归还记录" :image-size="56" />

        <el-divider style="margin:16px 0 12px">审批留痕</el-divider>
        <el-timeline v-if="detail.logs.length">
          <el-timeline-item v-for="l in detail.logs" :key="l.recordId" :timestamp="dt(l.approveTime)"
                            :type="l.approveResult === 1 ? 'success' : 'danger'">
            <b>{{ l.approverName }}</b> {{ l.approveResult === 1 ? '通过' : '驳回' }}
            <div style="font-size:12px;color:#94a3b8" v-if="l.opinion">意见：{{ l.opinion }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无审批记录" :image-size="56" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Download, Printer } from '@element-plus/icons-vue'
import api, { getUser } from '../api'
import { BORROW_STATUS, BORROW_TAG, CONDITION, ACCEPT, money, num, date, dt } from '../common'
import { usePager } from '../../../composables/usePager'
import { exportExcel, printPdf } from '../../../utils/export'

const user = getUser()
const canApply = computed(() => ['APPLICANT', 'STUDENT', 'DEPT_HEAD'].includes(user?.roleCode))

const list = ref([])
const loading = ref(false)
const tab = ref('all')
const kw = ref('')
const overdueOnly = ref(false)

const tabs = [
  { name: 'all', label: '全部' },
  { name: '0', label: '待审批' },
  { name: '1', label: '待借出' },
  { name: '2', label: '在借' },
  { name: '3', label: '已归还' },
  { name: '4', label: '已报损' },
  { name: '5', label: '已核销' },
]
const filtered = computed(() => list.value.filter(r => {
  const tabOk = tab.value === 'all' || r.borrowStatus === +tab.value
  const overdueOk = !overdueOnly.value || r.overdue
  const kwOk = !kw.value || (r.borrowNo || '').includes(kw.value) || (r.materialName || '').includes(kw.value)
    || (r.borrowerName || '').includes(kw.value)
  return tabOk && overdueOk && kwOk
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const countOf = n => n === 'all' ? list.value.length : list.value.filter(r => r.borrowStatus === +n).length

const materials = ref([])
const available = ref(0)
const create = reactive({ open: false, form: {} })
const detail = reactive({ open: false, row: null, items: [], logs: [] })
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
  const d = new Date(); d.setDate(d.getDate() + 7)
  create.form = { materialId: null, borrowQty: 1, dueDate: d.toISOString().slice(0, 10) }
  available.value = 0
  searchMaterials('')
  create.open = true
}
async function doCreate() {
  if (!create.form.materialId) return ElMessage.warning('请选择借用物资')
  if (!create.form.borrowQty || create.form.borrowQty <= 0) return ElMessage.warning('借用数量必须大于 0')
  if (!create.form.dueDate) return ElMessage.warning('请填写预计归还日期')
  saving.value = true
  try {
    await api.createOrder({ materialId: create.form.materialId, borrowQty: create.form.borrowQty, dueDate: create.form.dueDate })
    ElMessage.success('已提交，等待库管员审批')
    create.open = false
    refresh()
  } finally { saving.value = false }
}
async function openDetail(r) {
  detail.row = await api.order(r.borrowId)
  detail.items = await api.orderItems(r.borrowId)
  detail.logs = await api.orderLogs(r.borrowId)
  detail.open = true
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
  { label: '状态', prop: r => BORROW_STATUS[r.borrowStatus] },
  { label: '是否逾期', prop: r => (r.overdue ? '逾期' + r.overdueDays + '天' : '') },
]
function doExport(type) {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, filtered.value, '借用归还清单', '借用')
  else printPdf('借用归还清单', cols, filtered.value, '学校物资管理系统 · 子系统④ 借用归还报损')
}
</script>
