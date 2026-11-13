<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  Bike,
  ChevronRight,
  Coffee,
  Flame,
  MapPin,
  Salad,
  Search,
  Soup,
  Star,
  UtensilsCrossed,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { MerchantSummary } from '../../types'
import { money } from '../../lib/format'
import { showToast } from '../../lib/toast'

const merchants = ref<MerchantSummary[]>([])
const categories = ref<string[]>([])
const keyword = ref('')
const activeCategory = ref('全部')
const loading = ref(true)

const categoryIcons = [UtensilsCrossed, Flame, Salad, Soup, Coffee]

const heroMerchant = computed(() => merchants.value[0])

async function load() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (keyword.value.trim()) params.set('keyword', keyword.value.trim())
    if (activeCategory.value !== '全部') params.set('category', activeCategory.value)
    const query = params.size ? `?${params.toString()}` : ''
    ;[merchants.value, categories.value] = await Promise.all([
      api.get<MerchantSummary[]>(`/api/user/merchants${query}`),
      api.get<string[]>('/api/user/merchant-categories'),
    ])
  } catch (error) {
    showToast(error instanceof Error ? error.message : '商家加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function chooseCategory(category: string) {
  activeCategory.value = category
  void load()
}

onMounted(load)
</script>

<template>
  <div class="home-page">
    <section class="home-intro">
      <div class="home-intro-copy">
        <span class="eyebrow">附近好味道</span>
        <h1>今天，吃点认真做的。</h1>
        <p>从热炒、面食到轻食，把附近的好餐厅送到你手上。</p>
        <form class="home-search" @submit.prevent="load">
          <Search :size="20" />
          <input v-model="keyword" placeholder="搜索商家、菜品或口味" />
          <button>搜索</button>
        </form>
        <div class="home-location">
          <MapPin :size="16" />
          当前送至 <strong>云栖公寓 2 幢</strong>
        </div>
      </div>

      <RouterLink
        v-if="heroMerchant"
        :to="`/merchant/${heroMerchant.id}`"
        class="home-feature"
      >
        <img :src="heroMerchant.coverUrl" :alt="heroMerchant.name" />
        <div class="home-feature-shade"></div>
        <div class="home-feature-copy">
          <span>今日推荐</span>
          <h2>{{ heroMerchant.name }}</h2>
          <p>{{ heroMerchant.description }}</p>
          <strong>查看菜单 <ChevronRight :size="16" /></strong>
        </div>
        <div class="home-feature-meta">
          <Star :size="14" fill="currentColor" /> {{ heroMerchant.rating }}
          <span></span>
          {{ heroMerchant.etaMinutes }} 分钟
        </div>
      </RouterLink>
    </section>

    <section class="category-strip">
      <button
        class="category-pill"
        :class="{ active: activeCategory === '全部' }"
        @click="chooseCategory('全部')"
      >
        <span class="category-icon"><UtensilsCrossed :size="21" /></span>
        <strong>全部</strong>
      </button>
      <button
        v-for="(category, index) in categories"
        :key="category"
        class="category-pill"
        :class="{ active: activeCategory === category }"
        @click="chooseCategory(category)"
      >
        <span class="category-icon">
          <component :is="categoryIcons[index % categoryIcons.length]" :size="21" />
        </span>
        <strong>{{ category }}</strong>
      </button>
    </section>

    <section class="section-block">
      <header class="section-title-row">
        <div>
          <span class="eyebrow">配送范围内</span>
          <h2>{{ activeCategory === '全部' ? '附近商家' : activeCategory }}</h2>
        </div>
        <span class="section-count">{{ merchants.length }} 家营业中</span>
      </header>

      <div v-if="loading" class="merchant-grid">
        <div v-for="index in 4" :key="index" class="merchant-card skeleton"></div>
      </div>

      <div v-else-if="merchants.length" class="merchant-grid">
        <RouterLink
          v-for="merchant in merchants"
          :key="merchant.id"
          :to="`/merchant/${merchant.id}`"
          class="merchant-card"
        >
          <div class="merchant-cover">
            <img :src="merchant.coverUrl" :alt="merchant.name" />
            <span class="merchant-category">{{ merchant.category }}</span>
            <span class="merchant-eta"><Bike :size="14" /> {{ merchant.etaMinutes }} 分钟</span>
          </div>
          <div class="merchant-body">
            <div class="merchant-title-line">
              <div>
                <h3>{{ merchant.name }}</h3>
                <p>{{ merchant.description }}</p>
              </div>
              <span class="rating"><Star :size="14" fill="currentColor" /> {{ merchant.rating }}</span>
            </div>
            <div class="merchant-meta">
              <span>月售 {{ merchant.monthlySales }}</span>
              <span>起送 {{ money(merchant.minOrderAmount) }}</span>
              <span>配送 {{ money(merchant.deliveryFee) }}</span>
            </div>
            <div class="merchant-notice">{{ merchant.notice }}</div>
          </div>
        </RouterLink>
      </div>

      <div v-else class="empty-search">
        <Search :size="28" />
        <strong>没有找到匹配的商家</strong>
        <button class="text-button" @click="keyword = ''; chooseCategory('全部')">清除筛选</button>
      </div>
    </section>
  </div>
</template>
