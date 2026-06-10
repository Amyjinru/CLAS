export function formatFen(value, options = {}) {
  const { symbol = false, decimals = 2 } = options
  const amount = Number(value || 0) / 100
  const text = amount.toFixed(decimals)
  return symbol ? `¥${text}` : text
}

export function formatMoney(value) {
  if (value === null || value === undefined) return '-'
  return (Number(value) / 100).toFixed(2)
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

/**
 * 解析营业时间字符串（如 "09:00-21:00"）为分钟数
 * 返回 [startMinutes, endMinutes]，解析失败返回 [null, null]
 */
export function parseBusinessMinutes(hoursText) {
  if (!hoursText || !hoursText.includes('-')) return [null, null]
  const [startText, endText] = hoursText.split('-').map(s => s.trim())
  const toMinutes = (t) => {
    const parts = t.split(':').map(Number)
    if (parts.length < 2 || isNaN(parts[0]) || isNaN(parts[1])) return null
    return parts[0] * 60 + parts[1]
  }
  const start = toMinutes(startText)
  const end = toMinutes(endText)
  if (start === null || end === null || start === end) return [null, null]
  return [start, end]
}

/**
 * 判断商家是否在营业时间内
 */
export function isWithinBusinessHours(businessHours) {
  const [start, end] = parseBusinessMinutes(businessHours)
  if (start === null) return true // 无法解析时默认营业中
  const now = new Date()
  const nowMinutes = now.getHours() * 60 + now.getMinutes()
  if (start <= end) return nowMinutes >= start && nowMinutes <= end
  return nowMinutes >= start || nowMinutes <= end // 跨天情况
}
