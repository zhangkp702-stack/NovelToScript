<template>
  <div class="page">
    <header v-if="auth.authenticated" class="nav">
      <strong>NovelToScript</strong>
      <span class="hint">{{ auth.username }}</span>
      <button class="btn btn-secondary" type="button" @click="handleLogout">退出</button>
    </header>
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'login' })
}
</script>
