<script setup>
import { reactive, ref } from "vue";
import { currentUser, login, logout } from "../api/auth";

const form = reactive({
  username: "admin",
  password: "1233321"
});

const notice = ref("");
const noticeType = ref("info");
const loading = ref(false);

function showNotice(type, message) {
  noticeType.value = type;
  notice.value = message;
}

async function onLogin() {
  loading.value = true;
  try {
    const { response, payload } = await login({
      username: form.username.trim(),
      password: form.password
    });
    if (response.ok) {
      showNotice("success", "登录成功");
    } else {
      const message = typeof payload === "object" && payload?.message ? payload.message : "登录失败";
      showNotice("error", `登录失败：${message}`);
    }
  } catch (error) {
    showNotice("error", `登录异常：${error.message}`);
  } finally {
    loading.value = false;
  }
}

async function onCurrentUser() {
  loading.value = true;
  try {
    const { response, payload } = await currentUser();
    if (response.ok) {
      const username = typeof payload === "object" && payload?.username ? payload.username : "当前会话用户";
      showNotice("success", `当前登录用户：${username}`);
    } else {
      showNotice("error", "当前未登录");
    }
  } catch (error) {
    showNotice("error", `查询异常：${error.message}`);
  } finally {
    loading.value = false;
  }
}

async function onLogout() {
  loading.value = true;
  try {
    const { response } = await logout();
    if (response.status === 204) {
      showNotice("success", "已退出登录");
    } else {
      showNotice("error", "退出失败");
    }
  } catch (error) {
    showNotice("error", `退出异常：${error.message}`);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <h1>登录</h1>
      <p class="sub-text">登录后可访问需要认证的业务接口。</p>
      <p class="notice" :data-type="noticeType" v-if="notice">{{ notice }}</p>

      <div class="field">
        <label>账号</label>
        <input v-model="form.username" autocomplete="username" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" autocomplete="current-password" />
      </div>

      <button class="primary" :disabled="loading" @click="onLogin">登录</button>

      <div class="button-row">
        <button class="secondary" :disabled="loading" @click="onCurrentUser">查看当前用户</button>
        <button class="secondary" :disabled="loading" @click="onLogout">退出登录</button>
      </div>

      <router-link class="switch-link" to="/register">没有账号？去注册</router-link>
    </section>
  </main>
</template>
