<template>
  <div>
    <!-- 统计卡 -->
    <el-row :gutter="14">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <div class="stat-card clickable" :style="{ background: c.bg }" @click="$router.push(c.to)">
          <el-icon class="icon"><component :is="c.icon" /></el-icon>
          <div style="font-size:13px; opacity:.9">{{ c.label }}</div>
          <div class="num">{{ c.value }}<span class="delta" v-if="c.delta">{{ c.delta }}</span></div>
          <div class="foot">{{ c.foot }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top:14px">
      <el-col :span="16">
        <el-card class="page-card">
          <template #header><b>借用数量 TOP10（按物资）</b></template>
          <div ref="matEl" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="page-card">
          <template #header><b>单据状态分布</b></template>
          <div ref="statusEl" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top:14px">
      <el-col :span="8">
        <el-card class="page-card">
          <template #header><b>借用与领用的本质区别</b></template>
          <div class="rule-list">
            <div class="rule-row">
              <span class="dot ok"></span>
              <div>
                <div class="rule-t">借出</div>
                <div class="rule-d">在库总量不变，只减可用量（物资仍在账上，被个人占用）</div>
              </div>
            </div>
            <div class="rule-row">
              <span class="dot ok"></span>
              <div>
                <div class="rule-t">完好归还</div>
                <div class="rule-d">恢复可用量，不改变在库总量</div>
              </div>
            </div>
            <div class="rule-row">
              <span class="dot danger"></span>
              <div>
                <div class="rule-t">损坏 / 缺失</div>
                <div class="rule-d">转报损单，审批并核销后才扣减在库总量</div>
              </div>
            </div>
          </div>
          <el-divider style="margin:12px 0" />
          <div class="muted" style="line-height:1.9; font-size:12px">
            这样既保证账面数量真实，又能让「账面有量但实际不可用」的问题在提交申请时就被发现。
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card class="page-card">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>{{ todoTitle }}</b>
              <el-tag type="primary" effect="plain">共 {{ todoCount }} 条</el-tag>
            </div>
          </template>
          <el-tabs v-if="todoTabs.length">
            <el-tab-pane v-for="t in todoTabs" :key="t.label" :label="`${t.label} (${t.rows.length})`">
              <el-table :data="t.rows" size="small" height="200">
                <el-table-column prop="code" label="单号" width="150" />
                <el-table-column prop="title" label="物资" show-overflow-tooltip />
                <el-table-column label="数量" width="90" align="right">
                  <template #default="{ row: r }">{{ r.qty }}</template>
                </el-table-column>
                <el-table-column label="预计归还" width="110">
                  <template #default="{ row: r }">{{ r.due }}</template>
                </el-table-column>
                <el-table-column label="状态" width="95" align="center">
                  <template #default="{ row: r }">
                    <el-tag v-if="r.status" :type="r.tag || 'info'" effect="plain" size="small">{{ r.status }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100">
                  <template #default>
                    <router-link :to="t.to"><el-link type="primary">{{ t.action }}</el-link></router-link>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
          <el-empty v-else description="暂无待办事项" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import api, { getUser } from '../api'
import { BORROW_STATUS, BORROW_TAG, num, date } from '../common'

const user = reactive(getUser() || {})
const data = ref({})
const cards = ref([])
const all = ref([])
const waitingApprove = ref([])
const waitingLend = ref([])
const borrowedRows = ref([])
const overdueRows = ref([])
const matEl = ref(), statusEl = ref()

const isKeeper = computed(() => user.roleCode === 'STORE_KEEPER')
const isApplicant = computed(() => ['APPLICANT', 'STUDENT', 'DEPT_HEAD'].includes(user.roleCode))
const myPending = computed(() => all.value.filter(r => r.borrowStatus === 0).length)
const myBorrowed = computed(() => all.value.filter(r => r.borrowStatus === 2).length)

const TODO_TITLE = {
  APPLICANT: '我的借用', STUDENT: '我的借用', DEPT_HEAD: '我的借用',
  STORE_KEEPER: '我的待办 · 借出与归还', ADMIN: '全局待办',
}
const todoTitle = computed(() => TODO_TITLE[user.roleCode] || '待办事项')

const map = r => ({
  code: r.borrowNo, title: r.materialName, qty: num(r.borrowQty),
  due: date(r.dueDate), status: BORROW_STATUS[r.borrowStatus], tag: BORROW_TAG[r.borrowStatus],
})

const todoTabs = computed(() => {
  const rc = user.roleCode
  if (rc === 'APPLICANT' || rc === 'STUDENT' || rc === 'DEPT_HEAD')
    return [
      { label: '我的借用', action: '查看详情', to: '/orders', rows: all.value.map(map) },
      { label: '待归还', action: '查看', to: '/returns', rows: borrowedRows.value.map(map) },
      { label: '逾期未还', action: '去归还', to: '/returns?tab=overdue', rows: overdueRows.value.map(map) },
    ]
  if (rc === 'STORE_KEEPER')
    return [
      { label: '待借出', action: '去借出', to: '/lends', rows: waitingLend.value.map(map) },
      { label: '在借待归还', action: '去验收', to: '/returns', rows: borrowedRows.value.map(map) },
      { label: '逾期', action: '催还', to: '/returns?tab=overdue', rows: overdueRows.value.map(map) },
    ]
  return [
    { label: '待审批', action: '查看', to: '/orders', rows: all.value.filter(r => r.borrowStatus === 0).map(map) },
    { label: '在借', action: '查看', to: '/returns', rows: borrowedRows.value.map(map) },
  ]
})
const todoCount = computed(() => todoTabs.value.reduce((s, t) => s + t.rows.length, 0))

const buildCards = () => {
  if (isApplicant.value) {
    cards.value = [
      { label: '我的借用单', value: all.value.length, foot: '我提交的全部借用申请 · 点击查看', icon: 'Tickets',
        bg: 'linear-gradient(135deg,#2563eb,#3b82f6)', to: '/orders' },
      { label: '待审批', value: myPending.value, foot: '等待库管员审批后借出', icon: 'Bell',
        bg: 'linear-gradient(135deg,#1d4ed8,#2563eb)', to: '/orders' },
      { label: '在借未还', value: myBorrowed.value, foot: '请按预计归还日期归还', icon: 'Box',
        bg: 'linear-gradient(135deg,#2563eb,#60a5fa)', to: '/returns' },
      { label: '逾期未还', value: overdueRows.value.length, foot: '已超期，请尽快归还', icon: 'Warning',
        bg: 'linear-gradient(135deg,#b91c1c,#dc2626)', to: '/returns?tab=overdue' },
    ]
  } else if (isKeeper.value) {
    cards.value = [
      { label: '待借出', value: data.value.waitLend || 0, foot: '已批准待办理借出 · 点击办理', icon: 'Bell',
        bg: 'linear-gradient(135deg,#1d4ed8,#2563eb)', to: '/lends' },
      { label: '待归还', value: data.value.borrowed || 0, foot: '在借物资待验收归还', icon: 'Box',
        bg: 'linear-gradient(135deg,#2563eb,#60a5fa)', to: '/returns' },
      { label: '逾期未还', value: data.value.overdue || 0, foot: '超过预计归还日期 · 需催还', icon: 'Warning',
        bg: 'linear-gradient(135deg,#b91c1c,#dc2626)', to: '/returns?tab=overdue' },
      { label: '借用单总数', value: data.value.total || 0, foot: '全部借用单 · 点击查看', icon: 'Tickets',
        bg: 'linear-gradient(135deg,#2563eb,#3b82f6)', to: '/orders' },
    ]
  } else {
    cards.value = [
      { label: '借用单总数', value: data.value.total || 0, foot: '累计借用与归还 · 点击查看', icon: 'Tickets',
        bg: 'linear-gradient(135deg,#2563eb,#3b82f6)', to: '/orders' },
      { label: '在借', value: data.value.borrowed || 0, foot: '物资在个人手中占用', icon: 'Box',
        bg: 'linear-gradient(135deg,#1d4ed8,#2563eb)', to: '/returns' },
      { label: '逾期未还', value: data.value.overdue || 0, foot: '超过预计归还日期 · 需催还', icon: 'Warning',
        bg: 'linear-gradient(135deg,#b91c1c,#dc2626)', to: '/returns?tab=overdue' },
      { label: '待借出', value: data.value.waitLend || 0, foot: '已批准待办理借出', icon: 'Bell',
        bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/lends' },
    ]
  }
}

onMounted(async () => {
  data.value = await api.dashboard()
  try { all.value = await api.orders() } catch { all.value = [] }
  try { borrowedRows.value = await api.borrowed() } catch { borrowedRows.value = [] }
  try { overdueRows.value = await api.overdue() } catch { overdueRows.value = [] }
  if (isKeeper.value) { try { waitingApprove.value = await api.pendingApprove() } catch { /* 忽略 */ } }
  if (isKeeper.value) { try { waitingLend.value = await api.pendingLend() } catch { /* 忽略 */ } }
  buildCards()

  nextTick(() => {
    for (const el of [matEl.value, statusEl.value]) if (el) echarts.getInstanceByDom(el)?.dispose()

    // 借用数量 TOP10
    const agg = {}
    all.value.forEach(r => {
      const k = r.materialName || '未知物资'
      agg[k] = (agg[k] || 0) + Number(r.borrowQty || 0)
    })
    const tops = Object.entries(agg).sort((a, b) => b[1] - a[1]).slice(0, 10)
    if (matEl.value) {
      echarts.init(matEl.value).setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 130, right: 40, top: 14, bottom: 26 },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: tops.map(x => x[0]).reverse(), axisLabel: { fontSize: 11 } },
        series: [{ type: 'bar', data: tops.map(x => x[1]).reverse(),
          itemStyle: { borderRadius: 6, color: '#3b82f6' }, barWidth: 14 }]
      })
    }

    const bs = data.value.byStatus || {}
    const pieData = Object.keys(bs).length
      ? Object.entries(bs).map(([k, v]) => ({ name: BORROW_STATUS[k] || ('状态' + k), value: v }))
      : [{ name: '暂无', value: 1 }]
    if (statusEl.value) {
      echarts.init(statusEl.value).setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 单（{d}%）' },
        legend: { orient: 'horizontal', bottom: 2, left: 'center', itemGap: 10,
                  itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11, color: '#475569' } },
        series: [{ type: 'pie', radius: ['34%', '50%'], center: ['50%', '40%'],
          avoidLabelOverlap: true, minAngle: 8, minShowLabelAngle: 4,
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}\n{c}', fontSize: 11, color: '#475569', lineHeight: 14 },
          labelLine: { length: 6, length2: 8, smooth: true, lineStyle: { width: 1 } },
          data: pieData,
          emphasis: { itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,.25)' }, label: { fontSize: 13, fontWeight: 'bold' } } }]
      })
    }
  })
})
</script>

<style scoped>
.rule-list { display: flex; flex-direction: column; gap: 12px; }
.rule-row { display: flex; gap: 10px; align-items: flex-start; }
.rule-row .dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex: 0 0 8px; }
.rule-row .dot.ok { background: #16a34a; }
.rule-row .dot.danger { background: #dc2626; }
.rule-t { font-size: 13px; font-weight: 600; color: #334155; }
.rule-d { font-size: 12px; color: #94a3b8; line-height: 1.7; margin-top: 2px; }
.muted { color: #94a3b8; }
</style>
