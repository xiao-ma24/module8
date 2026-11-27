import { ref, computed, watch } from 'vue'

/** 前端分页：传入过滤后的列表（ref/computed），返回分页切片与页码状态 */
export function usePager(source, size = 10) {
  const page = ref(1)
  const pageSize = ref(size)
  const total = computed(() => (source.value || []).length)
  const paged = computed(() => {
    const s = (page.value - 1) * pageSize.value
    return (source.value || []).slice(s, s + pageSize.value)
  })
  watch(total, () => {
    const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))
    if (page.value > maxPage) page.value = maxPage
  })
  const reset = () => { page.value = 1 }
  return { page, pageSize, total, paged, reset }
}
