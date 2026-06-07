export function validateChapterContent(content, chapterNumber) {
  if (!content || !content.trim()) {
    return {
      ok: false,
      message: `请先填写第 ${chapterNumber} 章内容`
    };
  }
  return { ok: true, message: "" };
}
