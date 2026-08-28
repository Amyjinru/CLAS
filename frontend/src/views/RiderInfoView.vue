<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRiderInfo, requestRiderServicePhoneChange, sessionUser, setSessionUser, updateRiderInfo, uploadAvatar } from '../api/clas'

const loading = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const info = ref(null)
const editVisible = ref(false)
const fileInputRef = ref(null)
const editForm = reactive({ vehicleType: '', serviceArea: '', emergencyContactName: '', emergencyContactPhone: '', servicePhone: '' })
const statusText = { APPROVED: '资料正常', DISABLED: '已停用', PENDING: '审核中' }
const requestText = { PENDING: '审核中', APPROVED: '已通过', REJECTED: '未通过' }

function syncForm(data) {
  Object.assign(editForm, {
    vehicleType: data.vehicleType || '', serviceArea: data.serviceArea || '',
    emergencyContactName: data.emergencyContactName || '', emergencyContactPhone: data.emergencyContactPhone || '', servicePhone: data.servicePhone || ''
  })
}
const servicePhoneChanged = computed(() => Boolean(info.value) && editForm.servicePhone.trim() !== (info.value.servicePhone || '').trim())
async function load() {
  loading.value = true
  try { info.value = await getRiderInfo(); syncForm(info.value) } catch (error) { ElMessage.error(error?.message || '骑手资料加载失败') } finally { loading.value = false }
}
function openEdit() { if (info.value) { syncForm(info.value); editVisible.value = true } }
async function saveInfo() {
  saving.value = true
  try {
    const requestedPhoneChange = servicePhoneChanged.value
    info.value = await updateRiderInfo({
      vehicleType: editForm.vehicleType, serviceArea: editForm.serviceArea,
      emergencyContactName: editForm.emergencyContactName, emergencyContactPhone: editForm.emergencyContactPhone
    })
    if (requestedPhoneChange) await requestRiderServicePhoneChange({ phone: editForm.servicePhone })
    await load()
    editVisible.value = false
    ElMessage.success(requestedPhoneChange ? '普通资料已更新，联系电话已提交审核' : '资料已更新')
  } catch (error) { ElMessage.error(error?.message || '资料保存失败') } finally { saving.value = false }
}
function chooseAvatar() { fileInputRef.value?.click() }
async function onAvatarSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) return ElMessage.warning('仅支持 JPG、PNG 或 WebP 图片')
  if (file.size > 5 * 1024 * 1024) return ElMessage.warning('头像大小不能超过 5MB')
  avatarUploading.value = true
  try { const user = await uploadAvatar(file); setSessionUser({ ...sessionUser.value, ...user, password: undefined }); await load(); ElMessage.success('骑手头像已更新') } catch (error) { ElMessage.error(error?.message || '头像上传失败') } finally { avatarUploading.value = false }
}
onMounted(load)
</script>

