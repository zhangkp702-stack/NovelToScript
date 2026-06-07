<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { currentUser, logout } from "../api/auth";
import { generateScript } from "../api/script";
import ChapterFieldList from "../components/ChapterFieldList.vue";
import { collectFilledChapters, validateChapters } from "../utils/chapterValidation";

const router = useRouter();
const title = ref("");
const chapters = ref(["", "", ""]);
const notice = ref("");
const noticeType = ref("info");
const loading = ref(false);
const resultContent = ref("");
const resultModel = ref("");

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

async function onGenerate() {
  const validation = validateChapters(chapters.value);
  if (!validation.ok) {
    showNotice("error", validation.message);
    return;
  }

  loading.value = true;
  resultContent.value = "";
  resultModel.value = "";
  showNotice("info", "正在调用大模型生成剧本，请稍候（可能需要 1-2 分钟）...");
  try {
    const payload = {
      title: title.value.trim() || null,
      chapters: collectFilledChapters(chapters.value)
    };
    const { response, payload: resBody } = await generateScript(payload);
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (response.ok) {
      resultContent.value = typeof resBody?.content === "string" ? resBody.content : "";
      resultModel.value = typeof resBody?.modelName === "string" ? resBody.modelName : "";
      showNotice("success", "剧本生成成功");
    } else {
      const message = typeof resBody === "object" && resBody?.message
        ? resBody.message
        : `请求失败（HTTP ${response.status}）`;
      showNotice("error", `生成失败：${message}`);
    }
  } catch (error) {
    showNotice("error", `请求异常：${error.message}`);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card workbench-card">
      <h1>业务首页</h1>
      <p class="sub-text">填写至少 3 个章节内容，即可生成剧本。</p>
      <p class="notice" :data-type="noticeType" v-if="notice">{{ notice }}</p>

      <div class="field">
        <label for="title">作品标题（可选）</label>
        <input id="title" v-model="title" placeholder="请输入作品标题" />
      </div>

      <ChapterFieldList v-model="chapters" />

      <button class="primary" :disabled="loading" @click="onGenerate">生成剧本</button>

      <div v-if="resultContent" class="result-panel">
        <h2>生成结果</h2>
        <p v-if="resultModel" class="result-meta">模型：{{ resultModel }}</p>
        <pre class="result-content">{{ resultContent }}</pre>
      </div>

      <div class="button-row">
        <button class="secondary" :disabled="loading" @click="onLoadCurrentUser">查看当前用户</button>
        <button class="secondary" :disabled="loading" @click="onLogout">退出登录</button>
      </div>
    </section>
  </main>
</template>
