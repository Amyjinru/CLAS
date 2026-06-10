<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAdminChatMerchants, getAdminChatMessages, getAdminChatUsers } from '../../api/chat'
import { adminListMerchants } from '../../api/merchant'

const loading = ref(false)
const merchants = ref([])
const merchantIds = ref([])
const users = ref([])
const messages = ref([])
const selectedMerchantId = ref(null)
const selectedUserId = ref('')

const merchantMap = computed(() => new Map(merchants.value.map((item) => [item.id, item])))
const chatMerchants = computed(() =>
  merchantIds.value.map((id) => ({
    id,
    name: merchantMap.value.get(id)?.merchantName || `商家 #${id}`,
    logo: merchantMap.value.get(id)?.logo || ''
  }))
)

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function userLabel(userId) {
  return userId ? `用户 ${String(userId).slice(-4)}` : '未知用户'
}

async function load() {
  loading.value = true
  try {
    const [ids, allMerchants] = await Promise.all([
      getAdminChatMerchants(),
      adminListMerchants()
    ])
    merchantIds.value = ids
    merchants.value = allMerchants
  } finally {
    loading.value = false
  }
}

async function selectMerchant(merchantId) {
  selectedMerchantId.value = merchantId
  selectedUserId.value = ''
  messages.value = []
  const ids = await getAdminChatUsers(merchantId)
  users.value = await Promise.all(ids.map(async (userId) => {
    try {
      const rows = await getAdminChatMessages(merchantId, userId)
      return { userId, count: rows.length }
    } catch {
      return { userId, count: 0 }
    }
  }))
}

async function selectUser(userId) {
  selectedUserId.value = userId
  messages.value = await getAdminChatMessages(selectedMerchantId.value, userId)
}

onMounted(load)
</script>

<template>
  <section class="admin-messages" v-loading="loading">
    <header class="page-head">
      <h1>信息管理</h1>
    </header>

    <div class="message-layout">
      <aside class="panel">
        <h2>商家列表</h2>
        <button
          v-for="merchant in chatMerchants"
          :key="merchant.id"
          type="button"
          :class="['list-item', { active: selectedMerchantId === merchant.id }]"
          @click="selectMerchant(merchant.id)"
        >
          <span class="avatar" :class="{ 'has-logo': merchant.logo }" :style="merchant.logo ? { backgroundImage: `url(${merchant.logo})` } : null">
            {{ merchant.logo ? '' : merchant.name.slice(0, 1) }}
          </span>
          <span>
            <strong>{{ merchant.name }}</strong>
            <small>ID {{ merchant.id }}</small>
          </span>
        </button>
        <el-empty v-if="!chatMerchants.length" description="暂无聊天商家" />
      </aside>

      <aside class="panel">
        <h2>用户列表</h2>
        <button
          v-for="item in users"
          :key="item.userId"
          type="button"
          :class="['list-item', { active: selectedUserId === item.userId }]"
          @click="selectUser(item.userId)"
        >
          <span class="avatar user-avatar">{{ String(item.userId).slice(-2) }}</span>
          <span>
            <strong>{{ userLabel(item.userId) }}</strong>
            <small>{{ item.count }} 条消息</small>
          </span>
        </button>
        <el-empty v-if="selectedMerchantId && !users.length" description="暂无聊天用户" />
        <div v-if="!selectedMerchantId" class="hint">请选择商家查看聊天用户</div>
      </aside>

      <section class="panel record-column">
        <div v-if="!selectedMerchantId" class="empty-record">请选择商家查看聊天记录</div>
        <div v-else-if="!selectedUserId" class="empty-record">请选择用户查看聊天记录</div>
        <template v-else>
          <header class="record-head">
            <h2>{{ merchantMap.get(selectedMerchantId)?.merchantName || `商家 #${selectedMerchantId}` }}</h2>
            <span>{{ userLabel(selectedUserId) }}</span>
          </header>
          <div class="record-body">
            <div
              v-for="message in messages"
              :key="message.id"
              :class="['message', message.senderRole === 'MERCHANT' ? 'merchant' : 'user']"
            >
              <span class="message-meta">{{ message.senderRole }} · {{ formatTime(message.createdAt) }}</span>
              <p>{{ message.content }}</p>
            </div>
            <el-empty v-if="!messages.length" description="暂无聊天记录" />
          </div>
        </template>
      </section>
    </div>
  </section>
</template>

<style scoped>
.admin-messages {
  display: grid;
  gap: 18px;
}

.page-head h1 {
  color: var(--text-primary);
  font-size: 24px;
  margin: 0;
}

.message-layout {
  display: grid;
  gap: 16px;
  grid-template-columns: 280px 220px minmax(0, 1fr);
  min-height: calc(100vh - 170px);
}

.panel {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  min-width: 0;
  padding: 14px;
}

.panel h2 {
  color: var(--text-primary);
  font-size: 16px;
  margin: 0 0 12px;
}

.list-item {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  gap: 10px;
  min-height: 58px;
  padding: 8px;
  text-align: left;
  width: 100%;
}

.list-item:hover,
.list-item.active {
  background: var(--color-primary-light);
}

.avatar {
  align-items: center;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  background-position: center;
  background-size: cover;
  border-radius: 50%;
  color: #fff;
  display: flex;
  flex: 0 0 auto;
  font-weight: 700;
  height: 38px;
  justify-content: center;
  width: 38px;
}

.avatar.has-logo {
  border: 1px solid var(--border-color);
}

.user-avatar {
  background: linear-gradient(135deg, var(--color-accent), var(--clas-teal-700));
  font-size: 13px;
}

.list-item strong,
.list-item small {
  display: block;
}

.list-item strong {
  color: var(--text-primary);
  font-size: 14px;
}

.list-item small,
.hint,
.empty-record,
.record-head span,
.message-meta {
  color: var(--text-muted);
  font-size: 12px;
}

.hint,
.empty-record {
  padding: 80px 10px;
  text-align: center;
}

.record-column {
  display: flex;
  flex-direction: column;
}

.record-head {
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  gap: 10px;
  justify-content: space-between;
  margin: -2px 0 12px;
  padding-bottom: 12px;
}

.record-head h2 {
  margin: 0;
}

.record-body {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.message {
  display: grid;
  gap: 4px;
  margin-bottom: 12px;
  max-width: 72%;
}

.message.merchant {
  justify-items: end;
  margin-left: auto;
}

.message p {
  background: var(--clas-warm-100);
  border-radius: var(--radius-md);
  line-height: 1.55;
  margin: 0;
  padding: 9px 12px;
}

.message.merchant p {
  background: var(--color-primary);
  color: var(--text-primary);
}

@media (max-width: 1100px) {
  .message-layout {
    grid-template-columns: 1fr;
  }
}
</style>
