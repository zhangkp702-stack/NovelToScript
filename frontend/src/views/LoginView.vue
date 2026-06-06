<template>
  <div class="card" style="max-width: 420px; margin: 80px auto">
    <h2>登录</h2>
    <form @submit.prevent="handleSubmit">
      <div class="field">
        <label for="username">用户名</label>
        <input id="username" v-model="username" required />
      </div>
      <div class="field">
        <label for="password">密码</label>
        <input id="password" v-model="password" type="password" required />
      </div>
      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <button class="btn" type="submit" :disabled="submitting">登录</button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const submitting = ref(false)
const errorMessage = ref('')

async function handleSubmit() {
  submitting.value = true
  errorMessage.value = ''
  try {
    await auth.login({ username: username.value, password: password.value })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败'
  } finally {
    submitting.value = false
  }
}
</script>
