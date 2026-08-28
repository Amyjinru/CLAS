import { reactive, ref } from 'vue'

export function useTableQuery(fetcher, options = {}) {
  const rows = ref([])
  const total = ref(0)
  const page = ref(options.page || 1)
  const size = ref(options.size || 10)
  const loading = ref(false)
  const filters = reactive({ ...(options.filters || {}) })

  async function load(extraParams = {}) {
    loading.value = true
    try {
      const params = options.params
        ? options.params({ page: page.value, size: size.value, filters, extraParams })
        : { page: page.value, size: size.value, ...filters, ...extraParams }
      const data = await fetcher(params)
      rows.value = options.rows ? options.rows(data) : (data.records || data.rows || [])
      total.value = options.total ? options.total(data) : (data.total || rows.value.length)
      return data
    } finally {
      loading.value = false
    }
  }

  function search() {
    page.value = 1
    return load()
  }

  function reset(nextFilters = {}) {
    Object.keys(filters).forEach((key) => {
      filters[key] = Object.prototype.hasOwnProperty.call(nextFilters, key) ? nextFilters[key] : ''
    })
    return search()
  }

  function onPageChange(value) {
    page.value = value
    return load()
  }

  return { rows, total, page, size, loading, filters, load, search, reset, onPageChange }
}
