<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Bike,
  ChevronDown,
  Clock3,
  Minus,
  Plus,
  ShoppingBag,
  Star,
  X,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Cart, MerchantDetail, Product } from '../../types'
import { money } from '../../lib/format'
import { showToast } from '../../lib/toast'
import ProductOptionModal from '../../components/ProductOptionModal.vue'

const route = useRoute()
const router = useRouter()
const merchant = ref<MerchantDetail | null>(null)
const cart = ref<Cart | null>(null)
const selectedCategory = ref<number | null>(null)
const selectedProduct = ref<Product | null>(null)
const showCart = ref(false)
const loading = ref(true)
const adding = ref(false)

const merchantId = Number(route.params.id)
const currentCategory = computed(
  () => merchant.value?.categories.find((category) => category.id === selectedCategory.value)
    ?? merchant.value?.categories[0],
)
const cartCount = computed(
  () => cart.value?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0,
)

async function load() {
  loading.value = true
  try {
    ;[merchant.value, cart.value] = await Promise.all([
      api.get<MerchantDetail>(`/api/user/merchants/${merchantId}`),
      api.get<Cart>(`/api/user/cart?merchantId=${merchantId}`),
    ])
    selectedCategory.value = merchant.value.categories[0]?.id ?? null
  } catch (error) {
    showToast(error instanceof Error ? error.message : '商家加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function addToCart(optionIds: number[], quantity: number) {
  if (!selectedProduct.value) return
  adding.value = true
  try {
    cart.value = await api.post<Cart>('/api/user/cart/items', {
      merchantId,
      productId: selectedProduct.value.id,
      quantity,
      optionIds,
    })
    selectedProduct.value = null
    showToast('已加入购物车', 'success')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '加入购物车失败', 'error')
  } finally {
    adding.value = false
  }
}

async function updateQuantity(itemId: number, quantity: number) {
  try {
    cart.value = await api.patch<Cart>(`/api/user/cart/items/${itemId}`, { quantity })
    if (!cart.value.items.length) showCart.value = false
  } catch (error) {
    showToast(error instanceof Error ? error.message : '购物车更新失败', 'error')
  }
}

function goCheckout() {
  if (!cart.value?.canCheckout) {
    showToast('还未达到起送价', 'error')
    return
  }
  router.push(`/checkout/${merchantId}`)
}

onMounted(load)
</script>

<template>
  <div v-if="loading" class="page-loading">
    <span class="spinner"></span>
    正在加载菜单
  </div>

  <div v-else-if="merchant" class="merchant-page">
    <section class="merchant-hero">
      <img :src="merchant.coverUrl" :alt="merchant.name" />
      <div class="merchant-hero-shade"></div>
      <button class="round-back" title="返回" @click="router.back()">
        <ArrowLeft :size="20" />
      </button>
      <div class="merchant-hero-content">
        <span class="merchant-category">{{ merchant.category }}</span>
        <h1>{{ merchant.name }}</h1>
        <p>{{ merchant.description }}</p>
        <div class="merchant-stat-row">
          <span><Star :size="15" fill="currentColor" /> {{ merchant.rating }}</span>
          <span><Clock3 :size="15" /> {{ merchant.etaMinutes }} 分钟</span>
          <span><Bike :size="15" /> 配送 {{ money(merchant.deliveryFee) }}</span>
          <span>月售 {{ merchant.monthlySales }}</span>
        </div>
      </div>
    </section>

    <div class="merchant-notice-bar">
      <span>店铺公告</span>
      <p>{{ merchant.notice }}</p>
    </div>

    <section class="menu-layout">
      <aside class="menu-categories">
        <button
          v-for="category in merchant.categories"
          :key="category.id"
          :class="{ active: currentCategory?.id === category.id }"
          @click="selectedCategory = category.id"
        >
          {{ category.name }}
          <span>{{ category.products.length }}</span>
        </button>
      </aside>

      <div class="menu-content">
        <header class="menu-heading">
          <div>
            <span class="eyebrow">菜单</span>
            <h2>{{ currentCategory?.name }}</h2>
          </div>
          <span>{{ currentCategory?.products.length }} 道菜品</span>
        </header>

        <div class="product-list">
          <article
            v-for="product in currentCategory?.products"
            :key="product.id"
            class="product-row"
            :class="{ soldout: product.soldOut }"
          >
            <img :src="product.imageUrl" :alt="product.name" />
            <div class="product-row-info">
              <div>
                <h3>{{ product.name }}</h3>
                <p>{{ product.description }}</p>
              </div>
              <div class="product-sales">月售 {{ product.monthlySales }}</div>
              <div class="product-row-bottom">
                <div class="product-price">
                  <strong>{{ money(product.price) }}</strong>
                  <del v-if="product.originalPrice">{{ money(product.originalPrice) }}</del>
                </div>
                <button
                  class="add-product-button"
                  :disabled="product.soldOut"
                  @click="selectedProduct = product"
                >
                  <Plus :size="17" />
                  {{ product.soldOut ? '已售罄' : '选规格' }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <div v-if="cartCount > 0" class="floating-cart">
      <button class="cart-summary" @click="showCart = true">
        <span class="cart-bag">
          <ShoppingBag :size="22" />
          <b>{{ cartCount }}</b>
        </span>
        <span>
          <strong>{{ money(cart?.totalAmount) }}</strong>
          <small>另需配送 {{ money(cart?.deliveryFee) }}</small>
        </span>
      </button>
      <button class="button cart-checkout" @click="goCheckout">
        {{ cart?.canCheckout ? '去结算' : `差 ${money((cart?.minOrderAmount || 0) - (cart?.itemAmount || 0))} 起送` }}
      </button>
    </div>

    <ProductOptionModal
      v-if="selectedProduct"
      :product="selectedProduct"
      @close="selectedProduct = null"
      @add="addToCart"
    />
    <span v-if="adding" class="modal-loading">正在加入...</span>

    <div v-if="showCart" class="modal-backdrop cart-backdrop" @click.self="showCart = false">
      <section class="cart-sheet">
        <header>
          <div>
            <span class="eyebrow">已选餐品</span>
            <h2>购物车</h2>
          </div>
          <button class="icon-button" title="关闭" @click="showCart = false">
            <X :size="20" />
          </button>
        </header>
        <div class="cart-sheet-items">
          <article v-for="item in cart?.items" :key="item.id" class="cart-line">
            <img :src="item.imageUrl" :alt="item.name" />
            <div>
              <strong>{{ item.name }}</strong>
              <small>{{ item.optionsSnapshot || '标准份' }}</small>
              <span>{{ money(item.unitPrice) }}</span>
            </div>
            <div class="quantity-stepper small">
              <button title="减少数量" @click="updateQuantity(item.id, item.quantity - 1)">
                <Minus :size="15" />
              </button>
              <span>{{ item.quantity }}</span>
              <button title="增加数量" @click="updateQuantity(item.id, item.quantity + 1)">
                <Plus :size="15" />
              </button>
            </div>
          </article>
        </div>
        <div class="cart-breakdown">
          <span>商品小计 <b>{{ money(cart?.itemAmount) }}</b></span>
          <span>打包费 <b>{{ money(cart?.packingFee) }}</b></span>
          <span>配送费 <b>{{ money(cart?.deliveryFee) }}</b></span>
          <span class="total">合计 <b>{{ money(cart?.totalAmount) }}</b></span>
        </div>
        <button class="button primary wide" @click="goCheckout">
          去结算
          <ChevronDown :size="17" />
        </button>
      </section>
    </div>
  </div>
</template>
