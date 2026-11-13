<script setup lang="ts">
import { Bike, ChevronRight, Clock3 } from 'lucide-vue-next'
import type { Order } from '../types'
import { money, orderStatusLabel, shortTime } from '../lib/format'

defineProps<{
  order: Order
  compact?: boolean
}>()

const emit = defineEmits<{
  open: [order: Order]
}>()
</script>

<template>
  <article class="order-card" :class="{ compact }" @click="emit('open', order)">
    <header class="order-card-head">
      <div>
        <strong>{{ order.merchantName }}</strong>
        <small>{{ order.orderNo }}</small>
      </div>
      <span class="status-badge" :class="`status-${order.status.toLowerCase()}`">
        {{ orderStatusLabel[order.status] || order.status }}
      </span>
    </header>

    <div class="order-card-items">
      <div class="order-thumb-stack">
        <img
          v-for="item in order.items.slice(0, 3)"
          :key="item.id"
          :src="item.productImage"
          :alt="item.productName"
        />
        <span v-if="order.items.length > 3" class="more-count">+{{ order.items.length - 3 }}</span>
      </div>
      <div class="order-item-summary">
        <strong>{{ order.items.map((item) => item.productName).join('、') }}</strong>
        <span>共 {{ order.items.reduce((sum, item) => sum + item.quantity, 0) }} 件</span>
      </div>
      <div class="order-price">{{ money(order.totalAmount) }}</div>
    </div>

    <footer class="order-card-foot">
      <span><Clock3 :size="14" /> {{ shortTime(order.createdAt) }}</span>
      <span v-if="order.riderName"><Bike :size="14" /> {{ order.riderName }}</span>
      <span v-else>{{ order.addressSnapshot }}</span>
      <ChevronRight :size="17" />
    </footer>
  </article>
</template>
