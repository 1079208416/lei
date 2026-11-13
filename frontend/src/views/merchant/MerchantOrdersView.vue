<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  Check,
  ChefHat,
  ClipboardList,
  Clock3,
  MapPin,
  Phone,
  RefreshCw,
  X,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Order } from '../../types'
import { money, orderStatusLabel, shortTime } from '../../lib/format'
import { showToast } from '../../lib/toast'
import EmptyState from '../../components/EmptyState.vue'

const orders = ref<Order[]>([])
const loading = ref(true)
const activeStatus = ref('WAITING_ACCEPT')
const actionId = ref<number | null>(null)

const tabs = [
  { value: 'WAITING_ACCEPT', label: '待接单' },
  { value: 'PREPARING', label: '制作中' },
  { value: 'WAITING_PICKUP', label: '待取餐' },
  { value: 'ALL', label: '全部' },
]

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const status = activeStatus.value === 'ALL' ? '' : `?status=${activeStatus.value}`
    orders.value = await api.get<Order[]>(`/api/merchant/orders${status}`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '订单加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function accept(order: Order) {
  actionId.value = order.id
  try {
    await api.post(`/api/merchant/orders/${order.id}/accept`)
    showToast('已接单，订单进入制作中', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '接单失败', 'error')
  } finally {
    actionId.value = null
  }
}

async function reject(order: Order) {
  if (!window.confirm('确认拒单并为用户退款？')) return
  actionId.value = order.id
  try {
    await api.post(`/api/merchant/orders/${order.id}/reject`, { reason: '当前订单较多，无法及时出餐' })
    showToast('已拒单并完成退款', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '拒单失败', 'error')
  } finally {
    actionId.value = null
  }
}

async function ready(order: Order) {
  actionId.value = order.id
  try {
    await api.post(`/api/merchant/orders/${order.id}/ready`)
    showToast('已标记出餐，等待平台派单', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '出餐操作失败', 'error')
  } finally {
    actionId.value = null
  }
}

function handleOrderUpdate() {
  void load(true)
}

watch(activeStatus, () => load())

onMounted(() => {
  void load()
  window.addEventListener('shike:order-updated', handleOrderUpdate)
})

onBeforeUnmount(() => {
  window.removeEventListener('shike:order-updated', handleOrderUpdate)
})
</script>

<template>
  <div class="portal-page">
    <div class="toolbar-row">
      <div class="segmented-control">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="{ active: activeStatus === tab.value }"
          @click="activeStatus = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>
      <button class="button ghost small" @click="load()">
        <RefreshCw :size="16" :class="{ spinning: loading }" /> 刷新
      </button>
    </div>

    <div v-if="loading" class="page-loading embedded">
      <span class="spinner"></span>
      正在读取订单
    </div>

    <div v-else-if="orders.length" class="merchant-order-list">
      <article v-for="order in orders" :key="order.id" class="merchant-order-card">
        <header>
          <div>
            <span class="order-number">{{ order.orderNo }}</span>
            <span class="status-badge" :class="`status-${order.status.toLowerCase()}`">
              {{ orderStatusLabel[order.status] }}
            </span>
          </div>
          <div class="order-time"><Clock3 :size="15" /> {{ shortTime(order.createdAt) }}</div>
        </header>

        <div class="merchant-order-body">
          <div class="merchant-order-items">
            <div v-for="item in order.items" :key="item.id" class="merchant-order-item">
              <span>{{ item.productName }} <small>{{ item.optionsSnapshot }}</small></span>
              <strong>×{{ item.quantity }}</strong>
            </div>
            <div class="merchant-order-note" v-if="order.remark">
              备注：{{ order.remark }}
            </div>
          </div>

          <div class="merchant-order-info">
            <span><MapPin :size="15" /> {{ order.addressSnapshot }}</span>
            <span><Phone :size="15" /> {{ order.contactName }} · {{ order.contactPhone }}</span>
            <span v-if="order.riderName">骑手：{{ order.riderName }}</span>
          </div>
        </div>

        <footer>
          <strong class="order-total">{{ money(order.totalAmount) }}</strong>
          <div class="order-actions">
            <template v-if="order.status === 'WAITING_ACCEPT'">
              <button
                class="button ghost small danger-text"
                :disabled="actionId === order.id"
                @click="reject(order)"
              >
                <X :size="15" /> 拒单
              </button>
              <button
                class="button primary small"
                :disabled="actionId === order.id"
                @click="accept(order)"
              >
                <Check :size="15" /> 接单
              </button>
            </template>
            <button
              v-else-if="order.status === 'PREPARING'"
              class="button primary small"
              :disabled="actionId === order.id"
              @click="ready(order)"
            >
              <ChefHat :size="15" /> 标记出餐
            </button>
            <span v-else-if="order.status === 'WAITING_PICKUP'" class="muted-label">
              等待平台派单
            </span>
          </div>
        </footer>
      </article>
    </div>

    <EmptyState
      v-else
      :title="activeStatus === 'WAITING_ACCEPT' ? '暂无待接订单' : '当前没有相关订单'"
      description="新订单到达后会自动出现在这里。"
    >
      <ClipboardList :size="20" />
    </EmptyState>
  </div>
</template>
