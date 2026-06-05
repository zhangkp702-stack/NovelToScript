<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { currentUser, logout } from "../api/auth";

const router = useRouter();
const notice = ref("已进入业务首页，你可以开始后续主业务操作。");
const noticeType = ref("success");
const loading = ref(false);

function showNotice(type, message) {
  noticeType.value = type;
  notice.value = message;
}

async function onLoadCurrentUser() {
  loading.value = true;
  try {
    const { response, payload } = await currentUser();
    if (response.ok) {
      showNotice("success", `当前用户：${payload.username}`);
    } else {
      showNotice("error", "获取当前用户失败，请重新登录");
    }
  } catch (error) {
    showNotice("error", `请求异常：${error.message}`);
  } finally {
    loading.value = false;
  }
}

async function onLogout() {
  loading.value = true;
  try {
    await logout();
  } finally {
    loading.value = false;
    router.push("/login");
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <h1>业务首页</h1>
      <p class="sub-text">登录后先进入业务首页，再进行核心业务操作。</p>
      <p class="notice" :data-type="noticeType" v-if="notice">{{ notice }}</p>

      <div class="button-row">
        <button class="secondary" :disabled="loading" @click="onLoadCurrentUser">查看当前用户</button>
        <button class="secondary" :disabled="loading" @click="onLogout">退出登录</button>
      </div>
    </section>
  </main>
</template>
