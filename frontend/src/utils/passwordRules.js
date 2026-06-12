export const passwordRuleMessage = '至少6位，包含大小写字母、数字和特殊符号，不能包含空白字符'

export function passwordChecks(password) {
  const value = password || ''
  return [
    { key: 'length', label: '不少于6位', ok: value.length >= 6 },
    { key: 'lowercase', label: '包含小写字母', ok: /[a-z]/.test(value) },
    { key: 'uppercase', label: '包含大写字母', ok: /[A-Z]/.test(value) },
    { key: 'digit', label: '包含数字', ok: /\d/.test(value) },
    { key: 'symbol', label: '包含特殊符号', ok: /[\W_]/.test(value) },
    { key: 'no-space', label: '不能包含空白字符', ok: !/\s/.test(value) }
  ]
}

export function passwordStrength(password) {
  const value = password || ''
  let score = 0
  if (value.length >= 6) score++
  if (/\d/.test(value)) score++
  if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++
  if (/[\W_]/.test(value) && !/\s/.test(value)) score++
  return score
}
