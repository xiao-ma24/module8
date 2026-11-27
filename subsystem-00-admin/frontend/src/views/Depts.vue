<template>
  <div class="page-card fill" v-loading="loading">
    <div class="head">
      <div>
        <h3>部门管理</h3>
        <p class="sub">组织架构与月度预算额度 · 其他子系统的部门下拉均取自此处</p>
      </div>
      <div class="ops">
        <el-button :icon="Download" @click="exportXls">导出 Excel</el-button>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增部门</el-button>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name" :label="`${t.label} (${countOf(t.name)})`" />
    </el-tabs>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索部门名称 / 编码" clearable style="width: 240px" :prefix-icon="Search" />
      <div class="grow"></div>
      <span class="total">共 {{ total }} 个部门</span>
    </div>

    <el-table :data="paged" height="100%">
      <el-table-column prop="sortNo" label="排序" width="70" />
      <el-table-column label="部门" min-width="160">
        <template #default="{ row: r }">
          <div class="dept">
            <span class="ico"><el-icon><OfficeBuilding /></el-icon></span>
            <div>
              <div class="nm">{{ r.deptName }}</div>
              <div class="cd">{{ r.deptCode || '未设置编码' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="负责人" width="120">
        <template #default="{ row: r }">{{ empty(r.managerName) }}</template>
      </el-table-column>
      <el-table-column label="成员数" width="90">
        <template #default="{ row: r }">
          <el-tag size="small" effect="plain">{{ r.userCount || 0 }} 人</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="年度预算执行" min-width="220">
        <template #default="{ row: r }">
          <div class="quota">
            <el-progress :percentage="pct(r)" :stroke-width="8" :color="pct(r) > 90 ? '#ef4444' : '#2563eb'" />
            <span class="q-txt">{{ money(r.usedAmount) }} / {{ money(r.annualQuota) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row: r }">
          <el-tag size="small" :type="r.deptStatus === 1 ? 'success' : 'info'" effect="light">
            {{ r.deptStatus === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140">
        <template #default="{ row: r }">{{ empty(r.remark) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row: r }">
          <el-button link type="primary" @click="openForm(r)">编辑</el-button>
          <el-button link type="danger" @click="remove(r)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]"
                     :total="total" layout="total, sizes, prev, pager, next" background />
    </div>

    <el-dialog v-model="formVisible" :title="form.deptId ? '编辑部门' : '新增部门'" width="560px">
      <el-form :model="form" ref="formRef" :rules="rules" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门编码">
              <el-input v-model="form.deptCode" placeholder="如 SYS / JWC" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select v-model="form.managerId" placeholder="选择负责人" clearable style="width: 100%">
                <el-option v-for="u in users" :key="u.userId" :label="`${u.userName}（${u.userNo}）`" :value="u.userId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年度预算">
              <el-input-number v-model="form.annualQuota" :min="0" :step="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortNo" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.deptStatus" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { Search, Plus, Download, OfficeBuilding } from '@element-plus/icons-vue'
import api from '../api'
import { usePager } from '../composables/usePager'
import { exportExcel } from '../utils/export'
import { money, empty } from '../common'

const loading = ref(false)
const rows = ref([])
const users = ref([])
const keyword = ref('')
const tab = ref('all')
const formVisible = ref(false)
const formRef = ref()
const form = reactive({})

const tabs = [{ name: 'all', label: '全部部门' }, { name: 'on', label: '启用中' }, { name: 'off', label: '已停用' }]

const filtered = computed(() => {
  let list = rows.value
  if (tab.value === 'on') list = list.filter(r => r.deptStatus === 1)
  if (tab.value === 'off') list = list.filter(r => r.deptStatus !== 1)
  return list
})
const { page, pageSize, total, paged, reset } = usePager(filtered)

function countOf(name) {
  if (name === 'on') return rows.value.filter(r => r.deptStatus === 1).length
  if (name === 'off') return rows.value.filter(r => r.deptStatus !== 1).length
  return rows.value.length
}

const rules = { deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

function pct(r) {
  const q = Number(r.annualQuota || 0)
  if (!q) return 0
  return Math.min(100, Math.round((Number(r.usedAmount || 0) / q) * 100))
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.depts({ keyword: keyword.value || undefined })
    reset()
  } finally {
    loading.value = false
  }
}

function openForm(r) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, r ? { ...r } : { deptStatus: 1, sortNo: rows.value.length + 1, annualQuota: 10000 })
  formVisible.value = true
}

async function save() {
  await formRef.value.validate()
  if (form.deptId) await api.updateDept({ ...form })
  else await api.createDept({ ...form })
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

async function remove(r) {
  await ElMessageBox.confirm(`确定删除部门「${r.deptName}」吗？`, '删除部门', { type: 'warning' })
  await api.deleteDept(r.deptId)
  ElMessage.success('已删除')
  load()
}

function exportXls() {
  const data = filtered.value.map(r => [r.deptCode || '', r.deptName, r.managerName || '', r.userCount || 0,
    r.annualQuota || 0, r.usedAmount || 0, pct(r) + '%', r.deptStatus === 1 ? '启用' : '停用', r.remark || ''])
  exportExcel(['部门编码', '部门名称', '负责人', '成员数', '年度预算', '已使用', '执行率', '状态', '备注'], data, '部门清单', '部门')
  ElMessage.success('已导出部门清单')
}

onMounted(async () => {
  await load()
  users.value = await api.users({})
})
</script>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 6px; }
.head h3 { margin: 0 0 4px; font-size: 17px; color: #0f172a; }
.head .sub { margin: 0; font-size: 12px; color: #94a3b8; }
.ops { display: flex; gap: 8px; }
.total { font-size: 12px; color: #94a3b8; }
.dept { display: flex; align-items: center; gap: 10px; }
.dept .ico { width: 30px; height: 30px; border-radius: 8px; background: #eef4ff; color: #2563eb; display: grid; place-items: center; }
.dept .nm { font-size: 13.5px; color: #0f172a; }
.dept .cd { font-size: 11px; color: #94a3b8; }
.quota { display: flex; align-items: center; gap: 10px; }
.quota :deep(.el-progress) { flex: 1; }
.q-txt { font-size: 11.5px; color: #64748b; white-space: nowrap; }
</style>
