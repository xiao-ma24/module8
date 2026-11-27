<template>
  <div>
    <div class="page-head">
      <div>
        <div class="t">待我审批</div>
        <div class="d">部门负责人审本部门领用申请；库管员审全部待审批领用并做库存把关</div>
      </div>
      
    </div>
    <el-card class="page-card fill">
      <div class="toolbar">
        <el-input v-model="kw" placeholder="搜索单号 / 物资 / 申请人" clearable style="width:260px">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="grow"></div>
        <el-tag type="warning" effect="plain">待我审批 {{ filtered.length }} 条</el-tag>
        <el-button plain @click="refresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>

      <el-table :data="paged" stripe v-loading="loading">
        <el-table-column prop="reqNo" label="领用单号" width="150" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column label="物资" min-width="170" show-overflow-tooltip>
          <template #default="{ row: r }">
            {{ r.materialName }}
            <el-tag v-if="r.isControlled === 1" size="small" type="danger" effect="plain" style="margin-left:6px">管控</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请数量" width="110" align="right">
          <template #default="{ row: r }">{{ num(r.qtyApply) }} {{ r.unit }}</template>
        </el-table-column>
        <el-table-column label="可领库存" width="110" align="right">
          <template #default="{ row: r }">
            <span :style="{ color: Number(r.availableQty) < Number(r.qtyApply) ? '#dc2626' : '#16a34a' }">
              {{ num(r.availableQty) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="预估金额" width="110" align="right">
          <template #default="{ row: r }">{{ money(r.amount) }}</template>
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

      <el-empty v-if="!loading && !filtered.length" description="没有待审批的领用申请" :image-size="70" />

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total"
                       :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background />
      </div>
    </el-card>

    <el-dialog v-model="dlg.open" title="审批领用申请" width="560px">
      <el-descriptions v-if="dlg.row" :column="2" border size="small">
        <el-descriptions-item label="单号">{{ dlg.row.reqNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ dlg.row.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="物资">{{ dlg.row.materialName }}</el-descriptions-item>
        <el-descriptions-item label="可领库存">{{ num(dlg.row.availableQty) }}</el-descriptions-item>
        <el-descriptions-item label="申请数量">{{ num(dlg.row.qtyApply) }} {{ dlg.row.unit }}</el-descriptions-item>
        <el-descriptions-item label="预估金额">{{ money(dlg.row.amount) }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="dlg.form" label-width="100px" style="margin-top:16px">
        <el-form-item label="审批结论">
          <el-radio-group v-model="dlg.form.result">
            <el-radio-button :value="1">通过</el-radio-button>
            <el-radio-button :value="0">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="dlg.form.result === 1" label="核准数量">
          <el-input-number v-model="dlg.form.qtyApprove" :min="0.001" :precision="3" :step="1" style="width:160px" />
          <span style="font-size:12px;color:#94a3b8;margin-left:10px">可部分批准，须 ≤ 申请数量</span>
        </el-form-item>
        <el-form-item label="审批意见" :required="dlg.form.result === 0 || dlg.row?.isControlled === 1">
          <el-input v-model="dlg.form.opinion" type="textarea" :rows="3"
                    :placeholder="dlg.form.result === 0 ? '驳回必须填写意见'
                      : (dlg.row?.isControlled === 1 ? '管控物资需填写加签说明' : '选填')" />
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
import { money, num, dt } from '../common'
import { usePager } from '../../../composables/usePager'

const list = ref([])
const loading = ref(false)
const kw = ref('')
const filtered = computed(() => list.value.filter(r =>
  !kw.value || (r.reqNo || '').includes(kw.value) || (r.materialName || '').includes(kw.value)
  || (r.applicantName || '').includes(kw.value)))
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
  dlg.form = { result: 1, qtyApprove: Number(r.qtyApply), opinion: '' }
  dlg.open = true
}
async function doApprove() {
  saving.value = true
  try {
    await api.approveOrder(dlg.row.reqId, {
      result: dlg.form.result,
      qtyApprove: dlg.form.result === 1 ? dlg.form.qtyApprove : null,
      opinion: dlg.form.opinion
    })
    ElMessage.success('审批已提交')
    dlg.open = false
    refresh()
  } finally { saving.value = false }
}
</script>
