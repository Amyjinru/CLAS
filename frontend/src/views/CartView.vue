<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  cancelOrder, claimCoupon, clearInvalidCart, createOrderBatch, getCart, listAddresses,
  listClaimableCoupons, listMyDealOrders, listOrders, previewOrder
} from '../api/clas'
import { useCartActions } from '../composables/useCartActions'

const router = useRouter()
const { cartMessage, actionProductId, updateQuantity, removeItem } = useCartActions()
const items = ref([])
const selectedProductIds = ref(new Set())
const previews = ref({})
const previewErrors = ref({})
const previewRequests = new Map()
const selectedCoupons = ref({})
const previewLoadingIds = ref(new Set())
const pendingFoodOrders = ref([])
const pendingDealOrders = ref([])
const addresses = ref([])
const selectedAddressId = ref('')
const orderRemark = ref('')
const claimableCoupons = ref([])
const submitting = ref(false)
const message = cartMessage

const merchantGroups = computed(() => {
  const groups = new Map()
  for (const item of items.value) {
    const key = item.merchantId ?? `invalid-${item.id}`
    if (!groups.has(key)) {
      groups.set(key, {
        merchantId: item.merchantId,
        merchantName: item.merchantName || (item.merchantId ? `店铺 #${item.merchantId}` : '失效商品'),
        items: []
      })
    }
    groups.get(key).items.push(item)
  }
  return [...groups.values()]
})

const invalidCount = computed(() => items.value.filter((item) => item.valid === false).length)
const selectedGroups = computed(() => merchantGroups.value
  .map((group) => ({
    ...group,
    selectedItems: group.items.filter((item) => selectedProductIds.value.has(item.productId) && item.valid !== false)
  }))
  .filter((group) => group.merchantId && group.selectedItems.length))
const selectedItemCount = computed(() => selectedGroups.value.reduce((sum, group) => sum + group.selectedItems.length, 0))
const hasPendingPayments = computed(() => pendingFoodOrders.value.length > 0 || pendingDealOrders.value.length > 0)
const hasCartContent = computed(() => items.value.length > 0 || hasPendingPayments.value)
const aggregateTotal = computed(() => selectedGroups.value.reduce(
  (sum, group) => sum + (previews.value[group.merchantId]?.totalPrice || 0), 0
))
const canSubmit = computed(() => {
  if (!selectedAddressId.value || !selectedGroups.value.length || submitting.value) return false
  return selectedGroups.value.every((group) => (
    !previewLoadingIds.value.has(group.merchantId)
    && previews.value[group.merchantId]?.canCheckout
    && !previewErrors.value[group.merchantId]
  ))
})

function validItems(group) {
  return group.items.filter((item) => item.valid !== false)
}

function groupSelection(group) {
  const selectable = validItems(group)
  const selectedCount = selectable.filter((item) => selectedProductIds.value.has(item.productId)).length
  return {
    checked: selectable.length > 0 && selectedCount === selectable.length,
    indeterminate: selectedCount > 0 && selectedCount < selectable.length
  }
}

function replaceSelection(next) {
  selectedProductIds.value = new Set(next)
}

async function toggleItem(item, checked) {
  if (item.valid === false) return
  const next = new Set(selectedProductIds.value)
  if (checked) next.add(item.productId)
  else next.delete(item.productId)
  replaceSelection(next)
  await loadMerchantPreview(item.merchantId)
}

async function toggleMerchant(group, checked) {
  const next = new Set(selectedProductIds.value)
  for (const item of validItems(group)) {
    if (checked) next.add(item.productId)
    else next.delete(item.productId)
  }
  replaceSelection(next)
  await loadMerchantPreview(group.merchantId)
}

function selectedIdsForMerchant(merchantId) {
  return items.value
    .filter((item) => item.merchantId === merchantId && item.valid !== false && selectedProductIds.value.has(item.productId))
    .map((item) => item.productId)
}

function setPreviewLoading(merchantId, loading) {
  const next = new Set(previewLoadingIds.value)
  if (loading) next.add(merchantId)
  else next.delete(merchantId)
  previewLoadingIds.value = next
}

