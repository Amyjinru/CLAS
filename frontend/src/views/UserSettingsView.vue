<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  addBankCard,
  changePassword,
  deleteBankCard,
  getProfile,
  listAddresses,
  listBankCards,
  sendPhoneChangeCode,
  sessionUser,
  setSessionUser,
  updateBoundPhone,
  updateProfile,
  uploadAvatar
} from '../api/clas'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProfileHero from '../components/profile/ProfileHero.vue'
import ProfileAddressSection from '../components/profile/ProfileAddressSection.vue'
import { applyThemePreference, preferenceState, setLanguagePreference } from '../utils/preferences'

const activeTab = ref('profile')
const profileForm = reactive({ nickname: '', avatar: '' })
const phoneForm = reactive({ phone: '', code: '' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const bankForm = reactive({ bankName: '', cardholderName: '', cardNo: '', cardType: '借记卡', isDefault: false })
const generalForm = reactive({ language: preferenceState.language, theme: preferenceState.theme })
const addresses = ref([])
const bankCards = ref([])
const avatarUploading = ref(false)
const nicknameSaving = ref(false)
const sendingCode = ref(false)
const changingPhone = ref(false)
const changingPassword = ref(false)
const bankDialogVisible = ref(false)
const savingBankCard = ref(false)
const bankActionId = ref(null)

const currentUser = computed(() => sessionUser.value || {})
const displayName = computed(() => profileForm.nickname || currentUser.value?.username || currentUser.value?.phone || '未命名用户')

function getErrorMessage(error, fallback = '操作失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

function avatarText() {
  return (displayName.value || '?').slice(0, 1).toUpperCase()
}

async function loadProfile() {
  try {
    const profile = await getProfile({ silent: true })
    profileForm.nickname = profile.nickname || profile.username || ''
    profileForm.avatar = profile.avatar || ''
    if (sessionUser.value) {
      setSessionUser({ ...sessionUser.value, ...profile, password: undefined })
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '个人信息加载失败'))
  }
}

async function loadAddresses() {
  try {
    addresses.value = await listAddresses({ silent: true })
  } catch {
    addresses.value = []
  }
}

async function loadBankCards() {
  try {
    bankCards.value = await listBankCards()
  } catch {
    bankCards.value = []
  }
}

async function saveProfile() {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  nicknameSaving.value = true
  try {
    const profile = await updateProfile({ nickname: profileForm.nickname.trim() })
    profileForm.nickname = profile.nickname || profile.username || ''
    setSessionUser({ ...sessionUser.value, ...profile, password: undefined })
    ElMessage.success('昵称已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '昵称更新失败'))
  } finally {
    nicknameSaving.value = false
  }
}

async function onAvatarSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 2MB')
    return
  }
  avatarUploading.value = true
  try {
    const profile = await uploadAvatar(file)
    profileForm.avatar = profile.avatar || ''
    setSessionUser({ ...sessionUser.value, ...profile, password: undefined })
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '头像上传失败'))
  } finally {
    avatarUploading.value = false
  }
}

async function sendCode() {
  if (!phoneForm.phone.trim()) {
    ElMessage.warning('请填写新手机号')
    return
  }
  sendingCode.value = true
  try {
    await sendPhoneChangeCode({ phone: phoneForm.phone.trim() })
    ElMessage.success('验证码已发送')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码发送失败'))
  } finally {
    sendingCode.value = false
  }
}

async function submitPhoneChange() {
  if (!phoneForm.phone.trim() || !phoneForm.code.trim()) {
    ElMessage.warning('请填写新手机号和验证码')
    return
  }
  changingPhone.value = true
  try {
    const login = await updateBoundPhone({ phone: phoneForm.phone.trim(), code: phoneForm.code.trim() })
    setSessionUser({ ...login.user, token: login.token })
    phoneForm.phone = ''
    phoneForm.code = ''
    ElMessage.success('绑定手机号已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '手机号修改失败'))
  } finally {
    changingPhone.value = false
  }
}

async function submitPasswordChange() {
  if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请完整填写密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    await changePassword({ ...passwordForm })
    Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
    ElMessage.success('密码已更新')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '密码修改失败'))
  } finally {
    changingPassword.value = false
  }
}

