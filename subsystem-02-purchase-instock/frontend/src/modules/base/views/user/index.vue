<template>
  <div class="user-page">
    <el-card shadow="always">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane :label="`用户列表 (${total})`" name="user">
          <div class="toolbar">
            <el-input
              v-model="query.keyword"
              placeholder="工号 / 姓名"
              clearable
              style="width: 180px"
              @keyup.enter="handleSearch"
            />
            <el-select v-model="query.deptId" placeholder="全部部门" clearable style="width: 160px">
              <el-option
                v-for="item in deptOptions"
                :key="item.id"
                :label="item.deptName"
                :value="item.id"
              />
            </el-select>
            <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
              <el-option label="待启用" :value="0" />
              <el-option label="启用" :value="1" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button class="btn-outline" @click="handleReset">重置</el-button>
            <el-button type="primary" class="push-right" @click="openCreate">新增用户</el-button>
          </div>

          <el-table v-loading="loading" :data="userList">
            <el-table-column prop="userNo" label="工号" min-width="120" />
            <el-table-column prop="userName" label="姓名" min-width="110" />
            <el-table-column label="所属部门" min-width="150">
              <template #default="{ row }">{{ row.deptName || '-' }}</template>
            </el-table-column>
            <el-table-column label="角色" min-width="180">
              <template #default="{ row }">{{ row.roleNames || '未分配' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="140">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag effect="plain" size="small" :type="row.status === 1 ? 'success' : 'warning'">
                  {{ row.status === 1 ? '启用' : '待启用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 1">
                  <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
                  <el-button type="primary" link @click="handleDisable(row)">停用</el-button>
                </template>
                <el-button v-else type="primary" link @click="openAssign(row)">启用/分配权限</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <span class="pager-total">共 {{ total }} 条</span>
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              background
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="sizes, prev, pager, next"
              @size-change="loadUsers"
              @current-change="loadUsers"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="部门管理" name="dept">
          <div class="toolbar">
            <span class="toolbar-tip">部门树用于新用户建档时选择所属部门。</span>
            <el-button type="primary" class="push-right" @click="openDeptCreate">新增部门</el-button>
          </div>
          <div v-loading="deptLoading" class="dept-tree-wrap">
            <el-tree
              v-if="deptTree.length"
              :data="deptTree"
              node-key="id"
              default-expand-all
              :expand-on-click-node="false"
              :props="{ label: 'label', children: 'children' }"
            />
            <el-empty v-else description="暂无部门，点击右上角新增" :image-size="80" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="userDialogVisible" :title="userMode === 'create' ? '新增用户' : '编辑用户'" width="560px">
      <div class="dialog-tip">
        {{ userMode === 'create'
          ? '默认密码 123456；未分配角色时账号为待启用状态。'
          : '编辑不会修改密码与已分配角色。' }}
      </div>
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工号" prop="userNo">
              <el-input v-model="userForm.userNo" maxlength="32" placeholder="登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="userName">
              <el-input v-model="userForm.userName" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <el-select v-model="userForm.deptId" placeholder="请选择部门" style="width: 100%">
                <el-option
                  v-for="item in deptOptions"
                  :key="item.id"
                  :label="item.deptName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col v-if="userMode === 'create'" :span="24">
            <el-form-item label="角色">
              <el-checkbox-group v-model="userForm.roleIds">
                <el-checkbox v-for="role in roles" :key="role.id" :label="role.id">
                  {{ role.roleName }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button class="btn-outline" @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSubmitting" @click="handleUserSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配权限" width="460px">
      <div class="dialog-tip">
        用户：{{ assignRow ? `${assignRow.userName}（${assignRow.userNo}）` : '' }}。分配成功后账号自动启用。
      </div>
      <el-checkbox-group v-model="assignRoleIds">
        <el-checkbox v-for="role in roles" :key="role.id" :label="role.id">
          {{ role.roleName }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button class="btn-outline" @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSubmitting" @click="handleAssign">确认启用</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deptDialogVisible" title="新增部门" width="520px">
      <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="90px">
        <el-form-item label="部门编号" prop="deptNo">
          <el-input v-model="deptForm.deptNo" maxlength="32" placeholder="如 JWC" />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="deptForm.deptName" maxlength="128" placeholder="如 教务处" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="deptTreeSelect"
            check-strictly
            :render-after-expand="false"
            placeholder="不选则为顶级部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="deptForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="deptForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="btn-outline" @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="deptSubmitting" @click="handleDeptSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignUserRoles,
  getUserPage,
  registerUser,
  updateUser
} from '../../api/sysUser'
import { createDept, getDeptList } from '../../api/sysDept'
import { getRoleList } from '../../api/sysRole'

const route = useRoute()

const activeTab = ref('user')

/** 由「平台管理 - 部门管理」菜单进入时，直接定位到部门管理 Tab */
function syncTabFromQuery() {
  activeTab.value = route.query.tab === 'dept' ? 'dept' : 'user'
}

watch(() => route.query.tab, syncTabFromQuery)

const userList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const query = reactive({
  keyword: '',
  deptId: null,
  status: null
})

const deptOptions = ref([])
const deptLoading = ref(false)
const roles = ref([])

const userDialogVisible = ref(false)
const userMode = ref('create')
const userSubmitting = ref(false)
const userFormRef = ref()
const userForm = reactive({
  id: null,
  userNo: '',
  userName: '',
  deptId: null,
  phone: '',
  roleIds: [],
  status: 0,
  userType: 1
})
const userRules = {
  userNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  userName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }]
}

const roleDialogVisible = ref(false)
const assignRow = ref(null)
const assignRoleIds = ref([])
const assignSubmitting = ref(false)

const deptTree = computed(() => buildTree(deptOptions.value))
const deptTreeSelect = computed(() => buildTree(deptOptions.value, true))

const deptDialogVisible = ref(false)
const deptSubmitting = ref(false)
const deptFormRef = ref()
const deptForm = reactive({
  deptNo: '',
  deptName: '',
  parentId: null,
  sort: 0,
  remark: ''
})
const deptRules = {
  deptNo: [{ required: true, message: '请输入部门编号', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

function buildTree(list, forSelect = false) {
  const map = new Map()
  list.forEach((item) => {
    map.set(item.id, {
      ...item,
      label: forSelect ? item.deptName : `${item.deptName}（${item.deptNo}）`,
      children: []
    })
  })
  const roots = []
  map.forEach((node) => {
    const parent = map.get(node.parentId)
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function loadUsers() {
  loading.value = true
  try {
    const data = await getUserPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: query.keyword || undefined,
      deptId: query.deptId || undefined,
      status: query.status === null ? undefined : query.status
    })
    userList.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function loadDepts() {
  deptLoading.value = true
  try {
    deptOptions.value = await getDeptList()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    deptLoading.value = false
  }
}

async function loadRoles() {
  try {
    roles.value = await getRoleList({ status: 1 })
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  }
}

function handleSearch() {
  pageNum.value = 1
  loadUsers()
}

function handleReset() {
  query.keyword = ''
  query.deptId = null
  query.status = null
  pageNum.value = 1
  loadUsers()
}

function handleTabChange(name) {
  if (name === 'dept') {
    loadDepts()
  }
}

function openCreate() {
  userMode.value = 'create'
  Object.assign(userForm, {
    id: null,
    userNo: '',
    userName: '',
    deptId: null,
    phone: '',
    roleIds: [],
    status: 0,
    userType: 1
  })
  userDialogVisible.value = true
  userFormRef.value?.clearValidate()
}

function openEdit(row) {
  userMode.value = 'edit'
  Object.assign(userForm, {
    id: row.id,
    userNo: row.userNo,
    userName: row.userName,
    deptId: row.deptId,
    phone: row.phone,
    roleIds: [],
    status: row.status,
    userType: row.userType
  })
  userDialogVisible.value = true
  userFormRef.value?.clearValidate()
}

async function handleUserSubmit() {
  await userFormRef.value.validate()
  userSubmitting.value = true
  try {
    if (userMode.value === 'create') {
      const created = await registerUser({
        userNo: userForm.userNo,
        userName: userForm.userName,
        deptId: userForm.deptId,
        phone: userForm.phone,
        userType: userForm.userType
      })
      if (userForm.roleIds.length) {
        await assignUserRoles({ userId: created.id, roleIds: userForm.roleIds })
        ElMessage.success('用户已创建，并已分配角色启用')
      } else {
        ElMessage.success('用户已创建，待分配权限后启用')
      }
    } else {
      await updateUser(userForm.id, {
        userNo: userForm.userNo,
        userName: userForm.userName,
        deptId: userForm.deptId,
        phone: userForm.phone,
        userType: userForm.userType,
        status: userForm.status
      })
      ElMessage.success('用户信息已更新')
    }
    userDialogVisible.value = false
    await Promise.all([loadUsers(), loadDepts()])
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    userSubmitting.value = false
  }
}

function openAssign(row) {
  assignRow.value = row
  assignRoleIds.value = [...(row.roleIds || [])]
  roleDialogVisible.value = true
}

async function handleAssign() {
  if (!assignRoleIds.value.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  assignSubmitting.value = true
  try {
    await assignUserRoles({ userId: assignRow.value.id, roleIds: assignRoleIds.value })
    ElMessage.success(`${assignRow.value.userName} 已启用`)
    roleDialogVisible.value = false
    await loadUsers()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    assignSubmitting.value = false
  }
}

async function handleDisable(row) {
  try {
    await ElMessageBox.confirm(
      `确定停用「${row.userName}（${row.userNo}）」吗？停用后将无法登录。`,
      '停用用户',
      { type: 'warning', confirmButtonText: '确定停用', cancelButtonText: '取消' }
    )
  } catch (error) {
    return
  }
  try {
    await updateUser(row.id, {
      userNo: row.userNo,
      userName: row.userName,
      deptId: row.deptId,
      phone: row.phone,
      userType: row.userType,
      status: 0
    })
    ElMessage.success('用户已停用')
    await loadUsers()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  }
}

function openDeptCreate() {
  Object.assign(deptForm, {
    deptNo: '',
    deptName: '',
    parentId: null,
    sort: 0,
    remark: ''
  })
  deptDialogVisible.value = true
  deptFormRef.value?.clearValidate()
}

async function handleDeptSubmit() {
  await deptFormRef.value.validate()
  deptSubmitting.value = true
  try {
    await createDept({
      deptNo: deptForm.deptNo,
      deptName: deptForm.deptName,
      parentId: deptForm.parentId || 0,
      sort: deptForm.sort,
      status: 1,
      remark: deptForm.remark
    })
    ElMessage.success('部门已新增')
    deptDialogVisible.value = false
    await loadDepts()
  } catch (error) {
    // 错误提示已由 request 拦截器统一处理
  } finally {
    deptSubmitting.value = false
  }
}

onMounted(() => {
  syncTabFromQuery()
  loadUsers()
  loadDepts()
  loadRoles()
})
</script>

<style scoped>
.user-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.toolbar-tip {
  font-size: 13px;
  color: var(--text-3);
}

.push-right {
  margin-left: auto;
}

.pager {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.pager-total {
  font-size: 13px;
  color: var(--text-3);
}

.pager .el-pagination {
  margin-left: auto;
}

.dept-tree-wrap {
  min-height: 180px;
  padding: 8px 4px;
}
</style>
