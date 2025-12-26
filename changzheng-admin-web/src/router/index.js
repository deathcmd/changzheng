import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis' }
      },
      {
        path: 'nodes',
        name: 'NodeList',
        component: () => import('@/views/node/list.vue'),
        meta: { title: '节点管理', icon: 'Location' }
      },
      {
        path: 'nodes/:id/content',
        name: 'NodeContent',
        component: () => import('@/views/node/content.vue'),
        meta: { title: '内容管理', hidden: true }
      },
      {
        path: 'students',
        name: 'StudentList',
        component: () => import('@/views/student/list.vue'),
        meta: { title: '学生管理', icon: 'User' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue')
  }
]

const router = createRouter({
  history: createWebHistory('/admin'),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '管理后台'} - 云上重走长征路`
  
  const authStore = useAuthStore()
  
  if (to.meta.requiresAuth !== false && !authStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
