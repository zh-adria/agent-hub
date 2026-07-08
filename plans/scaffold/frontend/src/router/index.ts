import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/dashboard/Index.vue')
  },
  {
    path: '/agents',
    name: 'Agents',
    component: () => import('../views/agents/AgentList/Index.vue')
  },
  {
    path: '/agents/studio',
    name: 'AgentStudio',
    component: () => import('../views/agents/AgentStudio/Index.vue')
  },
  {
    path: '/functions',
    name: 'Functions',
    component: () => import('../views/functions/FunctionRegistry/Index.vue')
  },
  {
    path: '/sessions',
    name: 'Sessions',
    component: () => import('../views/sessions/Index.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router