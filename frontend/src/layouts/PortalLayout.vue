<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import {
  Bike,
  BookOpen,
  ClipboardList,
  LayoutDashboard,
  LogOut,
  RotateCcw,
  Store,
  UtensilsCrossed,
} from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const navByRole = {
  MERCHANT: [
    { to: '/merchant-console', label: '经营概览', icon: LayoutDashboard },
    { to: '/merchant-console/orders', label: '订单处理', icon: ClipboardList },
    { to: '/merchant-console/menu', label: '菜单管理', icon: BookOpen },
  ],
  RIDER: [{ to: '/rider-console', label: '配送任务', icon: Bike }],
  ADMIN: [
    { to: '/admin-console', label: '平台概览', icon: LayoutDashboard },
    { to: '/admin-console/orders', label: '订单调度', icon: ClipboardList },
    { to: '/admin-console/merchants', label: '商户管理', icon: Store },
    { to: '/admin-console/refunds', label: '售后处理', icon: RotateCcw },
  ],
} as const

const navItems = computed(() =>
  auth.account ? navByRole[auth.account.role as keyof typeof navByRole] : [],
)

const workspaceName = computed(() => {
  if (route.path.includes('admin')) return '平台运营中心'
  if (route.path.includes('rider')) return '骑手工作台'
  return '商家工作台'
})

const roleName = computed(() => {
  if (route.path.includes('admin')) return '运营'
  if (route.path.includes('rider')) return '骑手'
  return '商家'
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="portal-shell">
    <aside class="portal-sidebar">
      <RouterLink :to="auth.home" class="brand portal-brand">
        <span class="brand-mark"><UtensilsCrossed :size="20" /></span>
        <span class="brand-copy">
          <strong>食刻</strong>
          <small>{{ workspaceName }}</small>
        </span>
      </RouterLink>

      <nav class="portal-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="portal-nav-item"
          :class="{ active: route.path === item.to }"
        >
          <component :is="item.icon" :size="19" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="portal-account">
        <span class="portal-avatar">{{ auth.account?.name?.slice(0, 1) }}</span>
        <div>
          <strong>{{ auth.account?.name }}</strong>
          <small>{{ roleName }}</small>
        </div>
        <button class="icon-button subtle" title="退出登录" @click="logout">
          <LogOut :size="18" />
        </button>
      </div>
    </aside>

    <section class="portal-workspace">
      <header class="portal-topbar">
        <div>
          <span class="eyebrow">{{ workspaceName }}</span>
          <h1>{{ navItems.find((item) => item.to === route.path)?.label || workspaceName }}</h1>
        </div>
        <div class="portal-live">
          <span class="live-dot"></span>
          实时连接
        </div>
      </header>

      <main class="portal-content">
        <RouterView />
      </main>
    </section>

    <nav class="portal-mobile-nav">
      <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
        <component :is="item.icon" :size="20" />
        <span>{{ item.label }}</span>
      </RouterLink>
      <button @click="logout">
        <LogOut :size="20" />
        <span>退出</span>
      </button>
    </nav>
  </div>
</template>
