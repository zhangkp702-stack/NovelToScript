<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { currentUser, logout } from "../api/auth";
import {
  createCharacter,
  createWork,
  deleteCharacter,
  deleteWork,
  generateScriptStream,
  generateWorkTitle,
  updateWorkTitle,
  listCharacters,
  listScriptsByWorkId,
  listWorks,
  saveScript,
  updateCharacter
} from "../api/script";
import ChapterFieldList from "../components/ChapterFieldList.vue";
import TaskSidebar from "../components/TaskSidebar.vue";
import { validateChapterContent } from "../utils/chapterValidation";
import {
  loadWorkbenchDraft,
  loadWorkbenchState,
  removeWorkbenchDraft,
  saveWorkbenchDraft
} from "../utils/workbenchDraft";

const SIDEBAR_COLLAPSED_KEY = "ntc_task_sidebar_collapsed";
const TITLE_MIN_EXCERPT = 20;

const router = useRouter();
const title = ref("");
const workId = ref("");
const works = ref([]);
const characters = ref([]);
const characterForm = ref(createEmptyCharacterForm());
const editingCharacterId = ref("");
const characterLoading = ref(false);
const sidebarCollapsed = ref(localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === "1");
const titleNamingWorkId = ref("");
const titleNamedWorkIds = new Set();
const titleManualWorkIds = new Set();
const taskTitleInput = ref("");
const titleSaving = ref(false);
let titleGenerateTimer = null;
let titleSaveTimer = null;
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

function createEmptyCharacterForm() {
  return {
    name: "",
    displayName: "",
    description: "",
    personality: ""
  };
}

function createEmptyResult() {
  return {
    content: "",
    model: "",
    status: "idle",
    error: "",
    warning: "",
    saved: false,
    recordId: null,
    traceId: "",
    generationId: ""
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
  if (!workId.value) {
    return;
  }
  saveWorkbenchDraft(workId.value, {
    chapterItems: chapterItems.value,
    resultsById: resultsById.value
  });
}

function applyDraft(draft) {
  if (!draft) {
    chapterItems.value = [createChapterItem()];
    resultsById.value = {};
    return;
  }
  if (Array.isArray(draft.chapterItems) && draft.chapterItems.length > 0) {
    chapterItems.value = draft.chapterItems.map((item) => ({
      id: item.id || crypto.randomUUID(),
      content: item.content || ""
    }));
  } else {
    chapterItems.value = [createChapterItem()];
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
          recordId: result?.recordId || null,
          traceId: result?.traceId || "",
          generationId: result?.generationId || ""
        }
      ])
    );
  } else {
    resultsById.value = {};
  }
}

function restoreDraftForWork(selectedWorkId) {
  applyDraft(loadWorkbenchDraft(selectedWorkId));
}

function firstChapterExcerpt() {
  const content = chapterItems.value[0]?.content ?? "";
  return content.trim();
}

function isUntitledWorkName(value) {
  const normalized = (value || "").trim();
  return !normalized || normalized === "未命名作品";
}

function syncTaskTitleInput() {
  taskTitleInput.value = isUntitledWorkName(title.value) ? "" : title.value;
}

function scheduleTitleGeneration() {
  if (!workId.value || titleManualWorkIds.has(workId.value) || titleNamingWorkId.value) {
    return;
  }
  if (!isUntitledWorkName(title.value)) {
    titleNamedWorkIds.add(workId.value);
    return;
  }
  const excerpt = firstChapterExcerpt();
  if (excerpt.length < TITLE_MIN_EXCERPT) {
    return;
  }
  if (titleGenerateTimer) {
    clearTimeout(titleGenerateTimer);
  }
  titleGenerateTimer = setTimeout(() => {
    void generateTitleForCurrentWork(excerpt);
  }, 900);
}

