<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Bike, RefreshCw, Search } from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Order, RiderSummary } from '../../types'
import { money, orderStatusLabel, shortTime } from '../../lib/format'
import { showToast } from '../../lib/toast'

const orders = ref<Order[]>([])
const riders = ref<RiderSummary[]>([])
const loading = ref(true)
const activeStatus = ref('ALL')
const keyword = ref('')
const selectedRider = reactive<Record<number, number>>({})
const assigning = ref<number | null>(null)

const statuses = [
  { value: 'ALL', label: '全部' },
  { value: 'WAITING_ACCEPT', label: '待接单' },
  { value: 'PREPARING', label: '制作中' },
  { value: 'WAITING_PICKUP', label: '待取餐' },
  { value: 'DELIVERING', label: '配送中' },
  { value: 'COMPLETED', label: '已完成' },
]

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const params = new URLSearchParams()
    if (activeStatus.value !== 'ALL') params.set('status', activeStatus.value)
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    const query = params.size ? `?${params}` : ''
    ;[orders.value, riders.value] = await Promise.all([
      api.get<Order[]>(`/api/admin/orders${query}`),
      api.get<RiderSummary[]>('/api/admin/riders'),
    ])
  } catch (error) {
    showToast(error instanceof Error ? error.message : '订单加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function assign(order: Order) {
  const riderId = selectedRider[order.id] ?? riders.value[0]?.id
  if (!riderId) {
    showToast('暂无可派单骑手', 'error')
    return
  }
  assigning.value = order.id
  try {
    await api.post(`/api/admin/orders/${order.id}/assign`, { riderId })
    showToast('派单成功', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '派单失败', 'error')
  } finally {
    assigning.value = null
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
  <div class="portal-page">
    <div class="toolbar-row admin-order-toolbar">
      <form class="table-search" @submit.prevent="load()">
        <Search :size="17" />
        <input v-model="keyword" placeholder="搜索订单号或商户" />
        <button>搜索</button>
      </form>
      <button class="button ghost small" @click="load()">
        <RefreshCw :size="16" :class="{ spinning: loading }" /> 刷新
      </button>
    </div>

    <div class="filter-scroll">
      <div class="segmented-control">
        <button
          v-for="status in statuses"
          :key="status.value"
          :class="{ active: activeStatus === status.value }"
          @click="activeStatus = status.value; load()"
        >
          {{ status.label }}
        </button>
      </div>
    </div>

    <section class="panel table-panel">
      <div v-if="loading" class="page-loading embedded">
        <span class="spinner"></span>
        正在读取订单
      </div>
      <div v-else class="data-table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>订单</th>
              <th>商户 / 餐品</th>
              <th>收货信息</th>
              <th>金额</th>
              <th>状态</th>
              <th>骑手 / 派单</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td>
                <strong>{{ order.orderNo }}</strong>
                <small>{{ shortTime(order.createdAt) }}</small>
              </td>
              <td>
                <strong>{{ order.merchantName }}</strong>
                <small>{{ order.items.map((item) => item.productName).join('、') }}</small>
              </td>
              <td>
                <strong>{{ order.addressSnapshot }}</strong>
                <small>{{ order.contactName }} · {{ order.contactPhone }}</small>
              </td>
              <td><strong>{{ money(order.totalAmount) }}</strong></td>
              <td>
                <span class="status-badge" :class="`status-${order.status.toLowerCase()}`">
                  {{ orderStatusLabel[order.status] }}
                </span>
              </td>
              <td>
                <div v-if="order.status === 'WAITING_PICKUP'" class="table-dispatch">
                  <select v-model.number="selectedRider[order.id]">
                    <option v-for="rider in riders" :key="rider.id" :value="rider.id">
                      {{ rider.name }}
                    </option>
                  </select>
                  <button
                    class="button primary tiny"
                    :disabled="assigning === order.id"
                    @click="assign(order)"
                  >
                    <Bike :size="14" /> 派单
                  </button>
                </div>
                <span v-else>{{ order.riderName || '未分配' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!orders.length" class="inline-empty">没有匹配的订单</div>
      </div>
    </section>
  </div>
</template>
