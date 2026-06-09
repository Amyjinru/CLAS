<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  cancelOrder,
  claimCoupon,
  clearInvalidCart,
  createOrder,
  deleteCartItem,
  getCart,
  listAddresses,
  listClaimableCoupons,
  listMyDealOrders,
  listOrders,
  previewOrder,
  updateCart
} from '../api/clas'

const router = useRouter()
const items = ref([])
const pendingFoodOrders = ref([])
const pendingDealOrders = ref([])
const message = ref('')
const updatingProductId = ref(null)
const addresses = ref([])
const selectedAddressId = ref('')
const orderRemark = ref('')
const selectedUserCouponId = ref('')
const claimableCoupons = ref([])
const checkoutPreview = ref(null)

const merchantIds = () => [...new Set(items.value.map((item) => item.merchantId).filter(Boolean))]
const activeMerchantId = computed(() => merchantIds()[0] || null)
const invalidCount = computed(() => items.value.filter((item) => item.valid === false).length)
const hasPendingPayments = computed(
  () => pendingFoodOrders.value.length > 0 || pendingDealOrders.value.length > 0
)
const hasCartContent = computed(() => items.value.length > 0 || hasPendingPayments.value)
const canSubmit = computed(() => checkoutPreview.value?.canCheckout && !invalidCount.value)

async function loadPreview() {
  if (!activeMerchantId.value) {
    checkoutPreview.value = null
    return
  }
  try {
    checkoutPreview.value = await previewOrder(
      activeMerchantId.value,
      selectedAddressId.value || undefined,
      selectedUserCouponId.value || undefined
    )
  } catch {
    checkoutPreview.value = null
  }
}

async function loadClaimableCoupons() {
  try {
    claimableCoupons.value = await listClaimableCoupons()
  } catch {
    claimableCoupons.value = []
  }
}

async function handleClaimCoupon(couponId) {
  try {
    await claimCoupon(couponId)
    message.value = '优惠券领取成功'
    await loadClaimableCoupons()
    await loadPreview()
  } catch (error) {
    message.value = error.response?.data?.message || '领取失败'
  }
}

async function load() {
  try {
    const [cartItems, orderList, dealList, addressList] = await Promise.all([
      getCart(),
      listOrders(),
      listMyDealOrders(),
      listAddresses()
    ])
    items.value = cartItems
    pendingFoodOrders.value = orderList.filter((entry) => entry.order.status === 'PENDING_PAYMENT')
    pendingDealOrders.value = dealList.filter((entry) => entry.status === 'PENDING_PAYMENT')
    addresses.value = addressList
    selectedAddressId.value = addresses.value.find((item) => item.isDefault)?.id || addresses.value[0]?.id || ''
    message.value = ''
    await loadPreview()
  } catch {
    message.value = '请先登录后查看购物车'
  }
}

async function submit() {
  if (!items.value.length || !activeMerchantId.value) return
  if (!selectedAddressId.value) {
    message.value = '请先在个人中心添加并选择配送地址'
    return
  }
  if (invalidCount.value) {
    message.value = '请先清理失效商品'
    return
  }
  if (!checkoutPreview.value?.canCheckout) {
    message.value = checkoutPreview.value?.message || '当前订单不满足下单条件'
    return
  }
  try {
    const data = await createOrder({
      merchantId: activeMerchantId.value,
      addressId: selectedAddressId.value,
      remark: orderRemark.value.trim() || undefined,
      userCouponId: selectedUserCouponId.value || undefined
    })
    message.value = `订单 ${data.order.id} 已创建，请完成支付`
    await router.push(`/payment/${data.order.id}`)
  } catch (error) {
    message.value = error.response?.data?.message || '提交订单失败'
  }
}

async function removeInvalidItems() {
  try {
    items.value = await clearInvalidCart()
    message.value = '失效商品已清理'
    await loadPreview()
  } catch (error) {
    message.value = error.response?.data?.message || '清理失败'
  }
}

async function changeQuantity(item, quantity) {
  const nextQuantity = Number(quantity)
  if (!Number.isInteger(nextQuantity) || nextQuantity < 1) {
    message.value = '数量至少为 1'
    await load()
    return
  }
  updatingProductId.value = item.productId
  try {
    items.value = await updateCart({ productId: item.productId, quantity: nextQuantity })
    message.value = '购物车数量已更新'
    await loadPreview()
  } catch (error) {
    message.value = error.response?.data?.message || '更新数量失败'
    await load()
  } finally {
    updatingProductId.value = null
  }
}