<template>
  <section class="rider-info-page" v-loading="loading">
    <div class="page-head"><div><span class="eyebrow">RIDER SERVICE PROFILE</span><h1>骑手信息</h1><p>维护配送服务资料，让每一次接单都更安心、更高效。</p></div><el-button type="primary" size="large" @click="openEdit">修改服务档案</el-button></div>
    <div v-if="info" class="profile-layout">
      <aside class="profile-card">
        <button class="avatar-button" type="button" :disabled="avatarUploading" @click="chooseAvatar"><img v-if="info.avatar" :src="info.avatar" alt="骑手头像" /><span v-else>{{ info.displayName?.slice(0, 1) || '骑' }}</span><i class="avatar-mask">更换</i></button>
        <input ref="fileInputRef" class="file-input" type="file" accept="image/jpeg,image/png,image/webp" @change="onAvatarSelected" />
        <h2>{{ info.displayName }}</h2><p class="rider-id">骑手 ID：{{ info.userId }}</p><el-tag type="success" effect="light">{{ statusText[info.status] || info.status }}</el-tag><el-button plain size="small" :loading="avatarUploading" @click="chooseAvatar">上传头像</el-button>
        <div class="rating-block"><el-rate :model-value="info.averageRating" disabled allow-half text-color="#f59e0b" /><strong>{{ info.averageRating?.toFixed(1) || '0.0' }}</strong><span>{{ info.ratingCount }} 条配送评价</span></div>
        <dl class="side-summary"><div><dt>接单状态</dt><dd>{{ info.acceptingOrders ? '正在接单' : '暂不接单' }}</dd></div><div><dt>并发上限</dt><dd>{{ info.maxActiveOrders || 0 }} 单</dd></div><div><dt>服务区域</dt><dd>{{ info.serviceArea || '待设置' }}</dd></div></dl>
      </aside>
      <main class="detail-panel">
        <div class="panel-title"><div><h2>身份与服务档案</h2><p>身份证明由平台审核留存，敏感号码仅展示脱敏结果。</p></div></div>
        <el-descriptions :column="2" border><el-descriptions-item label="真实姓名">{{ info.realName || '待补充' }}</el-descriptions-item><el-descriptions-item label="身份证号">{{ info.idCardMasked || '待补充' }}</el-descriptions-item><el-descriptions-item label="配送工具">{{ info.vehicleType || '待补充' }}</el-descriptions-item><el-descriptions-item label="服务区域">{{ info.serviceArea || '待设置' }}</el-descriptions-item><el-descriptions-item label="紧急联系人">{{ info.emergencyContactName || '待补充' }}</el-descriptions-item><el-descriptions-item label="紧急联系电话">{{ info.emergencyContactPhone || '待补充' }}</el-descriptions-item></el-descriptions>
        <div class="phone-audit-card"><div><span class="audit-kicker">核心资料 · 需审核</span><h3>服务联系电话</h3><p>{{ info.servicePhone }} <template v-if="info.latestPhoneChange?.status === 'PENDING'">· 新号码 {{ info.latestPhoneChange.requestedPhone }} 正在审核</template></p></div><span class="audit-note">可在“修改服务档案”中提交变更</span></div>
        <div v-if="info.latestPhoneChange" class="request-status" :class="info.latestPhoneChange.status.toLowerCase()"><strong>最近一次联系电话申请：{{ requestText[info.latestPhoneChange.status] || info.latestPhoneChange.status }}</strong><span>申请号码 {{ info.latestPhoneChange.requestedPhone }} · {{ info.latestPhoneChange.reviewReason || (info.latestPhoneChange.status === 'PENDING' ? '等待平台审核' : '暂无备注') }}</span></div>
        <p class="hint">配送工具、服务区域和紧急联系人可直接更新；服务联系电话属于核心联系资料，提交后由管理员审核生效。</p>
      </main>
    </div>
    <el-dialog v-model="editVisible" title="修改服务档案" width="560px" destroy-on-close><el-alert title="普通资料保存后即时生效；服务联系电话发生变更时将自动提交管理员审核。" type="info" :closable="false" show-icon class="dialog-alert" /><el-form label-width="108px"><el-form-item label="配送工具"><el-select v-model="editForm.vehicleType" style="width:100%"><el-option label="电动车" value="E_BIKE" /><el-option label="摩托车" value="MOTORCYCLE" /><el-option label="自行车" value="BICYCLE" /><el-option label="步行配送" value="WALKING" /></el-select></el-form-item><el-form-item label="服务区域"><el-input v-model="editForm.serviceArea" maxlength="100" placeholder="如：大学城校区及周边" /></el-form-item><el-form-item label="紧急联系人"><el-input v-model="editForm.emergencyContactName" maxlength="50" /></el-form-item><el-form-item label="紧急联系电话"><el-input v-model="editForm.emergencyContactPhone" maxlength="11" /></el-form-item><el-divider content-position="left">核心联系资料</el-divider><el-form-item label="服务联系电话"><el-input v-model="editForm.servicePhone" maxlength="11" :disabled="info?.latestPhoneChange?.status === 'PENDING'" /><p class="field-tip">{{ info?.latestPhoneChange?.status === 'PENDING' ? '已有联系电话修改正在审核，请等待审核完成。' : '修改后将自动进入管理员审核，审核通过后生效。' }}</p></el-form-item></el-form><template #footer><el-button @click="editVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveInfo">保存修改</el-button></template></el-dialog>
  </section>
