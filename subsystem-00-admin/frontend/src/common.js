// 平台管理端通用工具与字典
export const USER_TYPE = { 1: '教职工', 2: '学生', 3: '外部人员' }
export const USER_TYPE_TAG = { 1: 'primary', 2: 'success', 3: 'info' }
export const DATA_SCOPE = { '全校': '全校数据', '部门': '本部门数据', '本人': '仅本人数据' }
export const MENU_TYPE = { 1: '目录', 2: '菜单', 3: '按钮' }
export const RESULT = { 1: '成功', 0: '失败' }
export const COLOR_TAG = { primary: 'primary', success: 'success', warning: 'warning', danger: 'danger', info: 'info' }

export const money = v => '¥' + Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
export const date = v => (v ? String(v).replace('T', ' ').slice(0, 10) : '—')
export const dt = v => (v ? String(v).replace('T', ' ').slice(0, 16) : '—')
export const empty = v => (v === null || v === undefined || v === '' ? '—' : v)
