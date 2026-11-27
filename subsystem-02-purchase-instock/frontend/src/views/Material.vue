<template>
  <div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name"
                     :label="`${t.label} (${countOf(t.name)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索物资编码 / 名称 / 规格" clearable style="width:250px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="stockFilter" style="width:140px" @change="reset">
          <el-option label="全部库存" value="all" /><el-option label="仅低库存" value="low" />
        </el-select>
        <div class="grow"></div>
        <el-upload v-if="!readonly" :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls,.csv" :on-change="onImport">
          <el-button plain><el-icon><Upload /></el-icon>导入 Excel</el-button>
        </el-upload>
        <el-button v-if="!readonly" plain @click="downloadTemplate"><el-icon><Document /></el-icon>导入模板</el-button>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button v-if="roleCode === 'STORE_KEEPER'" plain @click="exportFlows"><el-icon><Tickets /></el-icon>导出库存流水</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
        <el-button v-if="!readonly" type="primary" @click="openEdit(null)"><el-icon><Plus /></el-icon>新增物资</el-button>
      </div>

      <el-table :data="paged" stripe>
        <el-table-column prop="materialCode" label="物资编码" width="110" />
        <el-table-column prop="materialName" label="物资名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="分类" width="100">
          <template #default="{ row: r }"><el-tag size="small" effect="plain">{{ CATEGORY[r.categoryCode] || r.categoryCode }}</el-tag></template>
        </el-table-column>
        <el-table-column label="物资类型" width="110" align="center">
          <template #default="{ row: r }">
            <el-tag size="small" :type="r.materialType === 2 ? 'warning' : 'success'" effect="light">
              {{ TYPE[r.materialType] || '低值易耗品' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="限用/保质期" width="120" align="center">
          <template #default="{ row: r }">
            <span v-if="r.materialType === 2">{{ r.serviceLifeYears ? r.serviceLifeYears + ' 年' : '—' }}</span>
            <span v-else>{{ r.shelfLifeDays ? r.shelfLifeDays + ' 天' : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="spec" label="规格型号" width="140" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column label="参考单价" width="100" align="right"><template #default="{ row: r }">{{ money(r.refPrice) }}</template></el-table-column>
        <el-table-column label="公共可用" width="100" align="center">
          <template #default="{ row: r }">
            <span :style="{ color: stockOf(r.materialId) < r.safeStock ? '#ef4444' : '#16a34a', fontWeight: 600 }">
              {{ qty(stockOf(r.materialId)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="个人保管" width="100" align="center">
          <template #default="{ row: r }">
            <span v-if="personalOf(r.materialId) > 0" style="color:#b45309;font-weight:600">
              {{ qty(personalOf(r.materialId)) }}</span>
            <span v-else style="color:#cbd5e1">0</span>
          </template>
        </el-table-column>
        <el-table-column label="最近到期" width="140" align="center">
          <template #default="{ row: r }">
            <template v-if="expireOf(r.materialId)">
              <el-tooltip placement="top">
                <template #content>
                  <div v-for="b in expireOf(r.materialId).rows" :key="`${b.warehouseId}-${b.batchNo}`">
                    {{ b.batchNo || '无批次号' }} · {{ b.expireDate }} · {{ qty(b.qtyOnHand) }}{{ r.unit }}
                  </div>
                </template>
                <span :style="{ color: expireColor(expireOf(r.materialId).days), fontWeight: 600 }">
                  {{ expireOf(r.materialId).days < 0 ? '已过期 ' + Math.abs(expireOf(r.materialId).days) + ' 天'
                    : '剩 ' + expireOf(r.materialId).days + ' 天' }}
                </span>
              </el-tooltip>
              <div style="font-size:12px;color:#94a3b8">{{ expireOf(r.materialId).date }}</div>
            </template>
            <span v-else style="color:#cbd5e1">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="safeStock" label="安全库存" width="90" align="center" />
        <el-table-column label="管控" width="80" align="center">
          <template #default="{ row: r }">
            <el-tag v-if="r.isControlled" type="danger" size="small" effect="dark">管控</el-tag>
            <span v-else style="color:#cbd5e1">—</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="操作" width="120" fixed="right">
          <template #default="{ row: r }">
            <el-link type="primary" @click="openEdit(r)">编辑</el-link>
            <el-link type="danger" style="margin-left:10px" @click="remove(r)">删除</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <el-dialog v-model="edit.open" :title="edit.form.materialId ? '编辑物资' : '新增物资'" width="520px">
      <el-form :model="edit.form" label-width="90px">
        <el-form-item label="物资编码"><el-input v-model="edit.form.materialCode" placeholder="如 01-005" /></el-form-item>
        <el-form-item label="物资名称"><el-input v-model="edit.form.materialName" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="edit.form.categoryCode" style="width:100%">
            <el-option v-for="(v,k) in CATEGORY" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格型号"><el-input v-model="edit.form.spec" /></el-form-item>
        <el-form-item label="物资类型">
          <el-radio-group v-model="edit.form.materialType">
            <el-radio :value="1">低值易耗品</el-radio>
            <el-radio :value="2">低值耐用品</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="edit.form.materialType === 2" label="限用年限">
          <el-input-number v-model="edit.form.serviceLifeYears" :min="1" :precision="0" />
          <span style="font-size:12px;color:#94a3b8;margin-left:8px">年，到期进入报废评估</span>
        </el-form-item>
        <el-form-item v-else label="保质期天数">
          <el-input-number v-model="edit.form.shelfLifeDays" :min="1" :precision="0" />
          <span style="font-size:12px;color:#94a3b8;margin-left:8px">天，可空；实际到期日按入库批次记录</span>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-select v-model="edit.form.unit" style="width:160px">
            <el-option v-for="u in ['个','包','盒','瓶','支','卷','套','箱']" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="参考单价"><el-input-number v-model="edit.form.refPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="edit.form.safeStock" :min="0" /></el-form-item>
        <el-form-item label="管控物资">
          <el-switch v-model="edit.form.isControlled" :active-value="1" :inactive-value="0" />
          <span style="font-size:12px;color:#94a3b8;margin-left:8px">开启后领用需实验室负责人加签</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="edit.open = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api, { get } from '../api'
import { CATEGORY, money, qty } from '../common'
import { exportExcel, printPdf, readExcel } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([]), stocks = ref([])
const TYPE = { 1: '低值易耗品', 2: '低值耐用品' }
const tab = ref('all'), kw = ref(''), stockFilter = ref('all')
const tabs = [
  { name: 'all', label: '全部' }, { name: '01', label: '办公耗材' }, { name: '02', label: '实验器材' },
  { name: '03', label: '后勤物料' }, { name: '04', label: '通用物资' }, { name: 'ctrl', label: '管控物资' },
]
const filtered = computed(() => list.value.filter(r => {
  const lowOk = stockFilter.value === 'all' || stockOf(r.materialId) < r.safeStock
  const tabOk = tab.value === 'all' || (tab.value === 'ctrl' ? !!r.isControlled : r.categoryCode === tab.value)
  const kwOk = !kw.value || (r.materialCode || '').includes(kw.value) ||
    (r.materialName || '').includes(kw.value) || (r.spec || '').includes(kw.value)
  return lowOk && tabOk && kwOk
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 20)
const countOf = n => n === 'all' ? list.value.length
  : n === 'ctrl' ? list.value.filter(r => r.isControlled).length
  : list.value.filter(r => r.categoryCode === n).length

const edit = reactive({ open: false, form: {} })

/** 物资档案为唯一物资维护入口：仅管理员与库管员可维护，其余角色只读 */
const roleCode = ref('')
const readonly = computed(() => !['ADMIN', 'STORE_KEEPER'].includes(roleCode.value))

onMounted(() => {
  loadRole()
  refresh()
})

async function loadRole() {
  try {
    const p = await api.profile(Number(localStorage.getItem('mockUserId') || 2))
    roleCode.value = p?.info?.roleCode || ''
  } catch (e) {
    roleCode.value = ''
  }
}

async function refresh() {
  list.value = await api.materials()
  stocks.value = await api.stocks()
}
const stockOf = id => stocks.value.filter(s => s.materialId === id && !s.holderId && s.warehouseType !== 2)
  .reduce((a, b) => a + Number(b.qtyAvailable || 0), 0)
// 个人保管库存（科研经费自购登记）：不参与公共领用，单独展示
const personalOf = id => stocks.value.filter(s => s.materialId === id && (s.holderId || s.warehouseType === 2))
  .reduce((a, b) => a + Number(b.qtyOnHand || 0), 0)
// 最近到期批次：取该物资在库批次中到期日最早的一批，悬浮展示全部批次与数量
const expireMap = computed(() => {
  const map = {}
  stocks.value.filter(s => Number(s.qtyOnHand || 0) > 0 && s.expireDate)
    .forEach(s => { (map[s.materialId] = map[s.materialId] || []).push(s) })
  Object.values(map).forEach(rows => rows.sort((a, b) => a.expireDate.localeCompare(b.expireDate)))
  return map
})
const expireOf = id => {
  const rows = expireMap.value[id]
  if (!rows || !rows.length) return null
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const days = Math.round((new Date(rows[0].expireDate + 'T00:00:00') - today) / 86400000)
  return { date: rows[0].expireDate, days, rows }
}
const expireColor = d => d < 0 ? '#dc2626' : d <= 30 ? '#d97706' : '#16a34a'

function openEdit(row) {
  edit.form = row ? { ...row } : { materialType: 1, isControlled: 0, unit: '个', refPrice: 0, safeStock: 0 }
  edit.open = true
}
async function save() {
  if (!edit.form.materialCode || !edit.form.materialName) return ElMessage.warning('编码与名称必填')
  if (edit.form.materialType === 2 && !edit.form.serviceLifeYears) return ElMessage.warning('低值耐用品必须填写限用年限')
  if (edit.form.materialType === 1) edit.form.serviceLifeYears = null
  else edit.form.shelfLifeDays = null
  if (edit.form.materialId) await api.put(`/base/materials/${edit.form.materialId}`, edit.form)
  else await api.post('/base/materials', edit.form)
  ElMessage.success('已保存')
  edit.open = false
  refresh()
}
async function remove(row) {
  await ElMessageBox.confirm(`删除物资「${row.materialName}」？已产生单据的物资建议停用而非删除。`, '删除', { type: 'warning' })
  await api.del(`/base/materials/${row.materialId}`)
  ElMessage.success('已删除')
  refresh()
}

const cols = [
  { label: '物资编码', prop: 'materialCode' }, { label: '物资名称', prop: 'materialName', width: 26 },
  { label: '分类', prop: r => CATEGORY[r.categoryCode] || '' },
  { label: '物资类型', prop: r => TYPE[r.materialType] || TYPE[1] },
  { label: '限用年限/保质期', prop: r => r.materialType === 2 ? (r.serviceLifeYears || '') + ' 年' : (r.shelfLifeDays || '') + ' 天' },
  { label: '规格型号', prop: 'spec', width: 20 },
  { label: '单位', prop: 'unit' }, { label: '参考单价', prop: r => Number(r.refPrice || 0).toFixed(2) },
  { label: '公共可用库存', prop: r => stockOf(r.materialId) }, { label: '个人保管库存', prop: r => personalOf(r.materialId) },
  { label: '安全库存', prop: 'safeStock' },
  { label: '是否管控', prop: r => r.isControlled ? '是' : '否' },
]
function doExport(type) {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, filtered.value, '物资档案', '物资档案')
  else printPdf('物资档案清单', cols, filtered.value, '学校物资采购管理系统')
}
const BIZ = { 1: '入库', 2: '领用出库', 3: '借用出库', 4: '归还入库', 5: '盘盈', 6: '盘亏', 7: '报废核销' }
async function exportFlows() {
  try {
    const rows = await get('/purchase/inventory/flows', { keyword: kw.value })
    if (!rows || !rows.length) return ElMessage.warning('暂无库存流水')
    exportExcel([
      { label: '流水时间', prop: r => r.flowTime ? String(r.flowTime).replace('T', ' ').slice(0, 19) : '' },
      { label: '物资编码', prop: 'materialCode' },
      { label: '物资名称', prop: 'materialName' },
      { label: '业务类型', prop: r => BIZ[r.bizType] || r.bizType },
      { label: '变动数量', prop: r => Number(r.changeQty || 0) },
      { label: '单位', prop: 'unit' },
      { label: '批次号', prop: 'batchNo' },
      { label: '仓库', prop: 'warehouseName' },
      { label: '操作人', prop: 'operatorName' },
      { label: '关联单据ID', prop: 'bizId' },
    ], rows, '库存流水', '库存流水')
    ElMessage.success(`已导出 ${rows.length} 条库存流水`)
  } catch (e) { /* 错误提示由请求拦截器统一处理 */ }
}
function downloadTemplate() {
  exportExcel([
    { label: '物资编码', prop: 'a' }, { label: '物资名称', prop: 'b' }, { label: '分类编码', prop: 'c' },
    { label: '物资类型', prop: 'i' }, { label: '限用年限', prop: 'j' }, { label: '保质期天数', prop: 'k' },
    { label: '规格型号', prop: 'd' }, { label: '单位', prop: 'e' }, { label: '参考单价', prop: 'f' },
    { label: '安全库存', prop: 'g' }, { label: '是否管控', prop: 'h' },
  ], [{ a: '01-005', b: 'A4 彩色复印纸', c: '01', i: '低值易耗品', j: '', k: 365,
       d: '80g 100张/包', e: '包', f: 18.5, g: 30, h: '否' }], '物资导入模板')
}

async function onImport(file) {
  try {
    const rows = await readExcel(file.raw)
    if (!rows.length) return ElMessage.warning('文件无数据')
    let ok = 0
    for (const r of rows) {
      const name = r['物资名称']
      if (!name) continue
      await api.post('/base/materials', {
        materialCode: r['物资编码'] || 'TMP-' + Date.now().toString().slice(-5),
        materialName: name,
        categoryCode: String(r['分类编码'] || '01'),
        materialType: String(r['物资类型'] || '低值易耗品') === '低值耐用品' ? 2 : 1,
        serviceLifeYears: Number(r['限用年限']) || null,
        shelfLifeDays: Number(r['保质期天数']) || null,
        spec: r['规格型号'] || '',
        unit: r['单位'] || '个',
        refPrice: Number(r['参考单价']) || 0,
        safeStock: Number(r['安全库存']) || 0,
        isControlled: String(r['是否管控']) === '是' ? 1 : 0,
      })
      ok++
    }
    ElMessage.success(`导入成功 ${ok} 条`)
    refresh()
  } catch (e) {
    ElMessage.error('导入失败：请检查表头是否与模板一致')
  }
}
</script>
