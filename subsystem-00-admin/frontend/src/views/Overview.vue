<template>
  <div class="page-card fill" v-loading="loading">
    <div class="head">
      <div>
        <h3>平台数据总览</h3>
        <p class="sub">全平台账号、组织、配置与安全态势 · {{ today }}</p>
      </div>
      <div class="ops">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Download" @click="exportAll">导出总览</el-button>
      </div>
    </div>

    <el-row :gutter="14">
      <el-col :xs="24" :sm="12" :md="6" :lg="6" v-for="c in cards" :key="c.label" class="stat-col">
        <div class="stat-card clickable" :style="{ background: c.bg }" @click="onCardClick(c)">
          <el-icon class="icon"><component :is="c.icon" /></el-icon>
          <div class="label">{{ c.label }}</div>
          <div class="num">{{ c.value }}<span class="unit">{{ c.unit }}</span></div>
          <div class="foot">{{ c.foot }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14">
      <el-col :md="14">
        <el-card class="panel">
          <template #header>
            <div class="panel-head">
              <span>近 7 天登录趋势</span>
              <el-tag size="small" effect="plain">今日登录 {{ data.todayLogin || 0 }} 次</el-tag>
            </div>
          </template>
          <div ref="trendRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card class="panel">
          <template #header>
            <div class="panel-head">
              <span>角色分布</span>
              <el-tag size="small" type="info" effect="plain">共 {{ data.roleTotal || 0 }} 个启用角色</el-tag>
            </div>
          </template>
          <div ref="roleRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="row-gap">
      <el-col :md="14">
        <el-card class="panel">
          <template #header>
            <div class="panel-head">
              <span>最近操作</span>
              <el-link type="primary" :underline="false" @click="$router.push('/logs')">查看全部</el-link>
            </div>
          </template>
          <el-timeline class="tl">
            <el-timeline-item v-for="(r, i) in data.recentOps || []" :key="i" :timestamp="dt(r.createdAt)"
                              :type="i === 0 ? 'primary' : ''">
              <b>{{ r.userName }}</b> 在「{{ r.module }}」{{ r.action }} <span class="muted">{{ r.target }}</span>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="!(data.recentOps || []).length" description="暂无操作记录" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card class="panel">
          <template #header>
            <div class="panel-head"><span>平台健康检查</span></div>
          </template>
          <div class="health">
            <div class="h-row" v-for="h in health" :key="h.name">
              <span class="h-name">{{ h.name }}</span>
              <el-tag :type="h.type" size="small" effect="light">{{ h.text }}</el-tag>
            </div>
          </div>
          <el-divider style="margin: 10px 0" />
          <div class="quick">
            <el-button size="small" @click="$router.push('/users')">用户管理</el-button>
            <el-button size="small" @click="$router.push('/roles')">角色权限</el-button>
            <el-button size="small" @click="$router.push('/dicts')">数据字典</el-button>
            <el-button size="small" @click="$router.push('/configs')">系统参数</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Refresh, Download, User, OfficeBuilding, Key, Notebook, Goods, Warning, Document, Clock } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '../api'
import { exportExcel } from '../utils/export'
import { dt } from '../common'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const data = ref({})
const trendRef = ref()
const roleRef = ref()
let trendChart = null
let roleChart = null
const today = new Date().toLocaleDateString('zh-CN')

// 采购端 base URL（开发端口 5182，history 模式无 #）
const PURCHASE_BASE = 'http://localhost:5182/'
// 大系统是独立端口/独立登录态，跳转时携带当前平台用户，避免落到 5182 登录页
const platformUid = JSON.parse(localStorage.getItem('adminUser') || '{}').userId
const HANDOFF = platformUid ? `?uid=${platformUid}` : ''
// 统一蓝色主色（柔和商务蓝：深浅差异细微不花哨）
const CARD_BG = [
  'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
  'linear-gradient(135deg, #6366f1 0%, #4338ca 100%)',
  'linear-gradient(135deg, #38bdf8 0%, #0284c7 100%)',
  'linear-gradient(135deg, #1e40af 0%, #2563eb 100%)'
]

const cards = computed(() => [
  { label: '平台用户', value: data.value.userTotal ?? 0, unit: '人', foot: `启用 ${data.value.userActive ?? 0} · 停用 ${data.value.userDisabled ?? 0}`, to: '/users', icon: User },
  { label: '组织机构', value: data.value.deptTotal ?? 0, unit: '个', foot: '部门与预算归属', to: '/depts', icon: OfficeBuilding },
  { label: '角色数量', value: data.value.roleTotal ?? 0, unit: '个', foot: '按菜单授权', to: '/roles', icon: Key },
  { label: '字典项', value: data.value.dictTotal ?? 0, unit: '项', foot: `${data.value.configTotal ?? 0} 个系统参数`, to: '/dicts', icon: Notebook },
  { label: '在管物资', value: data.value.materialTotal ?? 0, unit: '种', foot: `供应商 ${data.value.supplierTotal ?? 0} 家（点击查看物资目录）`, to: PURCHASE_BASE + 'materials' + HANDOFF, icon: Goods, external: true },
  { label: '低库存预警', value: data.value.lowStock ?? 0, unit: '项', foot: '点击查看库存预警明细', to: PURCHASE_BASE + 'stocktake/warning' + HANDOFF, icon: Warning, external: true },
  { label: '待处理单据', value: (data.value.prPending ?? 0) + (data.value.inboundTodo ?? 0), unit: '张', foot: `采购在途 ${data.value.poInTransit ?? 0} 张（点击查看采购申请）`, to: PURCHASE_BASE + 'requests' + HANDOFF, icon: Document, external: true },
  { label: '今日操作', value: data.value.todayOps ?? 0, unit: '次', foot: `7 天登录失败 ${data.value.loginFail7d ?? 0} 次`, to: '/logs', icon: Clock }
].map((c, i) => ({ ...c, bg: CARD_BG[i % CARD_BG.length] })))

