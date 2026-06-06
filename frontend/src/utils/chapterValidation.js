export const MIN_CHAPTERS = 3;

export function countFilledChapters(chapters) {
  return chapters.filter((chapter) => chapter.trim().length > 0).length;
}

export function validateChapters(chapters) {
  const filledCount = countFilledChapters(chapters);
  if (filledCount < MIN_CHAPTERS) {
    return {
      ok: false,
      filledCount,
      message: `请至少填写 ${MIN_CHAPTERS} 个章节内容后再提交（当前已填写 ${filledCount} 个）`
    };
  }
  return {
    ok: true,
    filledCount,
    message: ""
  };
}

export function collectFilledChapters(chapters) {
  return chapters.map((chapter) => chapter.trim()).filter((chapter) => chapter.length > 0);
}
