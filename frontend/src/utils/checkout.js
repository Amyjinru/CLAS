export function preferredProductIds(items, merchantId) {
  const normalizedMerchantId = Number(merchantId)
  if (!Number.isInteger(normalizedMerchantId) || normalizedMerchantId <= 0) return []
  return items
    .filter((item) => item.valid !== false && item.merchantId === normalizedMerchantId)
    .map((item) => item.productId)
}

export function isCheckoutReady({ groups, submitting, loadingIds, previews, errors }) {
  if (!groups.length || submitting) return false
  return groups.every((group) => (
    !loadingIds.has(group.merchantId)
    && previews[group.merchantId]?.canCheckout
    && !errors[group.merchantId]
  ))
}
