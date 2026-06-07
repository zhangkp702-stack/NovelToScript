<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { currentUser, logout } from "../api/auth";
import { generateScriptStream, listScripts, saveScript } from "../api/script";
import ChapterFieldList from "../components/ChapterFieldList.vue";
import { validateChapterContent } from "../utils/chapterValidation";
import { loadWorkbenchDraft, saveWorkbenchDraft } from "../utils/workbenchDraft";

const router = useRouter();
const title = ref("");
const chapterItems = ref([createChapterItem()]);
const resultsById = ref({});
const notice = ref("");
const noticeType = ref("info");
const loading = ref(false);
const streamingIds = ref(new Set());
const resultPanelRefs = ref({});
const streamControllers = new Map();
let draftSaveTimer = null;

function createChapterItem(content = "") {
  return { id: crypto.randomUUID(), content };
}

function createEmptyResult() {
  return {
    content: "",
    model: "",
    status: "idle",
    error: "",
    warning: "",
    saved: false,
    recordId: null
  };
}

function ensureResult(id) {
  if (!resultsById.value[id]) {
    resultsById.value[id] = createEmptyResult();
  }
  return resultsById.value[id];
}

function syncResultsWithChapters() {
  const next = { ...resultsById.value };
  const activeIds = new Set(chapterItems.value.map((item) => item.id));
  for (const id of Object.keys(next)) {
    if (!activeIds.has(id)) {
      delete next[id];
    }
  }
  for (const item of chapterItems.value) {
    if (!next[item.id]) {
      next[item.id] = createEmptyResult();
    }
  }
  resultsById.value = next;
}

watch(chapterItems, syncResultsWithChapters, { deep: true, immediate: true });

function showNotice(type, message) {
  noticeType.value = type;
  notice.value = message;
}

function setStreaming(id, streaming) {
  const next = new Set(streamingIds.value);
  if (streaming) {
    next.add(id);
  } else {
    next.delete(id);
  }
  streamingIds.value = next;
  const result = ensureResult(id);
  result.status = streaming ? "streaming" : result.status === "streaming" ? "idle" : result.status;
}

function scheduleDraftSave() {
  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer);
  }
  draftSaveTimer = setTimeout(persistDraft, 400);
}

function persistDraft() {
  saveWorkbenchDraft({
    title: title.value,
    chapterItems: chapterItems.value,
    resultsById: resultsById.value
  });
}

function restoreDraft() {
  const draft = loadWorkbenchDraft();
  if (!draft) {
    return;
  }
  if (typeof draft.title === "string") {
    title.value = draft.title;
  }
  if (Array.isArray(draft.chapterItems) && draft.chapterItems.length > 0) {
    chapterItems.value = draft.chapterItems.map((item) => ({
      id: item.id || crypto.randomUUID(),
      content: item.content || ""
    }));
  }
  if (draft.resultsById && typeof draft.resultsById === "object") {
    resultsById.value = Object.fromEntries(
      Object.entries(draft.resultsById).map(([id, result]) => [
        id,
        {
          content: result?.content || "",
          model: result?.model || "",
          status: result?.status || "idle",
          error: result?.error || "",
          warning: result?.warning || "",
          saved: result?.saved || false,
          recordId: result?.recordId || null
        }
      ])
    );
  }
}

async function persistChapterResult(index, chapter) {
  const result = resultsById.value[chapter.id];
  if (!result?.content) {
    return;
  }
  const payload = {
    workTitle: title.value.trim() || "",
    chapterNumber: index + 1,
    chapterContent: chapter.content.trim(),
    scriptContent: result.content,
    modelName: result.model || null
  };
  try {
    const { response, payload: savePayload } = await saveScript(payload);
    if (response.ok && savePayload?.id) {
      result.saved = true;
      result.recordId = savePayload.id;
    }
  } catch {
    // 保存失败不阻断生成结果展示
  }
}

async function onLoadHistory() {
  loading.value = true;
  try {
    const { response, payload } = await listScripts(title.value.trim());
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (!response.ok) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `加载失败（HTTP ${response.status}）`;
      showNotice("error", message);
      return;
    }
    if (!Array.isArray(payload) || payload.length === 0) {
      showNotice("info", "当前作品标题下暂无历史记录");
      return;
    }
    const sorted = [...payload].sort((a, b) => a.chapterNumber - b.chapterNumber);
    chapterItems.value = sorted.map((record) => ({
      id: crypto.randomUUID(),
      content: record.chapterContent || ""
    }));
    resultsById.value = Object.fromEntries(
      chapterItems.value.map((item, idx) => [
        item.id,
        {
          content: sorted[idx].scriptContent || "",
          model: sorted[idx].modelName || "",
          status: "done",
          error: "",
          warning: "",
          saved: true,
          recordId: sorted[idx].id
        }
      ])
    );
    showNotice("success", `已加载 ${sorted.length} 章历史记录`);
  } catch (error) {
    showNotice("error", `加载历史失败：${error.message}`);
  } finally {
    loading.value = false;
  }
}

watch([title, chapterItems, resultsById], scheduleDraftSave, { deep: true });

function handleBeforeUnload() {
  persistDraft();
}

onMounted(() => {
  restoreDraft();
  window.addEventListener("beforeunload", handleBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer);
  }
  persistDraft();
  for (const controller of streamControllers.values()) {
    controller.abort();
  }
});

