<template>
  <div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name"
                     :label="`${t.label} (${countOf(t.name)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索入库单号 / 订单号 / 仓库" clearable style="width:250px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="whFilter" placeholder="仓库" clearable style="width:180px" @change="reset">
          <el-option v-for="w in warehouses" :key="w.warehouseId"
                     :label="w.warehouseName + (w.warehouseType === 2 ? '（个人保管）' : '')" :value="w.warehouseId" />
        </el-select>
        <div class="grow"></div>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
      </div>

      <!-- 临期预警视图 -->
      <el-table v-if="tab === 'exp'" :data="paged" stripe>
        <el-table-column prop="materialName" label="物资" min-width="180" />
        <el-table-column prop="batchNo" label="批次号" width="180" />
        <el-table-column label="临期天数" width="120">
          <template #default="{ row: r }">
            <el-tag :type="daysLeft(r.expireDate) <= 7 ? 'danger' : 'warning'" effect="dark" size="small">
              {{ daysLeft(r.expireDate) }} 天</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="120"><template #default="{ row: r }">{{ date(r.expireDate) }}</template></el-table-column>
        <el-table-column label="在库数量" width="110"><template #default="{ row: r }">{{ qty(r.inQty) }} {{ r.unit }}</template></el-table-column>
        <el-table-column label="处理建议" min-width="200">
          <template #default="{ row: r }">
            <span style="color:#b45309">优先发放 / 联系领用部门消化；过期前 3 天仍未出库的转报废流程</span>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else :data="paged" stripe>
        <el-table-column prop="inNo" label="入库单号" width="145" />
        <el-table-column prop="poNo" label="订单号" width="145" />
        <el-table-column prop="acptNo" label="验收单号" width="145" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column label="归属" width="155">
          <template #default="{ row: r }">
            <el-tag v-if="r.purchaseMode === 2 || r.warehouseType === 2" type="warning" size="small" effect="plain">个人保管</el-tag>
            <el-tag v-else type="success" size="small" effect="plain">公共库存</el-tag>
            <span v-if="r.purchaseMode === 2 || r.warehouseType === 2"
                  style="margin-left:6px;font-size:12px;color:#b45309">{{ r.holderName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="keeperName" label="库管" width="85" />
        <el-table-column label="入库金额" width="110" align="right"><template #default="{ row: r }">{{ money(r.totalAmount) }}</template></el-table-column>
        <el-table-column label="入库时间" width="150"><template #default="{ row: r }">{{ dt(r.inDate) }}</template></el-table-column>
        <el-table-column prop="voucherNo" label="报销凭证号" width="150">
          <template #default="{ row: r }">{{ r.voucherNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row: r }"><el-tag :type="IN_TAG[r.inStatus]" effect="dark" size="small">{{ IN_STATUS[r.inStatus] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row: r }">
            <el-link type="primary" @click="openDetail(r)">明细</el-link>
            <el-link v-if="r.inStatus === 0" type="warning" style="margin-left:8px" @click="openBatch(r)">批次登记</el-link>
            <el-link v-if="r.inStatus === 0" type="success" style="margin-left:8px" @click="confirmIn(r)">确认入库</el-link>
            <el-link v-if="r.inStatus === 1 && !r.voucherNo" type="primary" style="margin-left:8px"
                     @click="getVoucher(r)">取凭证</el-link>
            <el-link v-if="r.inStatus === 1" style="margin-left:8px" @click="printLabels(r)">二维码</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <!-- 批次登记 -->
    <el-dialog v-model="batch.open" :title="'批次与质保期登记　' + (batch.row?.inNo || '')" width="820px">
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
                title="批次号必填：入库后用于效期追溯与临期预警；批次号建议格式 入库单号-序号" />
      <el-table :data="batch.items" size="small" border>
        <el-table-column prop="materialName" label="物资" min-width="140" />
        <el-table-column label="数量" width="90"><template #default="{ row: r }">{{ qty(r.inQty) }} {{ r.unit }}</template></el-table-column>
        <el-table-column label="批次号" width="190">
          <template #default="{ row: r }"><el-input v-model="r.batchNo" size="small" placeholder="必填" /></template>
        </el-table-column>
        <el-table-column label="生产日期" width="160">
          <template #default="{ row: r }"><el-date-picker v-model="r.produceDate" type="date" value-format="YYYY-MM-DD" size="small" style="width:140px" /></template>
        </el-table-column>
        <el-table-column label="有效期至" width="160">
          <template #default="{ row: r }"><el-date-picker v-model="r.expireDate" type="date" value-format="YYYY-MM-DD" size="small" style="width:140px" /></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="batch.open = false">取消</el-button>
        <el-button type="primary" @click="saveBatch">保存</el-button>
      </template>
    </el-dialog>

    <!-- 明细 -->
    <el-drawer v-model="detail.open" :title="detail.row?.inNo" size="700px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="来源订单">{{ detail.row?.poNo }}</el-descriptions-item>
        <el-descriptions-item label="验收单">{{ detail.row?.acptNo }}</el-descriptions-item>
        <el-descriptions-item label="入库仓库">{{ detail.row?.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="库存归属">
          <el-tag v-if="detail.row?.purchaseMode === 2 || detail.row?.warehouseType === 2" type="warning" size="small" effect="dark">
            科研自购·个人保管</el-tag>
          <el-tag v-else type="success" size="small" effect="dark">公共库存</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="实际保管人">{{ detail.row?.holderName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="接收库管">{{ detail.row?.keeperName }}</el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ dt(detail.row?.inDate) }}</el-descriptions-item>
        <el-descriptions-item label="报销凭证">{{ detail.row?.voucherNo || '—' }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin:14px 0 8px">入库明细</h4>
      <el-table :data="detail.items" size="small" border>
        <el-table-column label="物资" min-width="150">
          <template #default="{ row: r }">
            {{ r.materialName }}
            <el-tag v-if="r.isControlled === 1" type="danger" size="small" effect="plain">管控·需加签</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="90"><template #default="{ row: r }">{{ qty(r.inQty) }} {{ r.unit }}</template></el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="170" />
        <el-table-column label="有效期" width="110"><template #default="{ row: r }">{{ date(r.expireDate) }}</template></el-table-column>
        <el-table-column label="单价" width="90"><template #default="{ row: r }">¥{{ r.unitPrice }}</template></el-table-column>
      </el-table>
    </el-drawer>

    <!-- 二维码标签 -->
    <el-dialog v-model="qr.open" title="物资二维码标签（可打印）" width="720px">
      <div id="qr-print" class="qr-wrap">
        <div class="qr-card" v-for="(l, i) in qr.labels" :key="i">
          <div class="qr-title">{{ l.materialName }}</div>
          <img :src="l.img" alt="qr" />
          <div class="qr-line">批次 {{ l.batchNo }}</div>
          <div class="qr-line">有效期 {{ l.expireDate || '长期' }}</div>
        </div>
      </div>
      <template #footer><el-button type="primary" @click="printQr">打印标签</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '../api'
import { IN_STATUS, IN_TAG, money, qty, date, dt } from '../common'
import { exportExcel, printPdf } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'

const list = ref([]), expiringList = ref([]), warehouses = ref([])
const tab = ref('0'), kw = ref(''), whFilter = ref(null)
const tabs = [
  { name: '0', label: '待入库' }, { name: '1', label: '已入库' },
  { name: 'exp', label: '临期预警' }, { name: 'all', label: '全部' },
]
const source = computed(() => {
  if (tab.value === 'exp') return expiringList.value.filter(r => !kw.value || (r.materialName || '').includes(kw.value))
  return list.value.filter(r =>
    (tab.value === 'all' || r.inStatus === +tab.value) &&
    (whFilter.value == null || r.warehouseId === whFilter.value) &&
    (!kw.value || (r.inNo || '').includes(kw.value) || (r.poNo || '').includes(kw.value) || (r.warehouseName || '').includes(kw.value)))
})
const { page, pageSize, total, paged, reset } = usePager(source, 10)
const countOf = n => n === 'exp' ? expiringList.value.length : n === 'all' ? list.value.length
  : list.value.filter(r => r.inStatus === +n).length

const batch = reactive({ open: false, row: null, items: [] })
const detail = reactive({ open: false, row: null, items: [] })
const qr = reactive({ open: false, labels: [] })

onMounted(refresh)
async function refresh() {
  list.value = await api.inbounds()
  warehouses.value = await api.warehouses()
  try { expiringList.value = await api.expiring() } catch { expiringList.value = [] }
}
const daysLeft = d => Math.max(0, Math.ceil((new Date(d) - new Date()) / 86400000))

async function openDetail(row) {
  detail.row = row
  detail.items = await api.inItems(row.inId)
  detail.open = true
}
async function openBatch(row) {
  batch.row = row
  batch.items = await api.inItems(row.inId)
  batch.open = true
}
async function saveBatch() {
  if (batch.items.some(i => !i.batchNo)) return ElMessage.warning('批次号必填')
  await api.inExtras(batch.row.inId, batch.items.map(i => ({
    iiId: i.iiId, batchNo: i.batchNo, produceDate: i.produceDate, expireDate: i.expireDate
  })))
  ElMessage.success('批次信息已保存')
  batch.open = false
}
async function confirmIn(row) {
  const items = await api.inItems(row.inId)
  const controlled = items.filter(i => i.isControlled === 1).map(i => i.materialName)
  const tip = controlled.length
    ? `\n\n⚠ 含管控物资：${controlled.join('、')}，需加签复核（部门负责人/资产管理岗签字）后方可入库。`
    : ''
  const personal = (row.purchaseMode === 2 || row.warehouseType === 2)
    ? `\n\n本单为科研经费自购：只增加个人保管仓在库量，不进入公共可用库存，保管人为 ${row.holderName || '登记教师'}。`
    : ''
  await ElMessageBox.confirm(
    `确认入库后将：①增加库存 ②写入库存流水 ③输出入库成本 ④扣减预算 ⑤回写订单到货量，不可撤销。确认？${tip}${personal}`,
    '确认入库', { type: controlled.length || personal ? 'warning' : 'info' })
  await api.confirmInbound(row.inId)
  ElMessage.success('入库完成，库存已更新')
  refresh()
}
async function getVoucher(row) {
  const voucherNo = 'FP' + new Date().toISOString().slice(0, 10).replaceAll('-', '') +
    String(Math.floor(Math.random() * 900) + 100)
  await api.fillVoucher(row.inId, { voucherNo })
  ElMessage.success('已从财务系统获取报销凭证：' + voucherNo)
  refresh()
}
async function printLabels(row) {
  const items = await api.inItems(row.inId)
  qr.labels = await Promise.all(items.map(async i => ({
    materialName: i.materialName, batchNo: i.batchNo, expireDate: i.expireDate,
    img: await QRCode.toDataURL(`${i.materialName}|${i.batchNo}|${i.expireDate || ''}|${row.inNo}`, { width: 150, margin: 1 })
  })))
  qr.open = true
}
function printQr() {
  const html = document.getElementById('qr-print').innerHTML
  const w = window.open('', '_blank')
  w.document.write(`<html><head><meta charset="utf-8"><title>二维码标签</title><style>
    body{font-family:'PingFang SC',sans-serif}
    .qr-wrap{display:flex;flex-wrap:wrap;gap:12px}
    .qr-card{border:1px dashed #999;border-radius:8px;padding:10px;width:170px;text-align:center;page-break-inside:avoid}
    .qr-title{font-size:12px;font-weight:700;margin-bottom:4px}
    .qr-card img{width:130px;height:130px}
    .qr-line{font-size:10px;color:#555}
  </style></head><body><div class="qr-wrap">${html.replaceAll('class="qr-wrap"', '')}</div>
  <script>window.onload=()=>setTimeout(()=>window.print(),300)<\/script></body></html>`)
  w.document.close()
}

const cols = [
  { label: '入库单号', prop: 'inNo' }, { label: '订单号', prop: 'poNo' },
  { label: '验收单号', prop: 'acptNo' }, { label: '仓库', prop: 'warehouseName' },
  { label: '归属', prop: r => (r.purchaseMode === 2 || r.warehouseType === 2) ? '个人保管' : '公共库存' },
  { label: '实际保管人', prop: r => (r.purchaseMode === 2 || r.warehouseType === 2) ? (r.holderName || '') : '' },
  { label: '库管', prop: 'keeperName' }, { label: '金额', prop: r => Number(r.totalAmount || 0).toFixed(2) },
  { label: '入库时间', prop: r => dt(r.inDate) }, { label: '凭证号', prop: r => r.voucherNo || '' },
  { label: '状态', prop: r => IN_STATUS[r.inStatus] },
]
function doExport(type) {
  if (!source.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, source.value, tab.value === 'exp' ? '临期预警清单' : '入库单清单', '入库管理')
  else printPdf(tab.value === 'exp' ? '临期物资预警清单' : '入库单清单', cols, source.value, '学校物资采购管理系统')
}
</script>

<style scoped>
.qr-wrap { display: flex; flex-wrap: wrap; gap: 12px; }
.qr-card { border: 1px dashed #cbd5e1; border-radius: 8px; padding: 10px; width: 168px; text-align: center; }
.qr-title { font-size: 12px; font-weight: 700; margin-bottom: 4px; }
.qr-card img { width: 128px; height: 128px; }
.qr-line { font-size: 10px; color: #64748b; }
</style>
