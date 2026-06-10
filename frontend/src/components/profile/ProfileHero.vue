<script setup>
import { ref } from 'vue'

const props = defineProps({
  displayName: {
    type: String,
    required: true
  },
  phone: {
    type: String,
    default: ''
  },
  avatar: {
    type: String,
    default: ''
  },
  avatarText: {
    type: String,
    default: ''
  },
  nickname: {
    type: String,
    default: ''
  },
  avatarUploading: {
    type: Boolean,
    default: false
  },
  nicknameSaving: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['avatar-selected', 'save-profile', 'update:nickname'])
const avatarInputRef = ref(null)

function openAvatarPicker() {
  avatarInputRef.value?.click()
}

function onAvatarSelected(event) {
  emit('avatar-selected', event)
}

function avatarStyle() {
  if (props.avatar) {
    return { backgroundImage: `url(${props.avatar})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return {}
}
</script>

<template>
  <section class="hero profile-hero">
    <div class="profile-head">
      <button
        type="button"
        class="avatar-btn"
        :class="{ uploading: avatarUploading }"
        :disabled="avatarUploading"
        @click="openAvatarPicker"
      >
        <div class="avatar" :style="avatarStyle()">{{ avatar ? '' : avatarText }}</div>
        <span class="avatar-tip">{{ avatarUploading ? '上传中...' : '点击更换头像' }}</span>
      </button>
      <input
        ref="avatarInputRef"
        type="file"
        accept="image/jpeg,image/png,image/gif,image/webp"
        class="avatar-input"
        @change="onAvatarSelected"
      />
      <div class="profile-meta">
        <h1>个人中心</h1>
        <p>{{ displayName }} · {{ phone || '未绑定手机号' }}</p>
        <div class="nickname-row">
          <el-input
            :model-value="nickname"
            maxlength="50"
            show-word-limit
            placeholder="设置昵称"
            @update:model-value="emit('update:nickname', $event)"
            @keyup.enter="emit('save-profile')"
          />
          <el-button type="primary" :loading="nicknameSaving" @click="emit('save-profile')">保存昵称</el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.profile-hero { display: grid; gap: 16px; }
.profile-head { align-items: center; display: flex; gap: 20px; flex-wrap: wrap; }
.avatar-btn {
  align-items: center; background: transparent; border: 0; cursor: pointer; display: grid; gap: 8px; justify-items: center; padding: 0;
}
.avatar-btn.uploading { cursor: wait; opacity: 0.75; }
.avatar-input { display: none; }
.avatar {
  align-items: center; background: linear-gradient(135deg, #2563eb, #0f766e); border: 3px solid #fff; border-radius: 50%;
  color: #fff; display: flex; font-size: 28px; font-weight: 800; height: 84px; justify-content: center; width: 84px;
}
.avatar-btn:hover .avatar { border-color: #2563eb; box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12); }
.avatar-tip { color: var(--text-secondary); font-size: 12px; }
.profile-meta { display: grid; gap: 10px; min-width: 240px; }
.profile-meta h1 { margin: 0; }
.profile-meta p { color: var(--text-secondary); margin: 0; }
.nickname-row { align-items: center; display: flex; flex-wrap: wrap; gap: 10px; max-width: 420px; }
.nickname-row .el-input { flex: 1; min-width: 180px; }
</style>
