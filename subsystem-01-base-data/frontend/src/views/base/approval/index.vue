<template>
  <div class="approval-page">
    <el-card shadow="always">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane :label="`待我审核（库存变动） (${pendingList.length})`" name="pending">
          <div class="toolbar">
            <span class="toolbar-tip">以下为待审核的库存变动申请，审核通过后才会真正增加或扣减库存。</span>
            <el-button class="btn-outline" :loading="pendingLoading" @click="loadPending">刷新</el-button>
          </div>

          <el-table v-loading="pendingLoading" :data="pendingList">
            <el-table-column prop="applyNo" label="申请单号" min-width="180" />
            <el-table-column label="物资" min-width="170">
              <template #default="{ row }">{{ materialLabel(row.materialId) }}</template>
            </el-table-column>
            <el-table-column label="仓库" min-width="110">
              <template #default="{ row }">{{ warehouseLabel(row.warehouseId) }}</template>
            </el-table-column>
            <el-table-column label="方向" width="80">
              <template #default="{ row }">
                <el-tag effect="plain" size="small" :type="row.direction === 'IN' ? 'success' : 'warning'">
                  {{ row.direction === 'IN' ? '入库' : '出库' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="qtyApply" label="数量" width="100" />
            <el-table-column prop="sourceNo" label="来源单号" min-width="150">
              <template #default="{ row }">{{ row.sourceNo || '-' }}</template>
            </el-table-column>
            <el-table-column prop="applicantUserId" label="申请人" width="110" />
            <el-table-column label="申请时间" min-width="160">
              <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  :loading="auditingId === row.id"
                  @click="handleApprove(row)"
                >
                  通过
                </el-button>
                <el-button
                  type="primary"
                  link
                  :disabled="auditingId === row.id"
                  @click="openReject(row)"
                >
                  驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`发票审核 (${invoiceList.length})`" name="invoice">
          <div class="toolbar">
            <span class="toolbar-tip">以下为「待验收」的个人发票登记，审核通过后立即入库。</span>
            <el-button class="btn-outline" :loading="invoiceLoading" @click="loadInvoices">刷新</el-button>
          </div>

          <el-table v-loading="invoiceLoading" :data="invoiceList">
            <el-table-column prop="invoiceNo" label="发票号码" min-width="150" />
            <el-table-column label="物资" min-width="170">
              <template #default="{ row }">{{ materialLabel(row.materialId) }}</template>
            </el-table-column>
            <el-table-column label="入库仓库" min-width="120">
              <template #default="{ row }">{{ warehouseLabel(row.warehouseId) }}</template>
            </el-table-column>
            <el-table-column prop="qtyApply" label="数量" width="100" />
            <el-table-column prop="supplierName" label="供应商" min-width="130">
              <template #default="{ row }">{{ row.supplierName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="registrarUserId" label="登记人" width="110" />
            <el-table-column label="登记时间" min-width="160">
              <template #default="{ row }">{{ formatTime(row.registerTime) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="130">
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  :loading="invoiceAuditingId === row.id"
                  @click="handleInvoiceApprove(row)"
                >
                  通过
                </el-button>
                <el-button
                  type="primary"
                  link
                  :disabled="invoiceAuditingId === row.id"
                  @click="openInvoiceReject(row)"
                >
                  驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="rejectVisible" title="驳回申请" width="460px">
      <div class="dialog-tip">
        申请单号：{{ rejectForm.applyNo }}。驳回后不会变更库存，申请人可在「我的申请记录」中查看驳回意见。
      </div>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
        <el-form-item label="驳回原因" prop="auditRemark">
          <el-input
            v-model="rejectForm.auditRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请填写驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="btn-outline" @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejecting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invoiceRejectVisible" title="驳回登记" width="460px">
      <div class="dialog-tip">
        发票号码：{{ invoiceRejectForm.invoiceNo }}。驳回后不会增加库存，教师可在「个人发票登记」中查看驳回意见。
      </div>
      <el-form
        ref="invoiceRejectFormRef"
        :model="invoiceRejectForm"
        :rules="rejectRules"
        label-width="90px"
      >
        <el-form-item label="驳回原因" prop="auditRemark">
          <el-input
            v-model="invoiceRejectForm.auditRemark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请填写驳回原因，将反馈给登记人"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="btn-outline" @click="invoiceRejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="invoiceRejecting" @click="handleInvoiceReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auditStockApply, getStockApplyList } from '@/api/stockApply'
import { auditPurchase, getPendingPurchases } from '@/api/purchase'
import { getMaterialPage } from '@/api/material'
import { getWarehousePage } from '@/api/warehouse'

const route = useRoute()

const activeTab = ref('pending')

/** 审批中心为管理员专用，仅保留库存变动与发票两个审核视图 */
function syncTabFromQuery() {
  activeTab.value = route.query.tab === 'invoice' ? 'invoice' : 'pending'
}

watch(() => route.query.tab, syncTabFromQuery)

const materialOptions = ref([])
const warehouseOptions = ref([])

const pendingList = ref([])
const pendingLoading = ref(false)
const auditingId = ref(null)

const rejectFormRef = ref()
const rejectVisible = ref(false)
const rejecting = ref(false)
const rejectForm = reactive({
  id: null,
  applyNo: '',
  auditRemark: ''
})
const rejectRules = {
  auditRemark: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

/** 发票审核（原「待我审批」） */
const invoiceList = ref([])
const invoiceLoading = ref(false)
const invoiceAuditingId = ref(null)

const invoiceRejectFormRef = ref()
const invoiceRejectVisible = ref(false)
const invoiceRejecting = ref(false)
const invoiceRejectForm = reactive({
  id: null,
  invoiceNo: '',
  auditRemark: ''
})

function materialLabel(id) {
  const item = materialOptions.value.find((it) => it.id === id)
  return item ? `${item.materialNo} ${item.materialName}` : `#${id}`
}

function warehouseLabel(id) {
  const item = warehouseOptions.value.find((it) => it.id === id)
  return item ? item.warehouseName : `#${id}`
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function handleTabChange(name) {
  if (name === 'invoice') {
    loadInvoices()
  } else {
    loadPending()
  }
}

async function loadOptions() {
  const [materialPage, warehousePage] = await Promise.all([
    getMaterialPage({ pageNum: 1, pageSize: 100, status: 1 }),
    getWarehousePage({ pageNum: 1, pageSize: 100, status: 1 })
  ])
  materialOptions.value = materialPage.list || []
  warehouseOptions.value = warehousePage.list || []
}

async function loadPending() {
  pendingLoading.value = true
  try {
    pendingList.value = await getStockApplyList({ auditStatus: 0 })
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    pendingLoading.value = false
  }
}

async function handleApprove(row) {
  auditingId.value = row.id
  try {
    await auditStockApply({ id: row.id, auditStatus: 1 })
    ElMessage.success(`${row.applyNo} 审核通过，库存已更新`)
    await loadPending()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    auditingId.value = null
  }
}

function openReject(row) {
  rejectForm.id = row.id
  rejectForm.applyNo = row.applyNo
  rejectForm.auditRemark = ''
  rejectVisible.value = true
  rejectFormRef.value?.clearValidate()
}

async function handleReject() {
  await rejectFormRef.value.validate()
  rejecting.value = true
  try {
    await auditStockApply({
      id: rejectForm.id,
      auditStatus: 2,
      auditRemark: rejectForm.auditRemark
    })
    ElMessage.success(`${rejectForm.applyNo} 已驳回`)
    rejectVisible.value = false
    await loadPending()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    rejecting.value = false
  }
}

async function loadInvoices() {
  invoiceLoading.value = true
  try {
    invoiceList.value = await getPendingPurchases()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    invoiceLoading.value = false
  }
}

async function handleInvoiceApprove(row) {
  invoiceAuditingId.value = row.id
  try {
    await auditPurchase({ id: row.id, auditStatus: 1 })
    ElMessage.success(`${row.invoiceNo} 审核通过，库存已增加`)
    await loadInvoices()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    invoiceAuditingId.value = null
  }
}

function openInvoiceReject(row) {
  invoiceRejectForm.id = row.id
  invoiceRejectForm.invoiceNo = row.invoiceNo
  invoiceRejectForm.auditRemark = ''
  invoiceRejectVisible.value = true
  invoiceRejectFormRef.value?.clearValidate()
}

async function handleInvoiceReject() {
  await invoiceRejectFormRef.value.validate()
  invoiceRejecting.value = true
  try {
    await auditPurchase({
      id: invoiceRejectForm.id,
      auditStatus: 2,
      auditRemark: invoiceRejectForm.auditRemark
    })
    ElMessage.success(`${invoiceRejectForm.invoiceNo} 已驳回`)
    invoiceRejectVisible.value = false
    await loadInvoices()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    invoiceRejecting.value = false
  }
}

onMounted(() => {
  syncTabFromQuery()
  loadOptions()
  loadPending()
  loadInvoices()
})
</script>

<style scoped>
.approval-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.toolbar-tip {
  font-size: 13px;
  color: var(--text-3);
}

.toolbar-total {
  margin-left: auto;
  font-size: 13px;
  color: var(--text-3);
}

.text-muted {
  color: var(--text-3);
}
</style>
