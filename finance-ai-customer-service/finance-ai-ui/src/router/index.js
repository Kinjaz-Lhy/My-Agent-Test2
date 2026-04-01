import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, parseUserFromToken } from '@/utils/auth'

/**
 * Role-based route definitions.
 * meta.roles: array of roles allowed to access the route.
 * If meta.roles is not set, the route is accessible to all authenticated users.
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    redirect: '/chat'
  },
  // Employee routes
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/chat/ChatView.vue'),
    meta: { roles: ['EMPLOYEE', 'OPERATOR', 'AUDITOR'] }
  },
  // Operator routes
  {
    path: '/admin',
    redirect: '/admin/dashboard',
    meta: { roles: ['OPERATOR'] },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { roles: ['OPERATOR'] }
      },
      {
        path: 'logs',
        name: 'LogQuery',
        component: () => import('@/views/admin/LogQuery.vue'),
        meta: { roles: ['OPERATOR'] }
      },
      {
        path: 'hot-topics',
        name: 'HotTopics',
        component: () => import('@/views/admin/HotTopics.vue'),
        meta: { roles: ['OPERATOR'] }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/admin/Knowledge.vue'),
        meta: { roles: ['OPERATOR'] }
      }
    ]
  },
  // Auditor routes
  {
    path: '/audit',
    name: 'AuditLogs',
    component: () => import('@/views/audit/AuditLogs.vue'),
    meta: { roles: ['AUDITOR'] }
  },
  // 403 Forbidden
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: { public: true }
  },
  // 404 catch-all
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * Navigation guard: enforce authentication and role-based access.
 */
router.beforeEach((to, from, next) => {
  // Public routes (login, error pages) are always accessible
  if (to.meta.public) {
    return next()
  }

  // Check authentication
  if (!isAuthenticated()) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  // Check role-based access
  const requiredRoles = to.meta.roles
  if (requiredRoles && requiredRoles.length > 0) {
    const user = parseUserFromToken()
    const userRoles = user?.roles || []
    const hasAccess = requiredRoles.some(role => userRoles.includes(role))
    if (!hasAccess) {
      return next({ name: 'Forbidden' })
    }
  }

  next()
})

export default router

/**
 * Helper: get navigation menu items based on user role.
 * Used by layout components to render role-specific menus.
 */
export function getMenusByRole(roles) {
  const menus = []

  // All authenticated users can access chat
  menus.push({ path: '/chat', title: '智能客服', icon: 'ChatDotRound' })

  if (roles.includes('OPERATOR')) {
    menus.push(
      { path: '/admin/dashboard', title: '运营看板', icon: 'DataAnalysis' },
      { path: '/admin/logs', title: '对话日志', icon: 'Document' },
      { path: '/admin/hot-topics', title: '热点问题', icon: 'TrendCharts' },
      { path: '/admin/knowledge', title: '知识库管理', icon: 'Collection' }
    )
  }

  if (roles.includes('AUDITOR')) {
    menus.push({ path: '/audit', title: '审计日志', icon: 'List' })
  }

  return menus
}
