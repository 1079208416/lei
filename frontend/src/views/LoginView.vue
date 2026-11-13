<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Bike,
  Check,
  LockKeyhole,
  Phone,
  ShieldCheck,
  ShoppingBag,
  Store,
  UtensilsCrossed,
} from 'lucide-vue-next'
import { useAuthStore } from '../stores/auth'
import type { Role } from '../types'
import { showToast } from '../lib/toast'

const auth = useAuthStore()
const router = useRouter()
const loadingRole = ref<Role | null>(null)
const phone = ref('13800000001')
const password = ref('demo123')
const loadingForm = ref(false)

const roles: Array<{ role: Role; name: string; hint: string; icon: typeof ShoppingBag }> = [
  { role: 'USER', name: '用户点餐', hint: '浏览商家与下单', icon: ShoppingBag },
  { role: 'MERCHANT', name: '商家工作台', hint: '接单与菜单管理', icon: Store },
  { role: 'RIDER', name: '骑手配送', hint: '取餐与送达', icon: Bike },
  { role: 'ADMIN', name: '平台运营', hint: '派单与售后', icon: ShieldCheck },
]

async function enterDemo(role: Role) {
  loadingRole.value = role
  try {
    const account = await auth.demoLogin(role)
    showToast(`已进入${roles.find((item) => item.role === role)?.name}`, 'success')
    await router.push(auth.roleHome[account.role])
  } catch (error) {
    showToast(error instanceof Error ? error.message : '登录失败', 'error')
  } finally {
    loadingRole.value = null
  }
}

async function submitLogin() {
  loadingForm.value = true
  try {
    const account = await auth.login(phone.value, password.value)
    showToast(`欢迎回来，${account.name}`, 'success')
    await router.push(auth.roleHome[account.role])
  } catch (error) {
    showToast(error instanceof Error ? error.message : '登录失败', 'error')
  } finally {
    loadingForm.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-visual">
      <div class="login-visual-overlay"></div>
      <div class="login-brand">
        <span class="brand-mark large"><UtensilsCrossed :size="26" /></span>
        <div>
          <strong>食刻</strong>
          <small>SHIKE DELIVERY</small>
        </div>
      </div>

      <div class="login-story">
        <span class="eyebrow light">本地生活 · 即时配送</span>
        <h1>从一餐开始，<br />把城市连接起来。</h1>
        <p>一个完整的单城市外卖 MVP，覆盖用户点餐、商家履约、骑手配送与平台调度。</p>
        <div class="login-proof">
          <span><Check :size="15" /> 四端真实业务闭环</span>
          <span><Check :size="15" /> 实时订单状态</span>
          <span><Check :size="15" /> 本地可运行</span>
        </div>
      </div>

      <div class="login-visual-footer">SHIKE MVP · 2026</div>
    </section>

    <section class="login-panel">
      <div class="login-panel-inner">
        <div class="login-heading">
          <span class="eyebrow">进入工作空间</span>
          <h2>今天想从哪里开始？</h2>
          <p>选择角色一键体验，或使用演示账号登录。</p>
        </div>

        <div class="role-grid">
          <button
            v-for="item in roles"
            :key="item.role"
            class="role-card"
            :class="{ loading: loadingRole === item.role }"
            :disabled="loadingRole !== null"
            @click="enterDemo(item.role)"
          >
            <span class="role-icon"><component :is="item.icon" :size="21" /></span>
            <span><strong>{{ item.name }}</strong><small>{{ item.hint }}</small></span>
            <ArrowRight :size="17" />
          </button>
        </div>

        <div class="login-divider"><span>或登录已有账号</span></div>

        <form class="login-form" @submit.prevent="submitLogin">
          <label class="field">
            <span>手机号</span>
            <div class="input-with-icon">
              <Phone :size="17" />
              <input v-model="phone" autocomplete="username" placeholder="请输入手机号" />
            </div>
          </label>
          <label class="field">
            <span>密码</span>
            <div class="input-with-icon">
              <LockKeyhole :size="17" />
              <input
                v-model="password"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
              />
            </div>
          </label>
          <button class="button primary wide" :disabled="loadingForm">
            {{ loadingForm ? '登录中...' : '登录' }}
          </button>
        </form>

        <p class="demo-hint">演示账号统一密码：<strong>demo123</strong></p>
      </div>
    </section>
  </div>
</template>