async function saveTaskTitle(manualTitle, { manual = true } = {}) {
  if (!workId.value || titleSaving.value) {
    return false;
  }
  const normalized = (manualTitle || "").trim();
  titleSaving.value = true;
  try {
    const { response, payload } = await updateWorkTitle(workId.value, normalized);
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return false;
    }
    if (!response.ok) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `任务名称保存失败（HTTP ${response.status}）`;
      showNotice("error", message);
      return false;
    }
    title.value = payload?.title ?? normalized;
    syncTaskTitleInput();
    if (manual && normalized) {
      titleManualWorkIds.add(workId.value);
      titleNamedWorkIds.add(workId.value);
    } else if (!normalized) {
      titleManualWorkIds.delete(workId.value);
      titleNamedWorkIds.delete(workId.value);
      scheduleTitleGeneration();
    } else {
      titleNamedWorkIds.add(workId.value);
    }
    await onRefreshWorks();
    return true;
  } catch (error) {
    showNotice("error", `任务名称保存失败：${error.message}`);
    return false;
  } finally {
    titleSaving.value = false;
  }
}

function scheduleTaskTitleSave() {
  if (!workId.value) {
    return;
  }
  if (titleSaveTimer) {
    clearTimeout(titleSaveTimer);
  }
  titleSaveTimer = setTimeout(() => {
    const input = taskTitleInput.value.trim();
    const current = isUntitledWorkName(title.value) ? "" : title.value.trim();
    if (input === current) {
      return;
    }
    void saveTaskTitle(input, { manual: true });
  }, 500);
}

async function onRenameTask({ work, title: nextTitle }) {
  if (!work?.workId) {
    return;
  }
  if (work.workId !== workId.value) {
    await loadWork(work);
  }
  taskTitleInput.value = nextTitle;
  const saved = await saveTaskTitle(nextTitle, { manual: Boolean(nextTitle) });
  if (saved && nextTitle) {
    showNotice("success", `任务已命名为「${nextTitle}」`);
  }
}

async function generateTitleForCurrentWork(excerpt = firstChapterExcerpt()) {
  const currentWorkId = workId.value;
  if (!currentWorkId || titleManualWorkIds.has(currentWorkId) || titleNamedWorkIds.has(currentWorkId)) {
    return;
  }
  if (excerpt.length < TITLE_MIN_EXCERPT) {
    return;
  }
  titleNamingWorkId.value = currentWorkId;
  try {
    const { response, payload } = await generateWorkTitle(currentWorkId, excerpt);
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (!response.ok) {
      return;
    }
    if (payload?.title) {
      title.value = payload.title;
      titleNamedWorkIds.add(currentWorkId);
      syncTaskTitleInput();
      await onRefreshWorks();
    }
  } catch {
    // 标题生成失败不阻断创作
  } finally {
    if (titleNamingWorkId.value === currentWorkId) {
      titleNamingWorkId.value = "";
    }
  }
}

function toggleSidebarCollapsed() {
  sidebarCollapsed.value = !sidebarCollapsed.value;
  localStorage.setItem(SIDEBAR_COLLAPSED_KEY, sidebarCollapsed.value ? "1" : "0");
}

async function persistChapterResult(index, chapter) {
  const result = resultsById.value[chapter.id];
  if (!result?.content) {
    return;
  }
  const payload = {
    workId: workId.value || null,
    workTitle: title.value.trim() || "",
    chapterNumber: index + 1,
    chapterContent: chapter.content.trim(),
    scriptContent: result.content,
    modelName: result.model || null,
    traceId: result.traceId || null,
    generationId: result.generationId || null
  };
  try {
    const { response, payload: savePayload } = await saveScript(payload);
    if (response.ok && savePayload?.id) {
      result.saved = true;
      result.recordId = savePayload.id;
      await onRefreshWorks();
    }
  } catch {
    // 保存失败不阻断生成结果展示
  }
}

function normalizeWorkTitle(value) {
  return (value || "").trim();
}

function displayWorkTitle(workTitle) {
  const normalized = normalizeWorkTitle(workTitle);
  if (!normalized || normalized === "未命名作品") {
    return "新任务";
  }
  return normalized;
}

function applyRecords(records) {
  const sorted = [...records].sort((a, b) => a.chapterNumber - b.chapterNumber);
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
        recordId: sorted[idx].id,
        traceId: sorted[idx].traceId || "",
        generationId: sorted[idx].generationId || ""
      }
    ])
  );
  return sorted.length;
}

