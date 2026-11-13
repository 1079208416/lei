<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { MapPin, Power, Star, Utensils } from 'lucide-vue-next'
import { api } from '../../api/client'
import type { MerchantSummary } from '../../types'
import { money } from '../../lib/format'
import { showToast } from '../../lib/toast'

const merchants = ref<MerchantSummary[]>([])
const loading = ref(true)
const togglingId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    merchants.value = await api.get<MerchantSummary[]>('/api/admin/merchants')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '商户加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function toggle(merchant: MerchantSummary) {
  togglingId.value = merchant.id
  try {
    const updated = await api.patch<MerchantSummary>(`/api/admin/merchants/${merchant.id}/status`, {
      open: !merchant.open,
    })
    const index = merchants.value.findIndex((item) => item.id === merchant.id)
    if (index >= 0) merchants.value[index] = updated
    showToast(updated.open ? '商户已恢复营业' : '商户已暂停营业', 'success')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '状态更新失败', 'error')
  } finally {
    togglingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div v-if="loading" class="page-loading">
    <span class="spinner"></span>
    正在读取商户
  </div>

  <div v-else class="portal-page">
    <div class="admin-page-heading">
      <div>
        <span class="eyebrow">供给管理</span>
        <h2>{{ merchants.length }} 家商户</h2>
      </div>
      <div class="inline-stat">
        <span class="live-dot"></span>
        {{ merchants.filter((merchant) => merchant.open).length }} 家营业中
      </div>
    </div>

    <div class="admin-merchant-grid">
      <article v-for="merchant in merchants" :key="merchant.id" class="admin-merchant-card">
        <div class="admin-merchant-cover">
          <img :src="merchant.coverUrl" :alt="merchant.name" />
          <span :class="merchant.open ? 'open' : 'closed'">
            {{ merchant.open ? '营业中' : '已暂停' }}
          </span>
        </div>
        <div class="admin-merchant-body">
          <div class="admin-merchant-title">
            <div>
              <span class="merchant-category">{{ merchant.category }}</span>
              <h3>{{ merchant.name }}</h3>
            </div>
            <span class="rating"><Star :size="14" fill="currentColor" /> {{ merchant.rating }}</span>
          </div>
          <p>{{ merchant.description }}</p>
          <div class="admin-merchant-meta">
            <span><MapPin :size="14" /> {{ merchant.address }}</span>
            <span><Utensils :size="14" /> 月售 {{ merchant.monthlySales }}</span>
            <span>起送 {{ money(merchant.minOrderAmount) }}</span>
          </div>
          <footer>
            <span class="merchant-notice">{{ merchant.notice }}</span>
            <button
              class="button small"
              :class="merchant.open ? 'ghost danger-text' : 'primary'"
              :disabled="togglingId === merchant.id"
              @click="toggle(merchant)"
            >
              <Power :size="15" /> {{ merchant.open ? '暂停营业' : '恢复营业' }}
            </button>
          </footer>
        </div>
      </article>
    </div>
  </div>
</template>
