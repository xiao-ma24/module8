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

    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :span="16">
        <el-card class="page-card">
          <template #header><b>{{ chartTitles.trend }}</b></template>
          <div ref="trendEl" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="page-card">
          <template #header><b>{{ chartTitles.cat }}</b></template>
          <div ref="catEl" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :span="8">
        <el-card class="page-card">
          <template #header><b>{{ chartTitles.sup }}</b></template>
          <div ref="supEl" style="height: 260px"></div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card class="page-card">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <b>{{ todoTitle }}</b>
              <el-tag type="success" effect="plain">验收合格率 {{ passRate }}%</el-tag>
            </div>
          </template>
          <el-tabs v-if="todoTabs.length">
            <el-tab-pane v-for="t in todoTabs" :key="t.label" :label="`${t.label} (${t.rows.length})`">
              <el-table :data="t.rows" size="small" height="200">
                <el-table-column prop="code" label="单号" width="150" />
                <el-table-column prop="title" label="内容" show-overflow-tooltip />
                <el-table-column label="金额" width="95">
                  <template #default="{ row: r }">{{ r.amount != null ? money(r.amount) : '—' }}</template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row: r }">
                    <el-tag v-if="r.status" :type="r.tag || 'info'" effect="plain" size="small">{{ r.status }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="110">
                  <template #default>
                    <router-link v-if="t.to" :to="t.to"><el-link type="primary">{{ t.action }}</el-link></router-link>
                    <el-link v-else type="danger" @click="advise">{{ t.action }}</el-link>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
          <el-empty v-else description="暂无待办事项" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="adviceDlg" title="AI 智能补货建议" width="640px">
      <div style="white-space: pre-wrap; line-height: 1.9; font-size: 13px">{{ adviceText }}</div>
      <template #footer>
        <el-button type="primary" @click="$router.push('/requests')">去创建采购申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import api, { post } from '../api'
import borrowApi from '../modules/borrow/api'
import { api as stApi } from '../modules/stocktake/api'
import { ST_STATUS } from '../modules/stocktake/common'
import { money, qty, PR_STATUS, PR_TAG, PO_STATUS, ACPT_STATUS, ACPT_TAG, IN_STATUS } from '../common'
import { ElMessage } from 'element-plus'

const cards = ref([])
const passRate = ref(100)
const pendingRequests = ref([])
const pendingInbounds = ref([])
const lowStock = ref([])
const trendEl = ref(), catEl = ref(), supEl = ref()
const expiringList = ref([])
const chartTitles = ref({ trend: '月度采购与入库金额趋势（近 6 个月）', cat: '采购类别占比', sup: '供应商集中度 TOP5' })
const adviceDlg = ref(false)
const adviceText = ref('')

const user = reactive({ userId: null, userName: '', roleCode: '' })
const myRequests = ref([]), approvedReq = ref([]), inTransitOrders = ref([])
const myAcpts = ref([]), myInbounds = ref([])
const stOrders = ref([])
const borrowOverdue = ref(0)

const TODO_TITLE = {
  APPLICANT: '我的采购申请', DEPT_HEAD: '我的待办 · 审批',
  PURCHASER: '我的待办 · 采购', CHECKER: '我的待办 · 验收',
  STORE_KEEPER: '我的待办 · 入库', STOCKTAKER: '我的待办 · 盘点',
}
const todoTitle = computed(() => TODO_TITLE[user.roleCode] || '待办事项')

const mapPr = r => ({
  code: r.prNo,
  title: r.usagePurpose || (r.sourceType === 4 ? '实验室自备（个人发票登记）' : '—'),
  amount: r.totalAmount, status: PR_STATUS[r.prStatus], tag: PR_TAG[r.prStatus]
})

// 流程待办：申请沿「申请人→负责人审批→采购→验收→入库」在每个人的工作台出现
const todoTabs = computed(() => {
  const rc = user.roleCode
  if (rc === 'APPLICANT') return [
    { label: '我的申请', action: '查看进度', to: '/requests', rows: myRequests.value.map(mapPr) },
  ]
  if (rc === 'DEPT_HEAD') return [
    { label: '待我审批', action: '去审批', to: '/requests?status=1', rows: pendingRequests.value.map(mapPr) },
    { label: '已审批记录', action: '查看', to: '/requests?status=2', rows: myRequests.value.filter(r => r.prStatus === 2).map(mapPr) },
  ]
  if (rc === 'PURCHASER') return [
    { label: '待采购（已批准申请）', action: '去采购', to: '/requests?status=2', rows: approvedReq.value.map(mapPr) },
    { label: '在途订单', action: '跟进', to: '/orders', rows: inTransitOrders.value.map(o => ({ code: o.poNo, title: o.supplierName, amount: o.totalAmount, status: PO_STATUS[o.poStatus], tag: 'warning' })) },
  ]
  if (rc === 'CHECKER') return [
    { label: '待我验收', action: '去验收', to: '/acceptances', rows: myAcpts.value.filter(a => a.acptStatus === 0 || a.acptStatus === 1).map(a => ({ code: a.acptNo, title: a.poNo, amount: a.totalAmount, status: ACPT_STATUS[a.acptStatus], tag: ACPT_TAG[a.acptStatus] })) },
  ]
  if (rc === 'STORE_KEEPER') return [
    { label: '待我入库', action: '去入库', to: '/inbounds', rows: myInbounds.value.filter(i => i.inStatus === 0).map(i => ({ code: i.inNo, title: i.warehouseName, amount: i.totalAmount, status: IN_STATUS[i.inStatus], tag: 'warning' })) },
    { label: '低库存提醒', action: 'AI 补货建议', to: '', rows: lowStock.value.map(s => ({ code: s.materialCode, title: s.materialName, amount: null })) },
  ]
  // 盘点员：只展示盘点任务与库存预警，不沿用采购/仓库待办
  if (rc === 'STOCKTAKER') return [
    { label: '待盘点任务', action: '去盘点', to: '/stocktake/orders', rows: stOrders.value.filter(o => [0, 1].includes(Number(o.status))).map(o => ({ code: o.stocktakeNo, title: o.warehouseName, amount: o.diffTotal, status: ST_STATUS[o.status], tag: o.status === 1 ? 'warning' : 'info' })) },
    { label: '低库存物资', action: 'AI 补货建议', to: '', rows: lowStock.value.map(s => ({ code: s.materialCode, title: s.materialName, amount: null })) },
  ]
  // 管理员等其他角色：全局视角
  return [
    { label: '待审批申请', action: '去审批', to: '/requests?status=1', rows: pendingRequests.value.map(mapPr) },
    { label: '待入库单', action: '去入库', to: '/inbounds', rows: pendingInbounds.value.map(i => ({ code: i.inNo, title: i.warehouseName, amount: i.totalAmount, status: IN_STATUS[i.inStatus] || '待入库', tag: 'warning' })) },
    { label: '低库存', action: 'AI 补货建议', to: '', rows: lowStock.value.map(s => ({ code: s.materialCode, title: s.materialName, amount: null })) },
  ]
})

// 环比：最后一个月 vs 上一个月
function deltaOf(trend, key) {
  if (!trend || trend.length < 2) return ''
  const a = Number(trend[trend.length - 1][key] || 0)
  const b = Number(trend[trend.length - 2][key] || 0)
  if (!b) return ''
  const d = ((a - b) / b) * 100
  return (d >= 0 ? ' ▲' : ' ▼') + Math.abs(d).toFixed(1) + '%'
}

onMounted(async () => {
  const s = await api.summary()
  const c = s.cards
  cards.value = [
    { label: '本月采购金额', value: money(c.monthOrderAmt), foot: '含在途与已入库 · 点击查看报表', icon: 'ShoppingCart', bg: 'linear-gradient(135deg,#3b82f6,#2563eb)', to: '/report', delta: deltaOf(s.trend, 'order') },
    { label: '待审批申请', value: c.pendingApprove, foot: '需要及时处理 · 点击去审批', icon: 'Bell', bg: 'linear-gradient(135deg,#6366f1,#4338ca)', to: '/requests?status=1' },
    { label: '在途订单', value: c.activeOrders, foot: '已确认/部分到货 · 点击跟进', icon: 'Van', bg: 'linear-gradient(135deg,#38bdf8,#0284c7)', to: '/orders' },
    { label: '本月入库金额', value: money(c.monthInboundAmt), foot: '已完成验收入库 · 点击查看', icon: 'Box', bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/inbounds', delta: deltaOf(s.trend, 'inbound') },
  ]
  passRate.value = s.passRate
  pendingRequests.value = s.pendingRequests
  pendingInbounds.value = s.pendingInbounds
  lowStock.value = s.lowStock

  // 按角色拉取流程待办
  try {
    const p = await api.profile(Number(localStorage.getItem('mockUserId') || 2))
    Object.assign(user, p.info || {})
  } catch {}
  const rc = user.roleCode
  // 部门负责人：待办只统计本部门，与采购申请页数据范围保持一致
  if (rc === 'DEPT_HEAD' && user.deptId) {
    pendingRequests.value = pendingRequests.value.filter(r => r.deptId != null && Number(r.deptId) === Number(user.deptId))
  }
  // 库管员：仓储视角卡片（待入库 / 库存预警 / 近效期 / 本月入库）
  if (rc === 'STORE_KEEPER') {
    try { expiringList.value = await api.expiring() } catch {}
    const expiring = expiringList.value
    cards.value = [
      { label: '待入库单', value: s.pendingInbounds.length, foot: '验收合格待入库 · 点击处理', icon: 'Box', bg: 'linear-gradient(135deg,#3b82f6,#2563eb)', to: '/inbounds' },
      { label: '库存预警', value: s.lowStock.length, foot: '低于安全库存 · 点击查看', icon: 'Warning', bg: 'linear-gradient(135deg,#f59e0b,#d97706)', to: '/stocktake/warning' },
      { label: '近效期批次', value: expiring.length, foot: '临近有效期 · 点击查看', icon: 'Clock', bg: 'linear-gradient(135deg,#38bdf8,#0284c7)', to: '/inbounds' },
      { label: '本月入库金额', value: money(c.monthInboundAmt), foot: '已确认入库 · 点击查看', icon: 'Wallet', bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/inbounds', delta: deltaOf(s.trend, 'inbound') },
    ]
  }
  // 部门负责人：审批与部门消耗视角
  if (rc === 'DEPT_HEAD') {
    cards.value = [
      { label: '待我审批', value: pendingRequests.value.length, foot: '本部门待审批 · 点击处理', icon: 'Bell', bg: 'linear-gradient(135deg,#6366f1,#4338ca)', to: '/requests?status=1' },
      { label: '本月申请金额', value: money(c.monthOrderAmt), foot: '本月采购申请规模', icon: 'ShoppingCart', bg: 'linear-gradient(135deg,#3b82f6,#2563eb)', to: '/requests' },
      { label: '在途订单', value: c.activeOrders, foot: '已确认/部分到货 · 点击跟进', icon: 'Van', bg: 'linear-gradient(135deg,#38bdf8,#0284c7)', to: '/orders' },
    ]
  }
  // 验收员：验收作业视角
  if (rc === 'CHECKER') {
    let acpts = [], returns = []
    try { acpts = await api.acceptances() } catch {}
    try { returns = await api.returns() } catch {}
    const today = new Date().toISOString().slice(0, 10)
    cards.value = [
      { label: '待我验收', value: acpts.filter(a => a.acptStatus === 0 || a.acptStatus === 1).length, foot: '到货待验收 · 点击处理', icon: 'Box', bg: 'linear-gradient(135deg,#3b82f6,#2563eb)', to: '/acceptances' },
      { label: '今日已验收', value: acpts.filter(a => a.acptStatus === 2 && String(a.acptDate || '').slice(0, 10) === today).length, foot: '当日完成验收单数', icon: 'CircleCheck', bg: 'linear-gradient(135deg,#10b981,#059669)', to: '/acceptances' },
      { label: '验收合格率', value: passRate.value + '%', foot: '合格数量 / 验收数量', icon: 'DataLine', bg: 'linear-gradient(135deg,#38bdf8,#0284c7)', to: '/acceptances' },
      { label: '退货待处理', value: returns.length, foot: '不合格品退货跟进', icon: 'RefreshLeft', bg: 'linear-gradient(135deg,#f59e0b,#d97706)', to: '/acceptances' },
    ]
  }
  // 盘点员：盘点与补货视角
  if (rc === 'STOCKTAKER') {
    let orders = [], warn = { total: 0 }, rep = { count: 0 }
    try { orders = await stApi.orders({ page: 1, size: 200 }) || [] } catch {}
    try { warn = await stApi.warningSummary() || warn } catch {}
    try { rep = await stApi.replenishSummary() || rep } catch {}
    const list = Array.isArray(orders) ? orders : (orders.records || [])
    stOrders.value = list
    cards.value = [
      { label: '待盘点任务', value: list.filter(o => [0, 1].includes(Number(o.status ?? o.orderStatus))).length, foot: '待开始/进行中 · 点击处理', icon: 'Files', bg: 'linear-gradient(135deg,#3b82f6,#2563eb)', to: '/stocktake/orders' },
      { label: '已完成盘点', value: list.filter(o => Number(o.status ?? o.orderStatus) === 2).length, foot: '已完成盘点任务 · 点击查看', icon: 'CircleCheck', bg: 'linear-gradient(135deg,#10b981,#059669)', to: '/stocktake/orders' },
      { label: '补货建议', value: rep.count || 0, foot: '建议补货物资数 · 点击生成计划', icon: 'ShoppingCart', bg: 'linear-gradient(135deg,#6366f1,#4338ca)', to: '/stocktake/replenish' },
      { label: '低库存物资', value: s.lowStock.length, foot: '低于安全库存 · 点击查看', icon: 'Goods', bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/materials' },
    ]
  }
  try {
    if (rc === 'APPLICANT' || rc === 'TEACHER') {
      const all = await api.requests()
      myRequests.value = all.filter(r => (r.applicantId ?? null) === user.userId || r.applicantName === user.userName)
    } else if (rc === 'DEPT_HEAD') {
      const all = await api.requests()
      myRequests.value = all.filter(r => (r.deptId != null && Number(r.deptId) === Number(user.deptId)) || r.applicantId === user.userId)
    } else if (rc === 'PURCHASER') {
      const all = await api.requests()
      approvedReq.value = all.filter(r => r.prStatus === 2)
      inTransitOrders.value = (await api.orders()).filter(o => o.poStatus === 1 || o.poStatus === 2)
      let acpts = []
      try { acpts = await api.acceptances() } catch {}
      // 采购员：只保留采购执行相关卡片
      cards.value = [
        { label: '已批准待采购', value: approvedReq.value.length, foot: '等待转采购订单 · 点击处理', icon: 'CircleCheck', bg: 'linear-gradient(135deg,#10b981,#059669)', to: '/requests?status=2' },
        { label: '在途订单', value: inTransitOrders.value.length, foot: '已确认/部分到货 · 点击跟进', icon: 'Van', bg: 'linear-gradient(135deg,#38bdf8,#0284c7)', to: '/orders' },
        { label: '待验收单', value: acpts.filter(a => a.acptStatus === 0 || a.acptStatus === 1).length, foot: '已到货待验收 · 点击查看', icon: 'Box', bg: 'linear-gradient(135deg,#3b82f6,#2563eb)', to: '/acceptances' },
        { label: '本月采购金额', value: money(c.monthOrderAmt), foot: '本月采购订单规模', icon: 'Wallet', bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/report' },
      ]
    } else if (rc === 'CHECKER') {
      myAcpts.value = await api.acceptances()
    } else if (rc === 'STORE_KEEPER') {
      myInbounds.value = await api.inbounds()
    }
  } catch {}

  // 申购人视角的借用逾期提醒：无借用令牌时先换发，保证一次登录可读全部业务
  if (rc === 'APPLICANT' || rc === 'TEACHER' || rc === 'STUDENT') {
    try {
      if (!localStorage.getItem('borrow_token')) {
        const uid = Number(localStorage.getItem('mockUserId') || user.userId || 2)
        const b = await post('/borrow/auth/handoff', { userId: uid })
        localStorage.setItem('borrow_token', b.token)
        localStorage.setItem('borrow_user', JSON.stringify(b.user))
      }
      borrowOverdue.value = (await borrowApi.overdue()).length
    } catch { borrowOverdue.value = 0 }
  }
  // 申购人/教师：我的申请视角
  if (rc === 'APPLICANT' || rc === 'TEACHER' || rc === 'STUDENT') {
    const mine = myRequests.value || []
    const month = new Date().toISOString().slice(0, 7)
    const monthAmt = mine.filter(r => String(r.createdAt || r.applyDate || '').slice(0, 7) === month)
      .reduce((a, r) => a + Number(r.totalAmount || 0), 0)
    cards.value = [
      { label: '我的申请', value: mine.length, foot: '全部状态 · 点击查看', icon: 'Document', bg: 'linear-gradient(135deg,#3b82f6,#2563eb)', to: '/requests' },
      { label: '本月申请金额', value: money(monthAmt), foot: '我本月提交的申请金额', icon: 'Wallet', bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/requests' },
      { label: '已批准待采购', value: mine.filter(r => r.prStatus === 2).length, foot: '等待采购员转订单', icon: 'CircleCheck', bg: 'linear-gradient(135deg,#10b981,#059669)', to: '/requests?status=2' },
      { label: '逾期未归还', value: borrowOverdue.value, foot: '借用超期未还 · 点击去归还', icon: 'Warning', bg: 'linear-gradient(135deg,#dc2626,#b91c1c)', to: '/borrow/returns' },
    ]
  }

  // 图表按角色切换口径
  const isKeeper = rc === 'STORE_KEEPER'
  const isStocktaker = rc === 'STOCKTAKER'
  const isChecker = rc === 'CHECKER'
  const isApplicant = ['APPLICANT', 'TEACHER', 'STUDENT'].includes(rc)
  const isDept = rc === 'DEPT_HEAD'
  if (isKeeper && !expiringList.value.length) {
    try { expiringList.value = await api.expiring() } catch {}
  }
  chartTitles.value = {
    trend: (isKeeper || isChecker || isStocktaker) ? '月度入库金额趋势（近 6 个月）'
      : (isApplicant || isDept) ? '月度采购金额趋势（近 6 个月）'
        : '月度采购与入库金额趋势（近 6 个月）',
    cat: (isKeeper || isStocktaker) ? '库存预警构成'
      : isChecker ? '验收状态分布'
        : (isApplicant || isDept) ? '申请状态分布' : '采购类别占比',
    sup: (isKeeper || isStocktaker) ? '低库存物资 TOP5' : '供应商集中度 TOP5',
  }

  nextTick(() => {
    // 先 dispose 旧实例避免重复 init 导致 legend/emphasis 失效
    for (const el of [trendEl.value, catEl.value, supEl.value]) {
      if (el) echarts.getInstanceByDom(el)?.dispose()
    }
    const inboundOnly = isKeeper || isChecker || isStocktaker
    const purchaseOnly = isApplicant || isDept
    const trendSeries = []
    if (!inboundOnly) trendSeries.push({ name: '采购金额', type: 'line', smooth: true, data: s.trend.map(t => t.order),
      areaStyle: { opacity: .15 }, itemStyle: { color: '#2563eb' } })
    if (!purchaseOnly) trendSeries.push({ name: '入库金额', type: 'line', smooth: true, data: s.trend.map(t => t.inbound),
      areaStyle: { opacity: .15 }, itemStyle: { color: '#16a34a' } })
    echarts.init(trendEl.value).setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: trendSeries.map(x => x.name) },
      grid: { left: 50, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: s.trend.map(t => t.month) },
      yAxis: { type: 'value' },
      series: trendSeries
    })
    let catData = s.category
    let catFmt = '{b}: ¥{c}（{d}%）'
    if (isKeeper || isStocktaker) {
      catData = [
        { name: '低库存', value: s.lowStock.length },
        { name: '近效期', value: expiringList.value.length },
      ]
      catFmt = '{b}: {c} 项（{d}%）'
    }
    if (isChecker) {
      catData = [0, 1, 2].map(k => ({ name: ACPT_STATUS[k], value: myAcpts.value.filter(a => a.acptStatus === k).length }))
      catFmt = '{b}: {c} 单（{d}%）'
    }
    if (isApplicant || isDept) {
      catData = [0, 1, 2, 3, 4].map(k => ({ name: PR_STATUS[k], value: myRequests.value.filter(r => r.prStatus === k).length }))
      catFmt = '{b}: {c} 单（{d}%）'
    }
    catData = (catData || []).filter(x => Number(x.value) > 0)
    echarts.init(catEl.value).setOption({
      tooltip: { trigger: 'item', formatter: catFmt },
      legend: { orient: 'horizontal', bottom: 2, left: 'center', itemGap: 10, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11, color: '#475569' } },
      series: [{ type: 'pie', radius: ['44%', '64%'], center: ['50%', '42%'],
        avoidLabelOverlap: true, minAngle: 8, minShowLabelAngle: 8,
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
        label: { position: 'inside', formatter: '{d}%', fontSize: 11, fontWeight: 600,
          color: '#fff', lineHeight: 14,
          // 添加描边，浅色扇区（黄/绿）上白字也能看清
          textBorderColor: 'rgba(15,30,61,.55)', textBorderWidth: 3 },
        labelLine: { show: false },
        data: catData.length ? catData : [{ name: '暂无', value: 1 }],
        emphasis: { scale: true, scaleSize: 6,
          itemStyle: { shadowBlur: 14, shadowColor: 'rgba(37,99,235,.30)' },
          label: { fontSize: 13, fontWeight: 'bold' } } }]
    })
    if (isKeeper || isStocktaker) {
      const top = [...s.lowStock].sort((a, b) => Number(a.qtyOnHand || 0) - Number(b.qtyOnHand || 0)).slice(0, 5)
      echarts.init(supEl.value).setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: v => v + ' 件' },
        grid: { left: 98, right: 62, top: 8, bottom: 8 },
        xAxis: { type: 'value', axisLabel: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: '#eef2f7' } } },
        yAxis: { type: 'category', data: top.map(x => x.materialName || x.materialCode).reverse(),
          axisTick: { show: false }, axisLine: { show: false },
          axisLabel: { color: '#475569', width: 88, overflow: 'truncate' } },
        series: [{ type: 'bar', data: top.map(x => Number(x.qtyOnHand || 0)).reverse(),
          itemStyle: { borderRadius: 6, color: '#f59e0b' }, barWidth: 14,
          label: { show: true, position: 'right', fontSize: 11, color: '#334155' } }]
      })
    } else {
      echarts.init(supEl.value).setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: v => money(v) },
        grid: { left: 98, right: 62, top: 8, bottom: 8 },
        xAxis: { type: 'value', axisLabel: { show: false }, axisTick: { show: false },
          splitLine: { lineStyle: { color: '#eef2f7' } } },
        yAxis: { type: 'category', data: s.supplierTop.map(x => x.name).reverse(),
          axisTick: { show: false }, axisLine: { show: false },
          axisLabel: { color: '#475569', width: 88, overflow: 'truncate' } },
        series: [{ type: 'bar', data: s.supplierTop.map(x => x.value).reverse(),
          itemStyle: { borderRadius: 6, color: '#60a5fa' }, barWidth: 14,
          label: { show: true, position: 'right', fontSize: 11, color: '#334155',
            formatter: p => p.value >= 10000 ? (p.value / 10000).toFixed(1) + ' 万' : p.value } }]
      })
    }
  })
})

async function advise() {
  const r = await api.aiAdvice()
  adviceText.value = r.text
  adviceDlg.value = true
}
</script>
