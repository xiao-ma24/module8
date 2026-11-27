<template>
  <div class="page-card fill">
    <div class="tabs">
      <div v-for="t in tabs" :key="t.key" class="tab" :class="{ active: tab === t.key }" @click="switchTab(t.key)">
        {{ t.label }} <span class="num">({{ countOf(t.key) }})</span>
      </div>
    </div>

    <div class="toolbar">
      <el-input v-model="q.keyword" placeholder="搜索工号 / 姓名 / 手机号" clearable style="width:220px" @keyup.enter="load">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="q.deptId" placeholder="全部部门" clearable style="width:150px" @change="load">
        <el-option v-for="d in depts" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
      </el-select>
      <el-select v-model="q.userType" placeholder="全部类型" clearable style="width:130px" @change="load">
        <el-option label="教职工" :value="1" />
        <el-option label="学生" :value="2" />
        <el-option label="外部人员" :value="3" />
      </el-select>
      <el-button @click="load" :icon="Search">查询</el-button>
      <div class="grow" />
      <el-button :icon="Refresh" @click="syncSource">身份源同步</el-button>
      <el-button :icon="Upload" @click="pickFile">导入</el-button>
      <el-button :icon="Download" @click="exportList('excel')">导出 Excel</el-button>
      <el-button :icon="Printer" @click="exportList('pdf')">导出 PDF</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
    </div>

    <div class="hint-bar">
      账号生命周期：提交身份信息 → 查重校验 → 建立账号（待启用，无任何业务权限）→ 分配部门与角色 → 启用并通知 → 变更 / 停用并撤权，全过程留痕。
    </div>

    <el-table :data="paged" v-loading="loading" height="100%" stripe>
      <el-table-column prop="userNo" label="工号/学号" width="110" />
      <el-table-column prop="userName" label="姓名" width="100" />
      <el-table-column label="类型" width="90">
        <template #default="{ row: r }">
          <el-tag size="small" effect="plain">{{ typeName(r.userType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deptName" label="所属部门" min-width="130" show-overflow-tooltip />
      <el-table-column label="角色" min-width="170">
        <template #default="{ row: r }">
          <template v-if="r.roleNames">
            <el-tag v-for="n in r.roleNames.split('、')" :key="n" size="small" class="role-tag">{{ n }}</el-tag>
          </template>
          <span v-else class="muted">未授权</span>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="125">
        <template #default="{ row: r }">{{ r.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="来源" width="95">
        <template #default="{ row: r }">{{ sourceName(r.sourceType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row: r }">
          <el-tag size="small" :type="statusMeta(r.userStatus).type" effect="plain">{{ statusMeta(r.userStatus).label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近登录" width="150">
        <template #default="{ row: r }">{{ dt(r.lastLoginAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="215" fixed="right">
        <template #default="{ row: r }">
          <el-button v-if="r.userStatus !== 1" size="small" text type="success" @click="activate(r)">启用</el-button>
          <el-button v-else size="small" text type="danger" @click="disable(r)">停用撤权</el-button>
          <el-button size="small" text type="primary" @click="openEdit(r)">编辑</el-button>
          <el-dropdown trigger="click" @command="c => more(c, r)">
            <el-button size="small" text>更多<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="reset">重置密码</el-dropdown-item>
                <el-dropdown-item command="view">查看建档信息</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                     v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" />
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dlg" :title="form.userId ? '编辑用户' : '新增用户（建立后为待启用）'" width="660px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="工号/学号" required>
              <el-select v-if="form.userType === 1" v-model="form.userNo" filterable remote clearable
                         :remote-method="loadTeachers" :loading="tLoading" placeholder="从师资系统选择工号"
                         style="width:100%" @change="pickTeacher" @visible-change="v => v && loadTeachers('')">
                <el-option v-for="t in teachers" :key="t.teacherNo" :value="t.teacherNo"
                           :label="`${t.teacherNo} ${t.teacherName}`"
                           :disabled="t.registered && t.teacherNo !== form.userNo">
                  <span>{{ t.teacherNo }} · {{ t.teacherName }}</span>
                  <span class="opt-sub">{{ t.deptName || '未映射部门' }}{{ t.registered ? ' · 已建档' : '' }}</span>
                </el-option>
              </el-select>
              <el-input v-else v-model="form.userNo" placeholder="唯一，如 20230001" @blur="checkDup" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" required><el-input v-model="form.userName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人员类型">
              <el-select v-model="form.userType" style="width:100%" @change="onTypeChange">
                <el-option label="教职工" :value="1" />
                <el-option label="学生" :value="2" />
                <el-option label="外部人员" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-select v-model="form.deptId" clearable style="width:100%" placeholder="未分配">
                <el-option v-for="d in depts" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称/身份"><el-input v-model="form.title" placeholder="如 讲师 / 本科生" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份来源">
              <el-select v-model="form.sourceType" style="width:100%">
                <el-option label="本地补录" value="LOCAL" />
                <el-option label="师资系统" value="TEACHER" />
                <el-option label="学籍系统" value="STUDENT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="启用前必填，用于通知" @blur="checkDup" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱"><el-input v-model="form.email" @blur="checkDup" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职/入学">
              <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色（可多）">
              <el-select v-model="form.roleCodes" multiple collapse-tags style="width:100%" placeholder="未授权">
                <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert v-if="dupMsg" :title="dupMsg" type="warning" :closable="false" show-icon />
        <el-alert v-else type="info" :closable="false" show-icon
                  title="新账号默认为「待启用」且不带任何业务权限；补齐部门、角色、手机号后由管理员启用。" />
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看 -->
    <el-dialog v-model="viewDlg" title="用户建档信息" width="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="工号/学号">{{ cur.userNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ cur.userName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ cur.deptName }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ cur.roleNames || '未授权' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ cur.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ cur.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份来源">{{ sourceName(cur.sourceType) }}（{{ dt(cur.sourceSyncAt) }}）</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusMeta(cur.userStatus).label }}</el-descriptions-item>
        <el-descriptions-item label="建档">{{ dt(cur.createdAt) }} 由 {{ cur.createdBy || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ empty(cur.remark) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <input ref="fileRef" type="file" accept=".xlsx,.xls" style="display:none" @change="onFile" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Download, Upload, Printer, Refresh, ArrowDown } from '@element-plus/icons-vue'
import api from '../api'
import { dt, empty } from '../common'
import { usePager } from '../composables/usePager'
import { exportExcel, printPdf, readExcel } from '../utils/export'

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待启用' },
  { key: 'active', label: '已启用' },
  { key: 'disabled', label: '已停用' }
]

const list = ref([])
const depts = ref([])
const roles = ref([])
const loading = ref(false)
const saving = ref(false)
const tab = ref('all')
const q = reactive({ keyword: '', deptId: null, userType: null })
const dlg = ref(false)
const viewDlg = ref(false)
const dupMsg = ref('')
const fileRef = ref()
const cur = ref({})
const form = reactive(blank())

const filtered = computed(() => list.value.filter(u => {
  if (tab.value === 'pending') return u.userStatus === 2
  if (tab.value === 'active') return u.userStatus === 1
  if (tab.value === 'disabled') return u.userStatus === 0
  return true
}))
const { page, pageSize, total, paged } = usePager(filtered)

const COLS = [
  { label: '工号/学号', prop: 'userNo' },
  { label: '姓名', prop: 'userName' },
  { label: '类型', prop: r => typeName(r.userType) },
  { label: '部门', prop: 'deptName' },
  { label: '角色', prop: r => r.roleNames || '未授权' },
  { label: '手机号', prop: 'phone' },
  { label: '来源', prop: r => sourceName(r.sourceType) },
  { label: '状态', prop: r => statusMeta(r.userStatus).label },
  { label: '最近登录', prop: r => dt(r.lastLoginAt) }
]

function blank() {
  return {
    userId: null, userNo: '', userName: '', userType: 1, deptId: null, title: '',
    phone: '', email: '', hireDate: '', sourceType: 'TEACHER', roleCodes: [], remark: ''
  }
}

const teachers = ref([])
const tLoading = ref(false)

async function loadTeachers(kw) {
  tLoading.value = true
  try {
    teachers.value = await api.teacherOptions({ keyword: kw || undefined })
  } finally {
    tLoading.value = false
  }
}

function pickTeacher(no) {
  const t = teachers.value.find(x => x.teacherNo === no)
  if (!t) return
  form.userName = t.teacherName
  form.sourceType = 'TEACHER'
  if (t.deptId) form.deptId = t.deptId
  if (!form.phone && t.phone) form.phone = t.phone
  checkDup()
}

function onTypeChange(t) {
  if (t === 1) form.sourceType = 'TEACHER'
}

function typeName(t) { return t === 2 ? '学生' : t === 3 ? '外部人员' : '教职工' }
function sourceName(s) { return s === 'SELF' ? '自助注册' : s === 'TEACHER' ? '师资' : s === 'STUDENT' ? '学籍' : '本地补录' }
function statusMeta(s) {
  if (s === 1) return { label: '已启用', type: 'success' }
  if (s === 2) return { label: '待启用', type: 'warning' }
  return { label: '已停用', type: 'info' }
}
function countOf(k) {
  if (k === 'pending') return list.value.filter(u => u.userStatus === 2).length
  if (k === 'active') return list.value.filter(u => u.userStatus === 1).length
  if (k === 'disabled') return list.value.filter(u => u.userStatus === 0).length
  return list.value.length
}
function switchTab(k) { tab.value = k; page.value = 1 }

async function load() {
  loading.value = true
  try {
    list.value = await api.users({
      keyword: q.keyword || undefined,
      deptId: q.deptId || undefined,
      userType: q.userType || undefined
    })
  } finally {
    loading.value = false
  }
}

async function loadMeta() {
  const [d, r] = await Promise.all([api.depts(), api.roles()])
  depts.value = d || []
  roles.value = r || []
}

async function checkDup() {
  dupMsg.value = ''
  const body = {
    userNo: form.userNo || undefined,
    phone: form.phone || undefined,
    email: form.email || undefined,
    excludeId: form.userId || undefined
  }
  if (!body.userNo && !body.phone && !body.email) return
  try {
    const d = await api.checkUser(body)
    if (d.duplicate) dupMsg.value = '校验未通过：' + d.conflicts.join('；') + '。请核准后合并或驳回。'
  } catch (e) { /* 忽略查重接口异常 */ }
}

function openCreate() {
  Object.assign(form, blank())
  dupMsg.value = ''
  dlg.value = true
  loadTeachers('')
}

function openEdit(r) {
  Object.assign(form, blank(), {
    ...r,
    roleCodes: r.roleCodes || (r.roleCode ? [r.roleCode] : []),
    hireDate: r.hireDate || ''
  })
  dupMsg.value = ''
  dlg.value = true
  loadTeachers('')
}

async function save() {
  if (!form.userNo || !form.userName) {
    ElMessage.warning(form.userType === 1 ? '请从师资系统选择工号并填写姓名' : '工号与姓名必填')
    return
  }
  if (dupMsg.value) { ElMessage.warning('存在重复账号，请先处理冲突'); return }
  saving.value = true
  try {
    if (form.userId) await api.updateUser({ ...form })
    else await api.createUser({ ...form })
    ElMessage.success(form.userId ? '已保存' : '已建立账号，状态为待启用，请分配部门与角色后启用')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function activate(r) {
  try {
    const { value } = await ElMessageBox.prompt('启用后将允许登录，并向其手机号发送首次登录提示。请填写启用原因：',
      `启用账号 · ${r.userName}`, { inputValue: '岗位到岗', inputPlaceholder: '启用原因' })
    await api.activateUser(r.userId, value)
    ElMessage.success('已启用，并已发送首次登录提示（演示）')
    load()
  } catch (e) {
    if (e !== 'cancel' && e?.msg) ElMessage.error(e.msg)
  }
}

async function disable(r) {
  try {
    const { value } = await ElMessageBox.prompt('停用后该账号立即失去全部角色权限，但历史单据与审计记录保留。请填写停用原因：',
      `停用并撤权 · ${r.userName}`,
      { inputPlaceholder: '如 离职 / 转岗 / 长期不在岗', inputValidator: v => (v && v.trim() ? true : '请填写停用原因') })
    await api.disableUser(r.userId, value)
    ElMessage.success('已停用并撤销角色权限')
    load()
  } catch (e) {
    if (e !== 'cancel' && e?.msg) ElMessage.error(e.msg)
  }
}

async function more(cmd, r) {
  if (cmd === 'view') { cur.value = r; viewDlg.value = true; return }
  if (cmd === 'reset') {
    try {
      await ElMessageBox.confirm(`将 ${r.userName} 的密码重置为初始密码 123456，并要求下次登录修改？`, '重置密码', { type: 'warning' })
      await api.resetPwd(r.userId)
      ElMessage.success('已重置，初始密码 123456')
    } catch (e) { /* 取消 */ }
  }
}

async function syncSource() {
  try {
    const d = await api.syncUsers()
    ElMessage.success(`同步完成：共 ${d.total} 人（师资 ${d.TEACHER || 0}、学籍 ${d.STUDENT || 0}、本地补录 ${d.LOCAL || 0}）`)
    load()
  } catch (e) { /* 已提示 */ }
}

function exportList(kind) {
  const name = `用户清单_${new Date().toISOString().slice(0, 10)}`
  if (kind === 'excel') exportExcel(COLS, filtered.value, name)
  else printPdf('用户清单', COLS, filtered.value, '平台管理端 · 用户管理')
}

function pickFile() { fileRef.value.click() }

async function onFile(e) {
  const f = e.target.files[0]
  if (!f) return
  try {
    const rows = await readExcel(f)
    const res = await api.importUsers(rows)
    if (res.failed) {
      ElMessageBox.alert(res.errors.join('<br>'), `导入完成：成功 ${res.success} 行，失败 ${res.failed} 行`, { dangerouslyUseHTMLString: true })
    } else {
      ElMessage.success(`导入成功 ${res.success} 行，均为待启用状态`)
    }
    load()
  } catch (err) {
    ElMessage.error('导入失败：' + (err?.message || '文件格式不正确'))
  } finally {
    e.target.value = ''
  }
}

onMounted(async () => { await loadMeta(); await load() })
</script>

<style scoped>
.tabs { display: flex; gap: 6px; padding: 2px 2px 14px; }
.tab { padding: 7px 16px; border-radius: 9px; font-size: 13px; color: #64748b; cursor: pointer; }
.tab:hover { background: #f1f5fb; }
.tab.active { background: #eff6ff; color: #1d4ed8; font-weight: 600; }
.tab .num { font-size: 12px; color: #94a3b8; }
.toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 10px; }
.grow { flex: 1; }
.hint-bar {
  font-size: 12px; color: #64748b; background: #f8fafc; border: 1px dashed #e2e8f0;
  border-radius: 8px; padding: 8px 12px; margin-bottom: 10px;
}
.role-tag { margin-right: 4px; }
.opt-sub { float: right; font-size: 11px; color: #94a3b8; margin-left: 12px; }
.pager { display: flex; justify-content: flex-end; padding-top: 12px; }
</style>
