import { defineStore } from 'pinia'
import { fetchCurrentUser, login as loginApi, logout as logoutApi, type LoginRequest } from '../api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    username: '' as string,
    loaded: false,
    authenticated: false,
  }),
  actions: {
    async bootstrap() {
      try {
        const user = await fetchCurrentUser()
        this.username = user.username
        this.authenticated = user.authenticated
      } catch {
        this.username = ''
        this.authenticated = false
      } finally {
        this.loaded = true
      }
    },
    async login(payload: LoginRequest) {
      const response = await loginApi(payload)
      this.username = response.username
      this.authenticated = true
      this.loaded = true
    },
    async logout() {
      await logoutApi()
      this.username = ''
      this.authenticated = false
    },
  },
})
