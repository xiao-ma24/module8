// 状态字典与工具
export const PR_STATUS = { 0: '草稿', 1: '待审批', 2: '已批准', 3: '已驳回', 4: '已转订单' }
export const PR_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'primary' }
export const PO_STATUS = { 0: '待确认', 1: '已确认', 2: '部分到货', 3: '已完成', 4: '已取消' }
export const PO_TAG = { 0: 'info', 1: 'primary', 2: 'warning', 3: 'success', 4: 'danger' }
export const ACPT_STATUS = { 0: '待验收', 1: '验收中', 2: '已完成', 3: '质量异议' }
export const ACPT_TAG = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
export const IN_STATUS = { 0: '待入库', 1: '已入库' }
export const IN_TAG = { 0: 'warning', 1: 'success' }
export const SOURCE = { 1: '部门申购', 2: '领用转申购', 3: '安全库存预警', 4: '实验室自备' }
export const CATEGORY = { '01': '办公耗材', '02': '实验器材', '03': '后勤物料', '04': '通用物资' }
export const PAY_TYPE = { 1: '货到付款', 2: '预付', 3: '月度结算' }
export const INVOICE = { 1: '增值税专票', 2: '普票', 3: '无需发票' }
export const HANDLE = { 1: '入库', 2: '退货', 3: '换货' }

export const money = v => '¥' + Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
export const qty = v => Number(v || 0).toLocaleString()
export const date = v => v ? String(v).replace('T', ' ').slice(0, 10) : '—'
export const dt = v => v ? String(v).replace('T', ' ').slice(0, 16) : '—'
