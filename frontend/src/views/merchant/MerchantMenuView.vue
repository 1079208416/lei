<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  CircleOff,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  Utensils,
  X,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { MerchantSummary, Product, ProductCategory } from '../../types'
import { money } from '../../lib/format'
import { showToast } from '../../lib/toast'

interface MenuResponse {
  merchant: MerchantSummary
  categories: ProductCategory[]
}

const menu = ref<MenuResponse | null>(null)
const loading = ref(true)
const saving = ref(false)
const showProductForm = ref(false)
const editingProduct = ref<Product | null>(null)
const newCategoryName = ref('')
const addingCategory = ref(false)

const productForm = reactive({
  categoryId: 0,
  name: '',
  description: '',
  imageUrl: 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=800&q=85',
  priceYuan: '28',
  originalPriceYuan: '',
  soldOut: false,
  sortOrder: 99,
})

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    menu.value = await api.get<MenuResponse>('/api/merchant/menu')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '菜单加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function openCreate(categoryId?: number) {
  editingProduct.value = null
  Object.assign(productForm, {
    categoryId: categoryId ?? menu.value?.categories[0]?.id ?? 0,
    name: '',
    description: '',
    imageUrl: 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=800&q=85',
    priceYuan: '28',
    originalPriceYuan: '',
    soldOut: false,
    sortOrder: 99,
  })
  showProductForm.value = true
}

function openEdit(product: Product) {
  editingProduct.value = product
  Object.assign(productForm, {
    categoryId: product.categoryId,
    name: product.name,
    description: product.description,
    imageUrl: product.imageUrl,
    priceYuan: (product.price / 100).toFixed(2),
    originalPriceYuan: product.originalPrice ? (product.originalPrice / 100).toFixed(2) : '',
    soldOut: product.soldOut,
    sortOrder: 99,
  })
  showProductForm.value = true
}

async function saveProduct() {
  const price = Math.round(Number(productForm.priceYuan) * 100)
  const originalPrice = productForm.originalPriceYuan
    ? Math.round(Number(productForm.originalPriceYuan) * 100)
    : null
  if (!productForm.name.trim() || !productForm.categoryId || price <= 0) {
    showToast('请填写商品名称、分类和有效价格', 'error')
    return
  }
  saving.value = true
  try {
    const payload = {
      categoryId: productForm.categoryId,
      name: productForm.name.trim(),
      description: productForm.description.trim(),
      imageUrl: productForm.imageUrl.trim(),
      price,
      originalPrice,
      soldOut: productForm.soldOut,
      sortOrder: productForm.sortOrder,
    }
    if (editingProduct.value) {
      await api.put(`/api/merchant/products/${editingProduct.value.id}`, payload)
      showToast('商品已更新', 'success')
    } else {
      await api.post('/api/merchant/products', payload)
      showToast('商品已添加', 'success')
    }
    showProductForm.value = false
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '商品保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function toggleSoldOut(product: Product) {
  try {
    await api.patch(`/api/merchant/products/${product.id}/sold-out`, {
      soldOut: !product.soldOut,
    })
    showToast(product.soldOut ? '商品已恢复售卖' : '商品已标记售罄', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '状态更新失败', 'error')
  }
}

async function deleteProduct(product: Product) {
  if (!window.confirm(`确认删除“${product.name}”？`)) return
  try {
    await api.delete(`/api/merchant/products/${product.id}`)
    showToast('商品已删除', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '删除失败', 'error')
  }
}

async function addCategory() {
  if (!newCategoryName.value.trim()) return
  addingCategory.value = true
  try {
    await api.post('/api/merchant/categories', {
      name: newCategoryName.value.trim(),
      sortOrder: (menu.value?.categories.length ?? 0) + 1,
    })
    newCategoryName.value = ''
    showToast('分类已添加', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '分类添加失败', 'error')
  } finally {
    addingCategory.value = false
  }
}

async function deleteCategory(category: ProductCategory) {
  if (!window.confirm(`删除“${category.name}”分类？分类下不能有商品。`)) return
  try {
    await api.delete(`/api/merchant/categories/${category.id}`)
    showToast('分类已删除', 'success')
    await load(true)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '分类删除失败', 'error')
  }
}

