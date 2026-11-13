<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { Bike, House, MapPin, ReceiptText, UserRound, UtensilsCrossed } from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()

const navItems = [
  { to: '/', label: '首页', icon: House },
  { to: '/orders', label: '订单', icon: ReceiptText },
  { to: '/profile', label: '我的', icon: UserRound },
]

const isDetail = computed(() =>
  ['merchant-detail', 'checkout', 'order-detail'].includes(String(route.name)),
)
</script>

<template>
  <div class="user-app">
    <header class="user-header">
      <div class="user-header-inner">
        <RouterLink to="/" class="brand">
          <span class="brand-mark"><UtensilsCrossed :size="20" /></span>
          <span class="brand-copy">
            <strong>食刻</strong>
            <small>SHIKE DELIVERY</small>
          </span>
        </RouterLink>

        <div v-if="!isDetail" class="header-location">
          <MapPin :size="17" />
          <div>
            <small>送至</small>
            <strong>云栖公寓 2 幢</strong>
          </div>
        </div>

        <nav class="desktop-user-nav">
          <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
            {{ item.label }}
          </RouterLink>
        </nav>

        <RouterLink to="/profile" class="header-avatar">
          {{ auth.account?.name?.slice(0, 1) || '食' }}
        </RouterLink>
      </div>
    </header>

    <main class="user-main" :class="{ 'user-main-detail': isDetail }">
      <RouterView />
    </main>

    <nav class="mobile-user-nav">
      <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="mobile-nav-item">
        <component :is="item.icon" :size="21" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div v-if="isDetail" class="delivery-note">
      <Bike :size="15" />
      平均 30 分钟送达
    </div>
  </div>
</template>
