import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '../api/client'
import type { Account, Role } from '../types'

const roleHome: Record<Role, string> = {
  USER: '/',
  MERCHANT: '/merchant-console',
  RIDER: '/rider-console',
  ADMIN: '/admin-console',
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('shike_token') ?? '')
  const account = ref<Account | null>(
    JSON.parse(localStorage.getItem('shike_account') ?? 'null') as Account | null,
  )
  const initialized = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value && account.value))
  const home = computed(() => (account.value ? roleHome[account.value.role] : '/login'))

  function setSession(nextToken: string, nextAccount: Account) {
    token.value = nextToken
    account.value = nextAccount
    localStorage.setItem('shike_token', nextToken)
    localStorage.setItem('shike_account', JSON.stringify(nextAccount))
  }

  async function initialize() {
    if (initialized.value) return
    initialized.value = true
    if (!token.value) return
    try {
      const me = await api.get<Account>('/api/auth/me')
      account.value = me
      localStorage.setItem('shike_account', JSON.stringify(me))
    } catch {
      logout()
    }
  }

  async function demoLogin(role: Role) {
    const result = await api.post<{ token: string; account: Account }>(
      `/api/auth/demo/${role.toLowerCase()}`,
    )
    setSession(result.token, result.account)
    return result.account
  }

  async function login(phone: string, password: string) {
    const result = await api.post<{ token: string; account: Account }>('/api/auth/login', {
      phone,
      password,
    })
    setSession(result.token, result.account)
    return result.account
  }

  function logout() {
    token.value = ''
    account.value = null
    localStorage.removeItem('shike_token')
    localStorage.removeItem('shike_account')
  }

  return {
    token,
    account,
    initialized,
    isLoggedIn,
    home,
    initialize,
    demoLogin,
    login,
    logout,
    roleHome,
  }
})
