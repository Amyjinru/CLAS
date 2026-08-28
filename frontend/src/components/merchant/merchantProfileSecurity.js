export function trimValue(value) {
  return String(value ?? '').trim()
}

export function shouldResetVerification(currentValue, sentValue) {
  return trimValue(currentValue) !== trimValue(sentValue)
}

export function isVerificationReady({ changed, sent, sentValue, currentValue, code }) {
  if (!changed) return true
  return Boolean(sent && trimValue(code) && trimValue(sentValue) === trimValue(currentValue))
}

export function isBankAccountReadyForSave(bankAccount, bankChanged) {
  if (!bankChanged) return true
  return /^\d{9,25}$/.test(trimValue(bankAccount))
}

function normalizeInteger(value, min = 0, max = Number.MAX_SAFE_INTEGER) {
  return Math.min(max, Math.max(min, Math.trunc(Number(value || 0))))
}

export function buildMerchantProfilePayload(form, { phoneChanged, bankChanged }) {
  const payload = {
    merchantName: trimValue(form.merchantName),
    phone: trimValue(form.phone),
    bankAccount: trimValue(form.bankAccount),
    address: trimValue(form.address),
    longitude: form.longitude,
    latitude: form.latitude,
    deliveryRadiusM: normalizeInteger(form.deliveryRadiusM, 500, 10000),
    businessHours: trimValue(form.businessHours)
  }

  if (phoneChanged) {
    payload.phoneCode = trimValue(form.phoneCode)
  }
  if (bankChanged) {
    payload.bankCode = trimValue(form.bankCode)
  }

  return payload
}
