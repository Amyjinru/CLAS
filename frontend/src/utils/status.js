export const orderStatusMap = {
  PENDING_PAYMENT: { text: '待支付', type: 'warning' },
  PAID: { text: '待接单', type: 'primary' },
  ACCEPTED: { text: '已接单', type: 'info' },
  COMPLETED: { text: '已完成', type: 'success' },
  REFUND_REQUESTED: { text: '退款中', type: 'warning' },
  REFUNDED: { text: '已退款', type: 'danger' },
  CANCELED: { text: '已取消', type: 'info' }
}

export const merchantStatusMap = {
  OPEN: { text: '营业中', type: 'success' },
  CLOSED: { text: '休息中', type: 'info' },
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已拒绝', type: 'danger' }
}

export const productStatusMap = {
  ON_SALE: { text: '上架中', type: 'success' },
  OFF_SALE: { text: '下架中', type: 'info' }
}

export const bookingStatusMap = {
  PENDING: { text: '待确认', type: 'warning' },
  CONFIRMED: { text: '已确认', type: 'success' },
  CANCELED: { text: '已取消', type: 'info' }
}

export const appealStatusMap = {
  PENDING: { text: '待处理', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已拒绝', type: 'danger' }
}

export function statusMeta(status, map = orderStatusMap) {
  return map[status] || { text: status || '-', type: 'info' }
}