// 卡片点击：业务模块在大系统（5182）内，当前标签直接打开对应页面；平台自身页面走 hash 路由
function onCardClick(c) {
  if (c.external) {
    window.location.href = c.to
  } else {
    window.location.hash = '#' + c.to
  }
}

const health = computed(() => {
  const list = []
  list.push({
    name: '账号状态',
    text: (data.value.userDisabled ?? 0) > 0 ? `${data.value.userDisabled} 个账号停用` : '全部正常',
    type: (data.value.userDisabled ?? 0) > 0 ? 'warning' : 'success'
  })
  list.push({
    name: '登录安全',
    text: (data.value.loginFail7d ?? 0) > 3 ? `7 天失败 ${data.value.loginFail7d} 次，建议核查` : '无明显异常',
    type: (data.value.loginFail7d ?? 0) > 3 ? 'danger' : 'success'
  })
  list.push({
    name: '字典完整性',
    text: (data.value.dictTotal ?? 0) >= 5 ? '主要枚举已维护' : '建议补充字典',
    type: (data.value.dictTotal ?? 0) >= 5 ? 'success' : 'warning'
  })
  list.push({
    name: '审计留存',
    text: `已记录 ${data.value.todayOps ?? 0} 次今日操作`,
    type: 'info'
  })
  return list
})

async function load() {
  loading.value = true
  try {
    data.value = await api.overview()
    await nextTick()
    drawTrend()
    drawRole()
  } finally {
    loading.value = false
  }
}

function drawTrend() {
  if (!trendRef.value) return
  echarts.getInstanceByDom(trendRef.value)?.dispose()
  trendChart = echarts.init(trendRef.value)
  const rows = data.value.loginTrend || []
  trendChart.setOption({
    grid: { left: 34, right: 16, top: 24, bottom: 26 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: rows.map(r => r.label), axisLine: { lineStyle: { color: '#dbe2ef' } }, axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f8' } }, axisLabel: { color: '#94a3b8' } },
    series: [{
      type: 'line', smooth: true, data: rows.map(r => r.value), symbolSize: 7,
      itemStyle: { color: '#2563eb' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(37,99,235,.28)' }, { offset: 1, color: 'rgba(37,99,235,0)' }]) }
    }]
  })
}

function drawRole() {
  if (!roleRef.value) return
  echarts.getInstanceByDom(roleRef.value)?.dispose()
  roleChart = echarts.init(roleRef.value)
  const rows = data.value.roleDist || []
  roleChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人（{d}%）' },
    legend: { bottom: 0, left: 'center', itemGap: 12, itemWidth: 11, itemHeight: 11, textStyle: { color: '#475569', fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['26%', '40%'], center: ['50%', '34%'],
      avoidLabelOverlap: true, minAngle: 6,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}', fontSize: 11, color: '#475569', lineHeight: 14 },
      labelLine: { length: 5, length2: 7, smooth: true, lineStyle: { width: 1 } },
      emphasis: { itemStyle: { shadowBlur: 14, shadowColor: 'rgba(0,0,0,.25)' }, label: { fontSize: 13, fontWeight: 'bold' } },
      data: rows.map(r => ({ name: r.name, value: r.value }))
    }]
  })
}

function exportAll() {
  const rows = cards.value.map(c => [c.label, c.value + c.unit, c.foot])
  exportExcel(['指标', '数值', '说明'], rows, '平台总览', '总览')
  ElMessage.success('已导出平台总览 Excel')
}

function onResize() {
  trendChart?.resize()
  roleChart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  roleChart?.dispose()
})
</script>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
.head h3 { margin: 0 0 5px; font-size: 17px; font-weight: 700; color: var(--text-1); letter-spacing: .3px; }
.head .sub { margin: 0; font-size: 12px; color: var(--text-3); }
.ops { display: flex; gap: 8px; }

/* 统计卡：柔和蓝色渐变 + 右上角半透明大图标 */
.stat-col { margin-bottom: 14px; }
.stat-card { padding: 18px 20px; }
.stat-card .icon { font-size: 48px; opacity: .20; right: 16px; top: 16px; }
.stat-card .label { font-size: 13px; font-weight: 600; letter-spacing: .2px; opacity: .96; padding-right: 46px; }
.stat-card .num { margin: 8px 0 4px; }
.stat-card .unit { font-size: 12px; font-weight: 400; opacity: .85; margin-left: 3px; }
.stat-card .foot {
  font-size: 11.5px; opacity: .84; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.row-gap { margin-top: 14px; }
.panel { height: 100%; }
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 13.5px; font-weight: 600; color: var(--text-1);
}
.chart { height: 300px; }
.tl { padding-left: 4px; max-height: 210px; overflow-y: auto; margin-top: 6px; }
.muted { color: var(--text-3); }
.health { display: grid; gap: 10px; }
.h-row { display: flex; align-items: center; justify-content: space-between; font-size: 13px; color: #475569; }
.quick { display: flex; flex-wrap: wrap; gap: 8px; }
</style>
