<template>
  <div>
    <el-row :gutter="14" style="margin-bottom:14px">
      <el-col :span="8" v-for="(w, i) in warehouses" :key="w.warehouseId">
        <div class="wh-card" :style="{ background: grad[i % grad.length] }" @click="select(w.warehouseId)">
          <div class="wh-name">
            <el-icon><House /></el-icon>{{ w.warehouseName }}
            <el-tag size="small" :type="w.warehouseType === 2 ? 'warning' : 'success'" effect="dark">
              {{ w.warehouseType === 2 ? '个人保管仓' : '公共仓库' }}</el-tag>
          </div>
          <div class="wh-loc">{{ w.warehouseType === 2 ? '科研经费自购登记 · 不参与公共领用' : w.location }}</div>
          <div class="wh-foot">
            <span>在库品类 <b>{{ kinds(w.warehouseId) }}</b></span>
            <span v-if="w.warehouseType === 2">在库总量 <b>{{ onHandOf(w.warehouseId) }}</b></span>
            <span v-else>低库存 <b>{{ lowCount(w.warehouseId) }}</b></span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card class="page-card fill" style="min-height: calc(100vh - 300px)">
      <div class="toolbar">
        <b style="font-size:15px">库存台账</b>
        <el-select v-model="wh" placeholder="全部仓库" clearable style="width:170px" @change="reset">
          <el-option v-for="w in warehouses" :key="w.warehouseId"
                     :label="w.warehouseName + (w.warehouseType === 2 ? '（个人保管）' : '')" :value="w.warehouseId" />
        </el-select>
        <el-select v-model="typeFilter" style="width:150px" @change="reset">
          <el-option label="全部归属" value="all" />
          <el-option label="公共仓库" value="public" />
          <el-option label="个人保管仓" value="private" />
        </el-select>
        <el-input v-model="kw" placeholder="搜索物资名称" clearable style="width:220px" @input="reset">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="lowOnly" style="width:130px" @change="reset">
          <el-option label="全部" value="all" /><el-option label="仅低库存" value="low" />
        </el-select>
        <div class="grow"></div>
        <el-button plain @click="doExport('xlsx')"><el-icon><Download /></el-icon>导出 Excel</el-button>
        <el-button plain @click="doExport('pdf')"><el-icon><Printer /></el-icon>导出 PDF</el-button>
        <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增仓库</el-button>
      </div>

      <el-table :data="paged" stripe>
        <el-table-column prop="warehouseName" label="仓库" width="150" show-overflow-tooltip />
        <el-table-column label="归属" width="150">
          <template #default="{ row: r }">
            <el-tag v-if="r.warehouseType === 2" type="warning" size="small" effect="plain">个人保管</el-tag>
            <el-tag v-else type="success" size="small" effect="plain">公共仓库</el-tag>
            <span v-if="r.warehouseType === 2" style="margin-left:6px;font-size:12px;color:#b45309">
              {{ r.holderName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="materialName" label="物资" min-width="170" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column label="公共可用" width="100" align="right">
          <template #default="{ row: r }">
            <span v-if="r.warehouseType === 2" style="color:#94a3b8">—</span>
            <span v-else :style="{ color: r.qtyAvailable < r.safeStock ? '#ef4444' : '#16a34a', fontWeight: 600 }">
              {{ qty(r.qtyAvailable) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="在库总量" width="100" align="right">
          <template #default="{ row: r }">{{ qty(r.qtyOnHand) }}</template>
        </el-table-column>
        <el-table-column label="安全库存" width="100" align="right">
          <template #default="{ row: r }">
            <span v-if="r.warehouseType === 2" style="color:#94a3b8">—</span>
            <span v-else>{{ r.safeStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="在途量" width="95" align="right">
          <template #default="{ row: r }">
            <span v-if="r.warehouseType === 2" style="color:#94a3b8">—</span>
            <span v-else-if="Number(r.inTransit) > 0" style="color:#2563eb; font-weight:600">{{ qty(r.inTransit) }}</span>
            <span v-else style="color:#cbd5e1">0</span>
          </template>
        </el-table-column>
        <el-table-column label="库存状态" width="110" align="center">
          <template #default="{ row: r }">
            <el-tag v-if="r.warehouseType === 2" type="warning" size="small" effect="plain">个人保管</el-tag>
            <el-tag v-else :type="r.qtyAvailable < r.safeStock ? 'danger' : 'success'" size="small" effect="dark">
              {{ r.qtyAvailable < r.safeStock ? '低于安全线' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建议" min-width="180">
          <template #default="{ row: r }">
            <span v-if="r.warehouseType === 2" style="color:#b45309">科研自购物资，仅保管人可用，不进入公共领用</span>
            <span v-else-if="r.qtyAvailable < r.safeStock" style="color:#b45309">
              建议采购 {{ Math.max(r.safeStock * 2 - r.qtyAvailable - Number(r.inTransit || 0), 1) }} {{ r.unit }}<template v-if="Number(r.inTransit) > 0">（已扣除在途 {{ qty(r.inTransit) }}）</template></span>
            <span v-else style="color:#cbd5e1">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <el-dialog v-model="create.open" title="新增仓库" width="480px">
      <el-form :model="create.form" label-width="90px">
        <el-form-item label="仓库类型">
          <el-radio-group v-model="create.form.warehouseType">
            <el-radio :value="1">公共实体仓库</el-radio>
            <el-radio :value="2">虚拟个人保管仓</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="仓库名称"><el-input v-model="create.form.warehouseName" /></el-form-item>
        <el-form-item label="所在位置">
          <el-input v-model="create.form.location"
                    :placeholder="create.form.warehouseType === 2 ? '默认为虚拟保管库，可不填' : '如：实验楼101'" />
        </el-form-item>
        <el-form-item v-if="create.form.warehouseType !== 2" label="仓库管理员">
          <el-select v-model="create.form.keeperId" style="width:100%" placeholder="仅可选择具备仓库管理员角色的用户">
            <el-option v-for="u in keepers" :key="u.userId" :label="u.userName" :value="u.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="create.open = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '../api'
import { money, qty } from '../common'
import { exportExcel, printPdf } from '../utils/export'
import { usePager } from '../composables/usePager'
import { ElMessage } from 'element-plus'

const warehouses = ref([]), stocks = ref([]), users = ref([])
const wh = ref(null), kw = ref(''), lowOnly = ref('all'), typeFilter = ref('all')
const grad = ['linear-gradient(135deg,#2563eb,#60a5fa)', 'linear-gradient(135deg,#0d9488,#2dd4bf)', 'linear-gradient(135deg,#7c3aed,#a78bfa)']

const filtered = computed(() => stocks.value.filter(s =>
  (wh.value == null || s.warehouseId === wh.value) &&
  (typeFilter.value === 'all' || (typeFilter.value === 'private' ? s.warehouseType === 2 : s.warehouseType !== 2)) &&
  (lowOnly.value === 'all' || (s.warehouseType !== 2 && s.qtyAvailable < s.safeStock)) &&
  (!kw.value || (s.materialName || '').includes(kw.value))))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)
const keepers = computed(() => users.value.filter(u => u.roleCode === 'STORE_KEEPER'))

const create = reactive({ open: false, form: {} })

onMounted(refresh)
async function refresh() {
  warehouses.value = await api.warehouses()
  stocks.value = await api.stocks()
  users.value = await api.users()
}
const kinds = id => stocks.value.filter(s => s.warehouseId === id).length
const lowCount = id => stocks.value.filter(s => s.warehouseId === id && s.warehouseType !== 2 && s.qtyAvailable < s.safeStock).length
const onHandOf = id => qty(stocks.value.filter(s => s.warehouseId === id).reduce((a, b) => a + Number(b.qtyOnHand || 0), 0))
const select = id => { wh.value = id; reset() }

function openCreate() {
  create.form = { warehouseType: 1, keeperId: keepers.value[0]?.userId }
  create.open = true
}
async function save() {
  if (!create.form.warehouseName) return ElMessage.warning('仓库名称必填')
  if (create.form.warehouseType !== 2 && !create.form.keeperId) return ElMessage.warning('请选择仓库管理员')
  if (create.form.warehouseType === 2) {
    create.form.location = create.form.location || '虚拟个人保管库'
    create.form.keeperId = null
  }
  await api.post('/base/warehouses', create.form)
  ElMessage.success('已新增')
  create.open = false
  refresh()
}

const cols = [
  { label: '仓库', prop: 'warehouseName' },
  { label: '归属', prop: r => r.warehouseType === 2 ? '个人保管' : '公共仓库' },
  { label: '保管人', prop: r => r.warehouseType === 2 ? (r.holderName || '') : '' },
  { label: '物资', prop: 'materialName', width: 26 },
  { label: '单位', prop: 'unit' },
  { label: '公共可用', prop: r => r.warehouseType === 2 ? '' : qty(r.qtyAvailable) },
  { label: '在库总量', prop: r => qty(r.qtyOnHand) }, { label: '锁定库存', prop: 'qtyLocked' },
  { label: '安全库存', prop: r => r.warehouseType === 2 ? '' : r.safeStock },
  { label: '在途量', prop: r => r.warehouseType === 2 ? '' : qty(r.inTransit) },
  { label: '库存状态', prop: r => r.warehouseType === 2 ? '个人保管' : (r.qtyAvailable < r.safeStock ? '低于安全线' : '正常') },
]
function doExport(type) {
  if (!filtered.value.length) return ElMessage.warning('没有可导出的数据')
  if (type === 'xlsx') exportExcel(cols, filtered.value, '库存台账', '库存台账')
  else printPdf('库存台账', cols, filtered.value, '学校物资采购管理系统')
}
</script>

<style scoped>
.wh-card { border-radius: 14px; padding: 18px; color: #fff; cursor: pointer; transition: transform .18s; }
.wh-card:hover { transform: translateY(-3px); }
.wh-name { font-size: 16px; font-weight: 700; display: flex; align-items: center; gap: 6px; }
.wh-loc { font-size: 12px; opacity: .9; margin: 6px 0 12px; }
.wh-foot { display: flex; gap: 22px; font-size: 12px; opacity: .95; }
</style>
