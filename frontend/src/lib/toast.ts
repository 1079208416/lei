import { reactive } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

export interface ToastItem {
  id: number
  message: string
  type: ToastType
}

export const toastState = reactive<{ items: ToastItem[] }>({ items: [] })

let toastId = 0

export function showToast(message: string, type: ToastType = 'info') {
  const id = ++toastId
  toastState.items.push({ id, message, type })
  window.setTimeout(() => {
    const index = toastState.items.findIndex((item) => item.id === id)
    if (index >= 0) {
      toastState.items.splice(index, 1)
    }
  }, 3200)
}
