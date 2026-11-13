<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshCw } from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Order } from '../../types'
import { showToast } from '../../lib/toast'
import OrderCard from '../../components/OrderCard.vue'
import EmptyState from '../../components/EmptyState.vue'

const router = useRouter()
const orders = ref<Order[]>([])
const loading = ref(true)
const activeTab = ref<'ALL' | 'ACTIVE' | 'COMPLETED'>('ALL')

const tabs = [
  { value: 'ALL', label: '全部' },
  { value: 'ACTIVE', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
] as const

const activeStatuses = new Set([
  'WAITING_PAYMENT',
  'WAITING_ACCEPT',
  'PREPARING',
  'WAITING_PICKUP',
  'DELIVERING',
])

const filteredOrders = () => {
  if (activeTab.value === 'ACTIVE') {
    return orders.value.filter((order) => activeStatuses.has(order.status))
  }
  if (activeTab.value === 'COMPLETED') {
    return orders.value.filter((order) => order.status === 'COMPLETED')
  }
  return orders.value
}

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    orders.value = await api.get<Order[]>('/api/user/orders')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '订单加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleOrderUpdate() {
  void load(true)
}

onMounted(() => {
  void load()
  window.addEventListener('shike:order-updated', handleOrderUpdate)
})

onBeforeUnmount(() => {
  window.removeEventListener('shike:order-updated', handleOrderUpdate)
})
</script>

<template>
  <div class="orders-page">
    <header class="page-heading compact-heading">
      <div>
        <span class="eyebrow">我的订单</span>
        <h1>每一餐，都有进度。</h1>
      </div>
      <button class="icon-button" title="刷新订单" @click="load()">
        <RefreshCw :size="18" :class="{ spinning: loading }" />
      </button>
    </header>

    <div class="segmented-control">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="page-loading embedded">
      <span class="spinner"></span>
      正在读取订单
    </div>

    <div v-else-if="filteredOrders().length" class="orders-list">
      <OrderCard
        v-for="order in filteredOrders()"
        :key="order.id"
        :order="order"
        @open="(item) => router.push(`/orders/${item.id}`)"
      />
    </div>

    <EmptyState v-else title="还没有相关订单" description="去附近商家选一份喜欢的餐品吧。">
      <RouterLink to="/" class="button primary small">去点餐</RouterLink>
    </EmptyState>
  </div>
</template>
