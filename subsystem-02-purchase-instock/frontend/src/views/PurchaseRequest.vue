<template>
  <div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name"
                     :label="`${t.label} (${countOf(t.name)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索单号 / 用途 / 申请人" clearable style="width:230px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="sourceFilter" placeholder="需求来源" clearable style="width:130px" @change="reset">
          <el-option v-for="(v,k) in SOURCE" :key="k" :label="v" :value="+k" />
        </el-select>
        <el-select v-model="budgetFilter" placeholder="预算科目" clearable style="width:150px" @change="reset">
          <el-option v-for="b in budgets" :key="b.budgetNo" :label="b.subjectName" :value="b.budgetNo" />
        </el-select>
        <div class="grow"></div>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
        <el-button type="primary" v-if="canCreate" @click="openCreate"><el-icon><Plus /></el-icon>新建采购申请</el-button>
      </div>

      <el-table :data="paged" stripe @selection-change="sel = $event">
        <el-table-column type="selection" width="42" :selectable="r => r.prStatus === 0" />
        <el-table-column prop="prNo" label="单号" width="145" />
        <el-table-column label="需求来源" width="100">
          <template #default="{ row: r }">
            <el-tag size="small" :type="r.sourceType === 3 ? 'danger' : r.sourceType === 2 ? 'warning' : 'info'">
              {{ SOURCE[r.sourceType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="80" />
        <el-table-column label="部门" width="110">
          <template #default="{ row: r }">{{ r.deptName || '—' }}</template>
        </el-table-column>
        <el-table-column label="用途" show-overflow-tooltip>
          <template #default="{ row: r }">{{ purposeText(r) }}</template>
        </el-table-column>
        <el-table-column label="预算科目" width="130" show-overflow-tooltip>
          <template #default="{ row: r }">{{ budgetText(r) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row: r }">{{ money(r.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="70" align="center">
          <template #default="{ row: r }">
            <el-tag v-if="r.priority === 2" type="danger" size="small" effect="dark">加急</el-tag>
            <span v-else style="color:#94a3b8">普通</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row: r }"><el-tag :type="PR_TAG[r.prStatus]" effect="dark" size="small">{{ PR_STATUS[r.prStatus] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row: r }">
            <el-link type="primary" @click="openDetail(r)">详情</el-link>
            <el-link v-if="r.prStatus === 0" type="success" style="margin-left:8px" @click="submit(r)">提交</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-button v-if="sel.length" type="success" plain size="small" style="margin-right:auto" @click="batchSubmit">
          批量提交选中 {{ sel.length }} 单
        </el-button>
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <!-- 新建申请 -->
    <el-drawer v-model="create.open" title="新建采购申请" size="640px">
      <el-form label-width="90px">
        <el-form-item label="申请人">
          <el-select v-model="create.form.applicantId" @change="onApplicant" :disabled="cur.roleCode !== 'PURCHASER'" style="width:100%">
            <el-option v-for="u in users" :key="u.userId" :label="u.userName + '（' + (u.deptName||'') + '）'" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算科目">
          <el-select v-model="create.form.budgetNo" style="width:100%">
            <el-option v-for="b in budgets" :key="b.budgetNo"
                       :label="`${b.subjectName}（余额 ¥${b.balance}）`" :value="b.budgetNo" />
          </el-select>
        </el-form-item>
        <el-form-item label="用途说明" required>
          <el-input v-model="create.form.usagePurpose" placeholder="如：11月实验耗材补充" />
        </el-form-item>
        <el-form-item label="期望到货"><el-date-picker v-model="create.form.expectDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="create.form.priority">
            <el-radio :value="1">普通</el-radio><el-radio :value="2">加急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="申请明细">
          <el-button size="small" @click="addItem"><el-icon><Plus /></el-icon>添加物资</el-button>
          <el-table :data="create.form.items" size="small" style="margin-top:8px">
            <el-table-column label="物资" min-width="160">
              <template #default="{ row: r }">
                <el-select v-model="r.materialId" filterable placeholder="选择物资">
                  <el-option v-for="m in materials" :key="m.materialId"
                             :label="`${m.materialName}（${CATEGORY[m.categoryCode]}）`" :value="m.materialId" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="120">
              <template #default="{ row: r }"><el-input-number v-model="r.qtyApply" :min="1" size="small" /></template>
            </el-table-column>
            <el-table-column label="小计" width="90">
              <template #default="{ row: r }">{{ money(priceOf(r) * (r.qtyApply || 0)) }}</template>
            </el-table-column>
            <el-table-column width="50">
              <template #default="{ row: r }"><el-button type="danger" size="small" text @click="create.form.items.splice(r.$index,1)">删</el-button></template>
            </el-table-column>
          </el-table>
          <div style="text-align:right; font-weight:600; margin-top:6px">预估总额：{{ money(estTotal) }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="create.open = false">取消</el-button>
        <el-button type="primary" :loading="create.loading" @click="save">保存草稿</el-button>
      </template>
    </el-drawer>

    <!-- 详情 -->
    <el-drawer v-model="detail.open" :title="detail.row?.prNo" size="640px">
      <el-steps :active="stepActive" align-center finish-status="success" style="margin-bottom:16px">
        <el-step title="创建" />
        <el-step title="提交审批" />
        <el-step :title="detail.row?.prStatus === 3 ? '已驳回' : '审批通过'" :status="detail.row?.prStatus === 3 ? 'error' : undefined" />
        <el-step title="生成订单" />
      </el-steps>

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="申请人">{{ detail.row?.applicantName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.row?.deptName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="预算科目" :span="2">{{ budgetText(detail.row) }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ purposeText(detail.row) }}</el-descriptions-item>
        <el-descriptions-item label="期望到货">{{ date(detail.row?.expectDate) }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ money(detail.row?.totalAmount) }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin:14px 0 8px">申请明细</h4>
      <el-table :data="detail.items" size="small" border>
        <el-table-column prop="materialCode" label="编码" width="90" />
        <el-table-column prop="materialName" label="物资" />
        <el-table-column label="申请量" width="80">
          <template #default="{ row: r }">{{ qty(r.qtyApply) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column label="核准量" width="80">
          <template #default="{ row: r }">{{ qty(r.qtyConfirmed) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="90">
          <template #default="{ row: r }">{{ money(r.amount) }}</template>
        </el-table-column>
      </el-table>

      <h4 style="margin:14px 0 8px">审批记录</h4>
      <el-timeline v-if="detail.logs.length">
        <el-timeline-item v-for="(l, i) in detail.logs" :key="i" :timestamp="dt(l.approveTime)" placement="top"
                          :type="l.approveResult === 1 ? 'success' : 'danger'">
          <b>{{ l.approverName }}</b>　{{ l.approveResult === 1 ? '同意' : '驳回' }}
          <span style="color:#64748b; margin-left:6px">{{ l.opinion || '无意见' }}</span>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录" :image-size="60" />

      <div v-if="detail.row?.prStatus === 1 && canApproveRole" style="margin-top:16px">
        <el-alert v-if="detail.row?.applicantId === cur.userId" type="info" :closable="false"
                  title="这是您本人提交的申请，不能自审；请由采购员或其他负责人审批" />
        <template v-else-if="!canApproveAmount">
          <el-alert type="warning" :closable="false"
                    :title="`申请金额 ¥${money(detail.row?.totalAmount)} 超出当前岗位审批限额（部门负责人 2000 元、采购员 10000 元），请由更高层级审批`" />
        </template>
        <template v-else>
          <el-alert type="warning" :closable="false" title="分级授权：≤2000 部门负责人审批；2000~10000 采购员审批；>10000 管理员审批" />
          <div style="margin-top:10px; display:flex; gap:10px">
            <el-select v-model="detail.approver" placeholder="选择审批人" style="flex:1">
              <el-option v-for="u in approvableUsers" :key="u.userId" :label="u.roleName ? `${u.userName}（${u.roleName}）` : u.userName" :value="u.userId" />
            </el-select>
            <el-button type="success" @click="approve(1)">同意</el-button>
            <el-button type="danger" @click="approve(2)">驳回</el-button>
          </div>
        </template>
      </div>
      <el-alert v-else-if="detail.row?.prStatus === 1" type="info" :closable="false" style="margin-top:16px"
                title="当前岗位无审批权限，采购申请由采购员、部门负责人或管理员审批" />
      <div v-if="detail.row?.prStatus === 2 && canOrder" style="margin-top:16px">
        <el-button type="primary" @click="openToOrder">
          <el-icon><ShoppingCart /></el-icon>生成采购订单（比价）
        </el-button>
      </div>
      <el-alert v-else-if="detail.row?.prStatus === 2" type="info" :closable="false" style="margin-top:16px"
                title="申请已批准，等待采购员生成采购订单并下单" />
    </el-drawer>

    <!-- 转订单 -->
    <el-dialog v-model="toOrderDlg" title="生成采购订单" width="720px">
      <el-form label-width="90px">
        <el-form-item label="供应商">
          <el-select v-model="order.supplierId" style="width:100%">
            <el-option v-for="s in suppliers" :key="s.supplierId" :disabled="!s.supplierStatus"
                       :label="`${s.supplierName}　${'★'.repeat(s.starLevel)}　${s.contactPerson} ${s.contactPhone}`" :value="s.supplierId" />
          </el-select>
        </el-form-item>
        <el-form-item label="付款方式">
          <el-radio-group v-model="order.payType">
            <el-radio :value="1">货到付款</el-radio><el-radio :value="2">预付</el-radio><el-radio :value="3">月度结算</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="要求到货">
          <el-date-picker v-model="order.expectDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="成交单价">
          <el-table :data="order.items" size="small" border>
            <el-table-column prop="materialName" label="物资" min-width="140" />
            <el-table-column label="核准量" width="80">
              <template #default="{ row: r }">{{ qty(r.qtyConfirmed) }} {{ r.unit }}</template>
            </el-table-column>
            <el-table-column label="成交单价" width="140">
              <template #default="{ row: r }">
                <el-input-number v-model="r.unitPrice" :min="0.01" :precision="2" size="small" style="width:120px" />
              </template>
            </el-table-column>
            <el-table-column label="参考/历史价" width="110">
              <template #default="{ row: r }">¥{{ r.refPrice }}</template>
            </el-table-column>
          </el-table>
          <el-alert v-if="priceWarn" type="warning" :closable="false" :title="priceWarn" style="margin-top:6px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="toOrderDlg = false">取消</el-button>
        <el-button type="primary" :loading="order.loading" @click="saveOrder">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import { PR_STATUS, PR_TAG, SOURCE, CATEGORY, money, qty, date, dt } from '../common'
import { exportExcel, printPdf } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const list = ref([]), users = ref([]), approvers = ref([]), budgets = ref([]),
      materials = ref([]), suppliers = ref([])
// 当前登录用户（申请人默认本人；负责人审批默认本人）
const cur = reactive({ userId: null, userName: '', roleCode: '' })
const tab = ref(route.query.status ? String(route.query.status) : 'all')
const kw = ref(''), sourceFilter = ref(null), budgetFilter = ref(null), sel = ref([])

const tabs = [
  { name: 'all', label: '全部' }, { name: '0', label: '草稿' }, { name: '1', label: '待审批' },
  { name: '2', label: '已批准' }, { name: '3', label: '已驳回' }, { name: '4', label: '已转订单' },
]

// 部门比较：purchase_request.dept_id 为数字，用户 org_id 为 6 位字符串（如 000002）
const sameDept = (a, b) => a != null && b != null && Number(a) === Number(b)

// 角色数据范围：部门负责人仅本部门；申请人仅本人；采购员/管理员查看全部
const scoped = computed(() => {
  const rc = cur.roleCode
  if (rc === 'DEPT_HEAD') return list.value.filter(r => sameDept(r.deptId, cur.deptId) || r.applicantId === cur.userId)
  if (['APPLICANT', 'TEACHER', 'STUDENT'].includes(rc)) return list.value.filter(r => r.applicantId === cur.userId)
  return list.value
})

const filtered = computed(() => scoped.value.filter(r =>
  (tab.value === 'all' || r.prStatus === +tab.value) &&
  (sourceFilter.value == null || r.sourceType === sourceFilter.value) &&
  (!budgetFilter.value || r.budgetNo === budgetFilter.value) &&
  (!kw.value || r.prNo.includes(kw.value) || (r.usagePurpose || '').includes(kw.value) ||
    (r.applicantName || '').includes(kw.value))))

const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const countOf = name => name === 'all' ? scoped.value.length : scoped.value.filter(r => r.prStatus === +name).length

const create = reactive({ open: false, loading: false, form: { applicantId: null, budgetNo: '', usagePurpose: '', expectDate: '', priority: 1, items: [] } })
const detail = reactive({ open: false, row: null, items: [], logs: [], approver: null })
const toOrderDlg = ref(false)
const order = reactive({ supplierId: null, payType: 1, expectDate: '', items: [], loading: false })

const stepActive = computed(() => {
  const s = detail.row?.prStatus
  if (s === 0) return 0        // 草稿 → 创建
  if (s === 1) return 1        // 待审批 → 提交审批
  if (s === 2) return 3        // 已批准 → 审批通过完成，等待生成订单
  if (s === 3) return 2        // 已驳回 → 审批节点标红
  if (s === 4) return 4        // 已转订单 → 全部节点完成
  return 0
})

onMounted(async () => {
  try {
    const p = await api.profile(Number(localStorage.getItem('mockUserId') || 2))
    Object.assign(cur, p.info || {})
  } catch {}
  refresh()
})
async function refresh() {
  list.value = await api.requests()
  users.value = await api.users()
  approvers.value = users.value.filter(u => ['DEPT_HEAD', 'ADMIN', 'PURCHASER'].includes(u.roleCode))
  budgets.value = await api.budgets()
  materials.value = await api.materials()
  suppliers.value = await api.suppliers()
}

const priceOf = r => (materials.value.find(m => m.materialId === r.materialId)?.refPrice) || 0
const estTotal = computed(() => create.form.items.reduce((s, r) => s + priceOf(r) * (r.qtyApply || 0), 0))
function onApplicant(id) {
  const u = users.value.find(x => x.userId === id)
  if (u) create.form.deptId = u.deptId
}
function addItem() { create.form.items.push({ materialId: null, qtyApply: 10 }) }

function openCreate() {
  // 申请人默认登录者本人（采购员可代选他人）；用户信息未加载完成时不允许创建，避免落到列表第一个人名下
  if (!cur.userId || !cur.roleCode) return ElMessage.warning('正在加载当前用户信息，请稍后重试')
  const mine = cur.roleCode !== 'PURCHASER'
  create.form = { applicantId: mine ? cur.userId : users.value[0]?.userId, budgetNo: budgets.value[0]?.budgetNo || '', usagePurpose: '', expectDate: '', priority: 1, items: [] }
  create.form.deptId = cur.deptId
  create.open = true
}

async function save() {
  if (!create.form.applicantId || !create.form.budgetNo) return ElMessage.warning('请选择申请人与预算科目')
  if (!create.form.usagePurpose || !create.form.usagePurpose.trim()) return ElMessage.warning('请填写用途说明（采购理由）')
  if (!create.form.items.length) return ElMessage.warning('请添加申请明细')
  if (create.form.items.some(i => !i.materialId)) return ElMessage.warning('请选择物资')
  create.loading = true
  try {
    const r = await api.createRequest(create.form)
    ElMessage.success('已保存草稿：' + r.prNo)
    create.open = false
    refresh()
  } finally { create.loading = false }
}

// 业务规则：申请人发起申请，采购员只负责审批/下单，不代建申请
const canCreate = computed(() => ['APPLICANT', 'TEACHER', 'STUDENT', 'DEPT_HEAD', 'ADMIN'].includes(cur.roleCode))
// 来源为「实验室自备」的自购登记没有预算科目与用途，展示明确替代文案而非空白
const purposeText = r => r?.usagePurpose || (r?.sourceType === 4 ? '实验室自备（个人发票登记，个人保管）' : '—')
const budgetText = r => (r?.budgetNo === 'N/A' || !r?.budgetNo)
  ? '不适用（自备经费）'
  : `${r.budgetSubjectName || r.budgetNo}（${r.budgetNo}）`

async function submit(row) {
  await ElMessageBox.confirm(`提交后将在提交时校验预算「${row.budgetSubjectName}」余额并冻结，确认提交？`, '提交审批')
  await api.submitRequest(row.prId)
  ElMessage.success('已提交审批')
  refresh()
}

async function batchSubmit() {
  await ElMessageBox.confirm(`将批量提交选中的 ${sel.value.length} 张草稿申请，确认？`, '批量提交')
  for (const r of sel.value) await api.submitRequest(r.prId)
  ElMessage.success('已批量提交')
  sel.value = []
  refresh()
}

// 仅采购员/管理员可生成采购订单并下单，部门负责人只有申请与限额内审批权
const canOrder = computed(() => ['PURCHASER', 'ADMIN'].includes(cur.roleCode))
// 按申请金额分级授权：≤2000 部门负责人；2000~10000 采购员；>10000 管理员
const DEPT_HEAD_LIMIT = 2000
const PURCHASER_LIMIT = 10000
const reqAmount = computed(() => Number(detail.row?.totalAmount || 0))
const canApproveAmount = computed(() => {
  if (cur.roleCode === 'ADMIN') return true
  if (cur.roleCode === 'PURCHASER') return reqAmount.value <= PURCHASER_LIMIT
  if (cur.roleCode === 'DEPT_HEAD') return reqAmount.value <= DEPT_HEAD_LIMIT
  return false
})
// 可选审批人：排除申请人本人；按金额过滤有权限的岗位
const approvableUsers = computed(() =>
  approvers.value.filter(u => {
    if (u.userId === detail.row?.applicantId) return false
    if (reqAmount.value > PURCHASER_LIMIT) return u.roleCode === 'ADMIN'
    if (reqAmount.value > DEPT_HEAD_LIMIT) return ['PURCHASER', 'ADMIN'].includes(u.roleCode)
    if (cur.roleCode === 'DEPT_HEAD') {
      return ['PURCHASER', 'ADMIN'].includes(u.roleCode)
        || (u.roleCode === 'DEPT_HEAD' && sameDept(u.deptId, cur.deptId))
    }
    return ['DEPT_HEAD', 'PURCHASER', 'ADMIN'].includes(u.roleCode)
  }))
const canApproveRole = computed(() => ['DEPT_HEAD', 'ADMIN', 'PURCHASER'].includes(cur.roleCode))

async function openDetail(row) {
  detail.row = row
  detail.items = await api.requestItems(row.prId)
  detail.logs = await api.requestLogs(row.prId)
  const can = canApproveRole.value && canApproveAmount.value && cur.userId && cur.userId !== row.applicantId
  detail.approver = can ? cur.userId : null
  detail.open = true
}

async function approve(action) {
  // 审批人默认当前登录人，且不能审批本人提交的申请
  if (!canApproveAmount.value) {
    return ElMessage.warning('申请金额超出当前岗位审批限额，请选择有权限的审批人')
  }
  if (detail.approver && detail.approver === detail.row?.applicantId) {
    return ElMessage.warning('不能审批本人提交的申请')
  }
  if (!detail.approver && cur.userId && cur.userId !== detail.row?.applicantId) detail.approver = cur.userId
  if (!detail.approver) return ElMessage.warning('请选择审批人')
  const { value } = await ElMessageBox.prompt(
    action === 1 ? '审批意见（可留空）' : '请填写驳回原因', action === 1 ? '同意' : '驳回',
    { inputPlaceholder: action === 1 ? '同意' : '驳回原因' })
  await api.approveRequest(detail.row.prId, { approverId: detail.approver, action, opinion: value }, [])
  ElMessage.success('审批完成')
  detail.open = false
  refresh()
}

const priceWarn = computed(() => {
  const bad = order.items.filter(i => i.unitPrice > Number(i.refPrice) * 1.1)
  return bad.length ? `⚠ 价格异常：${bad.map(b => b.materialName).join('、')} 成交价高于参考价10%，请核实` : ''
})

function openToOrder() {
  order.items = detail.items.map(i => ({ ...i, unitPrice: Number(i.refPrice) }))
  order.supplierId = null
  toOrderDlg.value = true
}

async function saveOrder() {
  if (!order.supplierId) return ElMessage.warning('请选择供应商')
  order.loading = true
  try {
    const r = await api.toOrder(detail.row.prId, {
      supplierId: order.supplierId, buyerId: cur.userId, payType: order.payType, expectDate: order.expectDate,
      prices: order.items.map(i => ({ priId: i.priId, unitPrice: i.unitPrice }))
    })
    ElMessage.success('订单已生成：' + r.poNo)
    toOrderDlg.value = false
    detail.open = false
    refresh()
  } finally { order.loading = false }
}

const cols = [
  { label: '单号', prop: 'prNo', width: 16 }, { label: '需求来源', prop: r => SOURCE[r.sourceType] },
  { label: '申请人', prop: 'applicantName' }, { label: '部门', prop: 'deptName' },
  { label: '用途', prop: r => purposeText(r), width: 30 }, { label: '预算科目', prop: r => budgetText(r) },
  { label: '金额', prop: r => Number(r.totalAmount || 0).toFixed(2) },
  { label: '状态', prop: r => PR_STATUS[r.prStatus] },
]
function doExport(type) {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, filtered.value, '采购申请清单', '采购申请')
  else printPdf('采购申请清单', cols, filtered.value, '学校物资采购管理系统')
}
</script>
