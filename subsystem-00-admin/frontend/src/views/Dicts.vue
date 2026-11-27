<template>
  <div class="page-card fill" v-loading="loading">
    <div class="head">
      <div>
        <h3>数据字典</h3>
        <p class="sub">全平台枚举值的唯一来源 · 各子系统下拉框、状态标签统一从字典读取</p>
      </div>
      <div class="ops">
        <el-button :icon="Download" @click="exportXls">导出当前类型</el-button>
        <el-button type="primary" :icon="Plus" @click="openType()">新增字典类型</el-button>
      </div>
    </div>

    <div class="dict-body">
      <div class="types">
        <div class="types-head">
          <el-input v-model="typeKeyword" placeholder="搜索字典" size="small" clearable :prefix-icon="Search" />
        </div>
        <div class="type-list">
          <div v-for="t in filteredTypes" :key="t.dictType" class="type-item" :class="{ active: t.dictType === dictType }"
               @click="selectType(t.dictType)">
            <div class="t-main">
              <div class="t-name">{{ t.dictName }}</div>
              <div class="t-code">{{ t.dictType }}</div>
            </div>
            <el-tag size="small" effect="plain" :type="t.itemCount ? 'primary' : 'info'">{{ t.itemCount || 0 }}</el-tag>
            <el-dropdown trigger="click" @command="c => onTypeCommand(c, t)">
              <el-icon class="more" @click.stop><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑类型</el-dropdown-item>
                  <el-dropdown-item command="del" style="color: #ef4444">删除类型</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <el-empty v-if="!filteredTypes.length" description="暂无字典类型" :image-size="60" />
        </div>
      </div>

      <div class="items">
        <div class="toolbar">
          <span class="cur">{{ currentType?.dictName || '请选择字典类型' }}
            <el-tag size="small" effect="plain">{{ dictType || '—' }}</el-tag>
          </span>
          <el-input v-model="itemKeyword" placeholder="搜索字典项" clearable style="width: 200px" :prefix-icon="Search" />
          <div class="grow"></div>
          <el-button type="primary" size="default" :icon="Plus" @click="openItem()">新增字典项</el-button>
        </div>

        <el-table :data="paged" height="100%">
          <el-table-column label="字典值" width="120">
            <template #default="{ row: r }"><code>{{ r.itemValue }}</code></template>
          </el-table-column>
          <el-table-column prop="itemLabel" label="显示名称" width="150" />
          <el-table-column label="标签效果" width="160">
            <template #default="{ row: r }">
              <el-tag size="small" :type="r.itemColor || 'info'" effect="light">{{ r.itemLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortNo" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row: r }">
              <el-tag size="small" :type="r.itemStatus === 1 ? 'success' : 'info'" effect="plain">
                {{ r.itemStatus === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160">
            <template #default="{ row: r }">{{ empty(r.remark) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row: r }">
              <el-button link type="primary" @click="openItem(r)">编辑</el-button>
              <el-button link type="danger" @click="removeItem(r)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
                         :total="total" layout="total, sizes, prev, pager, next" background />
        </div>
      </div>
    </div>

    <!-- 类型表单 -->
    <el-dialog v-model="typeVisible" :title="typeForm.editing ? '编辑字典类型' : '新增字典类型'" width="480px">
      <el-form :model="typeForm" label-width="90px">
        <el-form-item label="字典编码">
          <el-input v-model="typeForm.dictType" :disabled="typeForm.editing" placeholder="如 material_category" />
        </el-form-item>
        <el-form-item label="字典名称">
          <el-input v-model="typeForm.dictName" placeholder="如 物资分类" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="typeForm.dictStatus" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeVisible = false">取消</el-button>
        <el-button type="primary" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典项表单 -->
    <el-dialog v-model="itemVisible" :title="itemForm.itemId ? '编辑字典项' : '新增字典项'" width="520px">
      <el-form :model="itemForm" label-width="90px">
        <el-form-item label="所属字典">
          <el-input :model-value="dictType" disabled />
        </el-form-item>
        <el-form-item label="字典值">
          <el-input v-model="itemForm.itemValue" placeholder="存入数据库的值，如 01" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="itemForm.itemLabel" placeholder="界面上展示的名称" />
        </el-form-item>
        <el-form-item label="标签颜色">
          <el-select v-model="itemForm.itemColor" style="width: 100%">
            <el-option v-for="c in ['primary', 'success', 'warning', 'danger', 'info']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="itemForm.itemStatus" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemVisible = false">取消</el-button>
        <el-button type="primary" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download, MoreFilled } from '@element-plus/icons-vue'
import api from '../api'
import { usePager } from '../composables/usePager'
import { exportExcel } from '../utils/export'
import { empty } from '../common'

const loading = ref(false)
const types = ref([])
const items = ref([])
const dictType = ref('')
const typeKeyword = ref('')
const itemKeyword = ref('')
const typeVisible = ref(false)
const itemVisible = ref(false)
const typeForm = reactive({})
const itemForm = reactive({})

const filteredTypes = computed(() => {
  const kw = typeKeyword.value.trim().toLowerCase()
  if (!kw) return types.value
  return types.value.filter(t => t.dictName.toLowerCase().includes(kw) || t.dictType.toLowerCase().includes(kw))
})
const currentType = computed(() => types.value.find(t => t.dictType === dictType.value))

const filteredItems = computed(() => {
  const kw = itemKeyword.value.trim().toLowerCase()
  if (!kw) return items.value
  return items.value.filter(i => i.itemLabel.toLowerCase().includes(kw) || i.itemValue.toLowerCase().includes(kw))
})
const { page, pageSize, total, paged, reset } = usePager(filteredItems)

async function loadTypes() {
  loading.value = true
  try {
    types.value = await api.dictTypes()
    if (!dictType.value && types.value.length) dictType.value = types.value[0].dictType
    await loadItems()
  } finally {
    loading.value = false
  }
}

async function loadItems() {
  if (!dictType.value) { items.value = []; return }
  items.value = await api.dictItems({ dictType: dictType.value })
  reset()
}

function selectType(t) {
  dictType.value = t
  loadItems()
}

function onTypeCommand(cmd, t) {
  if (cmd === 'edit') openType(t)
  else removeType(t)
}

function openType(t) {
  Object.keys(typeForm).forEach(k => delete typeForm[k])
  Object.assign(typeForm, t ? { ...t, editing: true } : { dictStatus: 1, editing: false })
  typeVisible.value = true
}

async function saveType() {
  if (!typeForm.dictType || !typeForm.dictName) {
    ElMessage.warning('字典编码与名称不能为空')
    return
  }
  await api.saveDictType({ ...typeForm })
  ElMessage.success('保存成功')
  typeVisible.value = false
  loadTypes()
}

async function removeType(t) {
  await ElMessageBox.confirm(`删除字典类型「${t.dictName}」会同时删除其下 ${t.itemCount || 0} 个字典项，确定继续？`,
    '删除字典类型', { type: 'warning' })
  await api.deleteDictType(t.dictType)
  ElMessage.success('已删除')
  if (dictType.value === t.dictType) dictType.value = ''
  loadTypes()
}

function openItem(r) {
  Object.keys(itemForm).forEach(k => delete itemForm[k])
  Object.assign(itemForm, r ? { ...r } : { dictType: dictType.value, itemStatus: 1, itemColor: 'primary', sortNo: (items.value.length + 1) })
  itemVisible.value = true
}

async function saveItem() {
  if (!itemForm.itemValue || !itemForm.itemLabel) {
    ElMessage.warning('字典值与显示名称不能为空')
    return
  }
  await api.saveDictItem({ ...itemForm, dictType: dictType.value })
  ElMessage.success('保存成功')
  itemVisible.value = false
  loadTypes()
}

async function removeItem(r) {
  await ElMessageBox.confirm(`确定删除字典项「${r.itemLabel}」吗？`, '删除字典项', { type: 'warning' })
  await api.deleteDictItem(r.itemId)
  ElMessage.success('已删除')
  loadTypes()
}

function exportXls() {
  const rows = filteredItems.value.map(i => [i.dictType, i.itemValue, i.itemLabel, i.itemColor || '', i.sortNo, i.itemStatus === 1 ? '启用' : '停用', i.remark || ''])
  exportExcel(['字典编码', '字典值', '显示名称', '标签颜色', '排序', '状态', '备注'], rows,
    `字典_${currentType.value?.dictName || '导出'}`, '字典项')
  ElMessage.success('已导出当前字典项')
}

onMounted(loadTypes)
</script>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 12px; }
.head h3 { margin: 0 0 4px; font-size: 17px; color: #0f172a; }
.head .sub { margin: 0; font-size: 12px; color: #94a3b8; }
.ops { display: flex; gap: 8px; }
.dict-body { flex: 1; display: grid; grid-template-columns: 290px 1fr; gap: 12px; min-height: 0; }
.types { border: 1px solid #eef1f6; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.types-head { padding: 10px; border-bottom: 1px solid #f1f5f9; }
.type-list { flex: 1; overflow-y: auto; padding: 6px; }
.type-item { display: flex; align-items: center; gap: 8px; padding: 9px 10px; border-radius: 8px; cursor: pointer; }
.type-item:hover { background: #f7f9fd; }
.type-item.active { background: #eef4ff; }
.t-main { flex: 1; overflow: hidden; }
.t-name { font-size: 13px; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t-code { font-size: 11px; color: #94a3b8; }
.more { cursor: pointer; color: #94a3b8; }
.items { border: 1px solid #eef1f6; border-radius: 12px; padding: 10px 12px; display: flex; flex-direction: column; min-height: 0; }
.cur { display: flex; align-items: center; gap: 8px; font-size: 13.5px; color: #0f172a; font-weight: 600; }
code { background: #f1f5f9; padding: 1px 6px; border-radius: 4px; font-size: 12px; }
</style>
