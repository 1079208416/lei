<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Check,
  LogOut,
  MapPin,
  Pencil,
  Plus,
  ShieldCheck,
  Trash2,
  UserRound,
  X,
} from 'lucide-vue-next'
import { api } from '../../api/client'
import type { Address } from '../../types'
import { useAuthStore } from '../../stores/auth'
import { showToast } from '../../lib/toast'

const auth = useAuthStore()
const router = useRouter()
const addresses = ref<Address[]>([])
const loading = ref(true)
const showForm = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  label: '家',
  contactName: auth.account?.name ?? '',
  phone: auth.account?.phone ?? '',
  address: '',
  detail: '',
  longitude: 120.1272,
  latitude: 30.2748,
  defaultAddress: false,
})

async function loadAddresses() {
  loading.value = true
  try {
    addresses.value = await api.get<Address[]>('/api/user/addresses')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '地址加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    label: '家',
    contactName: auth.account?.name ?? '',
    phone: auth.account?.phone ?? '',
    address: '',
    detail: '',
    longitude: 120.1272,
    latitude: 30.2748,
    defaultAddress: addresses.value.length === 0,
  })
  showForm.value = true
}

function openEdit(address: Address) {
  editingId.value = address.id
  Object.assign(form, {
    label: address.label,
    contactName: address.contactName,
    phone: address.phone,
    address: address.address,
    detail: address.detail,
    longitude: address.longitude,
    latitude: address.latitude,
    defaultAddress: address.defaultAddress,
  })
  showForm.value = true
}

async function saveAddress() {
  if (!form.address.trim() || !form.contactName.trim() || !form.phone.trim()) {
    showToast('请填写联系人、手机号和地址', 'error')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put<Address>(`/api/user/addresses/${editingId.value}`, form)
      showToast('地址已更新', 'success')
    } else {
      await api.post<Address>('/api/user/addresses', form)
      showToast('地址已添加', 'success')
    }
    showForm.value = false
    await loadAddresses()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '地址保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function removeAddress(address: Address) {
  if (!window.confirm(`删除“${address.address} ${address.detail}”这个地址？`)) return
  try {
    await api.delete(`/api/user/addresses/${address.id}`)
    showToast('地址已删除', 'success')
    await loadAddresses()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '删除失败', 'error')
  }
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(loadAddresses)
</script>

<template>
  <div class="profile-page">
    <header class="profile-hero">
      <div class="profile-avatar">{{ auth.account?.name?.slice(0, 1) }}</div>
      <div>
        <span class="eyebrow">个人中心</span>
        <h1>{{ auth.account?.name }}</h1>
        <p>{{ auth.account?.phone }} · 普通用户</p>
      </div>
      <span class="profile-auth-badge"><ShieldCheck :size="15" /> 已登录</span>
    </header>

    <div class="profile-grid">
      <section class="panel profile-info-panel">
        <header class="panel-title">
          <div><UserRound :size="19" /><h2>账户信息</h2></div>
        </header>
        <div class="info-list">
          <div><span>昵称</span><strong>{{ auth.account?.name }}</strong></div>
          <div><span>手机号</span><strong>{{ auth.account?.phone }}</strong></div>
          <div><span>账号角色</span><strong>普通用户</strong></div>
        </div>
        <button class="button ghost wide danger-text" @click="logout">
          <LogOut :size="17" /> 退出当前账号
        </button>
      </section>

      <section class="panel address-panel">
        <header class="panel-title">
          <div><MapPin :size="19" /><h2>收货地址</h2></div>
          <button class="button secondary small" @click="openCreate">
            <Plus :size="15" /> 新增地址
          </button>
        </header>

        <div v-if="loading" class="page-loading embedded">
          <span class="spinner"></span>
          正在读取地址
        </div>

        <div v-else-if="addresses.length" class="profile-address-list">
          <article v-for="address in addresses" :key="address.id" class="profile-address">
            <span class="address-icon"><MapPin :size="18" /></span>
            <div>
              <strong>{{ address.address }} {{ address.detail }}</strong>
              <small>{{ address.contactName }} · {{ address.phone }}</small>
              <em v-if="address.defaultAddress"><Check :size="12" /> 默认地址</em>
            </div>
            <div class="address-actions">
              <button class="icon-button subtle" title="编辑地址" @click="openEdit(address)">
                <Pencil :size="16" />
              </button>
              <button class="icon-button subtle danger-text" title="删除地址" @click="removeAddress(address)">
                <Trash2 :size="16" />
              </button>
            </div>
          </article>
        </div>

        <div v-else class="inline-empty">
          <MapPin :size="24" />
          <span>还没有收货地址</span>
          <button class="button secondary small" @click="openCreate">添加第一个地址</button>
        </div>
      </section>
    </div>

    <div v-if="showForm" class="modal-backdrop" @click.self="showForm = false">
      <section class="form-modal">
        <header>
          <div>
            <span class="eyebrow">{{ editingId ? '编辑地址' : '新增地址' }}</span>
            <h2>送到哪里？</h2>
          </div>
          <button class="icon-button" title="关闭" @click="showForm = false">
            <X :size="20" />
          </button>
        </header>
        <div class="form-grid two">
          <label class="field">
            <span>地址标签</span>
            <input v-model="form.label" placeholder="家 / 公司" />
          </label>
          <label class="field">
            <span>联系人</span>
            <input v-model="form.contactName" placeholder="联系人姓名" />
          </label>
          <label class="field">
            <span>手机号</span>
            <input v-model="form.phone" placeholder="联系电话" />
          </label>
          <label class="field">
            <span>小区或写字楼</span>
            <input v-model="form.address" placeholder="例如：云栖公寓 2 幢" />
          </label>
          <label class="field">
            <span>门牌详情</span>
            <input v-model="form.detail" placeholder="例如：502 室" />
          </label>
        </div>
        <label class="checkbox-row">
          <input v-model="form.defaultAddress" type="checkbox" />
          <span>设为默认收货地址</span>
        </label>
        <button class="button primary wide" :disabled="saving" @click="saveAddress">
          {{ saving ? '保存中...' : '保存地址' }}
        </button>
      </section>
    </div>
  </div>
</template>