async function deleteItem(item) {
  updatingProductId.value = item.productId
  try {
    items.value = await deleteCartItem(item.productId)
    message.value = '商品已从购物车删除'
    await loadPreview()
  } catch (error) {
    message.value = error.response?.data?.message || '删除商品失败'
  } finally {
    updatingProductId.value = null
  }
}

async function cancelPendingFood(order) {
  await cancelOrder(order.order.id)
  message.value = `订单 ${order.order.id} 已取消`
  await load()
}

onMounted(async () => {
  await loadClaimableCoupons()
  await load()
})

watch([selectedAddressId, activeMerchantId, selectedUserCouponId], loadPreview)
</script>

<template>
  <div class="user-page cart-page">
    <div class="cart-header">
      <h1>购物车</h1>
      <p v-if="merchantIds().length > 1" class="multi-merchant-warn">
        购物车包含多个商家商品，本次将提交第一个商家的商品
      </p>
    </div>

    <div class="user-page-split cart-layout">
      <div class="cart-main">
        <section v-if="hasPendingPayments" class="pending-section">
          <div class="section-title">
            <h2>待支付</h2>
            <span>下单或购买后未完成的订单会集中显示在这里</span>
          </div>

          <div class="pending-grid">
            <article v-for="order in pendingFoodOrders" :key="`food-${order.order.id}`" class="pending-card">
              <div class="pending-main">
                <div>
                  <strong>外卖订单 #{{ order.order.id }}</strong>
                  <p>{{ order.items.length }} 件商品 · ¥{{ (order.order.totalPrice / 100).toFixed(2) }}</p>
                </div>
                <span class="pending-tag">待支付</span>
              </div>
              <div class="pending-actions">
                <RouterLink class="pay-btn" :to="`/payment/${order.order.id}`">去支付</RouterLink>
                <button class="cancel-btn" type="button" @click="cancelPendingFood(order)">取消订单</button>
              </div>
            </article>

            <article v-for="deal in pendingDealOrders" :key="`deal-${deal.id}`" class="pending-card">
              <div class="pending-main">
                <div>
                  <strong>团购券订单 #{{ deal.id }}</strong>
                  <p>团购商品 #{{ deal.dealId }} · ¥{{ (deal.payAmount / 100).toFixed(2) }}</p>
                </div>
                <span class="pending-tag deal">团购待支付</span>
              </div>
              <div class="pending-actions">
                <RouterLink class="pay-btn" :to="`/payment/deal/${deal.id}`">去支付</RouterLink>
              </div>
            </article>
          </div>
        </section>

        <div class="cart-items" v-if="items.length">
          <div class="section-title compact">
            <h2>购物车商品</h2>
          </div>
          <div class="cart-item" :class="{ invalid: item.valid === false }" v-for="item in items" :key="item.productId">
            <div class="item-main">
              <div class="item-info">
                <h2 class="item-name">{{ item.productName }}</h2>
                <p class="item-meta">库存 {{ item.stock }} · 单价 ¥{{ (item.price / 100).toFixed(2) }}</p>
                <p v-if="item.valid === false" class="invalid-tip">{{ item.invalidReason || '商品不可购买' }}</p>
              </div>
              <div class="item-price">¥{{ (item.subtotal / 100).toFixed(2) }}</div>
            </div>
            <div class="cart-actions">
              <label class="quantity-field">
                数量
                <input
                  type="number"
                  min="1"
                  :max="item.stock"
                  :value="item.quantity"
                  :disabled="updatingProductId === item.productId"
                  @change="changeQuantity(item, $event.target.value)"
                />
              </label>
              <button
                class="delete-btn"
                :disabled="updatingProductId === item.productId"
                @click="deleteItem(item)"
              >
                删除
              </button>
            </div>
          </div>
          <div v-if="invalidCount" class="invalid-actions">
            <p>有 {{ invalidCount }} 件商品已失效或库存不足</p>
            <button class="secondary" type="button" @click="removeInvalidItems">清理失效商品</button>
          </div>
        </div>

        <div class="cart-empty" v-if="!hasCartContent">
          <p>购物车暂无商品，也没有待支付订单</p>
        </div>

        <p class="cart-message" v-if="message">{{ message }}</p>
      </div>

      <aside v-if="items.length" class="cart-sidebar">
        <footer class="checkout-bar checkout-bar-side">
          <label class="address-select address-select-block">
            <span class="total-label">配送地址</span>
            <select v-model="selectedAddressId">
              <option value="">暂不选择</option>
              <option v-for="item in addresses" :key="item.id" :value="item.id">
                {{ item.contactName }} · {{ item.address }}
              </option>
            </select>
          </label>
          <label class="address-select address-select-block">
            <span class="total-label">订单备注</span>
            <textarea v-model="orderRemark" rows="2" placeholder="口味、送达要求等（可选）" />
          </label>
          <label class="address-select address-select-block">
            <span class="total-label">优惠券</span>
            <select v-model="selectedUserCouponId">
              <option value="">不使用优惠券</option>
              <option
                v-for="coupon in checkoutPreview?.availableCoupons || []"
                :key="coupon.id"
                :value="coupon.id"
              >
                {{ coupon.title }} · 减 ¥{{ (coupon.discountAmount / 100).toFixed(2) }}
              </option>
            </select>
          </label>
          <div v-if="claimableCoupons.length" class="claimable-coupons">
            <p class="claimable-title">可领取优惠券</p>
            <div v-for="coupon in claimableCoupons" :key="coupon.id" class="claimable-item">
              <span>{{ coupon.title }}</span>
              <button type="button" class="secondary compact" @click="handleClaimCoupon(coupon.id)">领取</button>
            </div>
          </div>
          <div v-if="checkoutPreview" class="checkout-breakdown">
            <div class="breakdown-row">
              <span>商品小计</span>
              <span>¥{{ (checkoutPreview.subtotal / 100).toFixed(2) }}</span>
            </div>
            <div v-if="checkoutPreview.deliveryFee > 0 || checkoutPreview.distanceMeters" class="breakdown-row">
              <span>
                配送费
                <small v-if="checkoutPreview.distanceMeters">
                  · {{ checkoutPreview.distanceMeters < 1000 ? `${checkoutPreview.distanceMeters}m` : `${(checkoutPreview.distanceMeters / 1000).toFixed(1)}km` }}
                </small>
              </span>
              <span>¥{{ (checkoutPreview.deliveryFee / 100).toFixed(2) }}</span>
            </div>
            <div v-if="checkoutPreview.couponDiscount > 0" class="breakdown-row discount">
              <span>优惠券抵扣</span>
              <span>-¥{{ (checkoutPreview.couponDiscount / 100).toFixed(2) }}</span>
            </div>
            <div v-if="checkoutPreview.minOrderGap > 0" class="breakdown-row warn">
              <span>距起送价还差</span>
              <span>¥{{ (checkoutPreview.minOrderGap / 100).toFixed(2) }}</span>
            </div>
          </div>
          <div class="checkout-total">
            <span class="total-label">应付合计</span>
            <span class="total-price">¥{{ ((checkoutPreview?.totalPrice || 0) / 100).toFixed(2) }}</span>
          </div>
          <p v-if="checkoutPreview && !checkoutPreview.canCheckout" class="checkout-tip">{{ checkoutPreview.message }}</p>
          <button class="submit-btn" :disabled="!canSubmit" @click="submit">提交订单</button>
        </footer>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.cart-page {
  width: 100%;
}

