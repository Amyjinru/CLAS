<script setup>
import { computed, reactive, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { registerMerchant, currentUser, sendMerchantRegisterCode, setSessionUser } from '../api/clas'
import { ElMessage } from 'element-plus'
import LocationSelector from '../components/LocationSelector.vue'
import { passwordChecks } from '../utils/passwordRules'

const router = useRouter()
const user = ref(null)
const formRef = ref(null)

// ===== 多步骤向导 =====
const currentStep = ref(1)
const totalSteps = computed(() => user.value ? 1 : 2)

function nextStep() {
  if (currentStep.value < totalSteps.value) {
    currentStep.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function prevStep() {
  if (currentStep.value > 1) {
    currentStep.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const form = reactive({
  merchantName: '',
  category: '',
  address: '',
  longitude: null,
  latitude: null,
  deliveryRadiusM: 3000,
  accountPhone: '',
  contactPhone: '',
  code: '',
  bankAccount: '',
  settlementCycle: 7,
  username: '',
  password: '',
  confirmPassword: ''
})

const locationData = reactive({
  province: '',
  city: '',
  district: '',
  street: '',
  address: '',
  longitude: null,
  latitude: null,
  source: ''
})

function onLocationConfirm(loc) {
  syncLocationDraft(loc)
  ElMessage.success('地址位置已确认')
}

function syncLocationDraft(loc) {
  form.address = loc.address
  form.longitude = loc.longitude
  form.latitude = loc.latitude
}

const phonePattern = /^1[3-9]\d{9}$/

function validPhone(phone) {
  return phonePattern.test((phone || '').trim())
}

const merchantPasswordChecks = computed(() => passwordChecks(form.password))
const merchantPasswordMatches = computed(() => form.confirmPassword && form.password === form.confirmPassword)

onMounted(() => {
  user.value = currentUser()
})

const rules = reactive({
  merchantName: [
    { required: true, message: '请输入商家名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择或输入经营品类', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入商家地址', trigger: 'blur' }
  ],
  accountPhone: [
    { required: true, message: '请输入账号手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  bankAccount: [
    { pattern: /^\d{9,25}$/, message: '请输入9到25位数字账号', trigger: 'blur' }
  ],
  settlementCycle: [],
  username: [],
  password: [
    { required: true, message: '请输入账号密码', trigger: 'blur' }
  ],
  confirmPassword: []
})

const categories = ['美食', '饮品', '超市', '水果', '生鲜', '鲜花']
const submitting = ref(false)
const codeSending = ref(false)
const codeCooldown = ref(0)
const accountCodeSent = ref(false)
const lastSentAccountPhone = ref('')
let cooldownTimer = null

async function sendMerchantCode() {
  if (!validPhone(form.accountPhone)) {
    ElMessage.warning('请输入正确的账号手机号')
    return
  }
  codeSending.value = true
  try {
    const phone = form.accountPhone.trim()
    await sendMerchantRegisterCode({ phone })
    accountCodeSent.value = true
    lastSentAccountPhone.value = phone
    ElMessage.success('验证码已发送，请在60秒内输入')
    codeCooldown.value = 60
    cooldownTimer && clearInterval(cooldownTimer)
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) {
        clearInterval(cooldownTimer)
        cooldownTimer = null
      }
    }, 1000)
  } finally {
    codeSending.value = false
  }
}

const cooldownText = computed(() => codeCooldown.value ? `${codeCooldown.value}秒后重发` : '发送验证码')
const accountPhoneReady = computed(() => validPhone(form.accountPhone))

watch(() => form.accountPhone, (phone) => {
  if (phone.trim() !== lastSentAccountPhone.value) {
    form.code = ''
    accountCodeSent.value = false
  }
})

async function submitForm() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表单信息')
      return
    }

    if (!user.value && (!accountCodeSent.value || !form.code.trim())) {
      ElMessage.warning('请先发送并填写验证码')
      return
    }
    if (!user.value && form.confirmPassword && !merchantPasswordMatches.value) {
      ElMessage.warning('两次输入的密码不一致')
      return
    }
    if (!form.longitude || !form.latitude) {
      ElMessage.warning('请在地图中选择商家位置')
      return
    }
    
    submitting.value = true
    try {
      const payload = { ...form }
      if (user.value) {
        delete payload.accountPhone
        delete payload.code
        delete payload.username
        delete payload.password
        delete payload.confirmPassword
      }
      
      const data = await registerMerchant(payload)
      ElMessage.success('入驻申请提交成功，请等待管理员审核！')
      
      // 访问者注册后自动登录，直接跳转审核状态页
      if (!user.value && data.user && data.token) {
        setSessionUser({ ...data.user, token: data.token })
        ElMessage.success('入驻申请提交成功，已自动登录！请等待管理员审核')
      } else if (!user.value) {
        ElMessage.info('入驻申请已提交，请使用注册手机号登录查看审核进度')
        router.push({ path: '/login', query: { redirect: '/merchant/audit-status' } })
        return
      }
      router.push('/merchant/audit-status')
    } catch (err) {
      // Error is handled in client.js response interceptor
    } finally {
      submitting.value = false
    }
  })
}

