import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'module/:moduleKey',
        name: 'ModuleList',
        component: () => import('@/views/ModuleList.vue'),
        meta: { title: '模块列表' }
      },
      {
        path: 'module/:moduleKey/create',
        name: 'RecordCreate',
        component: () => import('@/views/RecordEdit.vue'),
        meta: { title: '新增记录' }
      },
      {
        path: 'module/:moduleKey/:recordId',
        name: 'RecordDetail',
        component: () => import('@/views/RecordDetail.vue'),
        meta: { title: '记录详情' }
      },
      {
        path: 'module/:moduleKey/:recordId/edit',
        name: 'RecordEdit',
        component: () => import('@/views/RecordEdit.vue'),
        meta: { title: '编辑记录' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '设置中心', requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  document.title = `${to.meta.title || '家记'} - 家里的事，都记在这里`

  // 检查认证
  if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 检查管理员权限
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router

// 需要在函数内导入，避免循环依赖
import { useAuthStore } from '@/stores/auth'