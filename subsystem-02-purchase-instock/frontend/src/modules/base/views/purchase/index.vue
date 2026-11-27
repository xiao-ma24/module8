<template>
  <div class="purchase-page">
    <el-alert
      v-if="isAdmin"
      class="admin-tip"
      type="warning"
      :closable="false"
      show-icon
      title="当前为管理员视角，建议前往发票审核"
    >
      <template #default>
        <span>发票验收审核已迁移至「发票审核」，可在此继续登记或有权限时前往处理。</span>
        <el-button type="primary" link @click="goApproval">前往发票审核</el-button>
      </template>
    </el-alert>

    <div class="page-tip">
      温馨提示：教师个人科研经费采购登记后，将自动同步至管理员验收入库队列。
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="个人科研经费发票登记（个人散单，验收后入库）"
      description="本页面与集体采购验收单相互独立，不涉及财务经费表。提交后记录进入待验收队列，管理员验收通过后才增加对应仓库库存。"
    />

    <el-card shadow="always" class="form-card">
      <template #header>
        <span class="card-title">发票登记</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        style="max-width: 640px"
      >
        <el-form-item label="发票号码" prop="invoiceNo">
          <el-input v-model="form.invoiceNo" maxlength="32" placeholder="个人科研经费发票号码，同时用于幂等校验" />
        </el-form-item>
        <el-form-item label="物资" prop="materialId">
          <el-select v-model="form.materialId" placeholder="请选择物资" filterable style="width: 100%">
            <el-option
              v-for="item in materialOptions"
              :key="item.id"
              :label="`${item.materialNo} ${item.materialName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入库仓库" prop="warehouseId">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" filterable style="width: 100%">
            <el-option
              v-for="item in warehouseOptions"
              :key="item.id"
              :label="item.warehouseName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="qtyApply">
          <el-input-number
            v-model="form.qtyApply"
            :min="0.001"
            :precision="3"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="供应商名称">
          <el-input v-model="form.supplierName" maxlength="128" placeholder="选填" />
        </el-form-item>
        <el-form-item label="登记人" prop="registrarUserId">
          <el-input v-model="form.registrarUserId" maxlength="32" placeholder="登记人工号，如 T0001" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="lastResult" shadow="always" class="result-card">
      <template #header>
        <span class="card-title">最近一次登记结果</span>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="发票号码">{{ lastResult.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="物资">{{ materialLabel(lastResult.materialId) }}</el-descriptions-item>
        <el-descriptions-item label="入库仓库">{{ warehouseLabel(lastResult.warehouseId) }}</el-descriptions-item>
        <el-descriptions-item label="登记数量">{{ lastResult.qtyApply }}</el-descriptions-item>
        <el-descriptions-item label="当前在库总量（未变更）">{{ lastResult.qtyOnHand }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ formatTime(lastResult.registerTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag
            effect="plain"
            size="small"
            :class="statusClass(lastResult.auditStatus)"
          >
            {{ statusText(lastResult.auditStatus, lastResult.auditStatusText) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="lastResult.duplicated"
        class="dup-tip"
        type="warning"
        :closable="false"
        show-icon
        title="该发票的该物资行已登记过，本次未重复登记，可查看下方登记记录的处理状态"
      />
    </el-card>

    <el-card shadow="always">
      <template #header>
        <span class="card-title">我的申请记录</span>
      </template>

      <div class="record-toolbar">
        <el-input
          v-if="isAdmin"
          v-model="recordQuery.registrarUserId"
          placeholder="登记人工号，留空查全部"
          clearable
          style="width: 220px"
          @keyup.enter="loadRecords"
        />
        <div v-else class="self-scope">
          仅显示本人记录：<strong>{{ ownUserNo }}</strong>
        </div>
        <el-select
          v-model="recordQuery.auditStatus"
          placeholder="审核状态"
          clearable
          style="width: 150px"
        >
          <el-option label="待验收" :value="0" />
          <el-option label="已入库" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button type="primary" :loading="recordsLoading" @click="loadRecords">查询</el-button>
        <el-button class="btn-outline" @click="handleRecordReset">重置</el-button>
        <span class="list-total">共 {{ records.length }} 条（已入库 {{ inbounded.length }} 条）</span>
        <el-button class="btn-outline" :disabled="!inbounded.length" @click="printRows(inbounded, '个人科研经费采购入库登记表')">
          <el-icon><Printer /></el-icon>打印登记表
        </el-button>
        <el-button class="btn-outline" :disabled="!inbounded.length" @click="exportRows">
          <el-icon><Download /></el-icon>导出 Excel
        </el-button>
      </div>

      <el-table v-loading="recordsLoading" :data="records" size="default">
        <el-table-column prop="invoiceNo" label="发票号码" min-width="150" />
        <el-table-column label="物资" min-width="180">
          <template #default="{ row }">{{ materialLabel(row.materialId) }}</template>
        </el-table-column>
        <el-table-column label="入库仓库" min-width="120">
          <template #default="{ row }">{{ warehouseLabel(row.warehouseId) }}</template>
        </el-table-column>
        <el-table-column prop="qtyApply" label="数量" width="100" />
        <el-table-column prop="supplierName" label="供应商" min-width="140">
          <template #default="{ row }">{{ row.supplierName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="registrarUserId" label="登记人" width="110" />
        <el-table-column label="登记时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.registerTime) }}</template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" size="small" :class="statusClass(row.auditStatus)">
              {{ statusText(row.auditStatus, row.auditStatusText) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.auditStatus === 1" type="primary" link @click="printRows([row], '个人科研经费采购入库登记表')">
              打印
            </el-button>
            <el-button
              v-if="row.auditStatus === 2"
              type="primary"
              link
              @click="openFeedback(row)"
            >
              查看反馈
            </el-button>
            <span v-if="row.auditStatus === 0" class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="feedbackVisible" title="驳回反馈" width="460px">
      <template v-if="feedbackRecord">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="发票号码">{{ feedbackRecord.invoiceNo }}</el-descriptions-item>
          <el-descriptions-item label="物资">{{ materialLabel(feedbackRecord.materialId) }}</el-descriptions-item>
          <el-descriptions-item label="登记数量">{{ feedbackRecord.qtyApply }}</el-descriptions-item>
          <el-descriptions-item label="驳回意见">
            {{ feedbackRecord.auditRemark || '管理员未填写驳回意见' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button class="btn-outline" @click="feedbackVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Printer, Download } from '@element-plus/icons-vue'
import { getPurchaseRecords, registerPurchase } from '../../api/purchase'
import { getMaterialPage } from '../../api/material'
import { getWarehousePage } from '../../api/warehouse'
import api from '../../../../api'

const router = useRouter()

/** 按当前登录人的真实角色判断管理员视角，避免沿用演示角色的旧开关 */
const roleCode = ref('')
const isAdmin = computed(() => roleCode.value === 'ADMIN')

/** 非管理员固定按自己的工号查询，避免看到他人记录 */
const OWN_USER_KEY = 'demo_user_no'
const ownUserNo = ref(localStorage.getItem(OWN_USER_KEY) || 'T0001')

const formRef = ref()
const submitting = ref(false)
const materialOptions = ref([])
const warehouseOptions = ref([])
const lastResult = ref(null)

const records = ref([])
const recordsLoading = ref(false)
const recordQuery = reactive({
  registrarUserId: 'T0001',
  auditStatus: null
})

const feedbackVisible = ref(false)
const feedbackRecord = ref(null)

const form = reactive({
  invoiceNo: '',
  materialId: null,
  warehouseId: null,
  qtyApply: 1,
  supplierName: '',
  registrarUserId: 'T0001',
  remark: ''
})

const rules = {
  invoiceNo: [{ required: true, message: '请输入发票号码', trigger: 'blur' }],
  materialId: [{ required: true, message: '请选择物资', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择入库仓库', trigger: 'change' }],
  qtyApply: [{ required: true, message: '请输入入库数量', trigger: 'blur' }],
  registrarUserId: [{ required: true, message: '请输入登记人', trigger: 'blur' }]
}

const materialMap = computed(() => {
  const map = {}
  materialOptions.value.forEach((item) => {
    map[item.id] = `${item.materialNo} ${item.materialName}`
  })
  return map
})

const warehouseMap = computed(() => {
  const map = {}
  warehouseOptions.value.forEach((item) => {
    map[item.id] = item.warehouseName
  })
  return map
})

function materialLabel(id) {
  return materialMap.value[id] || `#${id}`
}

function warehouseLabel(id) {
  return warehouseMap.value[id] || `#${id}`
}

function statusText(status, text) {
  if (text) return text
  return { 0: '待验收', 1: '已入库', 2: '已驳回' }[status] || '-'
}

function statusClass(status) {
  if (status === 0) return 'tag-pending'
  if (status === 2) return 'tag-rejected'
  return 'tag-normal'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function goApproval() {
  router.push('/base/approval?tab=invoice')
}

async function loadOptions() {
  const [materialPage, warehousePage] = await Promise.all([
    getMaterialPage({ pageNum: 1, pageSize: 100, status: 1 }),
    getWarehousePage({ pageNum: 1, pageSize: 100, status: 1 })
  ])
  materialOptions.value = materialPage.list || []
  warehouseOptions.value = warehousePage.list || []
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    records.value = await getPurchaseRecords({
      // 教师端只查本人，管理员可按工号查询或留空查全部
      registrarUserId: isAdmin.value
        ? (recordQuery.registrarUserId || undefined)
        : ownUserNo.value,
      auditStatus: recordQuery.auditStatus ?? undefined
    })
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    recordsLoading.value = false
  }
}

function handleRecordReset() {
  recordQuery.registrarUserId = ''
  recordQuery.auditStatus = null
  loadRecords()
}

/** 只有已入库的记录才能打印/导出，用于交财务报销 */
const inbounded = computed(() => records.value.filter((r) => r.auditStatus === 1))

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function toLedgerRows(rows) {
  return rows.map((r, i) => {
    const label = materialLabel(r.materialId)
    const parts = label.split(' ')
    return {
      idx: i + 1,
      invoiceNo: r.invoiceNo || '',
      materialNo: parts.length > 1 ? parts[0] : '',
      materialName: parts.length > 1 ? parts.slice(1).join(' ') : label,
      qty: Number(r.qtyApply || 0),
      warehouse: warehouseLabel(r.warehouseId),
      supplier: r.supplierName || '—',
      registrar: r.registrarUserId || '',
      time: formatTime(r.registerTime),
      remark: r.remark || ''
    }
  })
}

function buildLedgerHtml(rows, title) {
  const list = toLedgerRows(rows)
  const totalQty = list.reduce((sum, r) => sum + r.qty, 0)
  const totalAmount = rows.reduce((sum, r) => sum + Number(r.totalAmount || 0), 0)
  const now = new Date().toLocaleString('zh-CN', { hour12: false })
  const body = list
    .map(
      (r) => `<tr>
        <td>${r.idx}</td><td>${escapeHtml(r.invoiceNo)}</td><td>${escapeHtml(r.materialNo)}</td>
        <td>${escapeHtml(r.materialName)}</td><td class="num">${r.qty}</td>
        <td>${escapeHtml(r.warehouse)}</td><td>${escapeHtml(r.supplier)}</td>
        <td>${escapeHtml(r.registrar)}</td><td>${escapeHtml(r.time)}</td>
        <td>${escapeHtml(r.remark)}</td>
      </tr>`
    )
    .join('')
  return `<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(title)}</title>
    <style>
      @page { size: A4 landscape; margin: 12mm; }
      body { font-family: "PingFang SC", "Microsoft YaHei", sans-serif; color: #111; }
      h1 { font-size: 18px; text-align: center; margin: 0 0 6px; }
      .meta { text-align: center; font-size: 12px; color: #555; margin-bottom: 12px; }
      table { width: 100%; border-collapse: collapse; font-size: 12px; }
      th, td { border: 1px solid #444; padding: 6px 8px; }
      th { background: #eef2ff; font-weight: 600; }
      td.num { text-align: right; }
      tfoot td { font-weight: 600; }
      .sign { margin-top: 18px; font-size: 12px; display: flex; justify-content: space-between; }
    </style></head><body>
    <h1>${escapeHtml(title)}</h1>
    <div class="meta">登记人：${escapeHtml(list[0]?.registrar || '')}　打印时间：${escapeHtml(now)}　共 ${list.length} 笔</div>
    <table>
      <thead><tr>
        <th style="width:42px">序号</th><th>发票号码</th><th>物资编码</th><th>物资名称</th>
        <th style="width:70px">数量</th><th>入库仓库</th><th>供应商</th><th>登记人</th>
        <th style="width:140px">登记时间</th><th>备注</th>
      </tr></thead>
      <tbody>${body}</tbody>
      <tfoot><tr>
        <td colspan="4">合计</td><td class="num">${totalQty}</td>
        <td colspan="5">${totalAmount > 0 ? '金额合计 ¥' + totalAmount.toFixed(2) : ''}</td>
      </tr></tfoot>
    </table>
    <div class="sign"><span>经办人签字：____________</span><span>财务审核：____________</span><span>日期：____________</span></div>
    </body></html>`
}

function printRows(rows, title) {
  if (!rows || !rows.length) {
    ElMessage.warning('暂无可打印的已入库记录')
    return
  }
  const win = window.open('', '_blank', 'width=1100,height=760')
  if (!win) {
    ElMessage.warning('浏览器拦截了打印窗口，请允许本站弹窗后重试')
    return
  }
  win.document.write(buildLedgerHtml(rows, title))
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 400)
}

function exportRows() {
  const rows = inbounded.value
  if (!rows.length) {
    ElMessage.warning('暂无已入库记录可导出')
    return
  }
  const html = buildLedgerHtml(rows, '个人科研经费采购入库登记表')
  const blob = new Blob(['\ufeff' + html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  const link = document.createElement('a')
  const day = new Date().toISOString().slice(0, 10)
  link.href = URL.createObjectURL(blob)
  link.download = `个人发票入库登记表_${day}.xls`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
  ElMessage.success(`已导出 ${rows.length} 条已入库记录`)
}

function openFeedback(row) {
  feedbackRecord.value = row
  feedbackVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const result = await registerPurchase({
      invoiceNo: form.invoiceNo,
      materialId: form.materialId,
      warehouseId: form.warehouseId,
      qtyApply: form.qtyApply,
      supplierName: form.supplierName || null,
      registrarUserId: form.registrarUserId,
      remark: form.remark || null
    })
    lastResult.value = result
    // 教师端记住本人登记工号，后续记录查询始终按该工号过滤
    const registrar = form.registrarUserId.trim()
    if (!isAdmin.value && registrar) {
      ownUserNo.value = registrar
      localStorage.setItem(OWN_USER_KEY, registrar)
    }
    if (result.duplicated) {
      ElMessage.warning('该发票已登记过，本次未重复登记，请勿重复提交')
    } else {
      ElMessage.success(`发票 ${form.invoiceNo} 登记成功，已进入待验收队列`)
      // 提交成功后清空表单，保留登记人工号方便连续录入
      formRef.value.resetFields()
      form.registrarUserId = registrar
    }
    await loadRecords()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  const registrar = form.registrarUserId
  formRef.value?.resetFields()
  form.registrarUserId = registrar
}

onMounted(async () => {
  // 统一登录后以当前登录人作为登记人，避免沿用上一个演示账号的工号
  try {
    const uid = Number(localStorage.getItem('mockUserId') || 0)
    if (uid) {
      const p = await api.profile(uid)
      roleCode.value = p?.info?.roleCode || ''
      const userNo = p?.info?.userNo
      if (userNo) {
        ownUserNo.value = userNo
        form.registrarUserId = userNo
        recordQuery.registrarUserId = userNo
        localStorage.setItem(OWN_USER_KEY, userNo)
      }
    }
  } catch (error) {
    // 拉取登录人失败时保持原有默认值
  }
  loadOptions()
  loadRecords()
})
</script>

<style scoped>
.purchase-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-tip {
  padding: 10px 16px;
  border-radius: 10px;
  background-color: var(--primary-soft);
  border-left: 4px solid var(--primary);
  color: #1e3a8a;
  font-size: 13px;
  line-height: 1.6;
}

.card-title {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.3px;
  color: var(--text-1);
}

.dup-tip {
  margin-top: 12px;
}

.record-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.self-scope {
  padding: 0 12px;
  height: 32px;
  display: flex;
  align-items: center;
  border-radius: 8px;
  background: var(--primary-soft);
  color: var(--text-2);
  font-size: 13px;
}

.self-scope strong {
  margin-left: 4px;
  color: var(--primary-dark, #1e40af);
}

.list-total {
  margin-left: auto;
  font-size: 13px;
  color: var(--text-3);
}

.text-muted {
  color: var(--text-3);
}

.tag-normal {
  background: #e7f8ee;
  color: #15803d;
}

.tag-pending {
  background: #fff3e2;
  color: #b45309;
}

.tag-rejected {
  background: #fdeceb;
  color: #b91c1c;
}
</style>
