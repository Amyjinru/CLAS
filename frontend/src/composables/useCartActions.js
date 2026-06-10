import { ref } from 'vue'
import { addCart, removeCart, updateCart, deleteCartItem } from '../api/clas'

/**
 * 购物车通用操作 composable
 * CartView 和 MerchantDetailView 共享的购物车增删改逻辑
 */
export function useCartActions() {
  const cartLoading = ref(false)
  const cartMessage = ref('')
  const actionProductId = ref(null)

  function getErrorMessage(error, fallback = '操作失败') {
    return error?.response?.data?.message || error?.message || fallback
  }

  /** 添加商品到购物车（增加1件） */
  async function increaseItem(productId, productName) {
    actionProductId.value = productId
    try {
      await addCart({ productId, quantity: 1 })
      cartMessage.value = productName ? `${productName} 已加入购物车` : '已添加'
      return true
    } catch (error) {
      cartMessage.value = getErrorMessage(error, '请先登录')
      return false
    } finally {
      actionProductId.value = null
    }
  }

  /** 减少购物车商品数量（减少1件） */
  async function decreaseItem(productId) {
    actionProductId.value = productId
    try {
      await removeCart({ productId, quantity: 1 })
      return true
    } catch (error) {
      cartMessage.value = getErrorMessage(error, '操作失败')
      return false
    } finally {
      actionProductId.value = null
    }
  }

  /** 更新购物车商品数量（设置明确数量） */
  async function updateQuantity(productId, quantity) {
    const qty = Number(quantity)
    if (!Number.isInteger(qty) || qty < 1) {
      cartMessage.value = '数量至少为 1'
      return null
    }
    actionProductId.value = productId
    try {
      const result = await updateCart({ productId, quantity: qty })
      cartMessage.value = '数量已更新'
      return result
    } catch (error) {
      cartMessage.value = getErrorMessage(error, '更新数量失败')
      return null
    } finally {
      actionProductId.value = null
    }
  }

  /** 从购物车移除商品 */
  async function removeItem(productId) {
    actionProductId.value = productId
    try {
      const result = await deleteCartItem(productId)
      cartMessage.value = '商品已从购物车删除'
      return result
    } catch (error) {
      cartMessage.value = getErrorMessage(error, '删除失败')
      return null
    } finally {
      actionProductId.value = null
    }
  }

  /** 清除指定数量（用于MerchantDetailView的完全移除） */
  async function removeAll(productId, quantity) {
    actionProductId.value = productId
    try {
      await removeCart({ productId, quantity })
      return true
    } catch (error) {
      cartMessage.value = getErrorMessage(error, '操作失败')
      return false
    } finally {
      actionProductId.value = null
    }
  }

  function clearMessage() {
    cartMessage.value = ''
  }

  return {
    cartLoading,
    cartMessage,
    actionProductId,
    increaseItem,
    decreaseItem,
    updateQuantity,
    removeItem,
    removeAll,
    clearMessage
  }
}
