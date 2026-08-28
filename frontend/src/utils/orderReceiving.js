const excludedOrderStatuses = new Set(['REFUNDED', 'REFUND_PENDING', 'CANCELED', 'REJECTED'])

export function isReceivingOrder(order) {
  if (!order || excludedOrderStatuses.has(order.status)) return false
  if (order.refundStatus && order.refundStatus !== 'NONE') return false
  return ['PAID', 'ACCEPTED'].includes(order.status) && order.deliveryStatus !== 'DELIVERED'
}

export function isReceivingDealOrder(order) {
  return order?.status === 'UNUSED'
}
