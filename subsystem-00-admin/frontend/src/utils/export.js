import * as XLSX from 'xlsx'

/**
 * 列定义同时支持两种写法：
 *   1) 字符串数组：['工号', '姓名']，行数据为二维数组
 *   2) 对象数组：[{ label: '工号', prop: 'userNo' }]，行数据为对象数组
 */
function normCols(columns) {
  return columns.map((c, i) => (typeof c === 'string' ? { label: c, index: i } : c))
}

function cell(col, row, i) {
  if (col.index !== undefined) return Array.isArray(row) ? row[col.index] : row[col.prop]
  return typeof col.prop === 'function' ? col.prop(row) : row[col.prop]
}

/** 导出 Excel */
export function exportExcel(columns, rows, filename, sheetName = 'Sheet1') {
  const cols = normCols(columns)
  const head = cols.map(c => c.label)
  const data = rows.map((r, ri) => cols.map((c, ci) => cell(c, r, ci)))
  const ws = XLSX.utils.aoa_to_sheet([head, ...data])
  ws['!cols'] = cols.map(c => ({ wch: c.width || Math.max(10, c.label.length * 2.2) }))
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, sheetName)
  XLSX.writeFile(wb, filename.endsWith('.xlsx') ? filename : filename + '.xlsx')
}

/** 导出 CSV */
export function exportCsv(columns, rows, filename) {
  const cols = normCols(columns)
  const head = cols.map(c => c.label).join(',')
  const body = rows.map(r => cols.map((c, i) => {
    const v = cell(c, r, i)
    const s = v == null ? '' : String(v)
    return /[",\n]/.test(s) ? `"${s.replaceAll('"', '""')}"` : s
  }).join(','))
  const content = '\uFEFF' + [head, ...body].join('\n')
  const a = document.createElement('a')
  a.href = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }))
  a.download = filename.endsWith('.csv') ? filename : filename + '.csv'
  a.click()
}

/** 导出 PDF：调用浏览器打印（在打印对话框选择「另存为 PDF」），中文不会乱码 */
export function printPdf(title, columns, rows, subtitle = '') {
  const w = window.open('', '_blank')
  if (!w) return
  const cols = normCols(columns)
  const th = cols.map(c => `<th>${c.label}</th>`).join('')
  const tr = rows.map((r, i) => `<tr class="${i % 2 ? 'alt' : ''}">` +
    cols.map((c, ci) => {
      const v = cell(c, r, ci)
      return `<td>${v == null ? '' : String(v)}</td>`
    }).join('') + '</tr>').join('')
  w.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>${title}</title>
  <style>
    body{font-family:'PingFang SC','Microsoft YaHei',sans-serif;padding:28px;color:#1f2937}
    h1{font-size:20px;margin-bottom:6px}
    .sub{color:#6b7280;font-size:12px;margin-bottom:16px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th{background:#eff6ff;color:#1e40af;text-align:left;padding:8px 10px;border-bottom:2px solid #bfdbfe}
    td{padding:7px 10px;border-bottom:1px solid #eef2f7}
    tr.alt td{background:#fafcff}
    .foot{margin-top:16px;font-size:11px;color:#9ca3af}
  </style></head><body>
  <h1>${title}</h1>
  <div class="sub">${subtitle || ''}　导出时间：${new Date().toLocaleString('zh-CN')}　共 ${rows.length} 条</div>
  <table><thead><tr>${th}</tr></thead><tbody>${tr}</tbody></table>
  <div class="foot">本表由学校物资管理大系统 · 平台管理端生成</div>
  <script>window.onload=()=>{setTimeout(()=>window.print(),300)}</script>
  </body></html>`)
  w.document.close()
}

/** 读取 Excel 文件为对象数组（首行为表头） */
export function readExcel(file) {
  return new Promise((resolve, reject) => {
    const fr = new FileReader()
    fr.onload = e => {
      try {
        const wb = XLSX.read(new Uint8Array(e.target.result), { type: 'array' })
        const ws = wb.Sheets[wb.SheetNames[0]]
        resolve(XLSX.utils.sheet_to_json(ws, { defval: '' }))
      } catch (err) { reject(err) }
    }
    fr.onerror = reject
    fr.readAsArrayBuffer(file)
  })
}
