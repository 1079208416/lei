<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Check,
  ChevronRight,
  CreditCard,
  MapPin,
  MessageSquareText,
  ShieldCheck,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Address, Cart, Order } from '../../types'
import { money } from '../../lib/format'
import { showToast } from '../../lib/toast'

const route = useRoute()
const router = useRouter()
const merchantId = Number(route.params.merchantId)
const cart = ref<Cart | null>(null)
const addresses = ref<Address[]>([])
const selectedAddressId = ref<number | null>(null)
const remark = ref('')
const loading = ref(true)
const submitting = ref(false)

const selectedAddress = computed(
  () => addresses.value.find((address) => address.id === selectedAddressId.value) ?? null,
)

async function load() {
  loading.value = true
  try {
    ;[cart.value, addresses.value] = await Promise.all([
      api.get<Cart>(`/api/user/cart?merchantId=${merchantId}`),
      api.get<Address[]>('/api/user/addresses'),
    ])
    selectedAddressId.value =
      addresses.value.find((address) => address.defaultAddress)?.id
      ?? addresses.value[0]?.id
      ?? null
  } catch (error) {
    showToast(error instanceof Error ? error.message : '结算信息加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  if (!selectedAddress.value) {
    showToast('请先选择收货地址', 'error')
    return
  }
  submitting.value = true
  try {
    const order = await api.post<Order>('/api/user/orders', {
      merchantId,
      addressId: selectedAddress.value.id,
      remark: remark.value,
    })
    await router.replace(`/orders/${order.id}`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '订单创建失败', 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-if="loading" class="page-loading">
    <span class="spinner"></span>
    正在确认订单
  </div>

  <div v-else-if="cart" class="checkout-page">
    <header class="page-heading">
      <button class="round-back light" title="返回" @click="router.back()">
        <ArrowLeft :size="20" />
      </button>
      <div>
        <span class="eyebrow">确认订单</span>
        <h1>{{ cart.merchant.name }}</h1>
      </div>
    </header>

    <div class="checkout-grid">
      <div class="checkout-main">
        <section class="panel">
          <header class="panel-title">
            <div>
              <MapPin :size="19" />
              <h2>收货地址</h2>
            </div>
            <RouterLink to="/profile" class="text-button">管理地址 <ChevronRight :size="15" /></RouterLink>
          </header>

          <div v-if="addresses.length" class="address-options">
            <button
              v-for="address in addresses"
              :key="address.id"
              class="address-option"
              :class="{ selected: selectedAddressId === address.id }"
              @click="selectedAddressId = address.id"
            >
              <span class="address-check">
                <Check v-if="selectedAddressId === address.id" :size="14" />
              </span>
              <span>
                <strong>{{ address.address }} {{ address.detail }}</strong>
                <small>{{ address.contactName }} · {{ address.phone }}</small>
              </span>
              <em v-if="address.defaultAddress">默认</em>
            </button>
          </div>

          <div v-else class="inline-empty">
            <MapPin :size="22" />
            <span>还没有收货地址</span>
            <RouterLink to="/profile" class="button secondary small">去添加</RouterLink>
          </div>
        </section>

        <section class="panel">
          <header class="panel-title">
            <div>
              <MessageSquareText :size="19" />
              <h2>订单备注</h2>
            </div>
          </header>
          <textarea
            v-model="remark"
            class="text-area"
            maxlength="200"
            placeholder="口味、餐具或配送相关说明"
          ></textarea>
        </section>

        <section class="panel">
          <header class="panel-title">
            <div>
              <CreditCard :size="19" />
              <h2>支付方式</h2>
            </div>
          </header>
          <div class="payment-option selected">
            <span class="wechat-mark">微</span>
            <div>
              <strong>微信支付</strong>
              <small>开发环境使用模拟支付，提交后可立即支付</small>
            </div>
            <Check :size="18" />
          </div>
        </section>
      </div>

      <aside class="checkout-summary panel">
        <span class="eyebrow">订单明细</span>
        <h2>{{ cart.items.length }} 种餐品</h2>
        <div class="checkout-items">
          <article v-for="item in cart.items" :key="item.id">
            <img :src="item.imageUrl" :alt="item.name" />
            <div>
              <strong>{{ item.name }} × {{ item.quantity }}</strong>
              <small>{{ item.optionsSnapshot || '标准份' }}</small>
            </div>
            <span>{{ money(item.subtotal) }}</span>
          </article>
        </div>
        <div class="checkout-price-lines">
          <span>商品金额 <b>{{ money(cart.itemAmount) }}</b></span>
          <span>打包费 <b>{{ money(cart.packingFee) }}</b></span>
          <span>配送费 <b>{{ money(cart.deliveryFee) }}</b></span>
          <span class="total">应付总额 <b>{{ money(cart.totalAmount) }}</b></span>
        </div>
        <button class="button primary wide large" :disabled="submitting" @click="submitOrder">
          {{ submitting ? '正在创建订单...' : '提交订单' }}
        </button>
        <div class="secure-note"><ShieldCheck :size="15" /> 订单金额由服务端校验</div>
      </aside>
    </div>
  </div>
</template>
