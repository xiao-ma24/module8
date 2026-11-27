<template>
  <div class="log-page">
    <el-card shadow="always">
      <div class="toolbar">
        <el-input
          v-model="query.operator"
          placeholder="操作人"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 160px">
          <el-option v-for="item in modules" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button class="btn-outline" @click="handleReset">重置</el-button>
        <span class="toolbar-tip push-right">仅记录增删改接口调用</span>
      </div>

      <el-table v-loading="loading" :data="logs">
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column label="操作类型" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" size="small" :type="typeTag(row.operationType)">
              {{ row.operationType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="涉及数据" min-width="220">
          <template #default="{ row }">
            <span class="data-preview">{{ previewParams(row.requestParams) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="请求" min-width="230">
          <template #default="{ row }">
            <span class="method">{{ row.requestMethod }}</span>
            <span class="uri">{{ row.requestUri }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP 地址" width="150" />
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag effect="plain" size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
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
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作时间">{{ formatTime(current.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ current.operator }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ current.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ current.operationType }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ current.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ current.ip }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag effect="plain" size="small" :type="current.status === 1 ? 'success' : 'danger'">
            {{ current.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ current.requestUri }}</el-descriptions-item>
        <el-descriptions-item v-if="current.errorMsg" label="失败原因" :span="2">
          {{ current.errorMsg }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="param-block">
        <div class="param-title">请求参数</div>
        <pre class="param-code">{{ prettyParams }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getLogPage } from '@/api/sysLog'

const modules = [
  '物资分类',
  '物资档案',
  '仓库管理',
  '库存管理',
  '发票登记',
  '库存申请',
  '用户管理',
  '部门管理',
  '角色管理',
  '操作日志'
]

const query = reactive({ operator: '', module: '', status: null })
const logs = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const detailVisible = ref(false)
const current = ref({})

const prettyParams = computed(() => {
  const raw = current.value.requestParams
  if (!raw) {
    return '无'
  }
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch (e) {
    return raw
  }
})

async function loadLogs() {
  loading.value = true
  try {
    const data = await getLogPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      operator: query.operator || undefined,
      module: query.module || undefined,
      status: query.status === null || query.status === '' ? undefined : query.status
    })
    logs.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadLogs()
}

function handleReset() {
  query.operator = ''
  query.module = ''
  query.status = null
  pageNum.value = 1
  loadLogs()
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function previewParams(raw) {
  if (!raw) {
    return '-'
  }
  return raw.length > 60 ? `${raw.slice(0, 60)}...` : raw
}

function typeTag(type) {
  if (type === '删除') {
    return 'danger'
  }
  if (type === '修改' || type === '审核') {
    return 'warning'
  }
  return 'success'
}

onMounted(loadLogs)
</script>

<style scoped>
.log-page {
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

.method {
  display: inline-block;
  min-width: 52px;
  margin-right: 8px;
  font-size: 12px;
  color: var(--text-2);
}

.uri {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  color: var(--text-1);
}

.data-preview {
  font-size: 12px;
  color: var(--text-2);
  word-break: break-all;
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

.param-block {
  margin-top: 16px;
}

.param-title {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-2);
}

.param-code {
  max-height: 240px;
  padding: 12px;
  margin: 0;
  overflow: auto;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-1);
  white-space: pre-wrap;
  word-break: break-all;
  background: #f7faff;
  border: 1px solid var(--line);
  border-radius: 10px;
}
</style>
