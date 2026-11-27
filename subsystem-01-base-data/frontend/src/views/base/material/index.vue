<template>
  <div class="material-page">
    <div class="page-head">
      <div class="head-left">
        <h2 class="page-title">{{ readonly ? '物资目录' : '物资管理' }}</h2>
        <div class="page-sub">
          共 {{ stats.total }} 个物资，正常 {{ stats.normal }} 个，预警 {{ stats.warning }} 个
        </div>
      </div>
      <div class="head-right">
        <el-button v-if="!readonly" type="primary" :icon="Plus" @click="openCreate">新增物资</el-button>
        <el-button v-if="!readonly" class="btn-outline" :icon="FolderAdd" @click="openCategoryCreate">
          添加物资分类
        </el-button>
        <el-button class="btn-outline" @click="handleExport">导出 Excel</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="status-tabs" @tab-change="handleTabChange">
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.key"
        :name="tab.key"
        :label="`${tab.label} (${tab.count})`"
      />
    </el-tabs>

    <div class="search-bar">
      <el-input
        v-model="query.materialName"
        placeholder="搜索物资名称"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select
        v-model="query.categoryCode"
        placeholder="全部分类"
        clearable
        class="search-select"
      >
        <el-option
          v-for="item in categoryOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button text @click="handleReset">重置</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list">
        <el-table-column prop="materialNo" label="物资编码" width="120" />
        <el-table-column prop="materialName" label="物资名称" min-width="150" />
        <el-table-column label="分类" min-width="120">
          <template #default="{ row }">
            {{ categoryLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            {{ materialTypeLabel(row.materialType) }}
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="参考单价" width="110" align="right">
          <template #default="{ row }">
            {{ formatPrice(row.referencePrice) }}
          </template>
        </el-table-column>
        <el-table-column label="目前库存" width="110" align="right">
          <template #default="{ row }">
            {{ stockOf(row.id) }}
          </template>
        </el-table-column>
        <el-table-column label="安全库存" width="110" align="right">
          <template #default="{ row }">
            <span :class="{ 'text-warning': isBelowSafe(row) }">
              {{ formatSafeStock(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="管控" width="80" align="center">
          <template #default="{ row }">
            {{ isControlledLabel(row.isControlled) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <span class="pager-total">共 {{ filteredTotal }} 条</span>
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="filteredTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增物资' : '编辑物资'"
      width="680px"
    >
      <div class="dialog-tip">
        新增物资请先确认所属分类与物资类型；保存后立即生效，库存数量请到「库存查询」中维护。
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="物资编码" prop="materialNo">
              <el-input v-model="form.materialNo" maxlength="32" placeholder="如 M0003" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物资名称" prop="materialName">
              <el-input v-model="form.materialName" maxlength="128" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="分类" prop="categoryCode">
              <el-select v-model="form.categoryCode" placeholder="请选择分类" filterable style="width: 100%">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物资类型" prop="materialType">
              <el-radio-group v-model="form.materialType">
                <el-radio :value="1">低值易耗品</el-radio>
                <el-radio :value="2">低值耐用品</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" maxlength="16" placeholder="如 包 / 个 / 箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参考单价">
              <el-input-number
                v-model="form.referencePrice"
                :min="0"
                :precision="2"
                :step="1"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="安全库存">
              <el-input-number
                v-model="form.safeStock"
                :min="0"
                :precision="3"
                :step="10"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否管控">
              <el-radio-group v-model="form.isControlled">
                <el-radio :value="1">是</el-radio>
                <el-radio :value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="限用年限">
              <el-input-number
                v-model="form.serviceLifeYears"
                :min="0"
                :precision="0"
                :step="1"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保质期天数">
              <el-input-number
                v-model="form.shelfLifeDays"
                :min="0"
                :precision="0"
                :step="1"
                :controls="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryDialogVisible" title="添加物资分类" width="480px">
      <div class="dialog-tip">
        分类统一写入平台字典（sys_dict_item，dict_type = material_category），保存后可在分类下拉中直接选择。
      </div>

      <el-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        label-width="90px"
      >
        <el-form-item label="分类编号" prop="categoryNo">
          <el-input v-model="categoryForm.categoryNo" maxlength="32" placeholder="如 05" />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" maxlength="64" placeholder="如 体育器材" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number
            v-model="categoryForm.sort"
            :min="0"
            :max="9999"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="categoryForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="categorySubmitting" @click="handleCategorySubmit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderAdd, Plus } from '@element-plus/icons-vue'
import {
  createMaterial,
  deleteMaterial,
  getMaterialPage,
  updateMaterial
} from '@/api/material'
import { createCategory, getCategoryTree } from '@/api/category'
import { getStockList } from '@/api/stock'

const loading = ref(false)
const submitting = ref(false)
const allList = ref([])

const route = useRoute()
/** 物资目录（教师视图）只读 */
const readonly = computed(() => route.meta.readonly === true)
const categoryMap = ref({})
const categoryOptions = ref([])
const stockMap = ref({})
const activeTab = ref('all')

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()

const categoryDialogVisible = ref(false)
const categorySubmitting = ref(false)
const categoryFormRef = ref()

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  materialName: '',
  categoryCode: null
})

const form = reactive({
  id: null,
  materialNo: '',
  materialName: '',
  categoryCode: null,
  materialType: 1,
  unit: '',
  referencePrice: null,
  safeStock: null,
  isControlled: 0,
  serviceLifeYears: null,
  shelfLifeDays: null
})

const rules = {
  materialNo: [{ required: true, message: '请输入物资编码', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入物资名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请选择分类', trigger: 'change' }],
  materialType: [{ required: true, message: '请选择物资类型', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

const categoryForm = reactive({
  categoryNo: '',
  categoryName: '',
  sort: 0,
  status: 1
})

const categoryRules = {
  categoryNo: [{ required: true, message: '请输入分类编号', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

function categoryLabel(row) {
  return row.categoryName || categoryMap.value[row.categoryCode] || '-'
}

function materialTypeLabel(materialType) {
  if (Number(materialType) === 1) return '低值易耗品'
  if (Number(materialType) === 2) return '低值耐用品'
  return '-'
}

function isControlledLabel(isControlled) {
  return isControlled === true || Number(isControlled) === 1 ? '是' : '否'
}

function stockOf(materialId) {
  return stockMap.value[materialId] || 0
}

function isBelowSafe(row) {
  if (row.safeStock === null || row.safeStock === undefined) return false
  return stockOf(row.id) < Number(row.safeStock)
}

const warningCount = computed(() => allList.value.filter((row) => isBelowSafe(row)).length)

const stats = computed(() => ({
  total: allList.value.length,
  normal: allList.value.length - warningCount.value,
  warning: warningCount.value
}))

const tabs = computed(() => [
  { key: 'all', label: '全部', count: stats.value.total },
  { key: 'normal', label: '正常', count: stats.value.normal },
  { key: 'warning', label: '预警', count: stats.value.warning }
])

const filteredList = computed(() => {
  if (activeTab.value === 'warning') return allList.value.filter((row) => isBelowSafe(row))
  if (activeTab.value === 'normal') return allList.value.filter((row) => !isBelowSafe(row))
  return allList.value
})

const filteredTotal = computed(() => filteredList.value.length)

const list = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize
  return filteredList.value.slice(start, start + query.pageSize)
})

function formatPrice(value) {
  return value === null || value === undefined ? '-' : `￥${Number(value).toFixed(2)}`
}

function formatSafeStock(row) {
  return row.safeStock === null || row.safeStock === undefined ? '-' : Number(row.safeStock)
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

async function loadList() {
  loading.value = true
  try {
    const pageSize = 100
    let pageNum = 1
    let all = []
    let total = 0
    do {
      const data = await getMaterialPage({
        pageNum,
        pageSize,
        materialName: query.materialName || undefined,
        categoryCode: query.categoryCode || undefined
      })
      const rows = data.list || []
      total = data.total || 0
      all = all.concat(rows)
      pageNum += 1
      if (rows.length === 0) break
    } while (all.length < total)
    allList.value = all
  } finally {
    loading.value = false
  }
}

async function loadCategoryOptions() {
  const tree = await getCategoryTree()
  const map = {}
  const options = []
  const walk = (nodes, depth = 0) => {
    nodes.forEach((node) => {
      map[node.categoryNo] = node.categoryName
      options.push({
        value: node.categoryNo,
        label: `${'　'.repeat(depth)}${depth ? '└ ' : ''}${node.categoryName}`
      })
      if (node.children && node.children.length) walk(node.children, depth + 1)
    })
  }
  walk(tree || [])
  categoryMap.value = map
  categoryOptions.value = options
}

async function loadStockMap() {
  // 库存接口分页默认只返回 10 条，需逐页拉取全部库存后再按物资汇总
  const pageSize = 100
  const stocks = []
  let pageNum = 1
  let total = 0
  do {
    const data = await getStockList({ pageNum, pageSize })
    const list = Array.isArray(data) ? data : (data?.list || [])
    stocks.push(...list)
    total = Number(data?.total ?? stocks.length)
    if (list.length < pageSize) break
    pageNum += 1
  } while (stocks.length < total)

  const map = {}
  stocks.forEach((item) => {
    map[item.materialId] = (map[item.materialId] || 0) + Number(item.qtyOnHand || 0)
  })
  stockMap.value = map
}

function handleTabChange() {
  query.pageNum = 1
}

function resetForm() {
  Object.assign(form, {
    id: null,
    materialNo: '',
    materialName: '',
    categoryCode: null,
    materialType: 1,
    unit: '',
    referencePrice: null,
    safeStock: null,
    isControlled: 0,
    serviceLifeYears: null,
    shelfLifeDays: null
  })
  formRef.value?.clearValidate()
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openCategoryCreate() {
  Object.assign(categoryForm, {
    categoryNo: '',
    categoryName: '',
    sort: 0,
    status: 1
  })
  categoryFormRef.value?.clearValidate()
  categoryDialogVisible.value = true
}

async function handleCategorySubmit() {
  await categoryFormRef.value.validate()
  categorySubmitting.value = true
  try {
    await createCategory({
      categoryNo: categoryForm.categoryNo.trim(),
      categoryName: categoryForm.categoryName.trim(),
      sort: categoryForm.sort,
      status: categoryForm.status
    })
    ElMessage.success('分类添加成功')
    categoryDialogVisible.value = false
    await loadCategoryOptions()
    if (dialogVisible.value) {
      form.categoryCode = categoryForm.categoryNo.trim()
    }
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    categorySubmitting.value = false
  }
}

function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, {
    id: row.id,
    materialNo: row.materialNo,
    materialName: row.materialName,
    categoryCode: row.categoryCode,
    materialType: row.materialType ?? 1,
    unit: row.unit,
    referencePrice: row.referencePrice,
    safeStock: row.safeStock,
    isControlled: row.isControlled ?? 0,
    serviceLifeYears: row.serviceLifeYears,
    shelfLifeDays: row.shelfLifeDays
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      materialNo: form.materialNo,
      materialName: form.materialName,
      categoryCode: form.categoryCode,
      materialType: form.materialType,
      unit: form.unit,
      referencePrice: form.referencePrice,
      safeStock: form.safeStock,
      isControlled: form.isControlled,
      serviceLifeYears: form.serviceLifeYears,
      shelfLifeDays: form.shelfLifeDays
    }
    if (dialogMode.value === 'create') {
      await createMaterial(payload)
      ElMessage.success('新增成功')
    } else {
      await updateMaterial(form.id, payload)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await Promise.all([loadList(), loadStockMap()])
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除物资「${row.materialName}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteMaterial(row.id)
    ElMessage.success('删除成功')
    await Promise.all([loadList(), loadStockMap()])
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  }
}

function handleExport() {
  const header = ['物资编码', '物资名称', '分类', '类型', '单位', '参考单价', '安全库存', '是否管控', '创建时间']
  const rows = filteredList.value.map((row) => [
    row.materialNo,
    row.materialName,
    categoryLabel(row),
    materialTypeLabel(row.materialType),
    row.unit,
    row.referencePrice,
    formatSafeStock(row),
    isControlledLabel(row.isControlled),
    formatDateTime(row.createdAt)
  ])
  const csv = [header, ...rows]
    .map((cols) => cols.map((value) => `"${String(value ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `物资档案_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前列表')
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.materialName = ''
  query.categoryCode = null
  query.pageNum = 1
  loadList()
}

onMounted(() => {
  loadCategoryOptions()
  loadStockMap()
  loadList()
})
</script>

<style scoped>
.material-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head-right {
  display: flex;
  gap: 8px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.search-select {
  width: 200px;
}

.table-card {
  padding: 4px 0;
}

.pager {
  justify-content: space-between;
  border-top: 1px solid var(--line);
}

.pager-total {
  font-size: 13px;
  color: var(--text-3);
}

.text-warning {
  color: var(--danger);
  font-weight: 600;
}
</style>
