import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { notificationTarget } from './notificationTarget.js'

describe('notificationTarget', () => {
  it('opens an order notification on the single order detail page', () => {
    assert.equal(
      notificationTarget({
        type: 'ORDER_STATUS',
        targetType: 'ORDER',
        targetId: 3,
        orderId: 8,
        targetPath: '/merchant/messages?orderId=8'
      }),
      '/order/8?from=notifications'
    )
  })

  it('opens known order titles even when only a legacy order list path exists', () => {
    assert.equal(
      notificationTarget({
        title: '退款被拒绝',
        targetPath: '/orders?orderId=18'
      }),
      '/order/18?from=notifications'
    )
  })

  it('uses a whitelisted legacy path only when structured ids are missing', () => {
    assert.equal(
      notificationTarget({
        type: 'ORDER_STATUS',
        targetType: 'ORDER',
        targetPath: '/order/6'
      }),
      '/order/6?from=notifications'
    )
    assert.equal(
      notificationTarget({
        type: 'ORDER_STATUS',
        targetType: 'ORDER',
        targetPath: '/merchant/messages?orderId=6'
      }),
      ''
    )
  })

  it('opens review and reply notifications on the review page', () => {
    assert.equal(
      notificationTarget({
        type: 'REVIEW_REPLY',
        targetType: 'REPLY',
        targetId: 99,
        reviewId: 10,
        replyId: 99,
        orderId: 7
      }),
      '/review/7?reviewId=10&replyId=99&from=notifications'
    )
  })

  it('opens merchant review reply notifications by type', () => {
    assert.equal(
      notificationTarget({
        type: 'MERCHANT_REVIEW_REPLY',
        title: '商家回复了评价',
        orderId: 11,
        reviewId: 22,
        replyId: 33
      }),
      '/review/11?reviewId=22&replyId=33&from=notifications'
    )
  })

  it('opens booking notifications with blank type by title and safe target path', () => {
    assert.equal(
      notificationTarget({ title: '预约已提交', targetPath: '/bookings?bookingId=12' }),
      '/bookings?bookingId=12&from=notifications'
    )
    assert.equal(
      notificationTarget({ title: '新的预约申请', targetPath: '/merchant/bookings?bookingId=13' }),
      '/merchant/bookings?bookingId=13&from=notifications'
    )
  })

  it('keeps merchant booking notifications on the merchant booking page', () => {
    assert.equal(
      notificationTarget({
        type: 'BOOKING_STATUS',
        targetType: 'BOOKING',
        targetId: 13,
        targetPath: '/merchant/bookings?bookingId=13'
      }),
      '/merchant/bookings?bookingId=13&from=notifications'
    )
  })

  it('opens deal coupon notifications with blank type by title and safe target path', () => {
    assert.equal(
      notificationTarget({ title: '团购券购买成功', targetPath: '/deal-order/5' }),
      '/deal-order/5?from=notifications'
    )
  })

  it('does not open unrelated notifications even when a path is present', () => {
    assert.equal(
      notificationTarget({ title: '平台公告', targetPath: '/orders' }),
      ''
    )
  })
})
