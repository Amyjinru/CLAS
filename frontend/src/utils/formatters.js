export function formatFen(value, options = {}) {
  const { symbol = false, decimals = 2 } = options
  const amount = Number(value || 0) / 100
  const text = amount.toFixed(decimals)
  return symbol ? `¥${text}` : text
}

export function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

export function formatCompactDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ')
}

export function formatDistance(distance) {
  if (distance === null || distance === undefined) return '-'
  return distance < 1000 ? `${distance}m` : `${(distance / 1000).toFixed(1)}km`
}
