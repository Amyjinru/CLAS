<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { registerMerchant, currentUser } from '../api/clas'
import { ElMessage } from 'element-plus'

const router = useRouter()
const user = ref(null)
const formRef = ref(null)

const form = reactive({
  merchantName: '',
  category: '',
  address: '',
  phone: '',
  bankAccount: '',
  settlementCycle: 7,
  username: '',
  password: ''
})

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
  phone: [
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
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
})

const categories = ['美食', '饮品', '超市', '水果', '生鲜', '鲜花']
const submitting = ref(false)

async function submitForm() {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表单信息')
      return
    }
    
    submitting.value = true
    try {
      const payload = { ...form }
      if (user.value) {
        delete payload.username
        delete payload.password
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
          <el-form-item label="登录用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入拟注册的账号名" />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入拟注册的密码" />
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

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系手机号" />
        </el-form-item>

        <el-form-item label="商家地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入店铺具体地址" />
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
