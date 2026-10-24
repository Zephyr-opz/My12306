import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/tickets',
    children: [
      {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      },
      {
        path: '/users',
        name: 'Users',
        component: () => import('@/views/Users.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: '/admin/trains',
        name: 'AdminTrains',
        component: () => import('@/views/AdminTrains.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: '/admin/stations',
        name: 'AdminStations',
        component: () => import('@/views/AdminStations.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: '/admin/trains/preview',
        name: 'AdminTrainPreview',
        component: () => import('@/views/AdminTrainPreview.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: '/admin/schedules',
        name: 'AdminSchedules',
        component: () => import('@/views/AdminSchedules.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: '/admin/orders',
        name: 'AdminOrders',
        component: () => import('@/views/AdminOrders.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: '/tickets',
        name: 'Tickets',
        component: () => import('@/views/Tickets.vue')
      },
      {
        path: '/passengers',
        name: 'Passengers',
        component: () => import('@/views/Passengers.vue')
      },
      {
        path: '/orders',
        name: 'Orders',
        component: () => import('@/views/Orders.vue')
      },
      {
        path: '/orders/detail/:orderSn',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetail.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = userStore.isLoggedIn
  
  if (to.path !== '/login' && to.path !== '/register' && !isLoggedIn) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && isLoggedIn) {
    next('/')
  } else if (to.meta.requiresAdmin) {
    if (!userStore.userInfo) await userStore.getUserInfo()
    if (userStore.userInfo?.role !== 'ADMIN') next('/tickets')
    else next()
  } else {
    next()
  }
})

export default router
