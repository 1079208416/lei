import { onBeforeUnmount, onMounted, watch } from 'vue'
import { useAuthStore } from '../stores/auth'
import { showToast } from '../lib/toast'

export function useRealtime() {
  const auth = useAuthStore()
  let socket: WebSocket | null = null
  let reconnectTimer = 0
  let stopped = false

  function disconnect() {
    stopped = true
    window.clearTimeout(reconnectTimer)
    socket?.close()
    socket = null
  }

  function connect() {
    stopped = false
    if (!auth.token) return
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = import.meta.env.VITE_WS_HOST || window.location.host
    socket = new WebSocket(`${protocol}//${host}/ws?token=${encodeURIComponent(auth.token)}`)
    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as { message?: string; orderId?: number }
        if (payload.message) showToast(payload.message, 'info')
        window.dispatchEvent(
          new CustomEvent('shike:order-updated', { detail: payload }),
        )
      } catch {
        // Ignore malformed notifications and keep the connection open.
      }
    }
    socket.onclose = () => {
      socket = null
      if (!stopped) {
        reconnectTimer = window.setTimeout(connect, 3000)
      }
    }
  }

  watch(
    () => auth.token,
    (token) => {
      socket?.close()
      if (token) connect()
    },
  )

  onMounted(() => {
    if (auth.token) connect()
  })

  onBeforeUnmount(disconnect)
}
