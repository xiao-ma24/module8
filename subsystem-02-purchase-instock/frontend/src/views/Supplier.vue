<template>
  <div>
    <el-card class="page-card fill">
      <el-tabs v-model="tab" @tab-change="reset">
        <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name"
                     :label="`${t.label} (${countOf(t.name)})`" />
      </el-tabs>

      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索供应商名称 / 联系人 / 电话" clearable style="width:250px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="grow"></div>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls,.csv" :on-change="onImport">
          <el-button plain><el-icon><Upload /></el-icon>导入 Excel</el-button>
        </el-upload>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
        <el-button type="primary" @click="openEdit(null)"><el-icon><Plus /></el-icon>新增供应商</el-button>
      </div>

      <el-table :data="paged" stripe>
        <el-table-column prop="supplierName" label="供应商名称" min-width="230" show-overflow-tooltip />
        <el-table-column prop="contactPerson" label="联系人" width="90" />
        <el-table-column prop="contactPhone" label="联系电话" width="135" />
        <el-table-column label="评级" width="130">
          <template #default="{ row: r }"><span style="color:#f59e0b">{{ '★'.repeat(r.starLevel || 0) }}</span></template>
        </el-table-column>
        <el-table-column label="累计采购额" width="120" align="right">
          <template #default="{ row: r }">{{ money(amountOf(r.supplierId)) }}</template>
        </el-table-column>
        <el-table-column prop="taxNo" label="纳税人识别号" width="190" show-overflow-tooltip />
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row: r }">
            <el-tag :type="r.supplierStatus ? 'success' : 'danger'" size="small" effect="dark">
              {{ r.supplierStatus ? '合作中' : '已停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row: r }">
            <el-link type="primary" @click="openEdit(r)">编辑</el-link>
            <el-link type="danger" style="margin-left:10px" @click="remove(r)">删除</el-link>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <el-dialog v-model="edit.open" :title="edit.form.supplierId ? '编辑供应商' : '新增供应商'" width="540px">
      <el-form :model="edit.form" label-width="105px">
        <el-form-item label="供应商名称"><el-input v-model="edit.form.supplierName" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="edit.form.contactPerson" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="edit.form.contactPhone" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="edit.form.address" /></el-form-item>
        <el-form-item label="纳税人识别号"><el-input v-model="edit.form.taxNo" /></el-form-item>
        <el-form-item label="初始评级">
          <el-rate v-model="edit.form.starLevel" :max="5" />
          <span style="font-size:12px;color:#94a3b8;margin-left:8px">入库后由验收评分自动更新</span>
        </el-form-item>
        <el-form-item label="合作状态">
          <el-switch v-model="edit.form.supplierStatus" :active-value="1" :inactive-value="0" />
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
import api from '../api'
import { money } from '../common'
import { exportExcel, printPdf, readExcel } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([]), orders = ref([])
const tab = ref('all'), kw = ref('')
const tabs = [
  { name: 'all', label: '全部' }, { name: '5', label: '五星' }, { name: '4', label: '四星' },
  { name: 'low', label: '三星及以下' }, { name: 'off', label: '已停用' },
]
const filtered = computed(() => list.value.filter(r => {
  const tabOk = tab.value === 'all' ||
    (tab.value === 'low' ? (r.starLevel || 0) <= 3 : tab.value === 'off' ? !r.supplierStatus : r.starLevel === +tab.value)
  const kwOk = !kw.value || (r.supplierName || '').includes(kw.value) ||
    (r.contactPerson || '').includes(kw.value) || (r.contactPhone || '').includes(kw.value)
  return tabOk && kwOk
}))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const countOf = n => n === 'all' ? list.value.length
  : n === 'low' ? list.value.filter(r => (r.starLevel || 0) <= 3).length
  : n === 'off' ? list.value.filter(r => !r.supplierStatus).length
  : list.value.filter(r => r.starLevel === +n).length

const edit = reactive({ open: false, form: {} })

onMounted(refresh)
async function refresh() {
  list.value = await api.suppliers()
  orders.value = await api.orders()
}
const amountOf = id => orders.value.filter(o => o.supplierId === id && o.poStatus !== 4)
  .reduce((s, o) => s + Number(o.totalAmount || 0), 0)

function openEdit(row) {
  edit.form = row ? { ...row } : { starLevel: 3, supplierStatus: 1 }
  edit.open = true
}
async function save() {
  if (!edit.form.supplierName) return ElMessage.warning('供应商名称必填')
  if (edit.form.supplierId) await api.put(`/base/suppliers/${edit.form.supplierId}`, edit.form)
  else await api.post('/base/suppliers', edit.form)
  ElMessage.success('已保存')
  edit.open = false
  refresh()
}
async function remove(row) {
  await ElMessageBox.confirm(`删除供应商「${row.supplierName}」？建议改为停用。`, '删除', { type: 'warning' })
  await api.del(`/base/suppliers/${row.supplierId}`)
  ElMessage.success('已删除')
  refresh()
}

const cols = [
  { label: '供应商名称', prop: 'supplierName', width: 30 }, { label: '联系人', prop: 'contactPerson' },
  { label: '联系电话', prop: 'contactPhone' }, { label: '评级(星)', prop: r => r.starLevel || 0 },
  { label: '累计采购额', prop: r => amountOf(r.supplierId).toFixed(2) },
  { label: '纳税人识别号', prop: 'taxNo', width: 24 },
  { label: '状态', prop: r => r.supplierStatus ? '合作中' : '已停用' },
]
function doExport(type) {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, filtered.value, '供应商名录', '供应商')
  else printPdf('供应商名录', cols, filtered.value, '学校物资采购管理系统')
}
async function onImport(file) {
  try {
    const rows = await readExcel(file.raw)
    let ok = 0
    for (const r of rows) {
      if (!r['供应商名称']) continue
      await api.post('/base/suppliers', {
        supplierName: r['供应商名称'], contactPerson: r['联系人'] || '',
        contactPhone: String(r['联系电话'] || ''), address: r['地址'] || '',
        taxNo: String(r['纳税人识别号'] || ''), starLevel: Number(r['评级'] || 3), supplierStatus: 1,
      })
      ok++
    }
    ElMessage.success(`导入成功 ${ok} 条`)
    refresh()
  } catch { ElMessage.error('导入失败：请检查表头') }
}
</script>