onUnmounted(() => {
  cooldownTimer && clearInterval(cooldownTimer)
})
</script>

<template>
  <div class="register-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <h2>商家入驻申请</h2>
          <p class="subtitle">欢迎加入 CLAS 生活助手平台，请填写以下商家资质信息</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="demo-ruleForm"
        size="large"
      >
        <el-alert
          v-if="user"
          :title="`您当前已登录为: ${user.username} (${user.role})，入驻将绑定至当前账号。`"
          type="success"
          show-icon
          :closable="false"
          class="alert-tip"
        />

        <!-- ===== 步骤指示器 ===== -->
        <div class="step-indicator" v-if="!user">
          <div class="step" :class="{ active: currentStep === 1, done: currentStep > 1 }">
            <span class="step-num">{{ currentStep > 1 ? '✓' : '1' }}</span>
            <span class="step-label">验证账号</span>
          </div>
          <div class="step-line" :class="{ done: currentStep > 1 }"></div>
          <div class="step" :class="{ active: currentStep === 2 }">
            <span class="step-num">2</span>
            <span class="step-label">商家信息</span>
          </div>
        </div>

        <!-- ============================================ -->
        <!-- 步骤 1：验证登录账号（仅未登录访问者）         -->
        <!-- ============================================ -->
        <template v-if="!user && currentStep === 1">
          <el-alert
            title="请先验证登录账号；已注册手机号可直接使用，未注册手机号将自动创建账号。"
            type="info"
            show-icon
            :closable="false"
            class="alert-tip"
          />

          <h3 class="section-title">验证登录账号</h3>

          <el-form-item label="账号手机号" prop="accountPhone">
            <div class="code-row">
              <el-input v-model="form.accountPhone" maxlength="11" placeholder="用于登录平台的手机号" />
              <el-button
                :type="accountPhoneReady ? 'success' : 'info'"
                :loading="codeSending"
                :disabled="codeCooldown > 0 || !accountPhoneReady"
                @click="sendMerchantCode"
              >
                {{ cooldownText }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item v-if="accountCodeSent" label="验证码" prop="code">
            <el-input v-model="form.code" maxlength="6" placeholder="请输入6位短信验证码" />
            <p class="code-tip-inline">验证码会发送至您的手机，60秒内输入有效</p>
          </el-form-item>

          <el-form-item label="昵称" prop="username">
            <el-input v-model="form.username" placeholder="新账号必填；已注册账号可不填" />
          </el-form-item>

          <el-form-item label="账号密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="已有账号请输入当前密码；新账号至少6位" />
            <ul class="password-checks">
              <li
                v-for="item in merchantPasswordChecks"
                :key="item.key"
                :class="{ ok: item.ok }"
              >
                {{ item.ok ? '✓' : '·' }} {{ item.label }}
              </li>
            </ul>
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="新账号请再次输入密码；已注册账号可不填" />
            <p
              v-if="form.confirmPassword"
              class="match-tip"
              :class="{ ok: merchantPasswordMatches }"
            >{{ merchantPasswordMatches ? '两次密码一致' : '两次输入的密码不一致' }}</p>
          </el-form-item>
        </template>

        <!-- ============================================ -->
        <!-- 步骤 2：商家基本信息（已登录用户直接显示此步）  -->
        <!-- ============================================ -->
        <template v-if="currentStep === totalSteps">
          <el-alert
            v-if="!user"
            title="请填写商家资质信息，提交后将进入审核流程。"
            type="info"
            show-icon
            :closable="false"
            class="alert-tip"
          />

          <h3 class="section-title">商家基本信息</h3>

          <el-form-item label="商家名称" prop="merchantName">
            <el-input v-model="form.merchantName" placeholder="请输入店铺名称，如：校园轻食铺" />
          </el-form-item>

          <el-form-item label="经营品类" prop="category">
            <el-select v-model="form.category" placeholder="请选择品类" style="width: 100%">
              <el-option
                v-for="cat in categories"
                :key="cat"
                :label="cat"
                :value="cat"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="form.contactPhone" maxlength="11" placeholder="客户可见的联系电话" />
            <p class="field-hint">可与账号手机号相同，用于客户联系您的店铺</p>
          </el-form-item>

          <el-form-item label="商家地址" prop="address">
            <LocationSelector
              v-model="locationData"
              @update:modelValue="syncLocationDraft"
              @confirm="onLocationConfirm"
            />
          </el-form-item>

          <el-form-item label="配送范围(米)" prop="deliveryRadiusM">
            <el-input-number v-model="form.deliveryRadiusM" :min="500" :max="10000" :step="500" style="width: 100%" />
          </el-form-item>

          <!-- 银行信息移至审核通过后补充 -->
          <el-divider />
          <el-alert
            title="银行结算信息可在审核通过后补充，当前可跳过。"
            type="warning"
            show-icon
            :closable="false"
            class="alert-tip"
          />
          <el-form-item label="银行账号" prop="bankAccount">
            <el-input v-model="form.bankAccount" placeholder="选填：审核通过后可在控制台补充" />
          </el-form-item>

          <el-form-item label="结算周期(天)" prop="settlementCycle">
            <el-input-number v-model="form.settlementCycle" :min="1" :max="90" style="width: 100%" />
          </el-form-item>
        </template>

        <!-- ===== 步骤导航按钮 ===== -->
        <el-form-item class="form-actions">
          <el-button v-if="currentStep > 1" @click="prevStep">上一步</el-button>
          <el-button v-if="!user && currentStep < totalSteps" type="primary" @click="nextStep">
            下一步
          </el-button>
          <el-button
            v-if="currentStep === totalSteps"
            type="primary"
            :loading="submitting"
            @click="submitForm"
            class="submit-btn"
          >
            提交入驻申请
          </el-button>
          <el-button @click="router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.register-container {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.box-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.card-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
}

