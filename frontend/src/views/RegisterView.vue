<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { register } from "../api/auth";

const router = useRouter();
const form = reactive({
  account: "",
  password: "",
  userName: ""
});
const notice = ref("");
const noticeType = ref("info");
const loading = ref(false);

function showNotice(type, message) {
  noticeType.value = type;
  notice.value = message;
}

async function onRegister() {
  loading.value = true;
  try {
    const payload = {
      account: form.account.trim(),
      password: form.password,
      userName: form.userName.trim()
    };
    if (!payload.userName) {
      delete payload.userName;
    }
    const { response, payload: resBody } = await register(payload);
    if (response.status === 201) {
      showNotice("success", "注册成功，正在跳转登录页...");
    } else {
      const message = typeof resBody === "object" && resBody?.message ? resBody.message : "注册失败";
      showNotice("error", `注册失败：${message}`);
    }
    if (response.status === 201) {
      setTimeout(() => {
        router.push("/login");
      }, 600);
    }
  } catch (error) {
    showNotice("error", `注册异常：${error.message}`);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <h1>注册</h1>
      <p class="sub-text">创建新账号，注册成功后可直接返回登录页。</p>
      <p class="notice" :data-type="noticeType" v-if="notice">{{ notice }}</p>

      <div class="field">
        <label>账号</label>
        <input v-model="form.account" placeholder="4-32位字母、数字、下划线" autocomplete="username" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="6-64位" autocomplete="new-password" />
      </div>
      <div class="field">
        <label>昵称（可选）</label>
        <input v-model="form.userName" placeholder="不填默认同账号" />
      </div>

      <button class="primary" :disabled="loading" @click="onRegister">注册</button>
      <router-link class="switch-link" to="/login">已有账号？去登录</router-link>
    </section>
  </main>
</template>
