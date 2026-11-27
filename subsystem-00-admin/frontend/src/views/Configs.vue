<template>
  <div class="page-card fill" v-loading="loading">
    <div class="head">
      <div>
        <h3>系统参数</h3>
        <p class="sub">平台级开关与阈值 · 修改后各子系统读取同一份配置</p>
      </div>
      <div class="ops">
        <el-button :icon="Download" @click="exportXls">导出 Excel</el-button>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增参数</el-button>
      </div>
    </div>

    <el-tabs v-model="group">
      <el-tab-pane v-for="g in groups" :key="g.name" :name="g.name" :label="`${g.label} (${countOf(g.name)})`" />
    </el-tabs>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索参数键 / 名称" clearable style="width: 240px" :prefix-icon="Search" />
      <div class="grow"></div>
      <el-alert type="info" :closable="false" show-icon title="带「内置」标记的参数不可删除，仅可修改值" style="width: auto" />
    </div>

    <el-table :data="paged" height="100%">
      <el-table-column prop="configKey" label="参数键" min-width="200">
        <template #default="{ row: r }">
          <code>{{ r.configKey }}</code>
          <el-tag v-if="r.builtin === 1" size="small" type="warning" effect="plain" style="margin-left: 8px">内置</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="configName" label="参数名称" width="170" />
      <el-table-column label="参数值" min-width="180">
        <template #default="{ row: r }">
          <el-tag v-if="r.valueType === 'boolean'" size="small" :type="r.configValue === 'true' ? 'success' : 'info'" effect="light">
            {{ r.configValue === 'true' ? '已开启' : '已关闭' }}
          </el-tag>
          <span v-else class="val">{{ r.configValue }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="valueType" label="类型" width="90">
        <template #default="{ row: r }">
          <el-tag size="small" effect="plain">{{ TYPE_LABEL[r.valueType] || r.valueType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="configGroup" label="分组" width="90" />
      <el-table-column prop="remark" label="说明" min-width="220">
        <template #default="{ row: r }">{{ empty(r.remark) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="150">
        <template #default="{ row: r }">{{ dt(r.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row: r }">
          <el-button link type="primary" @click="editValue(r)">改值</el-button>
          <el-button link @click="openForm(r)">编辑</el-button>
          <el-button link type="danger" :disabled="r.builtin === 1" @click="remove(r)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
                     :total="total" layout="total, sizes, prev, pager, next" background />
    </div>

    <!-- 改值 -->
    <el-dialog v-model="valueVisible" :title="`修改参数 · ${current?.configKey || ''}`" width="460px">
      <el-form label-width="90px">
        <el-form-item label="参数名称">
          <el-input :model-value="current?.configName" disabled />
        </el-form-item>
        <el-form-item label="参数值">
          <el-switch v-if="current?.valueType === 'boolean'" v-model="boolValue" active-text="开启" inactive-text="关闭" />
          <el-input v-else v-model="newValue" />
        </el-form-item>
        <el-form-item label="说明">
          <span class="hint">{{ current?.remark || '—' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="valueVisible = false">取消</el-button>
        <el-button type="primary" @click="saveValue">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="form.configId ? '编辑参数' : '新增参数'" width="540px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="参数键">
          <el-input v-model="form.configKey" :disabled="!!form.configId" placeholder="如 purchase.autoApproveAmount" />
        </el-form-item>
        <el-form-item label="参数名称">
          <el-input v-model="form.configName" placeholder="如 小额自动审批上限" />
        </el-form-item>
        <el-form-item label="参数值">
          <el-input v-model="form.configValue" />
        </el-form-item>
        <el-form-item label="值类型">
          <el-select v-model="form.valueType" style="width: 100%">
            <el-option v-for="(v, k) in TYPE_LABEL" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="form.configGroup" placeholder="基础 / 安全 / 业务 / 智能 / 审计" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import api from '../api'
import { usePager } from '../composables/usePager'
import { exportExcel } from '../utils/export'
import { dt, empty } from '../common'

const TYPE_LABEL = { string: '文本', number: '数字', boolean: '开关' }

const loading = ref(false)
const rows = ref([])
const group = ref('all')
const keyword = ref('')
const valueVisible = ref(false)
const formVisible = ref(false)
const current = ref(null)
const newValue = ref('')
const boolValue = ref(false)
const form = reactive({})

const groups = computed(() => {
  const set = [...new Set(rows.value.map(r => r.configGroup || '基础'))]
  return [{ name: 'all', label: '全部' }, ...set.map(g => ({ name: g, label: g }))]
})

const filtered = computed(() => {
  let list = rows.value
  if (group.value !== 'all') list = list.filter(r => (r.configGroup || '基础') === group.value)
  return list
})
const { page, pageSize, total, paged, reset } = usePager(filtered)

function countOf(name) {
  if (name === 'all') return rows.value.length
  return rows.value.filter(r => (r.configGroup || '基础') === name).length
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.configs({ keyword: keyword.value || undefined })
    reset()
  } finally {
    loading.value = false
  }
}

function editValue(r) {
  current.value = r
  newValue.value = r.configValue
  boolValue.value = r.configValue === 'true'
  valueVisible.value = true
}

async function saveValue() {
  const v = current.value.valueType === 'boolean' ? String(boolValue.value) : newValue.value
  await api.updateConfigValue(current.value.configId, v)
  ElMessage.success('参数已更新')
  valueVisible.value = false
  load()
}

function openForm(r) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, r ? { ...r } : { valueType: 'string', configGroup: '自定义', configValue: '' })
  formVisible.value = true
}

async function save() {
  if (!form.configKey || !form.configName) {
    ElMessage.warning('参数键与名称不能为空')
    return
  }
  if (form.configId) await api.updateConfig({ ...form })
  else await api.createConfig({ ...form })
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

async function remove(r) {
  await ElMessageBox.confirm(`确定删除参数「${r.configKey}」吗？`, '删除参数', { type: 'warning' })
  await api.deleteConfig(r.configId)
  ElMessage.success('已删除')
  load()
}

function exportXls() {
  const data = filtered.value.map(r => [r.configKey, r.configName, r.configValue, TYPE_LABEL[r.valueType] || r.valueType,
    r.configGroup, r.builtin === 1 ? '是' : '否', r.remark || '', dt(r.updatedAt)])
  exportExcel(['参数键', '参数名称', '参数值', '类型', '分组', '内置', '说明', '更新时间'], data, '系统参数', '参数')
  ElMessage.success('已导出系统参数')
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 6px; }
.head h3 { margin: 0 0 4px; font-size: 17px; color: #0f172a; }
.head .sub { margin: 0; font-size: 12px; color: #94a3b8; }
.ops { display: flex; gap: 8px; }
code { background: #f1f5f9; padding: 1px 6px; border-radius: 4px; font-size: 12px; }
.val { color: #0f172a; font-weight: 600; }
.hint { font-size: 12px; color: #64748b; }
</style>