</template>

<style scoped>
.rider-info-page{max-width:1180px;margin:30px auto 54px;padding:0 20px}.page-head{display:flex;justify-content:space-between;align-items:end;gap:24px;margin-bottom:22px}.eyebrow{color:#ea580c;font-size:12px;font-weight:800;letter-spacing:.14em}.page-head h1{margin:5px 0 7px;font-size:30px;color:var(--text-primary)}.page-head p{margin:0;color:var(--text-secondary)}.profile-layout{display:grid;grid-template-columns:310px minmax(0,1fr);gap:24px;align-items:start}.profile-card,.detail-panel{background:rgba(255,255,255,.92);border:1px solid var(--border-color);border-radius:20px;box-shadow:0 12px 32px rgba(74,45,20,.07)}.profile-card{padding:27px 22px;text-align:center;position:sticky;top:84px}.avatar-button{width:104px;height:104px;border:0;border-radius:50%;padding:0;overflow:hidden;position:relative;cursor:pointer;background:linear-gradient(135deg,#fb923c,#ea580c);color:#fff;font-size:38px;font-weight:800}.avatar-button img{width:100%;height:100%;object-fit:cover}.avatar-mask{position:absolute;inset:0;display:grid;place-items:center;background:rgba(25,20,15,.54);font-style:normal;font-size:14px;opacity:0;transition:.2s}.avatar-button:hover .avatar-mask{opacity:1}.file-input{display:none}.profile-card h2{margin:15px 0 5px;font-size:21px}.rider-id{margin:0 0 12px;color:var(--text-tertiary);font-size:13px}.profile-card :deep(.el-button){margin-top:14px}.rating-block{margin:24px 0 18px;padding:18px 0;border-top:1px solid var(--border-light);border-bottom:1px solid var(--border-light);display:grid;gap:5px;justify-items:center}.rating-block strong{color:#b45309;font-size:26px}.rating-block span{font-size:12px;color:var(--text-tertiary)}.side-summary{margin:0;text-align:left;display:grid;gap:13px}.side-summary div{display:flex;justify-content:space-between;gap:16px}.side-summary dt{font-size:13px;color:var(--text-tertiary)}.side-summary dd{margin:0;max-width:160px;text-align:right;color:var(--text-secondary);font-size:13px;font-weight:600}.detail-panel{padding:27px}.panel-title{display:flex;align-items:start;justify-content:space-between;gap:16px;margin-bottom:20px}.panel-title h2{font-size:21px;margin:0 0 7px}.panel-title p,.hint{margin:0;color:var(--text-tertiary);font-size:13px;line-height:1.7}.phone-audit-card{display:flex;justify-content:space-between;align-items:center;gap:20px;margin-top:22px;padding:20px;border:1px solid #fed7aa;border-radius:14px;background:linear-gradient(135deg,#fff7ed,#fffbeb)}.audit-kicker{color:#c2410c;font-size:12px;font-weight:700}.phone-audit-card h3{margin:5px 0;color:var(--text-primary);font-size:18px}.phone-audit-card p{margin:0;color:var(--text-secondary)}.audit-note{color:#9a3412;font-size:13px;font-weight:600}.request-status{display:grid;gap:4px;margin-top:14px;padding:12px 15px;border-radius:10px;background:#fef3c7;color:#92400e;font-size:13px}.request-status.approved{background:#ecfdf5;color:#047857}.request-status.rejected{background:#fef2f2;color:#b91c1c}.hint{margin-top:18px}.dialog-alert{margin-bottom:18px}.field-tip{margin:6px 0 0;color:var(--text-tertiary);font-size:12px;line-height:1.5}@media(max-width:820px){.page-head{align-items:flex-start;flex-direction:column}.profile-layout{grid-template-columns:1fr}.profile-card{position:static}.phone-audit-card{align-items:flex-start;flex-direction:column}}@media(max-width:540px){.rider-info-page{margin-top:18px;padding:0 14px}.detail-panel{padding:18px}.panel-title{flex-direction:column}}
</style>
