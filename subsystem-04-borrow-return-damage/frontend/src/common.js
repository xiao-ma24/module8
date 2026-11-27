// 状态字典与格式化工具（④借用归还报损）
export const BORROW_STATUS = {
  0: '待审批', 1: '待借出', 2: '在借', 3: '已归还', 4: '已报损', 5: '已核销'
}
export const BORROW_TAG = {
  0: 'warning', 1: 'primary', 2: 'success', 3: 'success', 4: 'danger', 5: 'info'
}
/** 归还状态（condition_code） */
export const CONDITION = { 1: '完好', 2: '轻微损坏', 3: '严重损坏', 4: '缺失' }
/** 归还验收结果（borrow_return_item.accept_result） */
export const ACCEPT = { 1: '完好入库', 2: '损坏核销', 3: '缺失核销' }
/** 报损来源（loss_order.source_type） */
export const LOSS_SOURCE = { 1: '借用转报损', 2: '盘亏报损', 3: '到期或超限报废' }
/** 报损审批状态 */
export const LOSS_STATUS = { 0: '待审批', 1: '通过', 2: '驳回' }
export const LOSS_STATUS_TAG = { 0: 'warning', 1: 'success', 2: 'danger' }
/** 处置结论 */
export const DISPOSE = { 1: '折价赔偿', 2: '免赔', 3: '报废核销' }

export const ROLE_NAME = {
  ADMIN: '系统管理员', DEPT_HEAD: '部门负责人', PURCHASER: '采购员',
  STORE_KEEPER: '仓库管理员', APPLICANT: '教职工/申请人', STUDENT: '学生', CHECKER: '验收人'
}

export const money = v => '¥' + Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
export const num = v => Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
export const date = v => v ? String(v).replace('T', ' ').slice(0, 10) : '—'
export const dt = v => v ? String(v).replace('T', ' ').slice(0, 16) : '—'
