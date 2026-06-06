<template>
  <div class="card">
    <h2>欢迎，{{ auth.username }}</h2>
    <p class="hint">前端脚手架已就绪，剧本生成页面将在后续版本提供。</p>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>
