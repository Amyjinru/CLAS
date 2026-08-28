export function preferredProductIds(items, merchantId) {
  const normalizedMerchantId = Number(merchantId)
  if (!Number.isInteger(normalizedMerchantId) || normalizedMerchantId <= 0) return []
  return items
    .filter((item) => item.valid !== false && item.merchantId === normalizedMerchantId)
    .map((item) => item.productId)
}

export function isCheckoutReady({ groups, submitting, loadingIds, previews, errors, deliveryAddress, contactName, contactPhone, deliveryLongitude, deliveryLatitude }) {
  if (!groups.length || submitting) return false
  if (![deliveryAddress, contactName, contactPhone].every((value) => String(value || '').trim())) return false
  if (![deliveryLongitude, deliveryLatitude].every((value) => (
    value !== null && value !== undefined && value !== '' && Number.isFinite(Number(value))
  ))) return false
  return groups.every((group) => (
    !loadingIds.has(group.merchantId)
    && previews[group.merchantId]?.canCheckout
    && !errors[group.merchantId]
  ))
}
