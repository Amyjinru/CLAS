<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetForgotPassword, sendForgotPasswordCode, setSessionUser } from '../api/clas'

const route = useRoute()
const router = useRouter()

const phonePattern = /^1[3-9]\d{9}$/
const form = reactive({ phone: '', code: '', newPassword: '', confirmPassword: '' })
const loading = ref(false)
const codeSending = ref(false)
const codeCooldown = ref(0)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const message = ref('')
const messageType = ref('')
let cooldownTimer = null

const roleHome = {
  USER: '/home',
  MERCHANT: '/merchant-console',
  ADMIN: '/admin/dashboard'
}

function showMessage(text, type = '') {
  message.value = text
  messageType.value = type
}

function validPhone(phone) {
  return phonePattern.test((phone || '').trim())
}

function passwordChecks(password) {
  const value = password || ''
  return [
    { key: 'length', label: '不少于6位', ok: value.length >= 6 },
    { key: 'lower', label: '包含小写字母', ok: /[a-z]/.test(value) },
    { key: 'upper', label: '包含大写字母', ok: /[A-Z]/.test(value) },
    { key: 'digit', label: '包含数字', ok: /\d/.test(value) },
    { key: 'special', label: '包含特殊符号', ok: /[\W_]/.test(value) && !/\s/.test(value) }
  ]
}

function redirectByRole(user) {
  const redirect = route.query.redirect
  const target = typeof redirect === 'string' ? redirect : roleHome[user.role] || '/home'
  router.push(target)
}

const passwordState = computed(() => passwordChecks(form.newPassword))
const passwordOk = computed(() => passwordState.value.every((item) => item.ok))
const passwordMatches = computed(() => form.confirmPassword && form.newPassword === form.confirmPassword)
const cooldownText = computed(() => {
  if (!codeCooldown.value) return '发送验证码'
  return `${codeCooldown.value}秒后重发`
})

async function sendCode() {
  if (!validPhone(form.phone)) {
    showMessage('请输入正确的手机号', 'error')
    return
  }
  codeSending.value = true
  showMessage('')
  try {
    await sendForgotPasswordCode({ phone: form.phone })
    showMessage('验证码已发送，请查看后端控制台输出', 'success')
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    }, 1000)
  } catch (error) {
    showMessage(error.response?.data?.message || '发送验证码失败', 'error')
  } finally {
    codeSending.value = false
  }
}

