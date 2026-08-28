import { ref } from 'vue'
import { consultMerchant, getConversations, getMessagesWithMerchant, sendMessage } from '../api/chat'
import { getMerchant, getMyMerchant } from '../api/merchant'
import { currentRole } from '../api/session'

const sidebarOpen = ref(false)
const conversations = ref([])
const activeMerchantId = ref(null)
const activeUserId = ref(null)
const activeMessages = ref([])
const merchantCache = ref({})
const myMerchant = ref(null)
const polling = ref(null)
const loading = ref(false)

export function useChatStore() {
  async function loadConversations(options = {}) {
    try {
      conversations.value = await getConversations({ silent: options.silent })
    } catch {
      conversations.value = []
    }
  }

  async function ensureMerchantInfo(merchantId) {
    if (!merchantId || merchantCache.value[merchantId]) return
    try {
      merchantCache.value[merchantId] = await getMerchant(merchantId)
    } catch {
      merchantCache.value[merchantId] = { id: merchantId, merchantName: `商家 #${merchantId}` }
    }
  }

  async function openMerchantChat(merchantId) {
    activeMerchantId.value = merchantId
    activeUserId.value = null
    sidebarOpen.value = true
    await ensureMerchantInfo(merchantId)
    await loadConversations()
    await loadMessages()
    startPolling()
  }

  async function openReplyPanel() {
    const merchant = await loadMyMerchant()
    if (!merchant?.id) return
    activeMerchantId.value = merchant.id
    sidebarOpen.value = true
    await loadConversations()
    activeUserId.value = activeUserId.value || conversations.value[0]?.userId || null
    await loadMessages()
    startPolling()
  }

  async function loadMyMerchant() {
    if (myMerchant.value?.id) return myMerchant.value
    try {
      myMerchant.value = await getMyMerchant()
    } catch {
      myMerchant.value = null
    }
    return myMerchant.value
  }

  async function selectConversation(conversation) {
    if (currentRole() === 'MERCHANT') {
      activeMerchantId.value = conversation.merchantId
      activeUserId.value = conversation.userId
    } else {
      activeMerchantId.value = conversation.merchantId
      activeUserId.value = null
      await ensureMerchantInfo(conversation.merchantId)
    }
    await loadMessages()
    startPolling()
  }

  async function loadMessages(options = {}) {
    if (!activeMerchantId.value) {
      activeMessages.value = []
      return
    }
    if (options.showLoading !== false) {
      loading.value = true
    }
    try {
      activeMessages.value = await getMessagesWithMerchant(activeMerchantId.value, activeUserId.value, { silent: options.silent })
    } catch {
      if (options.showLoading !== false) {
        activeMessages.value = []
      }
    } finally {
      if (options.showLoading !== false) {
        loading.value = false
      }
    }
  }

  async function sendActive(content) {
    const text = content.trim()
    if (!text || !activeMerchantId.value) return
    if (currentRole() === 'MERCHANT') {
      if (!activeUserId.value) return
      await sendMessage({
        merchantId: activeMerchantId.value,
        userId: activeUserId.value,
        content: text
      })
    } else {
      await consultMerchant(activeMerchantId.value, text)
    }
    await loadMessages()
    await loadConversations()
  }

  function closeSidebar() {
    sidebarOpen.value = false
    stopPolling()
  }

  function startPolling() {
    stopPolling()
    polling.value = window.setInterval(() => {
      loadMessages({ showLoading: false, silent: true })
      loadConversations({ silent: true })
    }, 3000)
  }

  function stopPolling() {
    if (polling.value) {
      window.clearInterval(polling.value)
      polling.value = null
    }
  }

  return {
    sidebarOpen,
    conversations,
    activeMerchantId,
    activeUserId,
    activeMessages,
    merchantCache,
    myMerchant,
    loading,
    loadConversations,
    openMerchantChat,
    openReplyPanel,
    selectConversation,
    closeSidebar,
    sendActive,
    loadMessages
  }
}
