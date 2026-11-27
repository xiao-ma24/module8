<template>
  <div>
    <div class="page-head">
      <div>
        <h2 class="page-title">盘点预警与补货工作台</h2>
        <p class="page-desc">预警扫描 → 核实盘点 → 差异审批调账 → 补货建议 → 采购计划，库存变动统一由子系统② 库存服务执行。</p>
      </div>
      <div>
        <el-button :loading="scanning" @click="scan"><el-icon><Refresh /></el-icon>立即扫描预警</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi danger">
        <div class="label">预警总数</div>
        <div class="value">{{ summary.total || 0 }}</div>
        <div class="hint">低于补货点 {{ summary.low || 0 }} · 临期 {{ summary.nearExpire || 0 }}</div>
      </div>
      <div class="kpi warn">
        <div class="label">已过期批次</div>
        <div class="value">{{ summary.expired || 0 }}</div>
        <div class="hint">可用量已冻结，须转报损报废</div>
      </div>
      <div class="kpi">
        <div class="label">补货建议条目</div>
        <div class="value">{{ replenish.count || 0 }}</div>
        <div class="hint">预计金额 {{ money(replenish.amount) }}</div>
      </div>
      <div class="kpi ok">
        <div class="label">待审批采购计划</div>
        <div class="value">{{ pendingPlans }}</div>
        <div class="hint">草稿 {{ draftPlans }} 张</div>
      </div>
    </div>

    <el-row :gutter="12" style="margin-bottom: 12px">
      <el-col :span="24">
        <div class="entry-grid">
          <div class="entry" @click="$router.push('/stocktake')">
            <div class="icon"><Files /></div>
            <div class="name">盘点任务</div>
            <div class="desc">按仓库建单，账面数量做快照，录入实盘后自动算差异，超 10% 强制二次确认。</div>
          </div>
          <div class="entry" @click="$router.push('/warning')">
            <div class="icon"><Warning /></div>
            <div class="name">库存预警</div>
            <div class="desc">三档规则：低于补货点、临期、到期报废；预警只提示不改库存。</div>
          </div>
          <div class="entry" @click="$router.push('/replenish')">
            <div class="icon"><ShoppingCart /></div>
            <div class="name">补货计划</div>
            <div class="desc">按日均消耗与补货点计算建议量并取整到包装规格，生成采购计划转采购模块。</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <b>最紧急的 5 条预警</b>
          <el-button link type="primary" @click="$router.push('/warning')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="topWarnings" size="small">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="WARNING_TAG[row.warningType]">{{ row.warningLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="物资" min-width="180">
          <template #default="{ row }">
            <b>{{ row.materialName }}</b>
            <span class="muted" style="font-size:12px"> {{ row.materialCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次" width="150" />
        <el-table-column prop="message" label="说明" min-width="280" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { money, WARNING_TAG } from '../common'
import { Refresh, Files, Warning, ShoppingCart } from '@element-plus/icons-vue'

const summary = ref({})
const replenish = ref({})
const topWarnings = ref([])
const plans = ref([])
const scanning = ref(false)
const pendingPlans = ref(0)
const draftPlans = ref(0)

const load = async () => {
  const [s, r, w, p] = await Promise.all([
    api.warningSummary(), api.replenishSummary(), api.warnings(), api.plans()
  ])
  summary.value = s
  replenish.value = r
  topWarnings.value = (w || []).slice(0, 5)
  plans.value = p || []
  pendingPlans.value = plans.value.filter(x => x.planStatus === 1).length
  draftPlans.value = plans.value.filter(x => x.planStatus === 0).length
}

const scan = async () => {
  scanning.value = true
  try {
    const list = await api.scanWarnings()
    ElMessage.success(`扫描完成，共 ${list.length} 条预警`)
    await load()
  } finally {
    scanning.value = false
  }
}

onMounted(load)
</script>
