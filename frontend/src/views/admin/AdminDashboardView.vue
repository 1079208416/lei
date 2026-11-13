<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  Bike,
  CircleDollarSign,
  ClipboardList,
  RefreshCw,
  RotateCcw,
  Store,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Order, RiderSummary } from '../../types'
import { money, orderStatusLabel, shortTime } from '../../lib/format'
import { showToast } from '../../lib/toast'

interface Dashboard {
  orderCount: number
  gmv: number
  pendingDispatch: number
  activeDeliveries: number
  refundRequests: number
  merchantCount: number
}

const dashboard = ref<Dashboard | null>(null)
const dispatchOrders = ref<Order[]>([])
const riders = ref<RiderSummary[]>([])
const loading = ref(true)
const selectedRider = reactive<Record<number, number>>({})
const assigning = ref<number | null>(null)

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    ;[dashboard.value, dispatchOrders.value, riders.value] = await Promise.all([
      api.get<Dashboard>('/api/admin/dashboard'),
      api.get<Order[]>('/api/admin/orders?status=WAITING_PICKUP'),
      api.get<RiderSummary[]>('/api/admin/riders'),
    ])
  } catch (error) {
    showToast(error instanceof Error ? error.message : '平台数据加载失败', 'error')
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
    showToast('骑手已分配', 'success')
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
  <div v-if="loading" class="page-loading">
    <span class="spinner"></span>
    正在读取平台数据
  </div>

  <div v-else-if="dashboard" class="dashboard-page">
    <section class="metrics-grid admin-metrics">
      <article class="metric-card accent">
        <span class="metric-icon"><ClipboardList :size="20" /></span>
        <small>今日订单</small>
        <strong>{{ dashboard.orderCount }}</strong>
        <em>平台累计交易</em>
      </article>
      <article class="metric-card">
        <span class="metric-icon"><CircleDollarSign :size="20" /></span>
        <small>今日成交额</small>
        <strong>{{ money(dashboard.gmv) }}</strong>
        <em>{{ dashboard.merchantCount }} 家商户</em>
      </article>
      <article class="metric-card">
        <span class="metric-icon"><Store :size="20" /></span>
        <small>待派单</small>
        <strong>{{ dashboard.pendingDispatch }}</strong>
        <em>需要分配骑手</em>
      </article>
      <article class="metric-card">
        <span class="metric-icon"><Bike :size="20" /></span>
        <small>配送中</small>
        <strong>{{ dashboard.activeDeliveries }}</strong>
        <em>骑手正在路上</em>
      </article>
      <article class="metric-card warning">
        <span class="metric-icon"><RotateCcw :size="20" /></span>
        <small>待处理退款</small>
        <strong>{{ dashboard.refundRequests }}</strong>
        <em>等待平台审核</em>
      </article>
    </section>

    <div class="dashboard-columns wide-left">
      <section class="panel">
        <header class="panel-title">
          <div>
            <h2>待派单订单</h2>
            <span>{{ dispatchOrders.length }} 笔</span>
          </div>
          <RouterLink to="/admin-console/orders" class="text-button">
            全部调度 <ArrowRight :size="15" />
          </RouterLink>
        </header>

        <div v-if="dispatchOrders.length" class="dispatch-list">
          <article v-for="order in dispatchOrders" :key="order.id" class="dispatch-row">
            <div class="dispatch-order">
              <span class="status-badge status-waiting_pickup">
                {{ orderStatusLabel[order.status] }}
              </span>
              <strong>{{ order.merchantName }}</strong>
              <small>{{ order.orderNo }} · {{ shortTime(order.createdAt) }}</small>
            </div>
            <div class="dispatch-address">
              <span>{{ order.addressSnapshot }}</span>
              <small>{{ order.contactName }} · {{ order.contactPhone }}</small>
            </div>
            <select v-model.number="selectedRider[order.id]">
              <option v-for="rider in riders" :key="rider.id" :value="rider.id">
                {{ rider.name }} · 进行中 {{ rider.activeTasks }}
              </option>
            </select>
            <button
              class="button primary small"
              :disabled="assigning === order.id"
              @click="assign(order)"
            >
              <Bike :size="15" /> 派单
            </button>
          </article>
        </div>
        <div v-else class="inline-empty">暂时没有待派单订单</div>
      </section>

      <section class="panel quick-panel">
        <header class="panel-title">
          <div><h2>运营快捷入口</h2></div>
          <button class="icon-button subtle" title="刷新" @click="load()">
            <RefreshCw :size="17" />
          </button>
        </header>
        <RouterLink to="/admin-console/orders" class="quick-action">
          <span><ClipboardList :size="20" /></span>
          <div><strong>订单调度</strong><small>查询订单与人工派单</small></div>
          <ArrowRight :size="17" />
        </RouterLink>
        <RouterLink to="/admin-console/refunds" class="quick-action">
          <span><RotateCcw :size="20" /></span>
          <div><strong>售后处理</strong><small>{{ dashboard.refundRequests }} 笔待审核</small></div>
          <ArrowRight :size="17" />
        </RouterLink>
        <RouterLink to="/admin-console/merchants" class="quick-action">
          <span><Store :size="20" /></span>
          <div><strong>商户管理</strong><small>查看营业状态与商户信息</small></div>
          <ArrowRight :size="17" />
        </RouterLink>
      </section>
    </div>
  </div>
</template>
