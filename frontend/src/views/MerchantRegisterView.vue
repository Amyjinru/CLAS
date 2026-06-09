<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { registerMerchant, currentUser, sendRegisterCode } from '../api/clas'
import { ElMessage } from 'element-plus'
import LocationSelector from '../components/LocationSelector.vue'

const router = useRouter()
const user = ref(null)
const formRef = ref(null)

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

const merchantPasswordChecks = computed(() => passwordChecks(form.password))
const merchantPasswordOk = computed(() => merchantPasswordChecks.value.every((item) => item.ok))
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
    { required: true, message: '请输入银行账号', trigger: 'blur' },
    { pattern: /^\d{9,25}$/, message: '请输入9到25位数字账号', trigger: 'blur' }
  ],
  settlementCycle: [
    { required: true, message: '请输入结算周期', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入展示名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' }
  ]
})

const categories = ['美食', '饮品', '超市', '水果', '生鲜', '鲜花']
const submitting = ref(false)
const codeSending = ref(false)
const codeCooldown = ref(0)
let cooldownTimer = null

async function sendMerchantCode() {
  if (!validPhone(form.accountPhone)) {
    ElMessage.warning('请输入正确的账号手机号')
    return
  }
  codeSending.value = true
  try {
    await sendRegisterCode({ phone: form.accountPhone })
    ElMessage.success('验证码已发送，请查看后端控制台输出')
    codeCooldown.value = 60
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

async function submitForm() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表单信息')
      return
    }

    if (!user.value && !merchantPasswordOk.value) {
      ElMessage.warning('密码至少6位，必须包含大小写英文字母、数字和特殊符号')
      return
    }
    if (!user.value && !merchantPasswordMatches.value) {
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
      
      // If a visitor registered, auto log them in or redirect to login
      if (!user.value && data.userId) {
        ElMessage.info('系统已为您创建商家账号，请重新登录')
        router.push('/login')
      } else {
        router.push('/merchant-console')
      }
    } catch (err) {
      // Error is handled in client.js response interceptor
    } finally {
      submitting.value = false
    }
  })
}
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
        <el-alert
          v-else
          title="您尚未登录。请在下方填写商家信息，系统将自动为您注册专有的商家账号。"
          type="info"
          show-icon
          :closable="false"
          class="alert-tip"
        />

        <!-- Account registration section for visitors -->
        <template v-if="!user">
          <h3 class="section-title">1. 创建登录账号</h3>
          <el-form-item label="账号手机号" prop="accountPhone">
            <div class="code-row">
              <el-input v-model="form.accountPhone" maxlength="11" placeholder="请输入用于登录的手机号" />
              <el-button
                :loading="codeSending"
                :disabled="codeCooldown > 0 || !validPhone(form.accountPhone)"
                @click="sendMerchantCode"
              >
                {{ cooldownText }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <el-input v-model="form.code" maxlength="6" placeholder="请输入后端控制台输出的6位验证码" />
          </el-form-item>
          <el-form-item label="展示名" prop="username">
            <el-input v-model="form.username" placeholder="请输入展示名，可与他人重复" />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="至少6位，含大小写字母、数字、特殊符号" />
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
            <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
            <p
              v-if="form.confirmPassword"
              class="match-tip"
              :class="{ ok: merchantPasswordMatches }"
            >{{ merchantPasswordMatches ? '两次密码一致' : '两次输入的密码不一致' }}</p>
          </el-form-item>
          <el-divider />
        </template>

        <h3 class="section-title">2. 商家基本信息</h3>
        
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
          <el-input v-model="form.contactPhone" maxlength="11" placeholder="请输入店铺联系手机号，可与账号手机号不同" />
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

        <el-form-item label="银行账号" prop="bankAccount">
          <el-input v-model="form.bankAccount" placeholder="请输入对公/对私结算银行账号" />
        </el-form-item>

        <el-form-item label="结算周期(天)" prop="settlementCycle">
          <el-input-number v-model="form.settlementCycle" :min="1" :max="90" style="width: 100%" />
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button type="primary" :loading="submitting" @click="submitForm" class="submit-btn">
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
  color: #303133;
  font-size: 24px;
}

.subtitle {
  margin: 8px 0 0 0;
  color: #909399;
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
  color: #909399;
}

.password-checks li.ok {
  color: #16a34a;
}

.match-tip {
  width: 100%;
  margin: 6px 0 0;
  font-size: 12px;
  color: #ef4444;
}

.match-tip.ok {
  color: #16a34a;
}

.section-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 20px;
  padding-left: 8px;
  border-left: 4px solid #409eff;
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

</style>