function resetBankForm() {
  Object.assign(bankForm, { bankName: '', cardholderName: '', cardNo: '', cardType: '借记卡', isDefault: false })
}

async function submitBankCard() {
  if (!bankForm.bankName.trim() || !bankForm.cardholderName.trim() || !bankForm.cardNo.trim()) {
    ElMessage.warning('请完整填写银行卡信息')
    return
  }
  savingBankCard.value = true
  try {
    await addBankCard({
      bankName: bankForm.bankName.trim(),
      cardholderName: bankForm.cardholderName.trim(),
      cardNo: bankForm.cardNo.replace(/\s+/g, ''),
      cardType: bankForm.cardType,
      isDefault: bankForm.isDefault
    })
    ElMessage.success('银行卡已绑定')
    bankDialogVisible.value = false
    resetBankForm()
    await loadBankCards()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '银行卡绑定失败'))
  } finally {
    savingBankCard.value = false
  }
}

async function removeBankCard(card) {
  try {
    await ElMessageBox.confirm(`确定删除 ${card.bankName} ${card.maskedCardNo} 吗？`, '删除银行卡', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  bankActionId.value = card.id
  try {
    await deleteBankCard(card.id)
    ElMessage.success('银行卡已删除')
    await loadBankCards()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除失败'))
  } finally {
    bankActionId.value = null
  }
}

function updateTheme(value) {
  generalForm.theme = value
  applyThemePreference(value)
}

function updateLanguage(value) {
  generalForm.language = value
  setLanguagePreference(value)
  ElMessage.success('语言偏好已保存')
}

onMounted(async () => {
  await Promise.all([loadProfile(), loadAddresses(), loadBankCards()])
})
</script>

<template>
  <div class="user-page settings-page">
    <section class="panel settings-head">
      <div>
        <h1>设置</h1>
        <p>管理资料、地址、安全、支付和通用偏好</p>
      </div>
    </section>

    <section class="panel settings-workspace">
      <el-tabs v-model="activeTab" tab-position="left" class="settings-tabs">
        <el-tab-pane label="个人信息" name="profile">
          <ProfileHero
            v-model:nickname="profileForm.nickname"
            :display-name="displayName"
            :phone="currentUser.phone"
            :avatar="profileForm.avatar"
            :avatar-text="avatarText()"
            :avatar-uploading="avatarUploading"
            :nickname-saving="nicknameSaving"
            @avatar-selected="onAvatarSelected"
            @save-profile="saveProfile"
          />
        </el-tab-pane>

        <el-tab-pane label="收货地址" name="addresses" lazy>
          <ProfileAddressSection :addresses="addresses" @reload="loadAddresses" />
        </el-tab-pane>

        <el-tab-pane label="账号安全" name="security">
          <div class="settings-section">
            <div class="section-head">
              <div>
                <h2>绑定手机号</h2>
                <p>当前手机号：{{ currentUser.phone || '未绑定' }}</p>
              </div>
            </div>
            <el-form label-position="top" class="settings-form">
              <div class="form-row">
                <el-form-item label="新手机号">
                  <el-input v-model="phoneForm.phone" placeholder="请输入新手机号" />
                </el-form-item>
                <el-form-item label="验证码">
                  <div class="inline-field">
                    <el-input v-model="phoneForm.code" placeholder="短信验证码" />
                    <el-button :loading="sendingCode" @click="sendCode">获取验证码</el-button>
                  </div>
                </el-form-item>
              </div>
              <el-button type="primary" :loading="changingPhone" @click="submitPhoneChange">保存手机号</el-button>
            </el-form>
          </div>

          <div class="settings-section">
            <div class="section-head">
              <div>
                <h2>修改密码</h2>
                <p>需要输入当前密码，新密码需二次确认</p>
              </div>
            </div>
            <el-form label-position="top" class="settings-form">
              <el-form-item label="当前密码">
                <el-input v-model="passwordForm.currentPassword" type="password" show-password placeholder="请输入当前密码" />
              </el-form-item>
              <div class="form-row">
                <el-form-item label="新密码">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
                <el-form-item label="确认新密码">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
              </div>
              <el-button type="primary" :loading="changingPassword" @click="submitPasswordChange">更新密码</el-button>
            </el-form>
          </div>

        </el-tab-pane>

        <el-tab-pane label="支付设置" name="payment">
          <div class="section-head">
            <div>
              <h2>银行卡</h2>
              <p>可绑定多张银行卡，卡号仅脱敏展示</p>
            </div>
            <el-button type="primary" @click="bankDialogVisible = true">添加银行卡</el-button>
          </div>

          <el-empty v-if="!bankCards.length" description="暂无绑定银行卡">
            <el-button type="primary" plain @click="bankDialogVisible = true">添加银行卡</el-button>
          </el-empty>

          <div v-else class="bank-card-grid">
            <article v-for="card in bankCards" :key="card.id" class="bank-card">
              <div>
                <strong>{{ card.bankName }}</strong>
                <p>{{ card.cardType || '银行卡' }} · {{ card.maskedCardNo }}</p>
                <p>持卡人 {{ card.cardholderName }}</p>
              </div>
              <div class="row-actions">
                <el-tag v-if="card.isDefault" type="success">默认</el-tag>
                <el-button text type="danger" :loading="bankActionId === card.id" @click="removeBankCard(card)">删除</el-button>
              </div>
            </article>
          </div>
        </el-tab-pane>

        <el-tab-pane label="通用设置" name="general">
          <div class="settings-section">
            <div class="section-head">
              <div>
                <h2>通用设置</h2>
                <p>语言和显示模式会保存在当前浏览器</p>
              </div>
            </div>
            <el-form label-position="top" class="settings-form narrow-form">
              <el-form-item label="语言">
                <el-select v-model="generalForm.language" @change="updateLanguage">
                  <el-option label="简体中文" value="zh-CN" />
                  <el-option label="English" value="en" />
                </el-select>
              </el-form-item>
              <el-form-item label="显示模式">
                <el-radio-group v-model="generalForm.theme" @change="updateTheme">
                  <el-radio-button label="light">白天模式</el-radio-button>
                  <el-radio-button label="dark">黑夜模式</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="bankDialogVisible" title="添加银行卡" width="520px" @closed="resetBankForm">
      <el-form label-position="top">
        <el-form-item label="开户银行">
          <el-input v-model="bankForm.bankName" placeholder="例如 招商银行" />
        </el-form-item>
        <el-form-item label="持卡人">
          <el-input v-model="bankForm.cardholderName" placeholder="请输入持卡人姓名" />
        </el-form-item>
        <el-form-item label="银行卡号">
          <el-input v-model="bankForm.cardNo" placeholder="请输入银行卡号" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="卡类型">
            <el-select v-model="bankForm.cardType">
              <el-option label="借记卡" value="借记卡" />
              <el-option label="信用卡" value="信用卡" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认卡">
            <el-switch v-model="bankForm.isDefault" active-text="设为默认" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="bankDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingBankCard" @click="submitBankCard">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.settings-page { display: grid; gap: 20px; }
.settings-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
}
.settings-head h1,
.section-head h2 { margin: 0; }
.settings-head p,
.section-head p,
.bank-card p {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 6px 0 0;
}
.settings-workspace { overflow: hidden; }
.settings-tabs :deep(.el-tabs__content) { padding-left: 20px; }
.settings-section {
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 24px;
  padding-bottom: 24px;
}
.settings-section:last-child {
  border-bottom: 0;
  margin-bottom: 0;
  padding-bottom: 0;
}
.section-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 16px;
}
.settings-form { max-width: 760px; }
.narrow-form { max-width: 420px; }
.form-row {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.inline-field {
  align-items: center;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) auto;
}
.bank-card-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.bank-card {
  background: linear-gradient(135deg, var(--bg-card), var(--color-primary-light));
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  min-height: 132px;
  padding: 18px;
}
.bank-card strong { font-size: 18px; }
.row-actions { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
@media (max-width: 900px) {
  .settings-tabs :deep(.el-tabs) { display: block; }
  .settings-tabs :deep(.el-tabs__header) {
    float: none;
    margin-right: 0;
  }
  .settings-tabs :deep(.el-tabs__content) { padding-left: 0; }
  .form-row,
  .bank-card-grid,
  .inline-field { grid-template-columns: 1fr; }
  .bank-card { flex-direction: column; }
  .row-actions { justify-content: flex-start; }
}
</style>
