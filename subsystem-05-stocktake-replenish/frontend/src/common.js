// 状态字典与工具
export const ST_STATUS = { 0: '待盘点', 1: '进行中', 2: '已完成' }
export const ST_TAG = { 0: 'info', 1: 'warning', 2: 'success' }

export const WARNING_TYPE = { 1: '低于补货点', 2: '临期', 3: '已过期', 4: '到期报废' }
export const WARNING_TAG = { 1: 'danger', 2: 'warning', 3: 'danger', 4: 'warning' }

export const PLAN_STATUS = { 0: '草稿', 1: '待审批', 2: '已批准', 3: '已下单', 4: '已关闭' }
export const PLAN_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'primary', 4: 'danger' }

export const RULE_TYPE = { 1: '安全库存', 2: '临期', 3: '到期报废' }
export const CATEGORY = { '01': '办公耗材', '02': '实验器材', '03': '后勤物料', '04': '通用物资' }

export const money = v => '¥' + Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
export const qty = v => Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 3 })
export const num = v => (v === null || v === undefined || v === '' ? '—' : Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 3 }))
export const date = v => v ? String(v).replace('T', ' ').slice(0, 10) : '—'
export const dt = v => v ? String(v).replace('T', ' ').slice(0, 16) : '—'
