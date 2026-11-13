import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'
import type { Role } from './types'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('./views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('./layouts/UserLayout.vue'),
      meta: { roles: ['USER'] },
      children: [
        { path: '', name: 'home', component: () => import('./views/user/HomeView.vue') },
        {
          path: 'merchant/:id',
          name: 'merchant-detail',
          component: () => import('./views/user/MerchantView.vue'),
        },
        {
          path: 'checkout/:merchantId',
          name: 'checkout',
          component: () => import('./views/user/CheckoutView.vue'),
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('./views/user/OrdersView.vue'),
        },
        {
          path: 'orders/:id',
          name: 'order-detail',
          component: () => import('./views/user/OrderDetailView.vue'),
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('./views/user/ProfileView.vue'),
        },
      ],
    },
    {
      path: '/merchant-console',
      component: () => import('./layouts/PortalLayout.vue'),
      meta: { roles: ['MERCHANT'] },
      children: [
        {
          path: '',
          name: 'merchant-dashboard',
          component: () => import('./views/merchant/MerchantDashboardView.vue'),
        },
        {
          path: 'orders',
          name: 'merchant-orders',
          component: () => import('./views/merchant/MerchantOrdersView.vue'),
        },
        {
          path: 'menu',
          name: 'merchant-menu',
          component: () => import('./views/merchant/MerchantMenuView.vue'),
        },
      ],
    },
    {
      path: '/rider-console',
      component: () => import('./layouts/PortalLayout.vue'),
      meta: { roles: ['RIDER'] },
      children: [
        {
          path: '',
          name: 'rider-dashboard',
          component: () => import('./views/rider/RiderDashboardView.vue'),
        },
      ],
    },
    {
      path: '/admin-console',
      component: () => import('./layouts/PortalLayout.vue'),
      meta: { roles: ['ADMIN'] },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('./views/admin/AdminDashboardView.vue'),
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('./views/admin/AdminOrdersView.vue'),
        },
        {
          path: 'merchants',
          name: 'admin-merchants',
          component: () => import('./views/admin/AdminMerchantsView.vue'),
        },
        {
          path: 'refunds',
          name: 'admin-refunds',
          component: () => import('./views/admin/AdminRefundsView.vue'),
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.initialize()
  if (to.meta.public) {
    if (auth.isLoggedIn && to.name === 'login') return auth.home
    return true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  const roles = to.meta.roles as Role[] | undefined
  if (roles && auth.account && !roles.includes(auth.account.role)) {
    return auth.home
  }
  return true
})

export default router