async function scrollResultToBottom(id) {
  await nextTick();
  const panel = resultPanelRefs.value[id];
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

function stopChapterStream(id) {
  const controller = streamControllers.get(id);
  if (controller) {
    controller.abort();
    streamControllers.delete(id);
  }
}

function onCancelChapter(index) {
  const chapter = chapterItems.value[index];
  if (!chapter) {
    return;
  }
  stopChapterStream(chapter.id);
  const result = ensureResult(chapter.id);
  result.status = "cancelled";
  result.error = "";
  setStreaming(chapter.id, false);
}

function onRemoveChapter(index) {
  const chapter = chapterItems.value[index];
  if (!chapter) {
    return;
  }
  if (streamingIds.value.has(chapter.id)) {
    return;
  }
  stopChapterStream(chapter.id);
  chapterItems.value.splice(index, 1);
  if (chapterItems.value.length === 0) {
    chapterItems.value.push(createChapterItem());
  }
}

async function copyChapterResult(id) {
  const result = resultsById.value[id];
  if (!result?.content) {
    return;
  }
  try {
    await navigator.clipboard.writeText(result.content);
    showNotice("success", "本章结果已复制到剪贴板");
  } catch (error) {
    showNotice("error", `复制失败：${error.message}`);
  }
}

function statusLabel(status) {
  switch (status) {
    case "streaming":
      return "生成中";
    case "done":
      return "已完成";
    case "error":
      return "失败";
    case "cancelled":
      return "已取消";
    default:
      return "待生成";
  }
}

async function onGenerateChapter(index) {
  const chapter = chapterItems.value[index];
  if (!chapter) {
    return;
  }
  const chapterNumber = index + 1;
  const content = chapter.content ?? "";
  const validation = validateChapterContent(content, chapterNumber);
  if (!validation.ok) {
    const result = ensureResult(chapter.id);
    result.status = "error";
    result.error = validation.message;
    return;
  }

  stopChapterStream(chapter.id);
  const controller = new AbortController();
  streamControllers.set(chapter.id, controller);

  const result = ensureResult(chapter.id);
  result.content = "";
  result.model = "";
  result.error = "";
  result.warning = "";
  result.saved = false;
  result.recordId = null;
  result.status = "streaming";
  setStreaming(chapter.id, true);

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
          result.model = modelName;
        },
        onToken(token) {
          result.content += token;
          scrollResultToBottom(chapter.id);
        },
        onWarn(message) {
          result.warning = message;
        },
        onDone() {
          result.status = "done";
        },
        onError(message) {
          result.status = "error";
          result.error = message;
        }
      },
      controller.signal
    );

    if (response.status === 401) {
      result.status = "error";
      result.error = "登录已过期，请重新登录";
      router.push("/login");
      return;
    }

    if (!response.ok) {
      const message = typeof errorPayload === "object" && errorPayload?.message
        ? errorPayload.message
        : `请求失败（HTTP ${response.status}）`;
      result.status = "error";
      result.error = message;
    } else if (result.status === "done" && result.content) {
      await persistChapterResult(index, chapter);
    }
  } catch (error) {
    if (error.name === "AbortError") {
      if (result.status === "streaming") {
        result.status = "cancelled";
      }
    } else {
      result.status = "error";
      result.error = error.message;
    }
  } finally {
    setStreaming(chapter.id, false);
    streamControllers.delete(chapter.id);
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

        <div class="field work-title-field">
          <label for="title">作品标题（可选）</label>
          <div class="work-title-row">
            <input id="title" v-model="title" placeholder="请输入作品标题" />
            <button type="button" class="secondary load-history-btn" :disabled="loading" @click="onLoadHistory">
              加载历史
            </button>
          </div>
        </div>

        <ChapterFieldList
          v-model="chapterItems"
          :streaming-ids="streamingIds"
          @generate="onGenerateChapter"
          @cancel="onCancelChapter"
          @remove="onRemoveChapter"
        />
      </div>

      <div class="workbench-panel output-panel">
        <h2>大模型结果</h2>

        <div class="chapter-results">
          <div
            v-for="(chapter, index) in chapterItems"
            :key="chapter.id"
            class="chapter-result-item"
          >
            <div class="chapter-result-header">
              <h3 class="chapter-result-title">第 {{ index + 1 }} 章</h3>
              <div class="chapter-result-actions">
                <span class="chapter-status" :data-status="resultsById[chapter.id]?.status || 'idle'">
                  {{ statusLabel(resultsById[chapter.id]?.status || 'idle') }}
                  <template v-if="resultsById[chapter.id]?.saved"> · 已保存</template>
                </span>
                <button
                  type="button"
                  class="copy-result-btn"
                  :disabled="!resultsById[chapter.id]?.content"
                  @click="copyChapterResult(chapter.id)"
                >
                  复制
                </button>
              </div>
            </div>

            <p v-if="resultsById[chapter.id]?.model" class="result-meta">
              模型：{{ resultsById[chapter.id].model }}
            </p>
            <p v-else-if="streamingIds.has(chapter.id)" class="result-meta">等待模型响应...</p>

            <p
              v-if="resultsById[chapter.id]?.error"
              class="chapter-error"
            >
              {{ resultsById[chapter.id].error }}
            </p>
            <p
              v-if="resultsById[chapter.id]?.warning"
              class="chapter-warning"
            >
              结构提示：{{ resultsById[chapter.id].warning }}
            </p>

            <pre
              :ref="(el) => { if (el) resultPanelRefs[chapter.id] = el; }"
              class="result-content"
              :class="{
                streaming: streamingIds.has(chapter.id),
                empty: !resultsById[chapter.id]?.content && !streamingIds.has(chapter.id)
              }"
            >{{ resultsById[chapter.id]?.content || (streamingIds.has(chapter.id) ? '' : '本章生成结果将在这里展示') }}<span v-if="streamingIds.has(chapter.id)" class="stream-cursor">▋</span></pre>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>
