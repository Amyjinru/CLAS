import { ElMessageBox } from 'element-plus'

export function useConfirmAction() {
  async function confirmAction(message, action, options = {}) {
    await ElMessageBox.confirm(message, options.title || '确认操作', {
      confirmButtonText: options.confirmText || '确认',
      cancelButtonText: options.cancelText || '取消',
      type: options.type || 'warning'
    })
    return action()
  }

  return { confirmAction }
}
