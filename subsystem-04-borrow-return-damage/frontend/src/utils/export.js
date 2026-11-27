import * as XLSX from 'xlsx'

/** 导出 Excel：columns = [{ label, prop, width? }] */
export function exportExcel(columns, rows, filename, sheetName = 'Sheet1') {
  const head = columns.map(c => c.label)
  const data = rows.map(r => columns.map(c => (typeof c.prop === 'function' ? c.prop(r) : r[c.prop])))
  const ws = XLSX.utils.aoa_to_sheet([head, ...data])
  ws['!cols'] = columns.map(c => ({ wch: c.width || Math.max(10, c.label.length * 2.2) }))
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, sheetName)
  XLSX.writeFile(wb, filename.endsWith('.xlsx') ? filename : filename + '.xlsx')
}

/** 导出 CSV */
export function exportCsv(columns, rows, filename) {
  const head = columns.map(c => c.label).join(',')
  const body = rows.map(r => columns.map(c => {
    const v = typeof c.prop === 'function' ? c.prop(r) : r[c.prop]
    const s = v == null ? '' : String(v)
    return /[",\n]/.test(s) ? `"${s.replaceAll('"', '""')}"` : s
  }).join(','))
  const content = '\uFEFF' + [head, ...body].join('\n')
  const a = document.createElement('a')
  a.href = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }))
  a.download = filename.endsWith('.csv') ? filename : filename + '.csv'
  a.click()
}

/** 导出 PDF：内嵌 iframe 打印，不走 window.open 避免 popup-blocker */
export function printPdf(title, columns, rows, subtitle = '') {
  const th = columns.map(c => `<th>${c.label}</th>`).join('')
  const tr = rows.map((r, i) => `<tr class="${i % 2 ? 'alt' : ''}">` +
    columns.map(c => `<td>${(() => {
      const v = typeof c.prop === 'function' ? c.prop(r) : r[c.prop]
      return v == null ? '' : String(v)
    })()}</td>`).join('') + '</tr>').join('')
  const html = `<!doctype html><html><head><meta charset="utf-8"><title>${title}</title>
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
  <div class="foot">本表由学校物资采购管理系统生成</div>
  <script>window.onload=()=>{setTimeout(()=>window.print(),200)}<\/script>
  </body></html>`
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const iframe = document.createElement('iframe')
  iframe.style.display = 'none'
  iframe.src = url
  document.body.appendChild(iframe)
  setTimeout(() => { URL.revokeObjectURL(url) }, 8000)
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
