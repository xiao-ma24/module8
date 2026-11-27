<template>
  <div class="page-card fill" v-loading="loading">
    <div class="head">
      <div>
        <h3>角色权限</h3>
        <p class="sub">按菜单粒度授权 · 角色编码与各子系统 user.role_code 一一对应</p>
      </div>
      <div class="ops">
        <el-button :icon="Download" @click="exportXls">导出 Excel</el-button>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增角色</el-button>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane v-for="t in tabs" :key="t.name" :name="t.name" :label="`${t.label} (${countOf(t.name)})`" />
    </el-tabs>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索角色名称 / 编码" clearable style="width: 240px" :prefix-icon="Search" />
      <div class="grow"></div>
      <span class="total">共 {{ paged.length }} / {{ total }} 个角色</span>
    </div>

    <el-row :gutter="12" class="cards">
      <el-col v-for="r in paged" :key="r.roleId" :xs="24" :sm="12" :md="8" :lg="6">
        <div class="role-card">
          <div class="rc-head">
            <div class="ico" :style="{ background: colorOf(r.roleCode) }">{{ r.roleName.slice(0, 1) }}</div>
            <div class="meta">
              <div class="name">{{ r.roleName }}</div>
              <div class="code">{{ r.roleCode }}</div>
            </div>
            <el-tag size="small" :type="r.roleStatus === 1 ? 'success' : 'info'" effect="plain">
              {{ r.roleStatus === 1 ? '启用' : '停用' }}
            </el-tag>
          </div>
          <div class="rc-body">
            <div><span>数据范围</span><b>{{ DATA_SCOPE[r.dataScope] || '—' }}</b></div>
            <div><span>在岗人数</span><b>{{ r.userCount || 0 }}</b></div>
            <div><span>菜单权限</span><b>{{ (r.menuIds || []).length }} 项</b></div>
          </div>
          <div class="rc-foot">
            <el-button link type="primary" @click="grant(r)">分配权限</el-button>
            <el-button link @click="openForm(r)">编辑</el-button>
            <el-button link type="danger" :disabled="r.roleCode === 'ADMIN'" @click="remove(r)">删除</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="pager">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[8, 16, 24]"
                     :total="total" layout="total, sizes, prev, pager, next" background />
    </div>

    <!-- 角色表单 -->
    <el-dialog v-model="formVisible" :title="form.roleId ? '编辑角色' : '新增角色'" width="520px">
      <el-form :model="form" ref="formRef" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!form.roleId" placeholder="如 LAB_MANAGER，需与其他子系统一致" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如 实验室负责人" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option v-for="(v, k) in DATA_SCOPE" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.roleStatus" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配 -->
    <el-dialog v-model="grantVisible" :title="`分配菜单权限 · ${current?.roleName || ''}`" width="560px">
      <el-alert type="info" :closable="false" show-icon
                title="勾选的菜单决定该角色登录后能看到的侧边栏；取消勾选后需重新登录生效。" />
      <el-tree ref="treeRef" :data="menuTree" node-key="menuId" show-checkbox default-expand-all
               :props="{ label: 'menuName', children: 'children' }" class="tree" />
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGrant">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import api from '../api'
import { usePager } from '../composables/usePager'
import { exportExcel } from '../utils/export'
import { DATA_SCOPE } from '../common'

const loading = ref(false)
const roles = ref([])
const keyword = ref('')
const tab = ref('all')
const formVisible = ref(false)
const grantVisible = ref(false)
const menuTree = ref([])
const treeRef = ref()
const current = ref(null)
const formRef = ref()
const form = reactive({})

const tabs = [
  { name: 'all', label: '全部角色' },
  { name: 'on', label: '启用中' },
  { name: 'off', label: '已停用' }
]

const filtered = computed(() => {
  let list = roles.value
  if (tab.value === 'on') list = list.filter(r => r.roleStatus === 1)
  if (tab.value === 'off') list = list.filter(r => r.roleStatus !== 1)
  return list
})
const { page, pageSize, total, paged, reset } = usePager(filtered, 8)

