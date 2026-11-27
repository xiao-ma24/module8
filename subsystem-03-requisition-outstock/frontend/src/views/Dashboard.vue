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
          <template #header><b>{{ scopeLabel }}领用金额 TOP10<template v-if="!scopedRows">（已批准及以后）</template></b></template>
          <div ref="matEl" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="page-card">
          <template #header><b>{{ scopeLabel }}单据状态分布</b></template>
          <div ref="statusEl" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top:14px">
      <el-col :span="8">
        <el-card class="page-card">
          <template #header><b>部门月度领用额度{{ data.quotaPeriod ? '（' + data.quotaPeriod + '）' : '' }}</b></template>
          <template v-if="data.monthQuota !== undefined">
            <div class="quota-head">
              <span>{{ data.deptName }}</span>
              <el-tag :type="quotaPct > 90 ? 'danger' : quotaPct > 70 ? 'warning' : 'success'" effect="plain" size="small">
                已用 {{ quotaPct }}%
              </el-tag>
            </div>
            <el-progress :percentage="quotaPct" :stroke-width="16" :show-text="false"
                         :color="quotaPct > 90 ? '#dc2626' : quotaPct > 70 ? '#d97706' : '#16a34a'" />
            <div class="quota-foot">
              <div><span class="muted">已用</span><b>{{ money(data.usedAmount) }}</b></div>
              <div><span class="muted">月度额度</span><b>{{ money(data.monthQuota) }}</b></div>
              <div><span class="muted">剩余</span><b style="color:#16a34a">{{ money(data.quotaLeft) }}</b></div>
            </div>
          </template>
          <el-empty v-else description="本部门本月未核定额度" :image-size="60" />
          <el-divider style="margin:14px 0" />
          <div class="muted" style="line-height:1.9; font-size:12px">
            提交领用时校验可用库存与部门「月度」额度；出库后自动累加本月已用额度。
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
                <el-table-column label="数量/金额" width="110" align="right">
                  <template #default="{ row: r }">{{ r.amount != null ? money(r.amount) : r.qty }}</template>
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
import { REQ_STATUS, REQ_TAG, money, num } from '../common'

const user = reactive(getUser() || {})
const data = ref({})
const cards = ref([])
const byMaterial = ref({})
const mine = ref([])
const pendingOutboundRows = ref([])
const matEl = ref(), statusEl = ref()

const isKeeper = computed(() => user.roleCode === 'STORE_KEEPER')
const isApplicant = computed(() => ['APPLICANT', 'STUDENT', 'DEPT_HEAD'].includes(user.roleCode))
/** 申请人/部门负责人只统计自己的单据，库管员与管理员看全量 */
const scopedRows = computed(() => isApplicant.value ? mine.value : null)
const scopeLabel = computed(() => scopedRows.value ? '我的' : '全量')
const myPending = computed(() => mine.value.filter(r => r.reqStatus === 1).length)
const myApproved = computed(() => mine.value.filter(r => r.reqStatus === 3).length)
const myOut = computed(() => mine.value.filter(r => r.reqStatus >= 5).length)
const quotaPct = computed(() => {
  const t = Number(data.value.monthQuota || 0)
  if (!t) return 0
  return Math.min(100, Math.round((Number(data.value.usedAmount || 0) / t) * 100))
})

const TODO_TITLE = {
  APPLICANT: '我的领用申请', STUDENT: '我的领用申请', DEPT_HEAD: '我的领用申请',
  STORE_KEEPER: '我的待办 · 出库', ADMIN: '全局待办',
}
const todoTitle = computed(() => TODO_TITLE[user.roleCode] || '待办事项')

const map = r => ({
  code: r.reqNo, title: r.materialName, amount: r.amount, qty: num(r.qtyApply),
  status: REQ_STATUS[r.reqStatus], tag: REQ_TAG[r.reqStatus],
})

const todoTabs = computed(() => {
  const rc = user.roleCode
  if (rc === 'APPLICANT' || rc === 'STUDENT' || rc === 'DEPT_HEAD')
    return [{ label: '我的申请', action: '查看进度', to: '/orders', rows: mine.value.map(map) }]
  if (rc === 'STORE_KEEPER')
    return [
      { label: '待出库', action: '去出库', to: '/outbounds', rows: pendingOutboundRows.value.map(map) },
      { label: '已出库', action: '查看', to: '/outbounds', rows: mine.value.filter(r => r.reqStatus >= 5).map(map) },
    ]
  return [
    { label: '待审批', action: '查看', to: '/orders', rows: mine.value.filter(r => r.reqStatus === 1).map(map) },
    { label: '待出库', action: '查看', to: '/outbounds', rows: mine.value.filter(r => r.reqStatus === 3).map(map) },
  ]
})
const todoCount = computed(() => todoTabs.value.reduce((s, t) => s + t.rows.length, 0))

