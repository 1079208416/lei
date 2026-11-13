<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Bike,
  CheckCircle2,
  CircleDollarSign,
  Clock3,
  MapPin,
  MessageCircle,
  Phone,
  RefreshCw,
  ShieldCheck,
  UtensilsCrossed,
  XCircle,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Order } from '../../types'
import { fullTime, money, orderStatusLabel } from '../../lib/format'
import { showToast } from '../../lib/toast'

const route = useRoute()
const router = useRouter()
const order = ref<Order | null>(null)
const loading = ref(true)
const actionLoading = ref(false)
const orderId = Number(route.params.id)

const canPay = computed(() => order.value?.status === 'WAITING_PAYMENT')
const canCancel = computed(() =>
  ['WAITING_PAYMENT', 'WAITING_ACCEPT'].includes(order.value?.status ?? ''),
)
const canRefund = computed(() =>
  order.value?.paymentStatus === 'PAID'
  && ['WAITING_ACCEPT', 'PREPARING', 'WAITING_PICKUP', 'DELIVERING', 'COMPLETED'].includes(order.value.status)
  && order.value.refundStatus === 'NONE',
)

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    order.value = await api.get<Order>(`/api/user/orders/${orderId}`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '订单加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function runAction(action: 'pay' | 'cancel' | 'refund') {
  actionLoading.value = true
  try {
    if (action === 'pay') {
      order.value = await api.post<Order>(`/api/user/orders/${orderId}/pay`)
      showToast('支付成功，已通知商家', 'success')
    } else if (action === 'cancel') {
      order.value = await api.post<Order>(`/api/user/orders/${orderId}/cancel`, {
        reason: '临时有事，暂不需要了',
      })
      showToast('订单已取消', 'success')
    } else {
      order.value = await api.post<Order>(`/api/user/orders/${orderId}/refund`, {
        reason: '用户申请售后处理',
      })
      showToast('退款申请已提交', 'success')
    }
  } catch (error) {
    showToast(error instanceof Error ? error.message : '操作失败', 'error')
  } finally {
    actionLoading.value = false
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
    正在读取订单
  </div>

  <div v-else-if="order" class="order-detail-page">
    <header class="page-heading">
      <button class="round-back light" title="返回" @click="router.back()">
        <ArrowLeft :size="20" />
      </button>
      <div>
        <span class="eyebrow">订单详情</span>
        <h1>{{ order.merchantName }}</h1>
      </div>
      <button class="icon-button" title="刷新" @click="load()">
        <RefreshCw :size="18" />
      </button>
    </header>

    <section class="order-status-hero" :class="`state-${order.status.toLowerCase()}`">
      <div>
        <span>{{ orderStatusLabel[order.status] || order.status }}</span>
        <h2>
          {{ order.status === 'WAITING_PAYMENT' ? '订单已创建，等待支付' :
            order.status === 'WAITING_ACCEPT' ? '已支付，等待商家接单' :
            order.status === 'PREPARING' ? '商家正在认真制作' :
            order.status === 'WAITING_PICKUP' ? '餐品已出餐，等待骑手' :
            order.status === 'DELIVERING' ? '骑手正在送达' :
            order.status === 'COMPLETED' ? '订单已送达' :
            order.status === 'REJECTED' ? '商家未能接单' : '订单已结束' }}
        </h2>
        <p v-if="order.status === 'DELIVERING'">预计 {{ fullTime(order.estimatedDeliveryAt) }} 前送达</p>
        <p v-else>{{ order.orderNo }}</p>
      </div>
      <Bike v-if="order.status === 'DELIVERING'" :size="62" />
      <CircleDollarSign v-else-if="order.status === 'WAITING_PAYMENT'" :size="62" />
      <XCircle v-else-if="['CANCELLED', 'REJECTED'].includes(order.status)" :size="62" />
      <CheckCircle2 v-else :size="62" />
    </section>

    <div class="order-detail-grid">
      <div class="order-detail-main">
        <section class="panel">
          <header class="panel-title">
            <div><Clock3 :size="19" /><h2>订单进度</h2></div>
          </header>
          <div class="timeline">
            <div v-for="(event, index) in order.timeline" :key="index" class="timeline-item">
              <span class="timeline-dot"></span>
              <div>
                <strong>{{ event.note }}</strong>
                <small>{{ fullTime(event.createdAt) }}</small>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <header class="panel-title">
            <div><MapPin :size="19" /><h2>配送信息</h2></div>
          </header>
          <div class="info-list">
            <div><span>收货地址</span><strong>{{ order.addressSnapshot }}</strong></div>
            <div><span>联系人</span><strong>{{ order.contactName }} · {{ order.contactPhone }}</strong></div>
            <div v-if="order.riderName"><span>配送骑手</span><strong>{{ order.riderName }}</strong></div>
            <div v-if="order.remark"><span>订单备注</span><strong>{{ order.remark }}</strong></div>
          </div>
          <div v-if="order.riderName" class="contact-actions">
            <a :href="`tel:${order.riderPhone}`" class="button secondary small">
              <Phone :size="15" /> 联系骑手
            </a>
            <a :href="`sms:${order.riderPhone}`" class="button ghost small">
              <MessageCircle :size="15" /> 发消息
            </a>
          </div>
        </section>

        <section class="panel">
          <header class="panel-title">
            <div><UtensilsCrossed :size="19" /><h2>餐品明细</h2></div>
          </header>
          <div class="order-detail-items">
            <article v-for="item in order.items" :key="item.id">
              <img :src="item.productImage" :alt="item.productName" />
              <div>
                <strong>{{ item.productName }}</strong>
                <small>{{ item.optionsSnapshot || '标准份' }} · ×{{ item.quantity }}</small>
              </div>
              <span>{{ money(item.totalPrice) }}</span>
            </article>
          </div>
        </section>
      </div>

      <aside class="order-detail-aside">
        <section class="panel">
          <span class="eyebrow">金额</span>
          <div class="checkout-price-lines">
            <span>商品金额 <b>{{ money(order.itemAmount) }}</b></span>
            <span>打包费 <b>{{ money(order.packingFee) }}</b></span>
            <span>配送费 <b>{{ money(order.deliveryFee) }}</b></span>
            <span class="total">合计 <b>{{ money(order.totalAmount) }}</b></span>
          </div>
        </section>

        <section class="panel action-panel">
          <button
            v-if="canPay"
            class="button primary wide large"
            :disabled="actionLoading"
            @click="runAction('pay')"
          >
            <ShieldCheck :size="18" /> 立即支付 {{ money(order.totalAmount) }}
          </button>
          <button
            v-if="canRefund"
            class="button secondary wide"
            :disabled="actionLoading"
            @click="runAction('refund')"
          >
            <CircleDollarSign :size="17" /> 申请退款
          </button>
          <button
            v-if="canCancel"
            class="button ghost wide danger-text"
            :disabled="actionLoading"
            @click="runAction('cancel')"
          >
            取消订单
          </button>
          <div v-if="order.refundStatus !== 'NONE'" class="refund-state">
            退款状态：{{ order.refundStatus }}
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>