async function loadMerchantPreview(merchantId) {
  if (!merchantId) return
  const productIds = selectedIdsForMerchant(merchantId)
  if (!productIds.length) {
    const nextPreviews = { ...previews.value }
    const nextErrors = { ...previewErrors.value }
    const nextCoupons = { ...selectedCoupons.value }
    delete nextPreviews[merchantId]
    delete nextErrors[merchantId]
    delete nextCoupons[merchantId]
    previews.value = nextPreviews
    previewErrors.value = nextErrors
    selectedCoupons.value = nextCoupons
    return
  }

  const requestId = (previewRequests.get(merchantId) || 0) + 1
  previewRequests.set(merchantId, requestId)
  setPreviewLoading(merchantId, true)
  try {
    let couponId = selectedCoupons.value[merchantId] || undefined
    let preview = await previewOrder(merchantId, selectedAddressId.value || undefined, couponId, productIds)
    if (previewRequests.get(merchantId) !== requestId) return
    if (couponId && !(preview.availableCoupons || []).some((coupon) => String(coupon.id) === String(couponId))) {
      selectedCoupons.value = { ...selectedCoupons.value, [merchantId]: '' }
      couponId = undefined
      preview = await previewOrder(merchantId, selectedAddressId.value || undefined, couponId, productIds)
      if (previewRequests.get(merchantId) !== requestId) return
    }
    previews.value = { ...previews.value, [merchantId]: preview }
    const nextErrors = { ...previewErrors.value }
    delete nextErrors[merchantId]
    previewErrors.value = nextErrors
  } catch (error) {
    if (previewRequests.get(merchantId) !== requestId) return
    const nextPreviews = { ...previews.value }
    delete nextPreviews[merchantId]
    previews.value = nextPreviews
    previewErrors.value = { ...previewErrors.value, [merchantId]: error.response?.data?.message || '结算信息加载失败' }
  } finally {
    if (previewRequests.get(merchantId) === requestId) setPreviewLoading(merchantId, false)
  }
}

async function loadAllSelectedPreviews() {
  await Promise.all(selectedGroups.value.map((group) => loadMerchantPreview(group.merchantId)))
}

async function changeCoupon(merchantId, couponId) {
  selectedCoupons.value = { ...selectedCoupons.value, [merchantId]: couponId }
  await loadMerchantPreview(merchantId)
}

async function loadClaimableCoupons() {
  try { claimableCoupons.value = await listClaimableCoupons() } catch { claimableCoupons.value = [] }
}

async function handleClaimCoupon(couponId) {
  try {
    await claimCoupon(couponId)
    message.value = '优惠券领取成功'
    await loadClaimableCoupons()
    await loadAllSelectedPreviews()
  } catch (error) {
    message.value = error.response?.data?.message || '领取失败'
  }
}

async function load() {
  try {
    const [cartItems, orderList, dealList, addressList] = await Promise.all([
      getCart(), listOrders(), listMyDealOrders(), listAddresses()
    ])
    items.value = cartItems
    replaceSelection([])
    previews.value = {}
    selectedCoupons.value = {}
    pendingFoodOrders.value = orderList.filter((entry) => entry.order.status === 'PENDING_PAYMENT')
    pendingDealOrders.value = dealList.filter((entry) => entry.status === 'PENDING_PAYMENT')
    addresses.value = addressList
    selectedAddressId.value = addresses.value.find((item) => item.isDefault)?.id || addresses.value[0]?.id || ''
    message.value = ''
  } catch {
    message.value = '请先登录后查看购物车'
  }
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const data = await createOrderBatch({
      addressId: selectedAddressId.value,
      remark: orderRemark.value.trim() || undefined,
      merchantGroups: selectedGroups.value.map((group) => ({
        merchantId: group.merchantId,
        productIds: group.selectedItems.map((item) => item.productId),
        userCouponId: selectedCoupons.value[group.merchantId] || undefined
      }))
    })
    const orderIds = data.orders.map((entry) => entry.order.id)
    await router.push({ name: 'BatchPayment', query: { orderIds: orderIds.join(',') } })
  } catch (error) {
    message.value = error.response?.data?.message || '提交订单失败'
    await loadAllSelectedPreviews()
  } finally {
    submitting.value = false
  }
}

async function removeInvalidItems() {
  try {
    items.value = await clearInvalidCart()
    message.value = '失效商品已清理'
  } catch (error) { message.value = error.response?.data?.message || '清理失败' }
}

