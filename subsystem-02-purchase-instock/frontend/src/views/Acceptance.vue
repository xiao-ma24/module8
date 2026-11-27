<template>
  <div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name"
                     :label="`${t.label} (${countOf(t.name)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索验收单号 / 订单号 / 供应商" clearable style="width:250px" @input="onFilter">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="scoreFilter" placeholder="验收评分" clearable style="width:130px" @change="onFilter">
          <el-option label="5 星（优）" :value="5" /><el-option label="4 星（良）" :value="4" />
          <el-option label="3 星及以下" :value="3" />
        </el-select>
        <el-radio-group v-if="tab === 'returns'" v-model="returnStatus" @change="resetReturn">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">待处理</el-radio-button>
          <el-radio-button :value="1">已完成</el-radio-button>
        </el-radio-group>
        <div class="grow"></div>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
        <el-button v-if="tab !== 'returns' && canRegister" type="primary" @click="openCreate"><el-icon><Plus /></el-icon>到货登记</el-button>
      </div>

      <!-- 退换货跟踪 -->
      <el-table v-if="tab === 'returns'" :data="returnPaged" stripe>
        <el-table-column prop="acptNo" label="验收单号" width="145" />
        <el-table-column prop="poNo" label="订单号" width="145" />
        <el-table-column prop="supplierName" label="供应商" min-width="170" show-overflow-tooltip />
        <el-table-column label="物资" min-width="160" show-overflow-tooltip>
          <template #default="{ row: r }">
            {{ r.materialName }}
            <el-tag v-if="r.isControlled === 1" type="danger" size="small" effect="plain">管控</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="不合格数量" width="110" align="right">
          <template #default="{ row: r }"><span style="color:#ef4444">{{ qty(r.failQty) }} {{ r.unit }}</span></template>
        </el-table-column>
        <el-table-column label="处置" width="80" align="center">
          <template #default="{ row: r }"><el-tag :type="r.handleType === 2 ? 'danger' : 'warning'" effect="dark" size="small">{{ HANDLE[r.handleType] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="failReason" label="不合格原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="处理状态" width="100" align="center">
          <template #default="{ row: r }">
            <el-tag :type="r.handleStatus === 1 ? 'success' : 'warning'" effect="plain" size="small">
              {{ r.handleStatus === 1 ? '已完成' : '待处理' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handledBy" label="处理人" width="90" />
        <el-table-column label="处理时间" width="150"><template #default="{ row: r }">{{ dt(r.handledAt) }}</template></el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row: r }">
            <el-link v-if="r.handleStatus !== 1" type="success" @click="finishReturnRow(r)">标记完成</el-link>
            <template v-else>
              <span style="color:#94a3b8">已闭环</span>
              <el-link v-if="r.handleType === 3" type="primary" style="margin-left:6px" @click="router.push('/orders')">登记到货</el-link>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="paged" stripe>
        <el-table-column prop="acptNo" label="验收单号" width="145" />
        <el-table-column prop="poNo" label="订单号" width="145" />
        <el-table-column prop="supplierName" label="供应商" min-width="190" show-overflow-tooltip />
        <el-table-column prop="deliverer" label="送货人" width="90" />
        <el-table-column prop="inspectorName" label="验收人" width="90" />
        <el-table-column label="验收日期" width="105"><template #default="{ row: r }">{{ date(r.acptDate) }}</template></el-table-column>
        <el-table-column label="评分" width="140">
          <template #default="{ row: r }">
            <el-rate v-if="r.acptScore" :model-value="r.acptScore" disabled size="small" />
            <span v-else style="color:#94a3b8">未评分</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row: r }"><el-tag :type="ACPT_TAG[r.acptStatus]" effect="dark" size="small">{{ ACPT_STATUS[r.acptStatus] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row: r }">
            <el-link type="primary" @click="openDetail(r)">详情</el-link>
            <el-link v-if="r.acptStatus === 1 && canAccept" type="success" style="margin-left:8px" @click="openComplete(r)">完成验收</el-link>
            <el-link style="margin-left:8px" @click="printOne(r)">打印</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-if="tab === 'returns'" v-model:current-page="rPage" v-model:page-size="rPageSize"
                       :total="rTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
        <el-pagination v-else v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <!-- 到货登记 -->
    <el-dialog v-model="create.open" title="到货登记" width="820px">
      <el-form label-width="90px">
        <el-form-item label="采购订单">
          <el-select v-model="create.poId" filterable style="width:100%" @change="loadPoItems">
            <el-option v-for="o in receivable" :key="o.poId"
                       :label="`${o.poNo}｜${o.supplierName}｜${money(o.totalAmount)}`" :value="o.poId" />
          </el-select>
        </el-form-item>
        <el-form-item label="送货人"><el-input v-model="create.deliverer" style="width:220px" /></el-form-item>
        <el-form-item label="验收人">
          <el-select v-model="create.inspectorId" style="width:220px">
            <el-option v-for="u in inspectors" :key="u.userId" :label="u.userName" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="到货明细">
          <el-alert v-if="!create.hasRemain" type="warning" :closable="false" show-icon style="margin-bottom:8px"
                    title="该采购订单已全部到货，无待登记数量，请选择其他订单" />
          <el-alert type="info" :closable="false" style="margin-bottom:8px"
                    title="采购员只登记本次到货数量；送检、合格/不合格核验由验收人完成验收时填写" />
          <el-table :data="create.items" size="small" border style="width:100%">
            <el-table-column label="物资" min-width="170">
              <template #default="{ row: r }">
                {{ r.materialName }}
                <el-tag v-if="r.isControlled === 1" type="danger" size="small" effect="plain">管控</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="订购量" width="90"><template #default="{ row: r }">{{ qty(r.orderQty) }}</template></el-table-column>
            <el-table-column label="已到货" width="90"><template #default="{ row: r }">{{ qty(r.arrivedQty) }}</template></el-table-column>
            <el-table-column label="本次到货" width="150">
              <template #default="{ row: r }"><el-input-number v-model="r.checkQty" :min="0" :max="r.orderQty - r.arrivedQty" :disabled="r.orderQty - r.arrivedQty <= 0" size="small" style="width:120px" /></template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="create.open = false">取消</el-button>
        <el-button type="primary" :loading="create.loading" :disabled="!create.hasRemain" @click="saveCreate">提交到货登记</el-button>
      </template>
    </el-dialog>

    <!-- 完成验收 -->
    <el-dialog v-model="complete.open" title="完成验收并入库" width="560px">
      <el-alert v-if="complete.controlled.length" type="warning" :closable="false" show-icon
                style="margin-bottom:12px"
                :title="`含管控物资：${complete.controlled.join('、')}`"
                description="管控物资验收需加签复核（部门负责人/资产管理岗签字确认）后方可入库。" />
      <el-form label-width="100px">
        <el-form-item label="验收明细">
          <el-table :data="complete.items" size="small" border style="width:100%">
            <el-table-column label="物资" min-width="140">
              <template #default="{ row: r }">{{ r.materialName }}</template>
            </el-table-column>
            <el-table-column label="送检数" width="110">
              <template #default="{ row: r }"><el-input-number v-model="r.checkQty" :min="1" size="small" style="width:90px" @change="v => clampCheck(r, v)" /></template>
            </el-table-column>
            <el-table-column label="合格数" width="110">
              <template #default="{ row: r }"><el-input-number v-model="r.passQty" :min="0" :max="r.checkQty" size="small" style="width:90px" /></template>
            </el-table-column>
            <el-table-column label="不合格数" width="90">
              <template #default="{ row: r }"><span :style="{ color: r.checkQty - r.passQty > 0 ? '#e5484d' : '#94a3b8' }">{{ qty(r.checkQty - r.passQty) }}</span></template>
            </el-table-column>
            <el-table-column label="处置" width="110">
              <template #default="{ row: r }">
                <el-select v-model="r.handleType" size="small" :disabled="r.checkQty - r.passQty <= 0"
                           :placeholder="r.checkQty - r.passQty <= 0 ? '无不合格' : '请选择'">
                  <el-option label="退货" :value="2" />
                  <el-option label="换货" :value="3" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="不合格原因" min-width="120">
              <template #default="{ row: r }">
                <el-input v-model="r.failReason" size="small" :disabled="r.checkQty - r.passQty <= 0" placeholder="包装破损/规格不符…" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item label="验收报告" required>
          <el-input v-model="complete.report" type="textarea" :rows="8"
                    placeholder="请先填写验收情况：数量是否一致、外观质量、随机附件/资料、存在问题等" />
          <div style="margin-top:6px">
            <el-button size="small" type="primary" plain :loading="complete.genLoading" @click="genReport">
              <el-icon><MagicStick /></el-icon>AI 生成草稿
            </el-button>
            <el-button size="small" plain @click="copyReport">复制</el-button>
            <span style="font-size:12px;color:#94a3b8;margin-left:8px">AI 仅生成草稿，请核对后确认验收</span>
          </div>
        </el-form-item>
        <el-form-item label="验收评分">
          <el-rate v-model="complete.score" show-text :texts="['极差','较差','一般','良好','优秀']" />
          <div style="font-size:12px; color:#94a3b8; margin-top:2px">评分占供应商评级权重的 60%</div>
        </el-form-item>
        <el-form-item label="入库仓库">
          <el-select v-model="complete.warehouseId" style="width:100%">
            <el-option v-for="w in warehouses" :key="w.warehouseId" :label="`${w.warehouseName}（${w.location}）`" :value="w.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收库管">
          <el-select v-model="complete.keeperId" style="width:100%">
            <el-option v-for="u in keepers" :key="u.userId" :label="u.userName" :value="u.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complete.open = false">取消</el-button>
        <el-button type="primary" :loading="complete.loading" @click="saveComplete">确认完成并生成入库单</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detail.open" :title="detail.row?.acptNo" size="700px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="订单号">{{ detail.row?.poNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ detail.row?.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="送货人">{{ detail.row?.deliverer }}</el-descriptions-item>
        <el-descriptions-item label="验收人">{{ detail.row?.inspectorName }}</el-descriptions-item>
        <el-descriptions-item label="验收日期">{{ date(detail.row?.acptDate) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ ACPT_STATUS[detail.row?.acptStatus] }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin:14px 0 8px">验收明细</h4>
      <el-table :data="detail.items" size="small" border>
        <el-table-column label="物资" min-width="140">
          <template #default="{ row: r }">
            {{ r.materialName }}
            <el-tag v-if="r.isControlled === 1" type="danger" size="small" effect="plain">管控·需加签</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="送检" width="75"><template #default="{ row: r }">{{ qty(r.checkQty) }}</template></el-table-column>
        <el-table-column label="合格" width="75"><template #default="{ row: r }"><span style="color:#16a34a">{{ qty(r.passQty) }}</span></template></el-table-column>
        <el-table-column label="不合格" width="80"><template #default="{ row: r }"><span style="color:#ef4444">{{ qty(r.failQty) }}</span></template></el-table-column>
        <el-table-column label="处置" width="80"><template #default="{ row: r }">{{ HANDLE[r.handleType] }}</template></el-table-column>
        <el-table-column prop="failReason" label="不合格原因" show-overflow-tooltip />
      </el-table>
      <h4 style="margin:14px 0 8px">验收报告</h4>
      <div class="report">{{ detail.row?.qualityReport || '暂无报告，完成验收后自动生成' }}</div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { ACPT_STATUS, ACPT_TAG, HANDLE, money, qty, date, dt } from '../common'
import { exportExcel, printPdf } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const list = ref([]), receivable = ref([]), users = ref([]), warehouses = ref([])
const returnList = ref([])
const tab = ref('all'), kw = ref(''), scoreFilter = ref(null), returnStatus = ref(null)
const tabs = [
  { name: 'all', label: '全部' }, { name: '1', label: '验收中' },
  { name: '2', label: '已完成' }, { name: '3', label: '质量异议' },
  { name: 'returns', label: '退换货跟踪' },
]
const filtered = computed(() => list.value.filter(r =>
  (tab.value === 'all' || r.acptStatus === +tab.value) &&
  (scoreFilter.value == null || (scoreFilter.value === 3 ? (r.acptScore || 0) <= 3 : r.acptScore === scoreFilter.value)) &&
  (!kw.value || (r.acptNo || '').includes(kw.value) || (r.poNo || '').includes(kw.value) || (r.supplierName || '').includes(kw.value))))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const filteredReturns = computed(() => returnList.value.filter(r =>
  (returnStatus.value == null || (r.handleStatus || 0) === returnStatus.value) &&
  (!kw.value || (r.acptNo || '').includes(kw.value) || (r.poNo || '').includes(kw.value) ||
   (r.supplierName || '').includes(kw.value) || (r.materialName || '').includes(kw.value))))
const { page: rPage, pageSize: rPageSize, total: rTotal, paged: returnPaged, reset: resetReturn } = usePager(filteredReturns, 10)
const countOf = n => n === 'returns' ? returnList.value.filter(r => (r.handleStatus || 0) !== 1).length
  : n === 'all' ? list.value.length : list.value.filter(r => r.acptStatus === +n).length
function onFilter() { reset(); resetReturn() }
function onTabChange() { onFilter() }
const inspectors = computed(() => users.value.filter(u => u.roleCode === 'CHECKER'))
const keepers = computed(() => users.value.filter(u => u.roleCode === 'STORE_KEEPER'))

const create = reactive({ open: false, loading: false, poId: null, deliverer: '', inspectorId: null, items: [], hasRemain: true })
const complete = reactive({ open: false, loading: false, genLoading: false, id: null, score: 5, warehouseId: null, keeperId: null, report: '', controlled: [], items: [] })
const detail = reactive({ open: false, row: null, items: [] })

const cur = reactive({ userId: null, roleCode: '', userName: '' })
// 到货登记由采购员或验收人办理；合格/不合格核验仍只能由验收人完成
const canRegister = computed(() => ['PURCHASER', 'CHECKER', 'ADMIN'].includes(cur.roleCode))
// 验收确认只能由验收人（或管理员）执行
const canAccept = computed(() => ['CHECKER', 'ADMIN'].includes(cur.roleCode))
async function refresh() {
  list.value = await api.acceptances()
  receivable.value = await api.receivableOrders()
  users.value = await api.users()
  warehouses.value = await api.warehouses()
  try { returnList.value = await api.returns() } catch { returnList.value = [] }
}
/** 从采购订单点「到货登记」带 poId 进来时自动打开登记弹窗（只在进入页面时执行一次） */
async function openFromQuery() {
  if (!route.query.poId) return
  create.poId = +route.query.poId
  await loadPoItems()
  create.open = true
}
async function loadPoItems() {
  const items = await api.orderItems(create.poId)
  create.items = items.map(i => ({ ...i, checkQty: i.orderQty - i.arrivedQty }))
  create.hasRemain = create.items.some(i => i.orderQty - i.arrivedQty > 0)
  if (!create.hasRemain) ElMessage.warning('该采购订单已全部到货，无待登记数量')
  create.deliverer = '送货师傅'
  create.inspectorId = inspectors.value.some(u => u.userId === cur.userId) ? cur.userId : inspectors.value[0]?.userId
}
function openCreate() {
  if (!canRegister.value) return ElMessage.warning('到货登记由采购员或验收人办理')
  create.poId = null; create.items = []; create.hasRemain = true
  create.open = true
}
/** 送检数调小后，合格数不能超过送检数 */
function clampPass(row, checkQty) {
  if (row.passQty > checkQty) row.passQty = checkQty
}
async function saveCreate() {
  if (!canRegister.value) return ElMessage.warning('到货登记由采购员或验收人办理')
  if (!create.poId) return ElMessage.warning('请选择采购订单')
  if (!create.hasRemain) return ElMessage.warning('该采购订单已全部到货，无待登记数量')
  if (!create.items.some(i => i.checkQty > 0)) return ElMessage.warning('请填写本次到货数量')
  create.loading = true
  try {
    const r = await api.createAcpt({
      poId: create.poId, deliverer: create.deliverer, inspectorId: create.inspectorId,
      items: create.items.filter(i => i.checkQty > 0).map(i => ({
        poiId: i.poiId, checkQty: i.checkQty, passQty: i.checkQty, handleType: 1, failReason: ''
      }))
    })
    ElMessage.success('验收单已创建：' + r.acptNo)
    create.open = false
    // 清掉 poId，避免刷新列表或再次进入时重复弹出到货登记
    if (route.query.poId) router.replace({ path: '/acceptances' })
    refresh()
  } finally { create.loading = false }
}
onMounted(async () => {
  try {
    const p = await api.profile(Number(localStorage.getItem('mockUserId') || 2))
    Object.assign(cur, { userId: p.info.userId, roleCode: p.info.roleCode, userName: p.info.userName })
  } catch (error) {
    // 拉取当前用户失败时按无验收权限处理
  }
  if (route.query.tab === 'returns') tab.value = 'returns'
  refresh()
  openFromQuery()
})

async function openComplete(row) {
  if (!canAccept.value) return ElMessage.warning('仅验收人可执行验收确认，采购员请完成到货登记后交由验收人核验')
  complete.id = row.acptId
  complete.score = 5; complete.report = ''
  complete.warehouseId = warehouses.value[0]?.warehouseId
  complete.keeperId = keepers.value[0]?.userId
  complete.controlled = []
  complete.items = []
  complete.open = true
  try {
    const items = await api.acptItems(row.acptId)
    complete.controlled = items.filter(i => i.isControlled === 1).map(i => i.materialName)
    complete.items = items.map(i => ({
      itemId: i.aiId, materialName: i.materialName,
      checkQty: Number(i.checkQty || 0), passQty: Number(i.passQty ?? i.checkQty ?? 0),
      handleType: i.handleType === 1 ? null : i.handleType, failReason: i.failReason || ''
    }))
  } catch {}
}
/** 送检数调小后，合格数不能超过送检数 */
function clampCheck(row, checkQty) {
  if (row.passQty > checkQty) row.passQty = checkQty
}
async function finishReturnRow(row) {
  const name = row.handleType === 2 ? '退货' : '换货'
  try {
    await ElMessageBox.confirm(
      `确认「${row.materialName}」的${name}（不合格 ${qty(row.failQty)}）已处理完成？`, `标记${name}完成`,
      { type: 'warning' })
  } catch { return }
  await api.finishReturn(row.aiId)
  ElMessage.success(row.handleType === 3
    ? `换货已标记完成，已释放 ${qty(row.failQty)} 个待补货数量，请到采购订单重新登记到货`
    : `${name}已标记完成`)
  refresh()
}
async function genReport() {
  if (!complete.id) return
  complete.genLoading = true
  try {
    // 直接使用页面当前核验数据生成草稿，避免后端沿用登记时的“全合格”默认值
    complete.report = await api.acptReport(complete.id, {
      acptScore: complete.score,
      items: complete.items.map(i => ({
        itemId: i.itemId, checkQty: i.checkQty, passQty: i.passQty,
        handleType: i.checkQty > i.passQty ? i.handleType : 1,
        failReason: i.checkQty > i.passQty ? i.failReason : ''
      }))
    })
  } finally { complete.genLoading = false }
}
function copyReport() {
  navigator.clipboard.writeText(complete.report)
  ElMessage.success('已复制')
}
async function saveComplete() {
  // 先核验明细、写验收文字，再确认验收
  if (!complete.items.length) return ElMessage.warning('验收明细为空，请刷新后重试')
  const badQty = complete.items.find(i => !i.checkQty || i.checkQty <= 0)
  if (badQty) return ElMessage.warning(`「${badQty.materialName}」送检数量必须大于 0`)
  const badHandle = complete.items.find(i => i.checkQty > i.passQty && !i.handleType)
  if (badHandle) return ElMessage.warning(`「${badHandle.materialName}」存在不合格数量，请选择退货或换货`)
  const badReason = complete.items.find(i => i.checkQty > i.passQty && !i.failReason.trim())
  if (badReason) return ElMessage.warning(`「${badReason.materialName}」存在不合格品，请填写不合格原因`)
  if (!complete.report || !complete.report.trim()) {
    return ElMessage.warning('请先填写验收报告（验收情况说明）再确认验收')
  }
  complete.loading = true
  try {
    const r = await api.completeAcpt(complete.id, {
      acptScore: complete.score, warehouseId: complete.warehouseId, keeperId: complete.keeperId,
      operatorId: Number(localStorage.getItem('mockUserId') || 0) || null,
      report: complete.report.trim(),
      items: complete.items.map(i => ({
        itemId: i.itemId, checkQty: i.checkQty, passQty: i.passQty,
        handleType: i.checkQty > i.passQty ? i.handleType : 1,
        failReason: i.checkQty > i.passQty ? i.failReason : ''
      }))
    })
    ElMessage.success(r ? '验收完成，已生成待入库单：' + (r.inNo || '') : '验收完成，无合格数量入库，不合格部分已转退换货处理')
    complete.open = false
    refresh()
  } finally { complete.loading = false }
}
async function openDetail(row) {
  detail.row = row
  detail.items = await api.acptItems(row.acptId)
  detail.open = true
}
async function printOne(row) {
  const items = await api.acptItems(row.acptId)
  printPdf(`到货验收单 ${row.acptNo}`,
    [{ label: '物资', prop: 'materialName' }, { label: '送检', prop: 'checkQty' },
     { label: '合格', prop: 'passQty' }, { label: '不合格', prop: 'failQty' },
     { label: '处置', prop: r => HANDLE[r.handleType] }, { label: '原因', prop: 'failReason' }],
    items, `订单 ${row.poNo}｜供应商 ${row.supplierName}｜验收人 ${row.inspectorName}`)
}

const cols = [
  { label: '验收单号', prop: 'acptNo' }, { label: '订单号', prop: 'poNo' },
  { label: '供应商', prop: 'supplierName', width: 30 }, { label: '送货人', prop: 'deliverer' },
  { label: '验收人', prop: 'inspectorName' }, { label: '验收日期', prop: r => date(r.acptDate) },
  { label: '评分', prop: r => r.acptScore || '' }, { label: '状态', prop: r => ACPT_STATUS[r.acptStatus] },
]
const returnCols = [
  { label: '验收单号', prop: 'acptNo' }, { label: '订单号', prop: 'poNo' },
  { label: '供应商', prop: 'supplierName', width: 30 }, { label: '物资', prop: 'materialName', width: 30 },
  { label: '不合格数量', prop: r => qty(r.failQty) + (r.unit || '') },
  { label: '处置', prop: r => HANDLE[r.handleType] },
  { label: '不合格原因', prop: r => r.failReason || '' },
  { label: '处理状态', prop: r => (r.handleStatus === 1 ? '已完成' : '待处理') },
  { label: '处理人', prop: r => r.handledBy || '' },
  { label: '处理时间', prop: r => dt(r.handledAt) },
]
function doExport(type) {
  if (tab.value === 'returns') {
    if (!filteredReturns.value.length) return ElMessage.warning('没有可导出的数据')
    if (type === 'xlsx') exportExcel(returnCols, filteredReturns.value, '退换货跟踪', '到货验收')
    else printPdf('退换货跟踪清单', returnCols, filteredReturns.value, '学校物资采购管理系统')
    return
  }
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, filtered.value, '到货验收清单', '到货验收')
  else printPdf('到货验收清单', cols, filtered.value, '学校物资采购管理系统')
}
</script>

<style scoped>
.report { white-space: pre-wrap; background: #f8fafc; border: 1px solid #e8eef8; border-radius: 8px;
          padding: 14px; font-size: 13px; line-height: 1.9; color: #334155; max-height: 340px; overflow-y: auto; }
</style>