const buildCards = () => {
  const rc = user.roleCode
  if (isApplicant.value) {
    cards.value = [
      { label: '我的领用单', value: mine.value.length, foot: '我提交的全部领用申请 · 点击查看', icon: 'Tickets',
        bg: 'linear-gradient(135deg,#2563eb,#3b82f6)', to: '/orders' },
      { label: '待审批', value: myPending.value, foot: '等待库管员审批', icon: 'Bell',
        bg: 'linear-gradient(135deg,#1d4ed8,#2563eb)', to: '/orders' },
      { label: '已批准待领取', value: myApproved.value, foot: '待库管员办理出库', icon: 'Box',
        bg: 'linear-gradient(135deg,#2563eb,#60a5fa)', to: '/orders' },
      { label: '已出库', value: myOut.value, foot: '点击核对领用记录', icon: 'Money',
        bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/orders' },
    ]
  } else if (isKeeper.value) {
    cards.value = [
      { label: '待出库', value: data.value.waitOutbound || 0, foot: '已批准待拣货 · 点击办理', icon: 'Box',
        bg: 'linear-gradient(135deg,#2563eb,#60a5fa)', to: '/outbounds' },
      { label: '待审批', value: data.value.waitApprove || 0, foot: '库管员亦可审批领用', icon: 'Bell',
        bg: 'linear-gradient(135deg,#1d4ed8,#2563eb)', to: '/approve' },
      { label: '可见领用单', value: data.value.total || 0, foot: '全部领用单 · 点击查看', icon: 'Tickets',
        bg: 'linear-gradient(135deg,#2563eb,#3b82f6)', to: '/orders' },
      { label: '已出库金额', value: money(data.value.amount), foot: '出库即累加部门额度', icon: 'Money',
        bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/orders' },
    ]
  } else {
    cards.value = [
      { label: '可见领用单', value: data.value.total || 0, foot: '累计提交与处理 · 点击查看', icon: 'Tickets',
        bg: 'linear-gradient(135deg,#2563eb,#3b82f6)', to: '/orders' },
      { label: '待审批', value: data.value.waitApprove || 0, foot: '需要及时处理', icon: 'Bell',
        bg: 'linear-gradient(135deg,#1d4ed8,#2563eb)', to: '/orders' },
      { label: '待出库', value: data.value.waitOutbound || 0, foot: '已批准待拣货 · 点击办理', icon: 'Box',
        bg: 'linear-gradient(135deg,#2563eb,#60a5fa)', to: '/outbounds' },
      { label: '已出库金额', value: money(data.value.amount), foot: '出库即累加部门额度', icon: 'Money',
        bg: 'linear-gradient(135deg,#1e40af,#2563eb)', to: '/orders' },
    ]
  }
}

onMounted(async () => {
  data.value = await api.dashboard()
  try { byMaterial.value = (await api.stats()).byMaterial || {} } catch { byMaterial.value = {} }
  try { mine.value = await api.orders() } catch { mine.value = [] }
  if (isKeeper.value) { try { pendingOutboundRows.value = await api.pendingOutbound() } catch { /* 忽略 */ } }
  buildCards()

  nextTick(() => {
    for (const el of [matEl.value, statusEl.value]) if (el) echarts.getInstanceByDom(el)?.dispose()

    let mats
    if (scopedRows.value) {
      const agg = {}
      scopedRows.value.forEach(r => {
        const k = r.materialName || '未知物资'
        agg[k] = (agg[k] || 0) + Number(r.amount || 0)
      })
      mats = Object.entries(agg).sort((a, b) => b[1] - a[1]).slice(0, 10)
    } else {
      mats = Object.entries(byMaterial.value).sort((a, b) => b[1] - a[1]).slice(0, 10)
    }
    if (matEl.value) {
      echarts.init(matEl.value).setOption({
        tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/>金额 ¥${p[0].value}` },
        grid: { left: 130, right: 40, top: 14, bottom: 26 },
        xAxis: { type: 'value', axisLabel: { formatter: v => '¥' + v } },
        yAxis: { type: 'category', data: mats.map(x => x[0]).reverse(), axisLabel: { fontSize: 11 } },
        series: [{ type: 'bar', data: mats.map(x => x[1]).reverse(),
          itemStyle: { borderRadius: 6, color: '#3b82f6' }, barWidth: 14 }]
      })
    }

    const bs = data.value.byStatus || {}
    let pieData
    if (scopedRows.value) {
      const agg = {}
      scopedRows.value.forEach(r => { agg[r.reqStatus] = (agg[r.reqStatus] || 0) + 1 })
      pieData = Object.keys(agg).length
        ? Object.entries(agg).map(([k, v]) => ({ name: REQ_STATUS[k] || ('状态' + k), value: v }))
        : [{ name: '暂无', value: 1 }]
    } else {
      pieData = Object.keys(bs).length
        ? Object.entries(bs).map(([k, v]) => ({ name: REQ_STATUS[k] || ('状态' + k), value: v }))
        : [{ name: '暂无', value: 1 }]
    }
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
.quota-head { display: flex; align-items: center; justify-content: space-between; font-size: 13px;
  color: #334155; font-weight: 600; margin-bottom: 10px; }
.quota-foot { display: flex; justify-content: space-between; margin-top: 12px; font-size: 12px; }
.quota-foot b { display: block; font-size: 14px; color: #1f2937; margin-top: 2px; }
.muted { color: #94a3b8; }
</style>
