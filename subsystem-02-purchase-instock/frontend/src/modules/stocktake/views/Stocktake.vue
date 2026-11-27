<template>
  <div>
    <el-card class="page-card fill" shadow="never">
      <div class="page-head">
        <div>
          <h2 class="page-title">盘点任务</h2>
          <p class="page-desc">按仓库建单并做账面快照；录入实盘只产生差异，完成盘点时才调用子系统② 库存服务写盘盈/盘亏流水。</p>
        </div>
        <div>
          <el-button @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
          <el-button v-if="canCreate" type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新建盘点单</el-button>
        </div>
      </div>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索盘点单号 / 仓库" clearable style="width: 240px" @input="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="load">
          <el-option label="待盘点" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已完成" :value="2" />
        </el-select>
        <div class="grow"></div>
        <span class="muted" style="font-size: 12.5px">共 {{ total }} 张盘点单</span>
      </div>

      <el-table :data="paged" stripe>
        <el-table-column prop="stocktakeNo" label="盘点单号" width="170">
          <template #default="{ row }"><span class="mono">{{ row.stocktakeNo }}</span></template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="110" />
        <el-table-column label="盘点日期" width="110">
          <template #default="{ row }">{{ date(row.stocktakeDate) }}</template>
        </el-table-column>
        <el-table-column prop="auditorName" label="负责人" width="90">
          <template #default="{ row }">{{ row.auditorName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="itemCount" label="明细数" width="80" align="right" />
        <el-table-column label="差异合计" width="100" align="right">
          <template #default="{ row }">
            <span :class="Number(row.diffTotal) > 0 ? 'danger' : Number(row.diffTotal) < 0 ? 'warn' : 'muted'">
              {{ num(row.diffTotal) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="ST_TAG[row.status]" effect="dark" size="small">{{ ST_STATUS[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-link v-if="row.status === 0 && canCount" type="success" @click="start(row)">开始盘点</el-link>
            <el-link v-if="row.status === 1 && canCount" type="primary" @click="openEntry(row)">录入实盘</el-link>
            <el-link v-if="row.status === 1 && canCount" type="warning" style="margin-left: 8px" @click="openFinish(row)">完成调账</el-link>
            <el-link v-if="row.status === 2" type="info" @click="openDetail(row)">查看</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>

    <!-- 新建盘点单 -->
    <el-drawer v-model="create.open" title="新建盘点单" size="560px">
      <div class="hint-bar">
        盘点单号按 <b>PD + 日期 + 4 位流水</b> 自动生成；账面数量在建单时写入快照，之后出库不影响本次差异。
      </div>
      <el-form label-width="92px">
        <el-form-item label="盘点仓库" required>
          <el-select v-model="create.form.warehouseId" style="width: 100%">
            <el-option v-for="w in warehouses.filter(x => x.warehouseType === 1)"
                       :key="w.warehouseId" :label="w.warehouseName" :value="w.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点日期">
          <el-date-picker v-model="create.form.stocktakeDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="盘点负责人">
          <el-select v-model="create.form.auditorId" filterable placeholder="选择负责人" style="width: 100%">
            <el-option v-for="u in auditorUsers" :key="u.userId" :label="u.userName + '（' + (u.roleCode || '') + '）'" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点物资">
          <el-select v-model="create.form.materialIds" multiple filterable clearable
                     placeholder="留空表示盘点该仓库全部在库物资" style="width: 100%">
            <el-option v-for="m in materials" :key="m.materialId"
                       :label="m.materialName + '（' + m.materialCode + '）'" :value="m.materialId" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="create.form.remark" type="textarea" :rows="2" placeholder="如：11 月月末常规盘点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="create.open = false">取消</el-button>
        <el-button type="primary" :loading="create.loading" @click="submitCreate">创建并录入</el-button>
      </template>
    </el-drawer>

    <!-- 录入实盘 -->
    <el-drawer v-model="entry.open" :title="'录入实盘 · ' + (entry.order?.stocktakeNo || '')" size="880px">
      <div class="hint-bar">
        差异 = 实盘 − 账面；|差异| 超过账面数量 10% 时需要二次录入确认。保存只记录差异，不写库存流水。
      </div>
      <el-table :data="entry.items" size="small" max-height="520">
        <el-table-column label="物资" min-width="170">
          <template #default="{ row }">
            <b>{{ row.materialName }}</b>
            <div class="muted" style="font-size: 12px">{{ row.materialCode }} · 批次 {{ row.batchNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="账面数量" width="100" align="right">
          <template #default="{ row }">{{ num(row.bookQuantity) }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="实盘数量" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.actualQuantity" :min="0" :precision="3" :controls="false"
                             size="small" style="width: 110px" placeholder="必填" />
          </template>
        </el-table-column>
        <el-table-column label="差异" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.actualQuantity === null || row.actualQuantity === undefined" class="muted">待录入</span>
            <span v-else :class="diffOf(row) > 0 ? 'danger' : diffOf(row) < 0 ? 'warn' : 'muted'">
              {{ diffOf(row) > 0 ? '+' : '' }}{{ num(diffOf(row)) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="差异原因" min-width="170">
          <template #default="{ row }">
            <el-input v-model="row.diffReason" size="small" placeholder="如：领用未及时出库" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="saveItem(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="entry.open = false">返回任务</el-button>
        <el-button type="warning" @click="openFinish(entry.order)">完成并调账</el-button>
      </template>
    </el-drawer>

    <!-- 完成调账 -->
    <el-dialog v-model="finish.open" title="完成盘点并调账" width="520px">
      <div class="hint-bar">
        审批通过后按明细写入盘盈（业务类型 5）/ 盘亏（业务类型 6）流水，由子系统② 的 InventoryService
        在同一事务内同步库存余额；幂等键 (bizType, 盘点单, 明细) 保证重复提交不重复记账。
      </div>
      <el-form label-width="86px">
        <el-form-item label="盘点单号">
          <span class="mono">{{ finish.order?.stocktakeNo }}</span>
        </el-form-item>
        <el-form-item label="差异合计">
          <span>{{ num(finish.order?.diffTotal) }}</span>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="finish.opinion" type="textarea" :rows="2" placeholder="如：差异原因属实，同意调账" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finish.open = false">取消</el-button>
        <el-button type="primary" :loading="finish.loading" @click="submitFinish">确认调账</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detail.open" :title="'盘点单详情 · ' + (detail.data?.order?.stocktakeNo || '')" size="820px">
      <el-descriptions v-if="detail.data" :column="3" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="仓库">{{ detail.data.order.warehouseId }}</el-descriptions-item>
        <el-descriptions-item label="盘点日期">{{ date(detail.data.order.stocktakeDate) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ ST_STATUS[detail.data.order.status] }}</el-descriptions-item>
        <el-descriptions-item label="差异明细数">{{ detail.data.diffLines }}</el-descriptions-item>
        <el-descriptions-item label="差异合计">{{ num(detail.data.diffTotal) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.data.order.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="detail.data" :data="detail.data.items" size="small">
        <el-table-column prop="materialName" label="物资" min-width="170" />
        <el-table-column prop="batchNo" label="批次" width="160" />
        <el-table-column label="账面" width="90" align="right"><template #default="{ row }">{{ num(row.bookQuantity) }}</template></el-table-column>
        <el-table-column label="实盘" width="90" align="right"><template #default="{ row }">{{ num(row.actualQuantity) }}</template></el-table-column>
        <el-table-column label="差异" width="100" align="right">
          <template #default="{ row }">
            <span :class="Number(row.diffQuantity) > 0 ? 'danger' : Number(row.diffQuantity) < 0 ? 'warn' : 'muted'">
              {{ num(row.diffQuantity) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="diffReason" label="原因" min-width="140" />
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import mainApi from '../../../api'
import { ST_STATUS, ST_TAG, date, num } from '../common'
import { usePager } from '../composables/usePager'
import { useRoute } from 'vue-router'
import { Refresh, Plus, Search } from '@element-plus/icons-vue'

const route = useRoute()

const OPERATOR_ID = Number(localStorage.getItem('mockUserId') || 0) || null
// 库管员生成盘点单，盘点员执行盘点；管理员两者皆可
const cur = reactive({ roleCode: '' })
const canCreate = computed(() => ['STORE_KEEPER', 'ADMIN'].includes(cur.roleCode))
const canCount = computed(() => ['STOCKTAKER', 'ADMIN'].includes(cur.roleCode))
const rows = ref([])
const kw = ref('')
const statusFilter = ref(null)
const warehouses = ref([])
const materials = ref([])
const users = ref([])
// 盘点负责人只列盘点员/管理员，避免指派给无盘点权限的人
const auditorUsers = computed(() => {
  const list = users.value.filter(u => ['STOCKTAKER', 'ADMIN'].includes(u.roleCode))
  return list.length ? list : users.value
})

const filtered = computed(() => rows.value)
const { page, pageSize, total, paged } = usePager(filtered, 10)

const load = async () => {
  rows.value = await api.orders({ status: statusFilter.value ?? undefined, keyword: kw.value || undefined })
}

const create = reactive({ open: false, loading: false, form: { warehouseId: 1, stocktakeDate: '', auditorId: null, materialIds: [], remark: '' } })
const entry = reactive({ open: false, order: null, items: [] })
const finish = reactive({ open: false, loading: false, order: null, opinion: '差异原因属实，同意调账' })
const detail = reactive({ open: false, data: null })

const openCreate = (prefill = {}) => {
  if (!canCreate.value) return ElMessage.warning('盘点单须由库管员生成，请通知库管员建单')
  create.form = { warehouseId: warehouses.value.find(w => w.warehouseType === 1)?.warehouseId || 1,
    stocktakeDate: new Date().toISOString().slice(0, 10), auditorId: auditorUsers.value[0]?.userId || null, materialIds: [], remark: '' }
  if (prefill.warehouseId) create.form.warehouseId = prefill.warehouseId
  if (prefill.materialId) create.form.materialIds = [prefill.materialId]
  create.open = true
}

const submitCreate = async () => {
  create.loading = true
  try {
    const order = await api.createOrder({ ...create.form, operatorId: OPERATOR_ID })
    ElMessage.success(`盘点单 ${order.stocktakeNo} 已创建`)
    create.open = false
    await load()
    await openEntry(order)
  } finally {
    create.loading = false
  }
}

const start = async row => {
  if (!canCount.value) return ElMessage.warning('盘点操作须由盘点员执行')
  await api.startOrder(row.stocktakeId, OPERATOR_ID)
  ElMessage.success('已开始盘点，可以录入实盘数量')
  await load()
}

const diffOf = row => {
  if (row.actualQuantity === null || row.actualQuantity === undefined) return 0
  return Number((Number(row.actualQuantity) - Number(row.bookQuantity)).toFixed(3))
}

const openEntry = async row => {
  if (!canCount.value) return ElMessage.warning('盘点操作须由盘点员执行')
  const data = await api.order(row.stocktakeId)
  entry.order = data.order
  entry.items = data.items
  entry.open = true
}

const saveItem = async row => {
  if (row.actualQuantity === null || row.actualQuantity === undefined) {
    ElMessage.warning('请先填写实盘数量')
    return
  }
  const payload = {
    itemId: row.itemId,
    actualQuantity: row.actualQuantity,
    diffReason: row.diffReason,
    operatorId: OPERATOR_ID,
    confirmOver10: false,
  }
  try {
    const r = await api.entryItem(entry.order.stocktakeId, payload)
    row.diffQuantity = r.diffQuantity
    ElMessage.success(`已保存：${r.diffType}`)
  } catch (e) {
    if (e && e.code === 5024) {
      await ElMessageBox.confirm(e.msg, '二次录入确认', { type: 'warning', confirmButtonText: '数量无误，确认提交' })
      const r = await api.entryItem(entry.order.stocktakeId, { ...payload, confirmOver10: true })
      row.diffQuantity = r.diffQuantity
      ElMessage.success(`已确认保存：${r.diffType}`)
    }
  }
}

const openFinish = async row => {
  if (!canCount.value) return ElMessage.warning('盘点操作须由盘点员执行')
  const data = await api.order(row.stocktakeId)
  finish.order = { ...data.order, diffTotal: data.diffTotal }
  finish.open = true
}

const submitFinish = async () => {
  finish.loading = true
  try {
    const r = await api.finishOrder(finish.order.stocktakeId, { operatorId: OPERATOR_ID, opinion: finish.opinion })
    ElMessage.success(`调账完成：盘盈 ${r.profit} 条、盘亏 ${r.loss} 条、相符 ${r.same} 条`)
    finish.open = false
    entry.open = false
    await load()
  } finally {
    finish.loading = false
  }
}

const openDetail = async row => {
  detail.data = await api.order(row.stocktakeId)
  detail.open = true
}

onMounted(async () => {
  try {
    const p = await mainApi.profile(Number(localStorage.getItem('mockUserId') || 2))
    cur.roleCode = p.info.roleCode
  } catch (error) {
    // 拉取当前用户失败时按无权限处理
  }
  const [w, m, u] = await Promise.all([api.warehouses(), api.materials(), api.users()])
  warehouses.value = w
  materials.value = m
  users.value = u
  await load()
  // 从库存预警「去盘点」跳转过来时，带物资/仓库并直接打开建单
  if (route.query.materialId || route.query.warehouseId) {
    openCreate({
      materialId: route.query.materialId ? Number(route.query.materialId) : null,
      warehouseId: route.query.warehouseId ? Number(route.query.warehouseId) : null,
    })
  }
})
</script>
