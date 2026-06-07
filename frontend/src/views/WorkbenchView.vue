<script setup>
import { nextTick, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { currentUser, logout } from "../api/auth";
import { generateScriptStream } from "../api/script";
import ChapterFieldList from "../components/ChapterFieldList.vue";
import { validateChapterContent } from "../utils/chapterValidation";

const router = useRouter();
const title = ref("");
const chapters = ref([""]);
const results = ref([createEmptyResult()]);
const notice = ref("");
const noticeType = ref("info");
const loading = ref(false);
const streamingIndexes = ref(new Set());
const resultPanelRefs = ref([]);
const streamControllers = new Map();

function createEmptyResult() {
  return { content: "", model: "", streaming: false };
}

watch(
  () => chapters.value.length,
  (length) => {
    while (results.value.length < length) {
      results.value.push(createEmptyResult());
    }
  }
);

function showNotice(type, message) {
  noticeType.value = type;
  notice.value = message;
}

function setStreaming(index, streaming) {
  const next = new Set(streamingIndexes.value);
  if (streaming) {
    next.add(index);
  } else {
    next.delete(index);
  }
  streamingIndexes.value = next;
  results.value[index].streaming = streaming;
}

async function scrollResultToBottom(index) {
  await nextTick();
  const panel = resultPanelRefs.value[index];
  if (panel) {
    panel.scrollTop = panel.scrollHeight;
  }
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

function stopChapterStream(index) {
  const controller = streamControllers.get(index);
  if (controller) {
    controller.abort();
    streamControllers.delete(index);
  }
}

async function onGenerateChapter(index) {
  const chapterNumber = index + 1;
  const content = chapters.value[index] ?? "";
  const validation = validateChapterContent(content, chapterNumber);
  if (!validation.ok) {
    showNotice("error", validation.message);
    return;
  }

  stopChapterStream(index);
  const controller = new AbortController();
  streamControllers.set(index, controller);

  setStreaming(index, true);
  results.value[index].content = "";
  results.value[index].model = "";
  showNotice("info", `正在流式生成第 ${chapterNumber} 章剧本...`);

  try {
    const payload = {
      title: title.value.trim() || null,
      chapterNumber,
      chapterContent: content.trim()
    };

    const { response, payload: errorPayload } = await generateScriptStream(
      payload,
      {
        onOpen(modelName) {
          results.value[index].model = modelName;
        },
        onToken(token) {
          results.value[index].content += token;
          scrollResultToBottom(index);
        },
        onDone() {
          showNotice("success", `第 ${chapterNumber} 章剧本生成完成`);
        },
        onError(message) {
          showNotice("error", `第 ${chapterNumber} 章生成失败：${message}`);
        }
      },
      controller.signal
    );

    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }

    if (!response.ok) {
      const message = typeof errorPayload === "object" && errorPayload?.message
        ? errorPayload.message
        : `请求失败（HTTP ${response.status}）`;
      showNotice("error", `第 ${chapterNumber} 章生成失败：${message}`);
    }
  } catch (error) {
    if (error.name !== "AbortError") {
      showNotice("error", `第 ${chapterNumber} 章请求异常：${error.message}`);
    }
  } finally {
    setStreaming(index, false);
    streamControllers.delete(index);
  }
}
</script>

<template>
  <main class="workbench-page">
    <header class="workbench-header">
      <div>
        <h1>业务首页</h1>
        <p class="sub-text">左侧按章填写并生成，右侧对应展示每章的流式输出。</p>
      </div>
      <div class="button-row header-actions">
        <button class="secondary" :disabled="loading" @click="onLoadCurrentUser">查看当前用户</button>
        <button class="secondary" :disabled="loading" @click="onLogout">退出登录</button>
      </div>
    </header>

    <p class="notice workbench-notice" :data-type="noticeType" v-if="notice">{{ notice }}</p>

    <section class="workbench-layout">
      <div class="workbench-panel input-panel">
        <h2>用户提交</h2>

        <div class="field">
          <label for="title">作品标题（可选）</label>
          <input id="title" v-model="title" placeholder="请输入作品标题" />
        </div>

        <ChapterFieldList
          v-model="chapters"
          :streaming-indexes="streamingIndexes"
          @generate="onGenerateChapter"
        />
      </div>

      <div class="workbench-panel output-panel">
        <h2>大模型结果</h2>

        <div class="chapter-results">
          <div
            v-for="(result, index) in results"
            :key="index"
            class="chapter-result-item"
          >
            <h3 class="chapter-result-title">第 {{ index + 1 }} 章</h3>
            <p v-if="result.model" class="result-meta">模型：{{ result.model }}</p>
            <p v-else-if="result.streaming" class="result-meta">等待模型响应...</p>

            <pre
              :ref="(el) => { if (el) resultPanelRefs[index] = el; }"
              class="result-content"
              :class="{ streaming: result.streaming, empty: !result.content && !result.streaming }"
            >{{ result.content || (result.streaming ? "" : "本章生成结果将在这里展示") }}<span v-if="result.streaming" class="stream-cursor">▋</span></pre>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>
