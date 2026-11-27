<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">待我审批</div>
        <div class="d">部门负责人审本部门借用申请；库管员审全部待审批借用并做库存把关</div>
      </div>
      
    </div>
    <el-card class="page-card fill">
      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索单号 / 物资 / 借用人" clearable style="width:260px">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="grow"></div>
        <el-tag type="warning" effect="plain">待我审批 {{ filtered.length }} 条</el-tag>
        <el-button plain @click="refresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>

      <el-table :data="paged" stripe v-loading="loading">
        <el-table-column prop="borrowNo" label="借用单号" width="150" />
        <el-table-column prop="borrowerName" label="借用人" width="95" />
        <el-table-column prop="materialName" label="物资" min-width="170" show-overflow-tooltip />
        <el-table-column label="借用数量" width="110" align="right">
          <template #default="{ row: r }">{{ num(r.borrowQty) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column label="可借库存" width="110" align="right">
          <template #default="{ row: r }">
            <span :style="{ color: Number(r.availableQty) < Number(r.borrowQty) ? '#dc2626' : '#16a34a' }">
              {{ num(r.availableQty) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="预计归还" width="120">
          <template #default="{ row: r }">{{ date(r.dueDate) }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="140">
          <template #default="{ row: r }">{{ dt(r.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row: r }">
            <el-button type="primary" size="small" @click="openApprove(r)">审批</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !filtered.length" description="没有待审批的借用申请" :image-size="70" />

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>

    <el-dialog v-model="dlg.open" title="审批借用申请" width="540px">
      <el-descriptions v-if="dlg.row" :column="2" border size="small">
        <el-descriptions-item label="单号">{{ dlg.row.borrowNo }}</el-descriptions-item>
        <el-descriptions-item label="借用人">{{ dlg.row.borrowerName }}</el-descriptions-item>
        <el-descriptions-item label="物资">{{ dlg.row.materialName }}</el-descriptions-item>
        <el-descriptions-item label="借用数量">{{ num(dlg.row.borrowQty) }} {{ dlg.row.unit }}</el-descriptions-item>
        <el-descriptions-item label="预计归还">{{ date(dlg.row.dueDate) }}</el-descriptions-item>
        <el-descriptions-item label="可借库存">{{ num(dlg.row.availableQty) }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="dlg.form" label-width="100px" style="margin-top:16px">
        <el-form-item label="审批结论">
          <el-radio-group v-model="dlg.form.result">
            <el-radio-button :value="1">通过</el-radio-button>
            <el-radio-button :value="0">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" :required="dlg.form.result === 0">
          <el-input v-model="dlg.form.opinion" type="textarea" :rows="3"
                    :placeholder="dlg.form.result === 0 ? '驳回必须填写意见' : '选填'" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dlg.open = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doApprove">提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import api from '../api'
import { num, date, dt } from '../common'
import { usePager } from '../../../composables/usePager'

const list = ref([])
const loading = ref(false)
const kw = ref('')
const filtered = computed(() => list.value.filter(r =>
  !kw.value || (r.borrowNo || '').includes(kw.value) || (r.materialName || '').includes(kw.value)
  || (r.borrowerName || '').includes(kw.value)))
const { page, pageSize, total, paged, reset } = usePager(filtered, 10)

const dlg = reactive({ open: false, row: null, form: {} })
const saving = ref(false)

onMounted(refresh)
async function refresh() {
  loading.value = true
  try { list.value = await api.pendingApprove() } finally { loading.value = false }
  reset()
}
function openApprove(r) {
  dlg.row = r
  dlg.form = { result: 1, opinion: '' }
  dlg.open = true
}
async function doApprove() {
  saving.value = true
  try {
    await api.approveOrder(dlg.row.borrowId, { result: dlg.form.result, opinion: dlg.form.opinion })
    ElMessage.success('审批已提交')
    dlg.open = false
    refresh()
  } finally { saving.value = false }
}
</script>
