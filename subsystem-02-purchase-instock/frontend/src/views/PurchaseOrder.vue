<template>
  <div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name"
                     :label="`${t.label} (${countOf(t.name)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索订单号 / 供应商 / 关联申请" clearable style="width:250px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="supFilter" placeholder="供应商" clearable filterable style="width:200px" @change="reset">
          <el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
        </el-select>
        <el-select v-model="payFilter" placeholder="付款方式" clearable style="width:130px" @change="reset">
          <el-option v-for="(v,k) in PAY_TYPE" :key="k" :label="v" :value="+k" />
        </el-select>
        <div class="grow"></div>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
      </div>

      <el-table :data="paged" stripe>
        <el-table-column prop="poNo" label="订单号" width="145" />
        <el-table-column prop="supplierName" label="供应商" min-width="200" show-overflow-tooltip />
        <el-table-column label="评级" width="110">
          <template #default="{ row: r }"><span style="color:#f59e0b">{{ '★'.repeat(r.supplierStar || 0) }}</span></template>
        </el-table-column>
        <el-table-column prop="prNo" label="关联申请" width="140">
          <template #default="{ row: r }">{{ r.prNo || '直接采购' }}</template>
        </el-table-column>
        <el-table-column prop="planNo" label="采购计划" width="140">
          <template #default="{ row: r }">{{ r.planNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="订单日期" width="105"><template #default="{ row: r }">{{ date(r.orderDate) }}</template></el-table-column>
        <el-table-column label="要求到货" width="105"><template #default="{ row: r }">{{ date(r.expectDate) }}</template></el-table-column>
        <el-table-column label="金额" width="110" align="right"><template #default="{ row: r }">{{ money(r.totalAmount) }}</template></el-table-column>
        <el-table-column label="到货进度" width="150">
          <template #default="{ row: r }">
            <el-progress :percentage="progress[r.poId] ?? 0" :stroke-width="8"
                         :status="(progress[r.poId] ?? 0) >= 100 ? 'success' : undefined" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row: r }"><el-tag :type="PO_TAG[r.poStatus]" effect="dark" size="small">{{ PO_STATUS[r.poStatus] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row: r }">
            <el-link type="primary" @click="openDetail(r)">明细</el-link>
            <el-link v-if="r.poStatus === 0" type="success" style="margin-left:8px" @click="confirm(r)">确认</el-link>
            <el-link v-if="(r.poStatus === 1 || r.poStatus === 2) && r.remainQty > 0" type="warning" style="margin-left:8px"
                     @click="$router.push('/acceptances?poId=' + r.poId)">到货登记</el-link>
            <el-link v-if="r.poStatus === 0 || r.poStatus === 1" type="danger" style="margin-left:8px" @click="cancel(r)">取消</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <el-drawer v-model="detail.open" :title="detail.row?.poNo" size="720px">
      <!-- 流程条：按状态高亮当前节点 -->
      <el-steps :active="poStep(detail.row?.poStatus)" finish-status="success" align-center style="margin-bottom:18px">
        <el-step title="待确认" />
        <el-step title="已确认" />
        <el-step :title="detail.row?.poStatus === 2 ? '部分到货' : '到货中'" />
        <el-step title="已完成" />
        <template #process-status="{ status, index }">
          <el-icon v-if="status === 'error' || (index >= 0 && detail.row?.poStatus === 4)" style="color:#f56c6c"><CircleCloseFilled /></el-icon>
        </template>
      </el-steps>
      <div v-if="detail.row?.poStatus === 4" style="color:#f56c6c;font-size:12px;margin-top:-10px;text-align:center">⚠ 订单已取消</div>

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="供应商">{{ detail.row?.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.row?.contactPerson }} {{ detail.row?.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ PAY_TYPE[detail.row?.payType] }}</el-descriptions-item>
        <el-descriptions-item label="发票">{{ INVOICE[detail.row?.invoiceType] }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ money(detail.row?.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ PO_STATUS[detail.row?.poStatus] }}</el-descriptions-item>
        <el-descriptions-item label="采购计划">{{ detail.row?.planNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="累计到货">{{ qty(arrivedTotal) }} / {{ qty(detail.row?.poQty) }}</el-descriptions-item>
        <el-descriptions-item label="累计验收">{{ qty(detail.row?.acceptQty) }} / {{ qty(detail.row?.poQty) }}</el-descriptions-item>
        <el-descriptions-item label="剩余未到货">{{ qty(detail.row?.remainQty) }}</el-descriptions-item>
        <el-descriptions-item label="采购员">{{ detail.row?.buyerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ date(detail.row?.poDate) }}</el-descriptions-item>
      </el-descriptions>
      <el-alert v-if="pendingExchangeQty > 0" type="warning" :closable="false" style="margin-top:10px"
                :title="`该订单有 ${qty(pendingExchangeQty)} 件待换货：请先到「到货验收 → 退换货跟踪」标记换货完成，系统释放待补货数量后可重新登记到货`">
        <el-button size="small" type="primary" style="margin-top:6px" @click="goReturns">去处理换货</el-button>
      </el-alert>
      <el-alert v-else-if="pendingReturnQty > 0" type="info" :closable="false" style="margin-top:10px"
                :title="`该订单有 ${qty(pendingReturnQty)} 件待退货：请到「到货验收 → 退换货跟踪」标记退货完成`">
        <el-button size="small" @click="goReturns">去处理退货</el-button>
      </el-alert>
      <el-alert v-if="(acceptanceCount > 1)" type="warning" :closable="false" style="margin-top:10px"
                :title="`该订单已登记 ${acceptanceCount} 张验收单（分批到货），累计验收 ${qty(detail.row?.acceptQty)}，剩余未到货 ${qty(detail.row?.remainQty)}`" />

      <h4 style="margin:14px 0 8px">订单明细（红色为高于历史均价的成交价）</h4>
      <el-table :data="detail.items" size="small" border>
        <el-table-column prop="materialName" label="物资" min-width="150" />
        <el-table-column label="订购量" width="90"><template #default="{ row: r }">{{ qty(r.orderQty) }} {{ r.unit }}</template></el-table-column>
        <el-table-column label="已到货" width="80"><template #default="{ row: r }">{{ qty(r.arrivedQty) }}</template></el-table-column>
        <el-table-column label="已入库" width="80"><template #default="{ row: r }">{{ qty(r.inboundQty) }}</template></el-table-column>
        <el-table-column label="成交价" width="100">
          <template #default="{ row: r }">
            <span :style="{ color: r.historyAvgPrice && r.unitPrice > r.historyAvgPrice ? '#ef4444' : '#1f2937', fontWeight: 600 }">
              ¥{{ r.unitPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="历史均价" width="100">
          <template #default="{ row: r }"><span style="color:#94a3b8">¥{{ r.historyAvgPrice ?? '—' }}</span></template>
        </el-table-column>
      </el-table>
      <el-alert v-if="(detail.row?.poStatus === 1 || detail.row?.poStatus === 2) && pendingExchangeQty === 0" type="info" :closable="false"
                style="margin-top:12px" title="该订单可进行到货登记：验收合格后由系统自动生成待入库单" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '../api'
import { PO_STATUS, PO_TAG, PAY_TYPE, INVOICE, money, qty, date } from '../common'
import { exportExcel, printPdf } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const list = ref([]), suppliers = ref([]), progress = ref({})
const tab = ref('all'), kw = ref(''), supFilter = ref(null), payFilter = ref(null)
const tabs = [
  { name: 'all', label: '全部' }, { name: '0', label: '待确认' }, { name: '1', label: '已确认' },
  { name: '2', label: '部分到货' }, { name: '3', label: '已完成' }, { name: '4', label: '已取消' },
]
const filtered = computed(() => list.value.filter(r =>
  (tab.value === 'all' || r.poStatus === +tab.value) &&
  (supFilter.value == null || r.supplierId === supFilter.value) &&
  (payFilter.value == null || r.payType === payFilter.value) &&
  (!kw.value || (r.poNo || '').includes(kw.value) || (r.supplierName || '').includes(kw.value) || (r.prNo || '').includes(kw.value))))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const countOf = n => n === 'all' ? list.value.length : list.value.filter(r => r.poStatus === +n).length

// 订单状态 → stepper active index：0待确认→0, 1已确认→1, 2到货中→2, 3已完成→4(全部完成), 4已取消→0
function poStep(st) {
  if (st == null) return 0
  if (st === 4) return 0        // 已取消
  if (st === 3) return 4        // 已完成：四个节点全部完成
  return Math.min(st, 2)        // 待确认/已确认/到货中
}

const detail = reactive({ open: false, row: null, items: [], returns: [] })
// 累计到货取自订单明细 arrivedQty 之和，用于与累计验收、剩余数量对照，避免误以为重复验收
const arrivedTotal = computed(() => (detail.items || []).reduce((s, i) => s + Number(i.arrivedQty || 0), 0))
const acceptanceCount = ref(0)
// 待换货/待退货数量：换货需先在退换货跟踪闭环，系统才会释放待补货数量
const pendingExchangeQty = computed(() => (detail.returns || [])
  .filter(r => r.handleType === 3 && (r.handleStatus || 0) !== 1)
  .reduce((s, r) => s + Number(r.failQty || 0), 0))
const pendingReturnQty = computed(() => (detail.returns || [])
  .filter(r => r.handleType === 2 && (r.handleStatus || 0) !== 1)
  .reduce((s, r) => s + Number(r.failQty || 0), 0))
function goReturns() { router.push('/acceptances?tab=returns') }

onMounted(refresh)
async function refresh() {
  list.value = await api.orders()
  suppliers.value = await api.suppliers()
  for (const o of list.value) {
    const items = await api.orderItems(o.poId)
    const tot = items.reduce((s, i) => s + Number(i.orderQty || 0), 0)
    const arr = items.reduce((s, i) => s + Number(i.arrivedQty || 0), 0)
    progress.value[o.poId] = tot ? Math.round(arr / tot * 100) : 0
  }
}
async function openDetail(row) {
  detail.row = row
  detail.items = await api.orderItems(row.poId)
  acceptanceCount.value = 0
  detail.returns = []
  try {
    const rs = await api.returns()
    detail.returns = (rs || []).filter(r => r.poNo === row.poNo)
  } catch {}
  try {
    const acpts = await api.acceptances()
    acceptanceCount.value = (acpts || []).filter(a => a.poId === row.poId).length
  } catch {}
  detail.open = true
}
async function confirm(row) {
  await ElMessageBox.confirm(`确认订单 ${row.poNo} 并通知供应商发货？`, '确认订单')
  await api.confirmOrder(row.poId)
  ElMessage.success('已确认')
  refresh()
}
async function cancel(row) {
  await ElMessageBox.confirm(`取消订单 ${row.poNo} 会释放占用的预算，确认取消？`, '取消订单', { type: 'warning' })
  await api.cancelOrder(row.poId)
  ElMessage.success('已取消')
  refresh()
}

const cols = [
  { label: '订单号', prop: 'poNo' }, { label: '供应商', prop: 'supplierName', width: 30 },
  { label: '关联申请', prop: r => r.prNo || '直接采购' },
  { label: '采购计划', prop: r => r.planNo || '—' }, { label: '订单日期', prop: r => date(r.orderDate) },
  { label: '要求到货', prop: r => date(r.expectDate) },
  { label: '金额', prop: r => Number(r.totalAmount || 0).toFixed(2) },
  { label: '状态', prop: r => PO_STATUS[r.poStatus] },
]
function doExport(type) {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, filtered.value, '采购订单清单', '采购订单')
  else printPdf('采购订单清单', cols, filtered.value, '学校物资采购管理系统')
}
</script>