async function submitReset() {
  if (!validPhone(form.phone)) {
    showMessage('请输入正确的手机号', 'error')
    return
  }
  if (!passwordOk.value) {
    showMessage('密码至少6位，必须包含大小写英文字母、数字和特殊符号', 'error')
    return
  }
  if (!passwordMatches.value) {
    showMessage('两次输入的密码不一致', 'error')
    return
  }
  loading.value = true
  showMessage('')
  try {
    const data = await resetForgotPassword({
      phone: form.phone,
      code: form.code,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    })
    setSessionUser({ ...data.user, token: data.token })
    showMessage('密码已重置，已自动登录', 'success')
    setTimeout(() => redirectByRole(data.user), 600)
  } catch (error) {
    showMessage(error.response?.data?.message || '重置密码失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrapper">
    <section class="auth-panel">
      <button type="button" class="back-btn" @click="router.push('/login')">返回登录</button>
      <h1>重置密码</h1>
      <p class="hint">通过已绑定手机号获取验证码，设置符合强度要求的新密码。</p>

      <div class="form-group">
        <label>手机号 <span class="required">*</span></label>
        <input v-model="form.phone" placeholder="请输入已绑定手机号" maxlength="11" />
      </div>

      <div class="form-group">
        <label>验证码 <span class="required">*</span></label>
        <div class="code-row">
          <input
            v-model="form.code"
            placeholder="请输入6位验证码"
            maxlength="6"
            class="code-input"
          />
          <button
            type="button"
            class="resend-btn"
            :disabled="codeCooldown > 0 || !validPhone(form.phone)"
            @click="sendCode"
          >
            <span v-if="codeSending" class="spinner-small"></span>
            {{ codeSending ? '发送中' : cooldownText }}
          </button>
        </div>
        <p class="code-tip">演示环境请查看后端控制台输出获取验证码</p>
      </div>

      <div class="form-group">
        <label>新密码 <span class="required">*</span></label>
        <div class="password-wrap">
          <input
            v-model="form.newPassword"
            :type="showPassword ? 'text' : 'password'"
            placeholder="至少6位，含大小写字母、数字、特殊符号"
          />
          <button
            type="button"
            class="toggle-pwd"
            :class="{ visible: showPassword }"
            @click="showPassword = !showPassword"
            :title="showPassword ? '隐藏密码' : '显示密码'"
          >
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <template v-if="showPassword">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </template>
              <template v-else>
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </template>
            </svg>
          </button>
        </div>
        <ul class="password-checks">
          <li
            v-for="item in passwordState"
            :key="item.key"
            :class="{ ok: item.ok }"
          >
            {{ item.ok ? '✓' : '·' }} {{ item.label }}
          </li>
        </ul>
      </div>

      <div class="form-group">
        <label>确认新密码 <span class="required">*</span></label>
        <div class="password-wrap">
          <input
            v-model="form.confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            placeholder="请再次输入新密码"
          />
          <button
            type="button"
            class="toggle-pwd"
            :class="{ visible: showConfirmPassword }"
            @click="showConfirmPassword = !showConfirmPassword"
            :title="showConfirmPassword ? '隐藏密码' : '显示密码'"
          >
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <template v-if="showConfirmPassword">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </template>
              <template v-else>
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </template>
            </svg>
          </button>
        </div>
        <p
          v-if="form.confirmPassword"
          class="match-tip"
          :class="{ ok: passwordMatches }"
        >{{ passwordMatches ? '两次密码一致' : '两次输入的密码不一致' }}</p>
      </div>

      <button class="submit-btn" :disabled="loading" @click="submitReset">
        <span v-if="loading" class="spinner"></span>
        {{ loading ? '重置中...' : '重置密码并登录' }}
      </button>

      <p
        v-if="message"
        class="auth-message"
        :class="{ 'msg-success': messageType === 'success', 'msg-error': messageType === 'error' }"
      >{{ message }}</p>
    </section>
  </div>
</template>

<style scoped>
.auth-wrapper {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg-page, #f8fafb);
}

.auth-panel {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  background: var(--bg-card, #fff);
  border-radius: var(--radius-xl, 16px);
  box-shadow: var(--shadow-lg, 0 8px 30px rgba(0, 0, 0, 0.08));
  border: 1px solid var(--border-color, #e5e7eb);
}

.back-btn {
  border: none;
  background: transparent;
  color: var(--color-primary, #f97316);
  font-size: 13px;
  cursor: pointer;
  padding: 0;
  margin-bottom: 18px;
}

.back-btn:hover {
  text-decoration: underline;
}

h1 {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 8px;
  color: var(--text-primary, #1f2937);
  letter-spacing: 0.04em;
}

.hint {
  color: var(--text-secondary, #6b7280);
  font-size: 14px;
  margin: 0 0 20px;
  line-height: 1.6;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary, #4b5563);
  margin-bottom: 6px;
}

.required {
  color: var(--clas-error, #ef4444);
}

input:not(.code-input) {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--border-color, #d1d5db);
  border-radius: var(--radius-md, 10px);
  font-size: 15px;
  color: var(--text-primary, #1f2937);
  background: var(--bg-page, #f9fafb);
  transition: border-color var(--transition-fast, 0.2s), box-shadow var(--transition-fast, 0.2s);
  box-sizing: border-box;
}

input:focus,
.code-input:focus {
  outline: none;
  border-color: var(--color-primary, #f97316);
  box-shadow: 0 0 0 3px rgba(249, 115, 22, 0.1);
}

.password-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.password-wrap input {
  padding-right: 44px;
}

.toggle-pwd {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-muted, #9ca3af);
  cursor: pointer;
  border-radius: 6px;
  transition: color var(--transition-fast, 0.2s);
}

.toggle-pwd:hover,
.toggle-pwd.visible {
  color: var(--color-primary, #f97316);
}

.password-checks {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 10px;
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
  color: var(--text-muted, #9ca3af);
}

.password-checks li.ok {
  color: var(--clas-success, #16a34a);
}

.match-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--clas-error, #ef4444);
}

.match-tip.ok {
  color: var(--clas-success, #16a34a);
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-input {
  flex: 1;
  height: 44px;
  padding: 0 14px;
  border: 1px solid var(--border-color, #d1d5db);
  border-radius: var(--radius-md, 10px);
  font-size: 15px;
  letter-spacing: 0.2em;
  text-align: center;
  color: var(--text-primary, #1f2937);
  background: var(--bg-page, #f9fafb);
  box-sizing: border-box;
}

.resend-btn {
  height: 44px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid var(--border-color, #d1d5db);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-page, #f9fafb);
  color: var(--color-primary, #f97316);
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast, 0.2s);
  display: flex;
  align-items: center;
  gap: 6px;
}

.resend-btn:hover:not(:disabled) {
  background: var(--color-primary, #f97316);
  color: #fff;
  border-color: var(--color-primary, #f97316);
}

.resend-btn:disabled {
  color: var(--text-muted, #9ca3af);
  cursor: not-allowed;
}

.code-tip {
  font-size: 13px;
  color: var(--text-muted, #9ca3af);
  margin-top: 6px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
  border-radius: var(--radius-md, 10px);
  background: var(--color-primary, #f97316);
  color: #fff;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast, 0.2s);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-primary-hover, #ea580c);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md, 0 4px 12px rgba(249, 115, 22, 0.3));
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.spinner-small {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(249, 115, 22, 0.3);
  border-top-color: var(--color-primary, #f97316);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.auth-message {
  text-align: center;
  margin-top: 20px;
  padding: 10px 16px;
  border-radius: var(--radius-sm, 8px);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
}

.msg-success {
  color: var(--clas-success, #059669);
  background: var(--clas-success-light, #ecfdf5);
  border: 1px solid rgba(5, 150, 105, 0.15);
}

.msg-error {
  color: var(--clas-error, #dc2626);
  background: var(--clas-error-light, #fef2f2);
  border: 1px solid rgba(220, 38, 38, 0.15);
}

@media (max-width: 480px) {
  .auth-wrapper {
    padding: 12px;
    align-items: flex-start;
    padding-top: 24px;
  }

  .auth-panel {
    padding: 28px 20px;
  }
}
</style>
