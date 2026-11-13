<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Check, RotateCcw, X } from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Refund } from '../../types'
import { money, refundStatusLabel, shortTime } from '../../lib/format'
import { showToast } from '../../lib/toast'
import EmptyState from '../../components/EmptyState.vue'

const refunds = ref<Refund[]>([])
const loading = ref(true)
const activeStatus = ref('REQUESTED')
const actionId = ref<number | null>(null)

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const status = activeStatus.value === 'ALL' ? '' : `?status=${activeStatus.value}`
    refunds.value = await api.get<Refund[]>(`/api/admin/refunds${status}`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '退款列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function decide(refund: Refund, approve: boolean) {
  const action = approve ? '通过退款' : '驳回退款'
  if (!window.confirm(`确认${action}？`)) return
  actionId.value = refund.id
  try {
    await api.post(`/api/admin/refunds/${refund.id}/decision`, { approve })
    showToast(`${action}成功`, 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : `${action}失败`, 'error')
  } finally {
    actionId.value = null
  }
}

function changeStatus(status: string) {
  activeStatus.value = status
  void load()
}

onMounted(load)
</script>

<template>
  <div class="portal-page refund-page">
    <div class="toolbar-row">
      <div class="segmented-control">
        <button :class="{ active: activeStatus === 'REQUESTED' }" @click="changeStatus('REQUESTED')">
          待处理
        </button>
        <button :class="{ active: activeStatus === 'COMPLETED' }" @click="changeStatus('COMPLETED')">
          已退款
        </button>
        <button :class="{ active: activeStatus === 'REJECTED' }" @click="changeStatus('REJECTED')">
          已驳回
        </button>
        <button :class="{ active: activeStatus === 'ALL' }" @click="changeStatus('ALL')">
          全部
        </button>
      </div>
    </div>

    <div v-if="loading" class="page-loading embedded">
      <span class="spinner"></span>
      正在读取售后
    </div>

    <div v-else-if="refunds.length" class="refund-list">
      <article v-for="refund in refunds" :key="refund.id" class="refund-card">
        <span class="refund-icon"><RotateCcw :size="21" /></span>
        <div class="refund-main">
          <div>
            <strong>{{ refund.merchantName }}</strong>
            <span class="status-badge" :class="`refund-${refund.status.toLowerCase()}`">
              {{ refundStatusLabel[refund.status] }}
            </span>
          </div>
          <small>{{ refund.orderNo }} · {{ shortTime(refund.createdAt) }}</small>
          <p>{{ refund.reason }}</p>
        </div>
        <strong class="refund-amount">{{ money(refund.amount) }}</strong>
        <div v-if="refund.status === 'REQUESTED'" class="refund-actions">
          <button
            class="button ghost small danger-text"
            :disabled="actionId === refund.id"
            @click="decide(refund, false)"
          >
            <X :size="15" /> 驳回
          </button>
          <button
            class="button primary small"
            :disabled="actionId === refund.id"
            @click="decide(refund, true)"
          >
            <Check :size="15" /> 通过退款
          </button>
        </div>
      </article>
    </div>

    <EmptyState
      v-else
      title="当前没有售后记录"
      description="新的退款申请会实时出现在这里。"
    />
  </div>
</template>