function countOf(name) {
  if (name === 'on') return roles.value.filter(r => r.roleStatus === 1).length
  if (name === 'off') return roles.value.filter(r => r.roleStatus !== 1).length
  return roles.value.length
}

const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const palette = ['linear-gradient(135deg,#2563eb,#7c3aed)', 'linear-gradient(135deg,#0ea5e9,#22d3ee)',
  'linear-gradient(135deg,#f59e0b,#f97316)', 'linear-gradient(135deg,#10b981,#34d399)',
  'linear-gradient(135deg,#6366f1,#8b5cf6)', 'linear-gradient(135deg,#ef4444,#f472b6)']
function colorOf(code) {
  let n = 0
  for (const ch of code || '') n += ch.charCodeAt(0)
  return palette[n % palette.length]
}

async function load() {
  loading.value = true
  try {
    roles.value = await api.roles({ keyword: keyword.value || undefined })
    reset()
  } finally {
    loading.value = false
  }
}

function openForm(r) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, r ? { ...r } : { dataScope: '全校', roleStatus: 1, sortNo: roles.value.length + 1 })
  formVisible.value = true
}

async function save() {
  await formRef.value.validate()
  if (form.roleId) await api.updateRole({ ...form })
  else await api.createRole({ ...form })
  ElMessage.success('保存成功，重新登录后生效')
  formVisible.value = false
  load()
}

async function grant(r) {
  current.value = r
  menuTree.value = await api.menuTree()
  grantVisible.value = true
  await nextTick()
  treeRef.value?.setCheckedKeys(r.menuIds || [])
}

async function saveGrant() {
  const ids = [...treeRef.value.getCheckedKeys(), ...treeRef.value.getHalfCheckedKeys()]
  await api.grantMenus(current.value.roleId, ids)
  ElMessage.success('权限已保存')
  grantVisible.value = false
  load()
}

async function remove(r) {
  await ElMessageBox.confirm(`确定删除角色「${r.roleName}」吗？`, '删除角色', { type: 'warning' })
  await api.deleteRole(r.roleId)
  ElMessage.success('已删除')
  load()
}

function exportXls() {
  const rows = filtered.value.map(r => [r.roleCode, r.roleName, DATA_SCOPE[r.dataScope] || '', r.userCount || 0, (r.menuIds || []).length, r.roleStatus === 1 ? '启用' : '停用', r.remark || ''])
  exportExcel(['角色编码', '角色名称', '数据范围', '在岗人数', '菜单权限数', '状态', '备注'], rows, '角色清单', '角色')
  ElMessage.success('已导出角色清单')
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 6px; }
.head h3 { margin: 0 0 4px; font-size: 17px; color: #0f172a; }
.head .sub { margin: 0; font-size: 12px; color: #94a3b8; }
.ops { display: flex; gap: 8px; }
.total { font-size: 12px; color: #94a3b8; }
.cards { overflow-y: auto; }
.role-card { border: 1px solid #eef1f6; border-radius: 12px; padding: 14px; background: #fff; margin-bottom: 12px; }
.rc-head { display: flex; align-items: center; gap: 10px; }
.ico { width: 38px; height: 38px; border-radius: 10px; color: #fff; display: grid; place-items: center; font-size: 16px; }
.meta { flex: 1; }
.meta .name { font-size: 14px; font-weight: 600; color: #0f172a; }
.meta .code { font-size: 11px; color: #94a3b8; }
.rc-body { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 6px; margin: 12px 0 8px; font-size: 12px; }
.rc-body div { display: flex; flex-direction: column; gap: 2px; color: #94a3b8; }
.rc-body b { color: #334155; font-size: 13px; }
.rc-foot { border-top: 1px dashed #eef1f6; padding-top: 8px; display: flex; gap: 10px; }
.tree { max-height: 380px; overflow-y: auto; margin-top: 12px; border: 1px solid #eef1f6; border-radius: 8px; padding: 8px; }
</style>
