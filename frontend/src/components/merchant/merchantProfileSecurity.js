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

export function buildMerchantProfilePayload(form, { phoneChanged, bankChanged }) {
  const payload = {
    merchantName: trimValue(form.merchantName),
    phone: trimValue(form.phone),
    bankAccount: trimValue(form.bankAccount),
    address: trimValue(form.address),
    longitude: form.longitude,
    latitude: form.latitude,
    deliveryRadiusM: form.deliveryRadiusM,
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