function resetWorkbenchState() {
  title.value = "";
  chapterItems.value = [createChapterItem()];
  resultsById.value = {};
  characters.value = [];
  editingCharacterId.value = "";
  characterForm.value = createEmptyCharacterForm();
}

function resetCharacterForm() {
  editingCharacterId.value = "";
  characterForm.value = createEmptyCharacterForm();
}

function startEditCharacter(character) {
  editingCharacterId.value = character.id;
  characterForm.value = {
    name: character.name || "",
    displayName: character.displayName || "",
    description: character.description || "",
    personality: character.personality || ""
  };
}

async function onRefreshCharacters() {
  if (!workId.value) {
    characters.value = [];
    return;
  }
  characterLoading.value = true;
  try {
    const { response, payload } = await listCharacters(workId.value);
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (response.status === 404) {
      characters.value = [];
      return;
    }
    if (!response.ok) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `人物列表加载失败（HTTP ${response.status}）`;
      showNotice("error", message);
      return;
    }
    characters.value = Array.isArray(payload) ? payload : [];
  } catch (error) {
    showNotice("error", `人物列表加载失败：${error.message}`);
  } finally {
    characterLoading.value = false;
  }
}

async function onSaveCharacter() {
  if (!workId.value) {
    showNotice("info", "请先创建或选择作品，再添加人物设定");
    return;
  }
  const payload = {
    name: characterForm.value.name.trim(),
    displayName: characterForm.value.displayName.trim() || null,
    description: characterForm.value.description.trim() || null,
    personality: characterForm.value.personality.trim() || null
  };
  if (!payload.name) {
    showNotice("error", "人物名称不能为空");
    return;
  }
  characterLoading.value = true;
  try {
    const action = editingCharacterId.value
      ? updateCharacter(workId.value, editingCharacterId.value, payload)
      : createCharacter(workId.value, payload);
    const { response, payload: result } = await action;
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (!response.ok) {
      const message = typeof result === "object" && result?.message
        ? result.message
        : `保存人物失败（HTTP ${response.status}）`;
      showNotice("error", message);
      return;
    }
    resetCharacterForm();
    await onRefreshCharacters();
    showNotice("success", "人物设定已保存，下次生成将自动带上");
  } catch (error) {
    showNotice("error", `保存人物失败：${error.message}`);
  } finally {
    characterLoading.value = false;
  }
}

async function onDeleteCharacter(character) {
  if (!workId.value || !character?.id) {
    return;
  }
  if (!window.confirm(`确定删除人物「${character.name}」吗？`)) {
    return;
  }
  characterLoading.value = true;
  try {
    const { response, payload } = await deleteCharacter(workId.value, character.id);
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (!response.ok && response.status !== 204) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `删除人物失败（HTTP ${response.status}）`;
      showNotice("error", message);
      return;
    }
    if (editingCharacterId.value === character.id) {
      resetCharacterForm();
    }
    await onRefreshCharacters();
    showNotice("success", `已删除人物「${character.name}」`);
  } catch (error) {
    showNotice("error", `删除人物失败：${error.message}`);
  } finally {
    characterLoading.value = false;
  }
}

async function onRefreshWorks() {
  try {
    const { response, payload } = await listWorks();
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (!response.ok) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `作品列表加载失败（HTTP ${response.status}）`;
      showNotice("error", message);
      return;
    }
    works.value = Array.isArray(payload) ? payload : [];
  } catch (error) {
    showNotice("error", `作品列表加载失败：${error.message}`);
  }
}

async function loadWork(work) {
  if (!work?.workId) {
    return;
  }
  for (const controller of streamControllers.values()) {
    controller.abort();
  }
  streamControllers.clear();
  streamingIds.value = new Set();

  workId.value = work.workId;
  title.value = work.workTitle ?? "";
  syncTaskTitleInput();
  if (!isUntitledWorkName(title.value)) {
    titleNamedWorkIds.add(work.workId);
  }

  loading.value = true;
  try {
    const { response, payload } = await listScriptsByWorkId(work.workId);
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (!response.ok) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `任务加载失败（HTTP ${response.status}）`;
      showNotice("error", message);
      return;
    }
    if (Array.isArray(payload) && payload.length > 0) {
      applyRecords(payload);
    } else {
      restoreDraftForWork(work.workId);
    }
    await onRefreshCharacters();
    persistDraft();
  } catch (error) {
    showNotice("error", `任务加载失败：${error.message}`);
  } finally {
    loading.value = false;
  }
}