.cart-header h1 {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 8px 0;
  letter-spacing: 0.04em;
  padding-left: 16px;
  position: relative;
}
.cart-header h1::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 4px;
  background: linear-gradient(180deg, var(--color-primary), var(--clas-amber-300));
  border-radius: var(--radius-full);
}

.multi-merchant-warn {
  color: var(--clas-warning);
  font-size: 13px;
  background: var(--clas-warning-light);
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  margin: 12px 0 0 16px;
}

.section-title {
  margin: 24px 0 12px;
}
.section-title.compact {
  margin-top: 8px;
}
.section-title h2 {
  font-size: 18px;
  margin: 0 0 4px;
}
.section-title span {
  color: var(--text-secondary);
  font-size: 13px;
}

.pending-section {
  margin-top: 0;
}

.pending-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.pending-card {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: var(--radius-md);
  margin-bottom: 10px;
  padding: 16px 18px;
}

.pending-main {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.pending-main p {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 6px 0 0;
}

.pending-tag {
  background: #fef3c7;
  border-radius: 999px;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  white-space: nowrap;
}
.pending-tag.deal {
  background: #dbeafe;
  color: #1d4ed8;
}

.pending-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 12px;
}

.pay-btn {
  background: var(--color-primary);
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 16px;
  text-decoration: none;
}