async function changeQuantity(item, quantity) {
  const nextQuantity = Math.min(Number(item.stock || 1), Math.max(1, Math.trunc(Number(quantity || 1))))
  const result = await updateQuantity(item.productId, nextQuantity)
  if (result) items.value = result
  else await load()
  await loadMerchantPreview(item.merchantId)
}

async function deleteItem(item) {
  const result = await removeItem(item.productId)
  if (!result) return
  items.value = result
  const next = new Set(selectedProductIds.value)
  next.delete(item.productId)
  replaceSelection(next)
  await loadMerchantPreview(item.merchantId)
}

async function cancelPendingFood(order) {
  await cancelOrder(order.order.id)
  message.value = `订单 ${order.order.id} 已取消`
  await load()
}

watch(selectedAddressId, loadAllSelectedPreviews)
onMounted(async () => { await loadClaimableCoupons(); await load() })
</script>

<template>
  <div class="user-page cart-page">
    <header class="cart-header">
      <h1>购物车</h1>
      <p>按店铺选择商品，可跨店合并结算</p>
    </header>
    <div class="user-page-split cart-layout">
      <main class="cart-main">
        <section v-if="hasPendingPayments" class="pending-section">
          <div class="section-title"><h2>待支付</h2><span>此前未完成支付的订单</span></div>
          <div class="pending-grid">
            <article v-for="order in pendingFoodOrders" :key="`food-${order.order.id}`" class="pending-card">
              <div><strong>外卖订单 #{{ order.order.id }}</strong><p>{{ order.items.length }} 件商品 · ¥{{ (order.order.totalPrice / 100).toFixed(2) }}</p></div>
              <div class="pending-actions"><RouterLink class="pay-btn" :to="`/payment/${order.order.id}`">去支付</RouterLink><button class="cancel-btn" type="button" @click="cancelPendingFood(order)">取消订单</button></div>
            </article>
            <article v-for="deal in pendingDealOrders" :key="`deal-${deal.id}`" class="pending-card">
              <div><strong>团购券订单 #{{ deal.id }}</strong><p>团购商品 #{{ deal.dealId }} · ¥{{ (deal.payAmount / 100).toFixed(2) }}</p></div>
              <div class="pending-actions"><RouterLink class="pay-btn" :to="`/payment/deal/${deal.id}`">去支付</RouterLink></div>
            </article>
          </div>
        </section>

        <section v-if="items.length" class="cart-items">
          <div class="section-title compact"><h2>购物车商品</h2><span>已选择 {{ selectedItemCount }} 件</span></div>
          <article v-for="group in merchantGroups" :key="group.merchantId || group.merchantName" class="merchant-group">
            <header class="merchant-header">
              <el-checkbox :model-value="groupSelection(group).checked" :indeterminate="groupSelection(group).indeterminate" :disabled="!validItems(group).length" @change="toggleMerchant(group, $event)"><strong>{{ group.merchantName }}</strong></el-checkbox>
              <span>{{ validItems(group).length }} 件可结算</span>
            </header>
            <div v-for="item in group.items" :key="item.productId" class="cart-item" :class="{ invalid: item.valid === false, selected: selectedProductIds.has(item.productId) }">
              <el-checkbox :model-value="selectedProductIds.has(item.productId)" :disabled="item.valid === false" :aria-label="`选择 ${item.productName}`" @change="toggleItem(item, $event)" />
              <div class="item-main">
                <div class="item-info"><h3>{{ item.productName }}</h3><p>库存 {{ item.stock }} · 单价 ¥{{ (item.price / 100).toFixed(2) }}</p><p v-if="item.valid === false" class="invalid-tip">{{ item.invalidReason || '商品不可购买' }}</p></div>
                <strong class="item-price">¥{{ (item.subtotal / 100).toFixed(2) }}</strong>
              </div>
              <div class="cart-actions">
                <label class="quantity-field">数量<input type="number" min="1" step="1" :max="item.stock" :value="item.quantity" :disabled="actionProductId === item.productId || item.valid === false" @change="changeQuantity(item, $event.target.value)" /></label>
                <button class="delete-btn" :disabled="actionProductId === item.productId" @click="deleteItem(item)">删除</button>
              </div>
            </div>
          </article>
          <div v-if="invalidCount" class="invalid-actions"><p>有 {{ invalidCount }} 件商品已失效或库存不足</p><button class="secondary" type="button" @click="removeInvalidItems">清理失效商品</button></div>
        </section>
        <div v-if="!hasCartContent" class="cart-empty">购物车暂无商品，也没有待支付订单</div>
        <p v-if="message" class="cart-message">{{ message }}</p>
      </main>

      <aside v-if="items.length" class="cart-sidebar">
        <footer class="checkout-panel">
          <label class="field-block"><span>配送地址</span><select v-model="selectedAddressId"><option value="">请选择配送地址</option><option v-for="address in addresses" :key="address.id" :value="address.id">{{ address.contactName }} · {{ address.address }}</option></select></label>
          <label class="field-block"><span>订单备注</span><textarea v-model="orderRemark" rows="2" placeholder="所有店铺共用（可选）" /></label>
          <section v-for="group in selectedGroups" :key="`summary-${group.merchantId}`" class="merchant-summary">
            <h3>{{ group.merchantName }}</h3>
            <label class="field-block compact-field"><span>优惠券</span><select :value="selectedCoupons[group.merchantId] || ''" @change="changeCoupon(group.merchantId, $event.target.value)"><option value="">不使用优惠券</option><option v-for="coupon in previews[group.merchantId]?.availableCoupons || []" :key="coupon.id" :value="coupon.id">{{ coupon.title }} · 减 ¥{{ (coupon.discountAmount / 100).toFixed(2) }}</option></select></label>
            <p v-if="previewLoadingIds.has(group.merchantId)" class="checkout-tip">正在计算...</p>
            <p v-else-if="previewErrors[group.merchantId]" class="checkout-tip">{{ previewErrors[group.merchantId] }}</p>
            <template v-else-if="previews[group.merchantId]">
              <div class="breakdown-row"><span>商品小计</span><span>¥{{ (previews[group.merchantId].subtotal / 100).toFixed(2) }}</span></div>
              <div class="breakdown-row"><span>配送费</span><span>¥{{ (previews[group.merchantId].deliveryFee / 100).toFixed(2) }}</span></div>
              <div v-if="previews[group.merchantId].couponDiscount" class="breakdown-row discount"><span>优惠</span><span>-¥{{ (previews[group.merchantId].couponDiscount / 100).toFixed(2) }}</span></div>
              <p v-if="!previews[group.merchantId].canCheckout" class="checkout-tip">{{ previews[group.merchantId].message }}</p>
            </template>
          </section>
          <div v-if="claimableCoupons.length" class="claimable-coupons"><p>可领取优惠券</p><div v-for="coupon in claimableCoupons" :key="coupon.id"><span>{{ coupon.title }}</span><button type="button" class="secondary compact" @click="handleClaimCoupon(coupon.id)">领取</button></div></div>
          <div class="checkout-total"><span>已选 {{ selectedItemCount }} 件 · 应付合计</span><strong>¥{{ (aggregateTotal / 100).toFixed(2) }}</strong></div>
          <button class="submit-btn" :disabled="!canSubmit" @click="submit">{{ submitting ? '正在提交...' : '提交订单并付款' }}</button>
        </footer>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.cart-page{width:100%}.cart-header h1{margin:0;font-size:26px}.cart-header p,.section-title span,.merchant-header span{color:var(--text-secondary);font-size:13px}.cart-main{min-width:0}.section-title{align-items:baseline;display:flex;gap:10px;margin:24px 0 12px}.section-title.compact{margin-top:8px}.section-title h2{font-size:18px;margin:0}.pending-grid{display:grid;gap:12px;grid-template-columns:repeat(2,minmax(0,1fr))}.pending-card{background:var(--color-primary-light);border:1px solid var(--color-primary-soft);border-radius:var(--radius-lg);padding:16px 18px}.pending-card p{color:var(--text-secondary);font-size:13px;margin:6px 0 0}.pending-actions{display:flex;gap:10px;justify-content:flex-end;margin-top:12px}.pay-btn,.cancel-btn{border-radius:var(--radius-sm);font-size:13px;padding:8px 14px;text-decoration:none}.pay-btn{background:var(--color-primary);color:var(--text-primary);font-weight:600}.cancel-btn{background:transparent;border:1px solid var(--border-color);color:var(--text-secondary);cursor:pointer}.cart-items{display:flex;flex-direction:column;gap:14px;margin-top:24px}.merchant-group{background:var(--bg-card);border:1px solid var(--border-color);border-radius:var(--radius-lg);overflow:hidden}.merchant-header{align-items:center;background:var(--color-primary-light);display:flex;justify-content:space-between;padding:14px 18px}.cart-item{align-items:center;border-top:1px solid var(--border-color);display:grid;gap:16px;grid-template-columns:auto minmax(0,1fr) auto;padding:18px}.cart-item.selected{background:var(--bg-page)}.cart-item.invalid{opacity:.68}.item-main{align-items:center;display:flex;justify-content:space-between;min-width:0}.item-info h3{font-size:16px;margin:0 0 5px}.item-info p{color:var(--text-muted);font-size:13px;margin:0}.item-info .invalid-tip,.checkout-tip{color:var(--clas-warning)}.item-price{color:var(--clas-amber-600);font-size:18px;margin-left:18px;white-space:nowrap}.cart-actions{align-items:center;display:flex;gap:10px}.quantity-field{align-items:center;color:var(--text-secondary);display:flex;font-size:13px;gap:6px}.quantity-field input{background:var(--bg-page);border:1px solid var(--border-color);border-radius:var(--radius-sm);color:var(--text-primary);padding:6px;text-align:center;width:62px}.delete-btn{background:transparent;border:1px solid var(--border-color);border-radius:var(--radius-sm);color:var(--clas-error);cursor:pointer;padding:7px 12px}.delete-btn:disabled,.submit-btn:disabled{cursor:not-allowed;opacity:.55}.invalid-actions{align-items:center;display:flex;justify-content:space-between}.invalid-actions p{color:var(--clas-warning);margin:0}.cart-empty{color:var(--text-muted);padding:48px 0;text-align:center}.cart-message{color:var(--clas-warning)}.cart-sidebar{min-width:0}.checkout-panel{background:var(--bg-card);border:1px solid var(--border-color);border-radius:var(--radius-lg);box-shadow:var(--shadow-md);display:flex;flex-direction:column;gap:14px;padding:20px;position:sticky;top:88px}.field-block{display:flex;flex-direction:column;gap:7px}.field-block>span{color:var(--text-secondary);font-size:13px;font-weight:600}.field-block select,.field-block textarea{background:var(--bg-card);border:1px solid var(--border-color);border-radius:var(--radius-sm);color:var(--text-primary);font:inherit;padding:9px 10px;width:100%}.field-block textarea{resize:vertical}.merchant-summary{border-top:1px solid var(--border-color);padding-top:13px}.merchant-summary h3{font-size:14px;margin:0 0 10px}.compact-field{margin-bottom:10px}.breakdown-row{color:var(--text-secondary);display:flex;font-size:13px;justify-content:space-between;margin-top:6px}.breakdown-row.discount{color:var(--clas-success)}.checkout-tip{font-size:13px;margin:8px 0 0}.claimable-coupons{background:var(--clas-warning-light);border-radius:var(--radius-sm);padding:10px 12px}.claimable-coupons p{color:var(--text-secondary);font-size:13px;margin:0 0 8px}.claimable-coupons div{align-items:center;display:flex;font-size:13px;justify-content:space-between}.claimable-coupons div+div{margin-top:6px}button.compact{font-size:12px;padding:4px 10px}.checkout-total{align-items:flex-end;border-top:1px solid var(--border-color);display:flex;flex-direction:column;gap:4px;padding-top:14px}.checkout-total span{color:var(--text-secondary);font-size:13px}.checkout-total strong{color:var(--clas-amber-600);font-size:26px}.submit-btn{background:var(--color-primary);border:0;border-radius:var(--radius-sm);color:var(--text-primary);cursor:pointer;font-size:15px;font-weight:700;min-height:44px}
@media(max-width:900px){.pending-grid{grid-template-columns:1fr}.cart-item{grid-template-columns:auto minmax(0,1fr)}.cart-actions{grid-column:2;justify-content:flex-end}}
@media(max-width:640px){.cart-item{align-items:flex-start}.item-main{align-items:flex-start;flex-direction:column;gap:8px}.item-price{margin-left:0}.cart-actions{flex-wrap:wrap;justify-content:flex-start}}
</style>