async function onNewWork() {
  for (const controller of streamControllers.values()) {
    controller.abort();
  }
  streamControllers.clear();
  streamingIds.value = new Set();

  loading.value = true;
  try {
    const { response, payload } = await createWork("");
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (!response.ok || !payload?.workId) {
      showNotice("error", "新建任务失败，请稍后重试");
      return;
    }
    workId.value = payload.workId;
    resetWorkbenchState();
    syncTaskTitleInput();
    await onRefreshWorks();
    persistDraft();
    scheduleTitleGeneration();
    showNotice("info", "已创建新任务，粘贴小说内容后将自动命名");
  } catch (error) {
    showNotice("error", `新建任务失败：${error.message}`);
  } finally {
    loading.value = false;
  }
}

async function onDeleteWork(work = null) {
  const targetWorkId = work?.workId || workId.value;
  if (!targetWorkId) {
    return;
  }
  const label = displayWorkTitle(work?.displayTitle || work?.workTitle || title.value);
  if (!window.confirm(`确定删除任务「${label}」及其全部内容吗？`)) {
    return;
  }
  loading.value = true;
  try {
    const { response, payload } = await deleteWork({ workId: targetWorkId, workTitle: title.value });
    if (response.status === 401) {
      showNotice("error", "登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    if (!response.ok && response.status !== 204 && response.status !== 404) {
      const message = typeof payload === "object" && payload?.message
        ? payload.message
        : `删除失败（HTTP ${response.status}）`;
      showNotice("error", message);
      return;
    }
    removeWorkbenchDraft(targetWorkId);
    titleNamedWorkIds.delete(targetWorkId);
    titleManualWorkIds.delete(targetWorkId);
    await onRefreshWorks();
    if (works.value.length > 0) {
      await loadWork(works.value[0]);
    } else {
      workId.value = "";
      resetWorkbenchState();
    }
    showNotice("success", `已删除任务「${label}」`);
  } catch (error) {
    showNotice("error", `删除任务失败：${error.message}`);
  } finally {
    loading.value = false;
  }
}

watch(workId, async (nextWorkId, previousWorkId) => {
  if (nextWorkId === previousWorkId) {
    return;
  }
  resetCharacterForm();
  await onRefreshCharacters();
});

watch([chapterItems, resultsById], scheduleDraftSave, { deep: true });

watch(taskTitleInput, () => {
  scheduleTaskTitleSave();
});

watch(
  () => chapterItems.value[0]?.content ?? "",
  () => {
    scheduleTitleGeneration();
  }
);

function handleBeforeUnload() {
  persistDraft();
}

onMounted(async () => {
  await onRefreshWorks();
  const state = loadWorkbenchState();
  const initialWork = state.activeWorkId
    ? works.value.find((item) => item.workId === state.activeWorkId)
    : works.value[0];
  if (initialWork) {
    await loadWork(initialWork);
  } else {
    await onNewWork();
  }
  window.addEventListener("beforeunload", handleBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer);
  }
  if (titleGenerateTimer) {
    clearTimeout(titleGenerateTimer);
  }
  if (titleSaveTimer) {
    clearTimeout(titleSaveTimer);
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
  result.traceId = "";
  result.generationId = crypto.randomUUID();
  result.status = "streaming";
  setStreaming(chapter.id, true);

  try {
    const payload = {
      workId: workId.value || null,
      generationId: result.generationId,
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
        onMeta(metaJson) {
          try {
            const meta = JSON.parse(metaJson);
            if (meta.workId && !workId.value) {
              workId.value = meta.workId;
            }
            if (meta.traceId) {
              result.traceId = meta.traceId;
            }
            if (meta.generationId) {
              result.generationId = meta.generationId;
            }
          } catch {
            // 忽略 meta 解析失败
          }
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
        <h1>剧本工作台</h1>
        <p class="sub-text">
          左侧为任务列表，中间按章填写并生成，右侧展示流式输出。
          <template v-if="title">当前任务：{{ displayWorkTitle(title) }}</template>
        </p>
      </div>
      <div class="button-row header-actions">
        <button class="secondary" :disabled="loading" @click="onLoadCurrentUser">查看当前用户</button>
        <button class="secondary" :disabled="loading" @click="onLogout">退出登录</button>
      </div>
    </header>

    <p class="notice workbench-notice" :data-type="noticeType" v-if="notice">{{ notice }}</p>

    <section class="workbench-shell" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <TaskSidebar
        :works="works"
        :active-work-id="workId"
        :collapsed="sidebarCollapsed"
        :naming-work-id="titleNamingWorkId"
        :loading="loading"
        @select="loadWork"
        @new="onNewWork"
        @delete="onDeleteWork"
        @rename="onRenameTask"
        @toggle-collapse="toggleSidebarCollapsed"
      />

      <section class="workbench-layout">
      <div class="workbench-panel input-panel">
        <h2>用户提交</h2>

        <section class="task-title-field">
          <label for="task-title">任务名称</label>
          <input
            id="task-title"
            v-model="taskTitleInput"
            :disabled="!workId || titleSaving || Boolean(titleNamingWorkId)"
            maxlength="32"
            placeholder="自定义名称；留空则根据第一章内容自动生成短标题"
          />
          <p class="task-title-hint">
            <template v-if="titleNamingWorkId === workId">正在自动生成标题...</template>
            <template v-else>也可在左侧列表双击任务名快速重命名</template>
          </p>
        </section>

        <section class="character-manager">
          <div class="character-manager-header">
            <h3>人物设定</h3>
            <button
              type="button"
              class="secondary work-action-btn"
              :disabled="characterLoading || !workId"
              @click="onRefreshCharacters"
            >
              刷新
            </button>
          </div>
          <p v-if="!workId" class="character-empty">新建或选择任务后，可在此维护跨章人物设定。</p>
          <template v-else>
            <ul v-if="characters.length > 0" class="character-list">
              <li
                v-for="character in characters"
                :key="character.id"
                class="character-list-item"
                :class="{ active: editingCharacterId === character.id }"
              >
                <button type="button" class="character-select-btn" @click="startEditCharacter(character)">
                  <span class="character-name">{{ character.name }}</span>
                  <span v-if="character.displayName" class="character-alias">{{ character.displayName }}</span>
                </button>
                <button
                  type="button"
                  class="character-delete-btn"
                  :disabled="characterLoading"
                  @click="onDeleteCharacter(character)"
                >
                  删除
                </button>
              </li>
            </ul>
            <p v-else class="character-empty">暂无人物设定，添加后下次生成会自动注入 prompt。</p>

            <div class="character-form">
              <div class="character-form-row">
                <label for="character-name">名称</label>
                <input id="character-name" v-model="characterForm.name" placeholder="剧本中使用的名称" />
              </div>
              <div class="character-form-row">
                <label for="character-display-name">别名</label>
                <input id="character-display-name" v-model="characterForm.displayName" placeholder="可选" />
              </div>
              <div class="character-form-row">
                <label for="character-description">身份</label>
                <textarea
                  id="character-description"
                  v-model="characterForm.description"
                  rows="2"
                  placeholder="身份或背景描述"
                />
              </div>
              <div class="character-form-row">
                <label for="character-personality">性格</label>
                <textarea
                  id="character-personality"
                  v-model="characterForm.personality"
                  rows="2"
                  placeholder="性格特征"
                />
              </div>
              <div class="character-form-actions">
                <button
                  type="button"
                  class="secondary work-action-btn"
                  :disabled="characterLoading"
                  @click="onSaveCharacter"
                >
                  {{ editingCharacterId ? "更新人物" : "添加人物" }}
                </button>
                <button
                  v-if="editingCharacterId"
                  type="button"
                  class="secondary work-action-btn"
                  :disabled="characterLoading"
                  @click="resetCharacterForm"
                >
                  取消编辑
                </button>
              </div>
            </div>
          </template>
        </section>

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
              <template v-if="resultsById[chapter.id]?.traceId">
                · trace：{{ resultsById[chapter.id].traceId }}
              </template>
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
    </section>
  </main>
</template>
