function toPositiveId(value) {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

function addNotificationSource(path) {
  const sep = path.includes('?') ? '&' : '?'
  return path.includes('from=notifications') ? path : `${path}${sep}from=notifications`
}

function safePath(path) {
  if (!path || typeof path !== 'string') return ''
  const trimmed = path.trim()
  return trimmed.startsWith('/') && !trimmed.startsWith('//') ? trimmed : ''
}

function allowedLegacyPath(item, patterns) {
  const path = safePath(item.targetPath)
  return patterns.some((pattern) => pattern.test(path)) ? addNotificationSource(path) : ''
}

function legacyOrderPath(item) {
  const path = safePath(item.targetPath)
  let match = path.match(/^\/orders\?(?:.*&)?orderId=(\d+)(?:&.*)?$/)
  if (match) return addNotificationSource(`/order/${match[1]}`)
  match = path.match(/^\/order\/(\d+)(?:\?.*)?$/)
  return match ? addNotificationSource(path) : ''
}

function reviewPath(item) {
  const orderId = toPositiveId(item.orderId)
  if (!orderId) return ''

  const params = new URLSearchParams()
  const reviewId = toPositiveId(item.reviewId)
  const replyId = toPositiveId(item.replyId)
  if (reviewId) params.set('reviewId', String(reviewId))
  if (replyId) params.set('replyId', String(replyId))

  const query = params.toString()
  return query ? `/review/${orderId}?${query}` : `/review/${orderId}`
}

export function notificationTarget(item = {}) {
  const type = String(item.type || '').toUpperCase()
  const targetType = String(item.targetType || '').toUpperCase()
  const title = String(item.title || '')
  const orderTitles = new Set([
    '订单已创建',
    '已支付(自动接单中)',
    '订单配送中',
    '订单已完成',
    '商家已拒单',
    '退款申请已提交',
    '退款已通过',
    '退款被拒绝'
  ])
  const reviewTitles = new Set(['商家回复了评价', '评价收到新回复'])
  const bookingTitles = new Set(['预约已提交', '新的预约申请', '预约已取消', '预约状态更新'])
  const dealOrderTitles = new Set(['团购券待支付', '团购券购买成功', '团购券已核销', '团购券已退款'])

  if (targetType === 'ORDER' || type === 'ORDER_STATUS' || orderTitles.has(title)) {
    const orderId = toPositiveId(item.orderId || item.targetId)
    return orderId
      ? addNotificationSource(`/order/${orderId}`)
      : legacyOrderPath(item)
  }

  if (
    targetType === 'REVIEW' ||
    targetType === 'REPLY' ||
    type === 'REVIEW_REPLY' ||
    type === 'MERCHANT_REVIEW_REPLY' ||
    reviewTitles.has(title)
  ) {
    const path = reviewPath(item)
    return path
      ? addNotificationSource(path)
      : allowedLegacyPath(item, [/^\/review\/\d+(?:\?.*)?$/])
  }

  if (targetType === 'BOOKING' || type === 'BOOKING_STATUS' || bookingTitles.has(title)) {
    const bookingId = toPositiveId(item.targetId)
    const merchantPath = allowedLegacyPath(item, [
      /^\/merchant\/bookings\?(.+&)?bookingId=\d+(?:&.*)?$/
    ])
    if (merchantPath) return merchantPath
    return bookingId
      ? addNotificationSource(`/bookings?bookingId=${bookingId}`)
      : allowedLegacyPath(item, [
          /^\/bookings\?(.+&)?bookingId=\d+(?:&.*)?$/,
          /^\/merchant\/bookings\?(.+&)?bookingId=\d+(?:&.*)?$/
        ])
  }

  if (targetType === 'DEAL_ORDER' || type === 'DEAL_ORDER_STATUS' || dealOrderTitles.has(title)) {
    const dealOrderId = toPositiveId(item.orderId || item.targetId)
    return dealOrderId
      ? addNotificationSource(`/deal-order/${dealOrderId}`)
      : allowedLegacyPath(item, [/^\/deal-order\/\d+(?:\?.*)?$/])
  }

  return ''
}