.subtitle {
  margin: 8px 0 0 0;
  color: var(--text-muted);
  font-size: 14px;
}

.alert-tip {
  margin-bottom: 24px;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  width: 100%;
}

.password-checks {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 12px;
  width: 100%;
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
  color: var(--text-muted);
}

.password-checks li.ok {
  color: var(--clas-success);
}

.match-tip {
  width: 100%;
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--clas-danger);
}

.match-tip.ok {
  color: var(--clas-success);
}

.section-title {
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 20px;
  padding-left: 8px;
  border-left: 4px solid var(--color-primary);
}

.form-actions {
  margin-top: 32px;
  display: flex;
  justify-content: flex-end;
}

.submit-btn {
  padding-left: 32px;
  padding-right: 32px;
}

/* ===== 步骤指示器 ===== */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 28px;
  padding: 20px 40px;
  background: var(--bg-page, #f9fafb);
  border-radius: 12px;
}

.step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  background: var(--border-color, #e5e7eb);
  color: var(--text-muted, #9ca3af);
  transition: all 0.3s ease;
}

.step.active .step-num {
  background: var(--color-primary, #f97316);
  color: #fff;
  box-shadow: 0 2px 8px rgba(249, 115, 22, 0.3);
}

.step.done .step-num {
  background: var(--clas-success, #16a34a);
  color: #fff;
}

.step-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-muted, #9ca3af);
  transition: color 0.3s ease;
}

.step.active .step-label {
  color: var(--text-primary, #1f2937);
}

.step.done .step-label {
  color: var(--clas-success, #16a34a);
}

.step-line {
  width: 60px;
  height: 2px;
  margin: 0 16px;
  background: var(--border-color, #e5e7eb);
  transition: background 0.3s ease;
}

.step-line.done {
  background: var(--clas-success, #16a34a);
}

/* ===== 字段提示 ===== */
.field-hint {
  font-size: 12px;
  color: var(--text-muted, #9ca3af);
  margin-top: 4px;
  line-height: 1.4;
}

.code-tip-inline {
  font-size: 12px;
  color: var(--text-muted, #9ca3af);
  margin-top: 4px;
}

/* WCAG 2.3.3 — 尊重用户减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}

</style>
