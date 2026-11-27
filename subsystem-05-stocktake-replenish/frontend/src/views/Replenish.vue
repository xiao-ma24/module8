<template>
  <div>
    <el-card class="page-card fill" shadow="never">
      <div class="page-head">
        <div>
          <h2 class="page-title">补货计划</h2>
          <p class="page-desc">
            补货只读库存：按安全库存缺口生成建议（QS = S − qa − qt），再按包装规格取整为 QO；确认后写入共享表 purchase_plan（plan_type=1）。
          </p>
        </div>
        <div>
          <el-button @click="loadAll"><el-icon><Refresh /></el-icon>刷新</el-button>
        </div>
      </div>

      <el-tabs v-model="tab" @tab-change="loadAll">
        <!-- 补货建议 -->
        <el-tab-pane label="补货建议" name="suggest">
          <div class="kpi-grid">
            <div class="kpi warn">
              <div class="label">建议条目</div>
              <div class="value">{{ summary.count || 0 }}</div>
              <div class="hint">可用量已低于安全库存</div>
            </div>
            <div class="kpi">
              <div class="label">建议金额</div>
              <div class="value">{{ money(summary.amount) }}</div>
              <div class="hint">按参考单价估算</div>
            </div>
            <div class="kpi danger">
              <div class="label">紧急（可用量为 0）</div>
              <div class="value">{{ summary.urgent || 0 }}</div>
              <div class="hint">建议优先处理</div>
            </div>
            <div class="kpi ok">
              <div class="label">待审批计划</div>
              <div class="value">{{ pendingPlans }}</div>
              <div class="hint">草稿 {{ draftPlans }} 张</div>
            </div>
          </div>

          <div class="toolbar">
            <el-input v-model="kw" placeholder="搜索物资名称 / 编码" clearable style="width: 240px" @input="loadSuggestions">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <div class="grow"></div>
            <span v-if="picked.length" class="muted" style="font-size: 12.5px">
              已选 {{ picked.length }} 条，合计 {{ money(pickedAmount) }}
            </span>
            <el-button type="primary" @click="openPlanCreate"><el-icon><Plus /></el-icon>生成采购计划</el-button>
          </div>

          <el-table :data="pagedSuggestions" stripe @selection-change="picked = $event">
            <el-table-column type="selection" width="44" />
            <el-table-column label="物资" min-width="180">
              <template #default="{ row }">
                <b>{{ row.materialName }}</b>
                <div class="muted" style="font-size: 12px">{{ row.materialCode }} · {{ row.unit }}</div>
              </template>
            </el-table-column>
            <el-table-column label="日均消耗" width="96" align="right">
              <template #default="{ row }">{{ num(row.daily) }}</template>
            </el-table-column>
            <el-table-column label="提前期 / 覆盖" width="112" align="center">
              <template #default="{ row }">{{ row.leadTimeDays }} / {{ row.coverDays }} 天</template>
            </el-table-column>
            <el-table-column label="安全库存" width="92" align="right">
              <template #default="{ row }">{{ num(row.rop) }}</template>
            </el-table-column>
            <el-table-column label="可用量" width="86" align="right">
              <template #default="{ row }">
                <span :class="Number(row.qa) <= 0 ? 'danger' : 'warn'">{{ num(row.qa) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="在借量" width="80" align="right">
              <template #default="{ row }">{{ num(row.onLoan) }}</template>
            </el-table-column>
            <el-table-column label="在途量" width="80" align="right">
              <template #default="{ row }">{{ num(row.inTransit) }}</template>
            </el-table-column>
            <el-table-column label="建议量" width="92" align="right">
              <template #default="{ row }"><span class="warn">{{ num(row.suggestQty) }}</span></template>
            </el-table-column>
            <el-table-column label="包装 / 下单量" width="126" align="right">
              <template #default="{ row }">
                <span class="muted">{{ num(row.packSize) }}</span> / <b>{{ num(row.orderQty) }}</b>
              </template>
            </el-table-column>
            <el-table-column label="建议金额" width="110" align="right">
              <template #default="{ row }">{{ money(row.amount) }}</template>
            </el-table-column>
            <template #empty>
              <div class="muted" style="padding: 22px 0">当前没有低于安全库存的物资，或建议量已为 0</div>
            </template>
          </el-table>

          <div class="pager">
            <el-pagination v-model:current-page="sPage" :page-size="10" :total="suggestions.length"
                           layout="total, prev, pager, next" background />
          </div>
        </el-tab-pane>

        <!-- 采购计划 -->
        <el-tab-pane label="采购计划" name="plans">
          <div class="toolbar">
            <el-select v-model="planStatus" placeholder="全部状态" clearable style="width: 150px" @change="loadPlans">
              <el-option label="草稿" :value="0" />
              <el-option label="待审批" :value="1" />
              <el-option label="已批准" :value="2" />
              <el-option label="已下单" :value="3" />
              <el-option label="已关闭" :value="4" />
            </el-select>
            <div class="grow"></div>
            <el-button @click="submitPicked">提交审批</el-button>
            <el-button type="primary" @click="openApprove">审批</el-button>
          </div>

          <el-table :data="pagedPlans" stripe @selection-change="pickedPlans = $event">
            <el-table-column type="selection" width="44" />
            <el-table-column label="计划单号" width="160">
              <template #default="{ row }"><span class="mono">{{ row.planNo }}</span></template>
            </el-table-column>
            <el-table-column label="物资" min-width="180">
              <template #default="{ row }">
                <b>{{ row.materialName }}</b>
                <div class="muted" style="font-size: 12px">{{ row.materialCode }} · {{ row.unit }}</div>
              </template>
            </el-table-column>
            <el-table-column label="计划数量" width="110" align="right">
              <template #default="{ row }"><b>{{ num(row.planQty) }}</b></template>
            </el-table-column>
            <el-table-column label="生成时可用量" width="120" align="right">
              <template #default="{ row }">{{ num(row.stockQty) }}</template>
            </el-table-column>
            <el-table-column label="建议供应商" min-width="140">
              <template #default="{ row }">{{ row.supplierName || '待定' }}</template>
            </el-table-column>
            <el-table-column label="期望到货" width="110">
              <template #default="{ row }">{{ date(row.expectDate) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="96" align="center">
              <template #default="{ row }">
                <el-tag :type="PLAN_TAG[row.planStatus]" size="small" effect="dark">{{ PLAN_STATUS[row.planStatus] }}</el-tag>
              </template>
            </el-table-column>
            <template #empty>
              <div class="muted" style="padding: 22px 0">还没有采购计划，请先在“补货建议”中生成</div>
            </template>
          </el-table>

          <div class="pager">
            <el-pagination v-model:current-page="pPage" :page-size="10" :total="plans.length"
                           layout="total, prev, pager, next" background />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 生成采购计划 -->
    <el-dialog v-model="planCreate.open" title="由补货建议生成采购计划" width="620px">
      <div class="hint-bar">
        一计划一物资，单号按 <b>CG + 日期 + 4 位流水</b> 生成；计划写入后状态为“草稿”，提交后进入管理者审批。
      </div>
      <el-form label-width="96px">
        <el-form-item label="申请部门" required>
          <el-select v-model="planCreate.form.deptId" style="width: 100%">
            <el-option v-for="d in depts" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="建议供应商">
          <el-select v-model="planCreate.form.supplierId" clearable placeholder="可留空，由采购员确定" style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望到货">
          <el-date-picker v-model="planCreate.form.expectDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划明细">
          <el-table :data="planCreate.lines" size="small" max-height="240">
            <el-table-column prop="materialName" label="物资" min-width="150" />
            <el-table-column label="建议量" width="90" align="right">
              <template #default="{ row }">{{ num(row.suggestQty) }}</template>
            </el-table-column>
            <el-table-column label="下单量" width="110" align="right">
              <template #default="{ row }"><b>{{ num(row.orderQty) }}</b> {{ row.unit }}</template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planCreate.open = false">取消</el-button>
        <el-button type="primary" :loading="planCreate.loading" @click="createPlans">生成采购计划</el-button>
      </template>
    </el-dialog>

    <!-- 计划审批 -->
    <el-dialog v-model="approve.open" title="采购计划审批" width="520px">
      <el-form label-width="86px">
        <el-form-item label="审批计划">
          <span>{{ pickedPlans.length }} 张</span>
        </el-form-item>
        <el-form-item label="审批结论">
          <el-radio-group v-model="approve.pass">
            <el-radio :value="true">通过（已批准）</el-radio>
            <el-radio :value="false">驳回（已关闭）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approve.opinion" type="textarea" :rows="2" placeholder="如：同意补货，转采购员执行" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approve.open = false">取消</el-button>
        <el-button type="primary" :loading="approve.loading" @click="doApprove">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { PLAN_STATUS, PLAN_TAG, money, num, date } from '../common'
import { Refresh, Plus, Search } from '@element-plus/icons-vue'

const OPERATOR_ID = 1
const tab = ref('suggest')
const summary = ref({})
const suggestions = ref([])
const plans = ref([])
const kw = ref('')
const planStatus = ref(null)
const picked = ref([])
const pickedPlans = ref([])
const depts = ref([])
const suppliers = ref([])

const sPage = ref(1)
const pPage = ref(1)
const pagedSuggestions = computed(() => suggestions.value.slice((sPage.value - 1) * 10, sPage.value * 10))
const pagedPlans = computed(() => plans.value.slice((pPage.value - 1) * 10, pPage.value * 10))
const pickedAmount = computed(() => picked.value.reduce((n, r) => n + Number(r.amount || 0), 0))
const pendingPlans = computed(() => plans.value.filter(p => p.planStatus === 1).length)
const draftPlans = computed(() => plans.value.filter(p => p.planStatus === 0).length)

const planCreate = reactive({ open: false, loading: false, lines: [], form: { deptId: '', supplierId: null, expectDate: '' } })
const approve = reactive({ open: false, loading: false, pass: true, opinion: '同意补货，转采购员执行' })

const loadSuggestions = async () => {
  suggestions.value = await api.suggestions({ keyword: kw.value || undefined })
  summary.value = await api.replenishSummary()
  sPage.value = 1
}

const loadPlans = async () => {
  plans.value = await api.plans({ status: planStatus.value ?? undefined })
  pPage.value = 1
}

const loadAll = async () => {
  await Promise.all([loadSuggestions(), loadPlans()])
}

const openPlanCreate = () => {
  if (!picked.value.length) return ElMessage.warning('请先勾选补货建议')
  planCreate.lines = picked.value
  planCreate.form = {
    deptId: depts.value[0]?.deptId || '',
    supplierId: null,
    expectDate: new Date(Date.now() + 7 * 864e5).toISOString().slice(0, 10),
  }
  planCreate.open = true
}

const createPlans = async () => {
  planCreate.loading = true
  try {
    const r = await api.createPlans({
      deptId: planCreate.form.deptId,
      operatorId: OPERATOR_ID,
      lines: planCreate.lines.map(l => ({
        materialId: l.materialId,
        planQty: l.orderQty,
        stockQty: l.qa,
        suggestQty: l.suggestQty,
        rop: l.rop,
        daily: l.daily,
        supplierId: planCreate.form.supplierId,
        expectDate: planCreate.form.expectDate,
      })),
    })
    ElMessage.success(`已生成 ${r.created} 张采购计划（草稿）`)
    planCreate.open = false
    tab.value = 'plans'
    await loadAll()
  } finally {
    planCreate.loading = false
  }
}

const submitPicked = async () => {
  const ids = pickedPlans.value.filter(p => p.planStatus === 0).map(p => p.planId)
  if (!ids.length) return ElMessage.warning('请选择“草稿”状态的采购计划')
  const r = await api.submitPlans({ planIds: ids, approverId: OPERATOR_ID })
  ElMessage.success(`已提交 ${r.updated} 张采购计划，等待审批`)
  await loadPlans()
}

const openApprove = () => {
  const ids = pickedPlans.value.filter(p => p.planStatus === 1)
  if (!ids.length) return ElMessage.warning('请选择“待审批”的采购计划')
  approve.pass = true
  approve.open = true
}

const doApprove = async () => {
  approve.loading = true
  try {
    const ids = pickedPlans.value.filter(p => p.planStatus === 1).map(p => p.planId)
    const r = await api.approvePlans({
      planIds: ids, approverId: 4, pass: approve.pass, opinion: approve.opinion,
    })
    ElMessage.success(`已处理 ${r.updated} 张采购计划`)
    approve.open = false
    await loadPlans()
  } finally {
    approve.loading = false
  }
}

onMounted(async () => {
  const [d, s] = await Promise.all([api.depts(), api.suppliers()])
  depts.value = d
  suppliers.value = s
  await loadAll()
})
</script>
