// 状态字典与格式化工具（③领用）
export const REQ_STATUS = {
  0: '草稿', 1: '待审批', 2: '审批中', 3: '已批准', 4: '已驳回',
  5: '已出库', 6: '已完成', 7: '已关闭'
}
export const REQ_TAG = {
  0: 'info', 1: 'warning', 2: 'warning', 3: 'primary', 4: 'danger',
  5: 'success', 6: 'success', 7: 'info'
}
export const CATEGORY = { '01': '办公耗材', '02': '实验器材', '03': '后勤物料', '04': '通用物资' }
export const MATERIAL_TYPE = { 1: '低值易耗品', 2: '低值耐用品' }

export const ROLE_NAME = {
  ADMIN: '系统管理员', DEPT_HEAD: '部门负责人', PURCHASER: '采购员',
  STORE_KEEPER: '仓库管理员', APPLICANT: '教职工/申请人', STUDENT: '学生', CHECKER: '验收人'
}

export const money = v => '¥' + Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
export const num = v => Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
export const date = v => v ? String(v).replace('T', ' ').slice(0, 10) : '—'
export const dt = v => v ? String(v).replace('T', ' ').slice(0, 16) : '—'
