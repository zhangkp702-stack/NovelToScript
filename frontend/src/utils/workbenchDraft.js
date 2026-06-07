const DRAFT_KEY = "ntc_workbench_draft";

export function loadWorkbenchDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object") {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function saveWorkbenchDraft(draft) {
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
  } catch {
    // 存储失败时静默忽略，不影响生成流程
  }
}

export function clearWorkbenchDraft() {
  localStorage.removeItem(DRAFT_KEY);
}
