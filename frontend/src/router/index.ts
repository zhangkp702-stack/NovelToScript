import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/', name: 'home', component: HomeView },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.loaded) {
    await auth.bootstrap()
  }
  if (!to.meta.public && !auth.authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.authenticated) {
    return { name: 'home' }
  }
  return true
})

export default router
