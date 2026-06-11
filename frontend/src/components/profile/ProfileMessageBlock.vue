<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { notificationTarget } from '../../utils/notificationTarget'

const router = useRouter()

const props = defineProps({
  notifications: { type: Array, default: () => [] },
  unreadCount: { type: Number, default: 0 },
  markingAllRead: { type: Boolean, default: false },
  deletingAll: { type: Boolean, default: false },
  actionId: { type: [Number, String], default: null },
  conversations: { type: Array, default: () => [] },
  merchantCache: { type: Object, default: () => ({}) },
  chatLoading: { type: Boolean, default: false }
})

const emit = defineEmits(['read', 'read-all', 'remove', 'clear', 'open-chat'])

const activeSubTab = ref('notifications')

function formatChatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

function merchantName(merchantId) {
  return props.merchantCache[merchantId]?.merchantName || `商家 #${merchantId}`
}

function merchantLogo(merchantId) {
  return props.merchantCache[merchantId]?.logo || ''
}

function merchantInitial(merchantId) {
  return merchantName(merchantId).slice(0, 1)
}

function handleNotificationClick(item) {
  const target = notificationTarget(item)
  if (!target) {
    ElMessage.info('这条通知暂无可打开的详情页')
    return
  }
  if (!item.readFlag) {
    emit('read', item.id)
  }
  router.push(target).catch((err) => {
    ElMessage.error(err?.response?.data?.message || err?.message || '打开失败')
  })
}
</script>

<template>
  <div>
    <div class="section-head">
      <div>
        <h2>消息</h2>
        <p>{{ unreadCount }} 条未读通知</p>
      </div>
      <div class="row-actions">
        <RouterLink class="button secondary" to="/user/announcements">平台公告</RouterLink>
      </div>
    </div>

    <el-tabs v-model="activeSubTab" class="message-sub-tabs">
      <el-tab-pane label="系统通知" name="notifications">
        <div class="sub-tab-toolbar">
          <el-button type="primary" plain :disabled="!unreadCount" :loading="markingAllRead" @click="emit('read-all')">全部已读</el-button>
          <el-button type="danger" plain :disabled="!notifications.length" :loading="deletingAll" @click="emit('clear')">清空通知</el-button>
        </div>

        <el-empty v-if="!notifications.length" description="暂无通知" />

        <div v-else class="list-stack">
          <article
            v-for="item in notifications"
            :key="item.id"
            class="list-row notification-row"
            :class="{ unread: !item.readFlag, clickable: !!notificationTarget(item) }"
            tabindex="0"
            @click="handleNotificationClick(item)"
            @keydown.enter.prevent="handleNotificationClick(item)"
          >
            <div>
              <div class="row-title">
                <strong>{{ item.title }}</strong>
                <el-tag v-if="!item.readFlag" type="danger" size="small">未读</el-tag>
                <el-tag v-else type="info" size="small">已读</el-tag>
              </div>
              <p>{{ item.content }}</p>
            </div>
            <div class="row-actions">
              <el-button v-if="!item.readFlag" text type="primary" :loading="actionId === item.id" @click.stop="emit('read', item.id)">标记已读</el-button>
              <el-button type="danger" plain @click.stop="emit('remove', item.id)">删除</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="客服消息" name="chats">
        <div v-loading="chatLoading">
          <el-empty v-if="!conversations.length" description="暂无客服消息" />
          <div v-else class="list-stack">
            <article
              v-for="conv in conversations"
              :key="conv.merchantId"
              class="list-row chat-conversation-row"
              @click="emit('open-chat', conv.merchantId)"
            >
              <span class="chat-merchant-avatar" :style="merchantLogo(conv.merchantId) ? { backgroundImage: `url(${merchantLogo(conv.merchantId)})` } : null">
                <template v-if="!merchantLogo(conv.merchantId)">{{ merchantInitial(conv.merchantId) }}</template>
              </span>
              <div class="chat-conversation-meta">
                <div class="chat-conversation-head">
                  <strong>{{ merchantName(conv.merchantId) }}</strong>
                  <small>{{ formatChatTime(conv.lastMessageTime) }}</small>
                </div>
                <p>{{ conv.lastMessage || '暂无消息' }}</p>
              </div>
            </article>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.section-head {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-head h2 { margin: 0; }
.section-head p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }

.message-sub-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.message-sub-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.sub-tab-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.list-stack {
  display: grid;
  gap: 12px;
}

.list-row {
  align-items: center;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  display: flex;
  justify-content: space-between;
  padding: 14px 16px;
}
.list-row p { color: var(--text-secondary); font-size: 13px; margin: 6px 0 0; }

.row-title {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.row-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.notification-row.unread {
  background: var(--color-primary-light);
}

.notification-row.clickable {
  cursor: pointer;
}

.notification-row.clickable:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.notification-row.clickable:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.chat-conversation-row {
  cursor: pointer;
  gap: 12px;
}

.chat-conversation-row:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.chat-merchant-avatar {
  align-items: center;
  background: linear-gradient(135deg, #f97316, #0f766e);
  background-position: center;
  background-size: cover;
  border-radius: 50%;
  color: #fff;
  display: flex;
  flex: 0 0 auto;
  font-size: 16px;
  font-weight: 700;
  height: 40px;
  justify-content: center;
  width: 40px;
}

.chat-conversation-meta {
  display: grid;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.chat-conversation-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.chat-conversation-head strong {
  color: var(--text-primary);
  font-size: 14px;
}

.chat-conversation-head small {
  color: #94a3b8;
  font-size: 12px;
  white-space: nowrap;
}

.chat-conversation-meta p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0;
}

@media (max-width: 900px) {
  .list-row { align-items: flex-start; flex-direction: column; }
  .row-actions { justify-content: flex-start; }
}
@media (max-width: 640px) {
  .section-head { display: grid; }
}
</style>
