<template>
  <div>
    <el-card class="page-card fill" shadow="never">
      <div class="page-head">
        <div>
          <h2 class="page-title">库存预警</h2>
          <p class="page-desc">三档规则：低于补货点 / 临期 / 到期报废。预警只提示、不修改库存，核实后转盘点或转报损。</p>
        </div>
        <div>
          <el-button @click="openRules"><el-icon><Setting /></el-icon>预警规则</el-button>
          <el-button :loading="scanning" type="primary" @click="scan"><el-icon><Refresh /></el-icon>重新扫描</el-button>
        </div>
      </div>

      <div class="kpi-grid">
        <div class="kpi danger">
          <div class="label">预警总数</div>
          <div class="value">{{ summary.total || 0 }}</div>
          <div class="hint">扫描时间 {{ dt(summary.scanTime) }}</div>
        </div>
        <div class="kpi warn">
          <div class="label">低于补货点</div>
          <div class="value">{{ summary.low || 0 }}</div>
          <div class="hint">可用量 ≤ 补货点</div>
        </div>
        <div class="kpi warn">
          <div class="label">临期批次</div>
          <div class="value">{{ summary.nearExpire || 0 }}</div>
          <div class="hint">按规则提前天数判定</div>
        </div>
        <div class="kpi">
          <div class="label">已过期 / 到期报废</div>
          <div class="value">{{ (summary.expired || 0) + (summary.lifeDue || 0) }}</div>
          <div class="hint">须核验后转报损报废</div>
        </div>
      </div>

      <el-tabs v-model="tab" @tab-change="load">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="低于补货点" name="1" />
        <el-tab-pane label="临期" name="2" />
        <el-tab-pane label="已过期" name="3" />
        <el-tab-pane label="到期报废" name="4" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索物资名称 / 编码" clearable style="width: 240px" @input="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="grow"></div>
        <el-button link type="primary" @click="$router.push('/stocktake/replenish')">去生成补货建议 →</el-button>
      </div>

      <el-table :data="paged" stripe>
        <el-table-column label="预警类型" width="112">
          <template #default="{ row }">
            <el-tag size="small" :type="WARNING_TAG[row.warningType]">{{ row.warningLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="物资" min-width="190">
          <template #default="{ row }">
            <b>{{ row.materialName }}</b>
            <div class="muted" style="font-size: 12px">{{ row.materialCode }} · {{ row.unit }}</div>
          </template>
        </el-table-column>
        <el-table-column label="批次 / 到期日" width="180">
          <template #default="{ row }">
            <span v-if="row.batchNo" class="mono">{{ row.batchNo }}</span>
            <span v-else class="muted">—</span>
            <div v-if="row.expireDate" class="muted" style="font-size: 12px">{{ date(row.expireDate) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="当前值" width="110" align="right">
          <template #default="{ row }">{{ num(row.currentQty) }}</template>
        </el-table-column>
        <el-table-column label="阈值" width="100" align="right">
          <template #default="{ row }">{{ num(row.threshold) }}</template>
        </el-table-column>
        <el-table-column label="建议补货量" width="112" align="right">
          <template #default="{ row }"><span class="warn">{{ num(suggestMap[row.materialId]) }}</span></template>
        </el-table-column>
        <el-table-column label="级别" width="70" align="center">
          <template #default="{ row }">
            <span :class="row.level === '高' ? 'danger' : 'warn'">{{ row.level }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canScrap && isExpired(row)" link type="danger" @click="scrap(row)">报废核销</el-button>
            <el-tooltip v-else-if="row.stocktakeHandled" placement="top"
                        :content="`盘点单 ${row.stocktakeNo} 已完成；批次仍在库，需报废核销`">
              <span style="font-size: 12.5px; color: #94a3b8">已盘点 · 待报废</span>
            </el-tooltip>
            <el-button v-else link type="primary" @click="goStocktake(row)">去盘点</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>

    <!-- 规则管理 -->
    <el-drawer v-model="rules.open" title="预警规则（warning_rule）" size="760px">
      <div class="hint-bar">
        同一物资 + 仓库 + 类型的规则唯一；warehouse_id = 0 表示适用于全部仓库。
        未配置“安全库存”规则时，按 material.safe_stock 兜底判断补货点。
      </div>
      <div class="toolbar">
        <el-button type="primary" size="small" @click="openRuleEdit(null)"><el-icon><Plus /></el-icon>新增规则</el-button>
        <div class="grow"></div>
        <span class="muted" style="font-size: 12.5px">共 {{ ruleRows.length }} 条</span>
      </div>
      <el-table :data="ruleRows" size="small" max-height="480">
        <el-table-column label="物资" min-width="170">
          <template #default="{ row }">
            <b>{{ row.materialName }}</b>
            <div class="muted" style="font-size: 12px">{{ row.materialCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ RULE_TYPE[row.rule.warningType] }}</template>
        </el-table-column>
        <el-table-column label="阈值 / 参数" min-width="170">
          <template #default="{ row }">
            <span v-if="row.rule.warningType === 1">下限 {{ num(row.rule.minQuantity) }}</span>
            <span v-else-if="row.rule.warningType === 2">提前 {{ row.rule.expireDays || 30 }} 天</span>
            <span v-else>限用 {{ row.rule.serviceLifeYears || '物资默认' }} 年</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.rule.enable === 1 ? 'success' : 'info'">
              {{ row.rule.enable === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-link type="primary" @click="openRuleEdit(row.rule)">编辑</el-link>
            <el-link type="danger" style="margin-left: 8px" @click="removeRule(row.rule)">删除</el-link>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 规则编辑 -->
    <el-dialog v-model="ruleEdit.open" :title="ruleEdit.form.ruleId ? '编辑预警规则' : '新增预警规则'" width="520px">
      <el-form label-width="96px">
        <el-form-item label="物资" required>
          <el-select v-model="ruleEdit.form.materialId" filterable style="width: 100%">
            <el-option v-for="m in materials" :key="m.materialId"
                       :label="m.materialName + '（' + m.materialCode + '）'" :value="m.materialId" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警类型" required>
          <el-radio-group v-model="ruleEdit.form.warningType">
            <el-radio :value="1">安全库存</el-radio>
            <el-radio :value="2">临期</el-radio>
            <el-radio :value="3">到期报废</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ruleEdit.form.warningType === 1" label="库存下限">
          <el-input-number v-model="ruleEdit.form.minQuantity" :min="0" :precision="3" style="width: 180px" />
        </el-form-item>
        <el-form-item v-if="ruleEdit.form.warningType === 2" label="提前天数">
          <el-input-number v-model="ruleEdit.form.expireDays" :min="1" :max="365" style="width: 180px" />
        </el-form-item>
        <el-form-item v-if="ruleEdit.form.warningType === 3" label="限用年限">
          <el-input-number v-model="ruleEdit.form.serviceLifeYears" :min="1" :max="50" style="width: 180px" />
          <span class="muted" style="margin-left: 8px">留空取物资档案的限用年限</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleEdit.form.enable" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ruleEdit.form.remark" placeholder="如：按 11 月消耗调整" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleEdit.open = false">取消</el-button>
        <el-button type="primary" :loading="ruleEdit.loading" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { api as baseApi, post as httpPost } from '../../../api'
import { WARNING_TAG, RULE_TYPE, num, date, dt } from '../common'
import { usePager } from '../composables/usePager'
import { Refresh, Search, Setting, Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const OPERATOR_ID = 1
const userId = Number(localStorage.getItem('mockUserId') || 2)
const roleCode = ref('')
/** 到期 / 到期报废的批次，仅库管员与管理员可报废核销 */
const canScrap = computed(() => ['ADMIN', 'STORE_KEEPER'].includes(roleCode.value))
const isExpired = row => row.warningType === 3 || row.warningType === 4
const list = ref([])
const summary = ref({})
const kw = ref('')
const tab = ref('all')
const scanning = ref(false)
const suggestions = ref([])

const rules = reactive({ open: false })
const ruleRows = ref([])
const materials = ref([])
const ruleEdit = reactive({ open: false, loading: false, form: {} })

const filtered = computed(() => list.value)
const { page, pageSize, total, paged } = usePager(filtered, 10)

// 预警行本身不带建议量，按物资从补货建议里取，列表直接展示
const suggestMap = computed(() => Object.fromEntries(
  (suggestions.value || []).map(s => [s.materialId, s.suggestQty])
))

const load = async () => {
  const warningType = tab.value === 'all' ? undefined : Number(tab.value)
  const [warnings, sug] = await Promise.all([
    api.warnings({ warningType, keyword: kw.value || undefined }),
    api.suggestions().catch(() => []),
  ])
  list.value = warnings
  suggestions.value = Array.isArray(sug) ? sug : []
  summary.value = await api.warningSummary()
}

const scan = async () => {
  scanning.value = true
  try {
    const result = await api.scanWarnings()
    ElMessage.success(`扫描完成，共 ${result.length} 条预警`)
    await load()
  } finally {
    scanning.value = false
  }
}

const goStocktake = row => {
  router.push({
    path: '/stocktake/orders',
    query: {
      materialId: row.materialId,
      warehouseId: row.warehouseId || '',
    },
  })
}

/** 库管员报废核销：整批在库可用量一次性核销，写 7 号报废流水并扣减库存 */
const scrap = async row => {
  const qty = Number(row.currentQty || 0)
  if (!(qty > 0)) return ElMessage.warning('该批次可用库存为 0，无需报废')
  await ElMessageBox.confirm(
    `确认将「${row.materialName}」批次 ${row.batchNo || '无批次号'} 的 ${num(qty)} ${row.unit || ''} 报废核销？` +
    '核销后库存直接扣减，不可撤销。',
    '到期报废核销', { type: 'warning', confirmButtonText: '确认报废', cancelButtonText: '取消' })
  await httpPost('/purchase/inventory/scrap', {
    materialId: row.materialId,
    warehouseId: row.warehouseId,
    batchNo: row.batchNo,
    qty,
    // 报废核销没有业务单据，用时间戳作为唯一业务ID，避免流水幂等键冲突
    bizId: Date.now(),
    bizItemId: 0,
    operatorId: userId,
  })
  ElMessage.success('报废核销完成，库存已扣减')
  await api.scanWarnings()
  await load()
}

const openRules = async () => {
  ruleRows.value = await api.rules()
  rules.open = true
}

const openRuleEdit = rule => {
  ruleEdit.form = rule
    ? { ...rule }
    : { materialId: null, warningType: 1, enable: 1, minQuantity: null, expireDays: 30 }
  ruleEdit.open = true
}

const saveRule = async () => {
  if (!ruleEdit.form.materialId) return ElMessage.warning('请选择物资')
  ruleEdit.loading = true
  try {
    await api.saveRule({ ...ruleEdit.form, operatorId: OPERATOR_ID })
    ElMessage.success('规则已保存，已重新扫描')
    ruleEdit.open = false
    ruleRows.value = await api.rules()
    await load()
  } finally {
    ruleEdit.loading = false
  }
}

const removeRule = async rule => {
  await ElMessageBox.confirm('删除后该物资不再按此规则预警，是否继续？', '删除规则', { type: 'warning' })
  await api.deleteRule(rule.ruleId)
  ElMessage.success('已删除')
  ruleRows.value = await api.rules()
  await load()
}

onMounted(async () => {
  materials.value = await api.materials()
  await load()
  try {
    const p = await baseApi.profile(userId)
    roleCode.value = p?.info?.roleCode || ''
  } catch (e) {
    roleCode.value = ''
  }
})
</script>
