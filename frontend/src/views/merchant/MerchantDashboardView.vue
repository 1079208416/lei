<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  ArrowRight,
  CircleDollarSign,
  ClipboardList,
  Clock3,
  Power,
  RefreshCw,
  ShoppingBag,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { MerchantSummary, Order } from '../../types'
import { money, orderStatusLabel, shortTime } from '../../lib/format'
import { showToast } from '../../lib/toast'

interface Dashboard {
  merchant: MerchantSummary
  orderCount: number
  pendingAccept: number
  activeOrders: number
  completedOrders: number
  revenue: number
}

const dashboard = ref<Dashboard | null>(null)
const orders = ref<Order[]>([])
const loading = ref(true)
const toggling = ref(false)

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    ;[dashboard.value, orders.value] = await Promise.all([
      api.get<Dashboard>('/api/merchant/dashboard'),
      api.get<Order[]>('/api/merchant/orders'),
    ])
  } catch (error) {
    showToast(error instanceof Error ? error.message : '经营数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function toggleStore() {
  if (!dashboard.value) return
  toggling.value = true
  try {
    const merchant = await api.patch<MerchantSummary>('/api/merchant/store/status', {
      open: !dashboard.value.merchant.open,
    })
    dashboard.value.merchant = merchant
    showToast(merchant.open ? '店铺已开始营业' : '店铺已暂停接单', 'success')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '店铺状态更新失败', 'error')
  } finally {
    toggling.value = false
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
    正在读取经营数据
  </div>

  <div v-else-if="dashboard" class="dashboard-page">
    <section class="merchant-status-banner" :class="{ closed: !dashboard.merchant.open }">
      <div>
        <span class="eyebrow">{{ dashboard.merchant.open ? '营业中' : '休息中' }}</span>
        <h2>{{ dashboard.merchant.name }}</h2>
        <p>{{ dashboard.merchant.description }}</p>
      </div>
      <button class="button status-toggle" :disabled="toggling" @click="toggleStore">
        <Power :size="17" />
        {{ dashboard.merchant.open ? '暂停接单' : '开始营业' }}
      </button>
    </section>

    <section class="metrics-grid">
      <article class="metric-card accent">
        <span class="metric-icon"><ShoppingBag :size="20" /></span>
        <small>今日订单</small>
        <strong>{{ dashboard.orderCount }}</strong>
        <em>待接单 {{ dashboard.pendingAccept }}</em>
      </article>
      <article class="metric-card">
        <span class="metric-icon"><CircleDollarSign :size="20" /></span>
        <small>今日成交</small>
        <strong>{{ money(dashboard.revenue) }}</strong>
        <em>已完成 {{ dashboard.completedOrders }} 单</em>
      </article>
      <article class="metric-card">
        <span class="metric-icon"><ClipboardList :size="20" /></span>
        <small>履约中</small>
        <strong>{{ dashboard.activeOrders }}</strong>
        <em>待出餐、待配送与配送中</em>
      </article>
      <article class="metric-card">
        <span class="metric-icon"><Clock3 :size="20" /></span>
        <small>平均时长</small>
        <strong>{{ dashboard.merchant.etaMinutes }}<span>分钟</span></strong>
        <em>预计出餐与配送</em>
      </article>
    </section>

    <div class="dashboard-columns">
      <section class="panel">
        <header class="panel-title">
          <div>
            <h2>最新订单</h2>
            <span>{{ orders.length }} 笔</span>
          </div>
          <RouterLink to="/merchant-console/orders" class="text-button">
            全部订单 <ArrowRight :size="15" />
          </RouterLink>
        </header>
        <div v-if="orders.length" class="compact-order-list">
          <article v-for="order in orders.slice(0, 5)" :key="order.id" class="compact-order-row">
            <div>
              <strong>{{ order.items[0]?.productName }}{{ order.items.length > 1 ? ` 等 ${order.items.length} 件` : '' }}</strong>
              <small>{{ order.orderNo }} · {{ shortTime(order.createdAt) }}</small>
            </div>
            <span class="status-badge" :class="`status-${order.status.toLowerCase()}`">
              {{ orderStatusLabel[order.status] }}
            </span>
            <b>{{ money(order.totalAmount) }}</b>
          </article>
        </div>
        <div v-else class="inline-empty">今天还没有订单</div>
      </section>

      <section class="panel quick-panel">
        <header class="panel-title">
          <div><h2>快捷操作</h2></div>
          <button class="icon-button subtle" title="刷新" @click="load()">
            <RefreshCw :size="17" />
          </button>
        </header>
        <RouterLink to="/merchant-console/orders" class="quick-action">
          <span><ClipboardList :size="20" /></span>
          <div><strong>处理订单</strong><small>{{ dashboard.pendingAccept }} 笔待接单</small></div>
          <ArrowRight :size="17" />
        </RouterLink>
        <RouterLink to="/merchant-console/menu" class="quick-action">
          <span><ShoppingBag :size="20" /></span>
          <div><strong>管理菜单</strong><small>调整价格与售罄状态</small></div>
          <ArrowRight :size="17" />
        </RouterLink>
      </section>
    </div>
  </div>
</template>