.cancel-btn {
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  padding: 8px 14px;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 24px;
}

.cart-item {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 18px 22px;
  transition: all var(--transition-fast);
}
.cart-item:hover {
  border-color: var(--clas-amber-200);
  box-shadow: var(--shadow-sm);
}

.item-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.item-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}
.item-meta {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0;
}

.item-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--clas-amber-600);
  white-space: nowrap;
  margin-left: 24px;
}

.cart-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.quantity-field {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.quantity-field input {
  width: 72px;
  height: 32px;
  padding: 4px 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 14px;
  text-align: center;
  background: var(--bg-page);
  color: var(--text-primary);
  transition: border-color var(--transition-fast);
}
.quantity-field input:focus {
  border-color: var(--color-primary);
  outline: none;
}
.quantity-field input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-btn {
  height: 32px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--clas-error, #ef4444);
  border: 1px solid var(--clas-error-light, #fecaca);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.delete-btn:hover:not(:disabled) {
  background: var(--clas-error-light, #fef2f2);
  border-color: var(--clas-error, #ef4444);
}
.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cart-empty {
  text-align: center;
  padding: 48px 0;
  color: var(--text-muted);
  font-size: 15px;
}

.checkout-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  box-shadow: var(--shadow-md);
}

.checkout-bar-side textarea {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font: inherit;
  min-height: 64px;
  padding: 8px 10px;
  resize: vertical;
  width: 100%;
}

.checkout-breakdown {
  display: grid;
  gap: 8px;
  margin: 12px 0;
}

.breakdown-row {
  color: var(--text-secondary);
  display: flex;
  font-size: 13px;
  justify-content: space-between;
}

.breakdown-row.warn {
  color: var(--clas-warning);
}

.checkout-tip {
  color: var(--clas-warning);
  font-size: 13px;
  margin: 0 0 10px;
}

.cart-item.invalid {
  background: #fff7ed;
  border-color: #fdba74;
}

.invalid-tip {
  color: var(--clas-warning);
  font-size: 13px;
  margin: 4px 0 0;
}

.invalid-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  margin-top: 12px;
}

.invalid-actions p {
  color: var(--clas-warning);
  margin: 0;
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.breakdown-row.discount {
  color: var(--clas-success);
}

.breakdown-row small {
  color: var(--text-secondary);
  font-size: 12px;
}

.claimable-coupons {
  margin: 8px 0 12px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--clas-warning-light);
}

.claimable-title {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.claimable-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}

.claimable-item + .claimable-item {
  margin-top: 6px;
}

button.compact {
  padding: 4px 10px;
  font-size: 12px;
}

.checkout-bar-side {
  align-items: stretch;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 88px;
}

.address-select-block {
  align-items: stretch;
  flex-direction: column;
}

.address-select-block select {
  max-width: none;
  width: 100%;
}

.cart-sidebar {
  min-width: 0;
}

.checkout-total {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.address-select {
  align-items: center;
  display: flex;
  gap: 10px;
}
.address-select select {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  height: 38px;
  max-width: 240px;
  padding: 0 10px;
}
.total-label {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
}
.total-price {
  font-size: 26px;
  font-weight: 800;
  color: var(--clas-amber-600);
  letter-spacing: -0.02em;
}

.submit-btn {
  height: 44px;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.04em;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
  width: 100%;
}
.submit-btn:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}
.submit-btn:active {
  transform: scale(0.97);
}

.cart-message {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--clas-success);
  font-weight: 500;
  padding: 10px 16px;
  background: var(--clas-success-light);
  border-radius: var(--radius-sm);
}

@media (max-width: 768px) {
  .pending-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .cart-item {
    padding: 14px 16px;
  }
  .item-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .item-price {
    margin-left: 0;
    align-self: flex-end;
  }
  .cart-actions {
    justify-content: stretch;
  }
  .quantity-field {
    flex: 1;
  }
  .quantity-field input {
    flex: 1;
  }
  .checkout-bar {
    flex-direction: column;
    gap: 14px;
    padding: 16px 18px;
  }
  .total-price {
    font-size: 22px;
  }
  .submit-btn {
    width: 100%;
  }
}
</style>