onMounted(load)
</script>

<template>
  <div v-if="loading" class="page-loading">
    <span class="spinner"></span>
    正在读取菜单
  </div>

  <div v-else-if="menu" class="portal-page menu-admin-page">
    <div class="toolbar-row menu-toolbar">
      <form class="inline-create" @submit.prevent="addCategory">
        <input v-model="newCategoryName" placeholder="新增分类名称" />
        <button class="button secondary small" :disabled="addingCategory">
          <Plus :size="15" /> 添加分类
        </button>
      </form>
      <div class="toolbar-actions">
        <button class="button ghost small" @click="load()">
          <RefreshCw :size="16" /> 刷新
        </button>
        <button class="button primary small" @click="openCreate()">
          <Plus :size="16" /> 新增商品
        </button>
      </div>
    </div>

    <section v-for="category in menu.categories" :key="category.id" class="menu-admin-section">
      <header>
        <div>
          <span class="category-icon"><Utensils :size="18" /></span>
          <div>
            <h2>{{ category.name }}</h2>
            <small>{{ category.products.length }} 个商品</small>
          </div>
        </div>
        <div class="menu-admin-actions">
          <button class="text-button" @click="openCreate(category.id)">
            <Plus :size="15" /> 添加商品
          </button>
          <button class="icon-button subtle danger-text" title="删除分类" @click="deleteCategory(category)">
            <Trash2 :size="16" />
          </button>
        </div>
      </header>

      <div v-if="category.products.length" class="admin-product-list">
        <article v-for="product in category.products" :key="product.id" class="admin-product-row">
          <img :src="product.imageUrl" :alt="product.name" />
          <div class="admin-product-main">
            <div>
              <strong>{{ product.name }}</strong>
              <p>{{ product.description }}</p>
            </div>
            <small>月售 {{ product.monthlySales }}</small>
          </div>
          <strong class="admin-product-price">{{ money(product.price) }}</strong>
          <button
            class="availability-chip"
            :class="{ off: product.soldOut }"
            @click="toggleSoldOut(product)"
          >
            <CircleOff v-if="product.soldOut" :size="14" />
            {{ product.soldOut ? '已售罄' : '售卖中' }}
          </button>
          <div class="row-actions">
            <button class="icon-button subtle" title="编辑商品" @click="openEdit(product)">
              <Pencil :size="16" />
            </button>
            <button class="icon-button subtle danger-text" title="删除商品" @click="deleteProduct(product)">
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
      </div>
      <div v-else class="inline-empty">这个分类还没有商品</div>
    </section>

    <div v-if="showProductForm" class="modal-backdrop" @click.self="showProductForm = false">
      <section class="form-modal">
        <header>
          <div>
            <span class="eyebrow">{{ editingProduct ? '编辑商品' : '新增商品' }}</span>
            <h2>{{ editingProduct ? editingProduct.name : '添加到菜单' }}</h2>
          </div>
          <button class="icon-button" title="关闭" @click="showProductForm = false">
            <X :size="20" />
          </button>
        </header>
        <div class="form-grid two">
          <label class="field">
            <span>商品名称</span>
            <input v-model="productForm.name" placeholder="例如：黑椒鸡腿饭" />
          </label>
          <label class="field">
            <span>所属分类</span>
            <select v-model.number="productForm.categoryId">
              <option v-for="category in menu.categories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>售价（元）</span>
            <input v-model="productForm.priceYuan" inputmode="decimal" />
          </label>
          <label class="field">
            <span>原价（元，可选）</span>
            <input v-model="productForm.originalPriceYuan" inputmode="decimal" />
          </label>
          <label class="field field-wide">
            <span>商品描述</span>
            <textarea v-model="productForm.description" class="text-area small-area"></textarea>
          </label>
          <label class="field field-wide">
            <span>图片地址</span>
            <input v-model="productForm.imageUrl" />
          </label>
        </div>
        <label class="checkbox-row">
          <input v-model="productForm.soldOut" type="checkbox" />
          <span>保存后立即标记为售罄</span>
        </label>
        <button class="button primary wide" :disabled="saving" @click="saveProduct">
          {{ saving ? '保存中...' : '保存商品' }}
        </button>
      </section>
    </div>
  </div>
</template>
