<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  Bike,
  CheckCircle2,
  Clock3,
  MapPin,
  Navigation,
  PackageCheck,
  Phone,
  RefreshCw,
  Store,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Order } from '../../types'
import { deliveryStatusLabel, money, shortTime } from '../../lib/format'
import { showToast } from '../../lib/toast'
import EmptyState from '../../components/EmptyState.vue'

interface RiderDashboard {
  activeTasks: number
  deliveredTasks: number
  totalTasks: number
}

const dashboard = ref<RiderDashboard | null>(null)
const tasks = ref<Order[]>([])
const loading = ref(true)
const activeTab = ref<'ACTIVE' | 'COMPLETED'>('ACTIVE')
const actionId = ref<number | null>(null)

const filteredTasks = computed(() => {
  if (activeTab.value === 'COMPLETED') {
    return tasks.value.filter((task) => task.taskStatus === 'DELIVERED')
  }
  return tasks.value.filter((task) => ['ASSIGNED', 'PICKED_UP'].includes(task.taskStatus ?? ''))
})

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    ;[dashboard.value, tasks.value] = await Promise.all([
      api.get<RiderDashboard>('/api/rider/dashboard'),
      api.get<Order[]>('/api/rider/tasks'),
    ])
  } catch (error) {
    showToast(error instanceof Error ? error.message : '配送任务加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function pickup(task: Order) {
  actionId.value = task.id
  try {
    await api.post(`/api/rider/tasks/${task.taskId}/pickup`)
    showToast('已确认取餐，开始配送', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '取餐确认失败', 'error')
  } finally {
    actionId.value = null
  }
}

async function deliver(task: Order) {
  actionId.value = task.id
  try {
    await api.post(`/api/rider/tasks/${task.taskId}/deliver`)
    showToast('订单已送达，配送完成', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '送达确认失败', 'error')
  } finally {
    actionId.value = null
  }
}

function openMap(address: string) {
  window.open(
    `https://uri.amap.com/search?keyword=${encodeURIComponent(address)}&src=shike&callnative=1`,
    '_blank',
  )
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
    正在读取配送任务
  </div>

  <div v-else class="rider-page">
    <section class="rider-stats">
      <article>
        <span><Bike :size="20" /></span>
        <div><small>进行中任务</small><strong>{{ dashboard?.activeTasks ?? 0 }}</strong></div>
      </article>
      <article>
        <span><CheckCircle2 :size="20" /></span>
        <div><small>今日已送达</small><strong>{{ dashboard?.deliveredTasks ?? 0 }}</strong></div>
      </article>
      <article>
        <span><Clock3 :size="20" /></span>
        <div><small>今日总任务</small><strong>{{ dashboard?.totalTasks ?? 0 }}</strong></div>
      </article>
    </section>

    <div class="toolbar-row">
      <div class="segmented-control">
        <button :class="{ active: activeTab === 'ACTIVE' }" @click="activeTab = 'ACTIVE'">
          进行中
        </button>
        <button :class="{ active: activeTab === 'COMPLETED' }" @click="activeTab = 'COMPLETED'">
          已完成
        </button>
      </div>
      <button class="button ghost small" @click="load()">
        <RefreshCw :size="16" :class="{ spinning: loading }" /> 刷新
      </button>
    </div>

    <div v-if="filteredTasks.length" class="rider-task-list">
      <article v-for="task in filteredTasks" :key="task.taskId" class="rider-task-card">
        <header>
          <div>
            <span class="status-badge status-delivering">
              {{ deliveryStatusLabel[task.taskStatus ?? ''] }}
            </span>
            <span class="order-number">{{ task.orderNo }}</span>
          </div>
          <span>{{ shortTime(task.assignedAt) }}</span>
        </header>

        <div class="rider-route">
          <div class="route-stop pickup">
            <span><Store :size="17" /></span>
            <div>
              <small>取餐</small>
              <strong>{{ task.merchantName }}</strong>
            </div>
          </div>
          <div class="route-line"></div>
          <div class="route-stop dropoff">
            <span><MapPin :size="17" /></span>
            <div>
              <small>送达</small>
              <strong>{{ task.addressSnapshot }}</strong>
              <em>{{ task.contactName }} · {{ task.contactPhone }}</em>
            </div>
          </div>
        </div>

        <div class="rider-order-summary">
          <span>{{ task.items.map((item) => item.productName).join('、') }}</span>
          <strong>{{ money(task.totalAmount) }}</strong>
        </div>

        <div v-if="task.remark" class="rider-note">备注：{{ task.remark }}</div>

        <footer>
          <div class="rider-contact-actions">
            <a :href="`tel:${task.contactPhone}`" class="button ghost small">
              <Phone :size="15" /> 联系用户
            </a>
            <button class="button ghost small" @click="openMap(task.addressSnapshot)">
              <Navigation :size="15" /> 导航
            </button>
          </div>
          <button
            v-if="task.taskStatus === 'ASSIGNED'"
            class="button primary"
            :disabled="actionId === task.id"
            @click="pickup(task)"
          >
            <PackageCheck :size="17" /> 确认取餐
          </button>
          <button
            v-else-if="task.taskStatus === 'PICKED_UP'"
            class="button primary"
            :disabled="actionId === task.id"
            @click="deliver(task)"
          >
            <CheckCircle2 :size="17" /> 确认送达
          </button>
        </footer>
      </article>
    </div>

    <EmptyState
      v-else
      :title="activeTab === 'ACTIVE' ? '当前没有配送任务' : '还没有完成记录'"
      description="平台派单后会实时出现在这里。"
    />
  </div>
</template>
