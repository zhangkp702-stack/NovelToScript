package com.zkp.my12306.ntc.script.input;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScriptInputValidator {

    public static final int MIN_CHAPTERS = 3;

    public void validate(List<String> chapters) {
        List<String> normalized = trimTrailingBlanks(chapters);
        int filledCount = 0;
        for (int i = 0; i < normalized.size(); i++) {
            if (isBlank(normalized.get(i))) {
                throw blankChapter(i + 1, countFilled(normalized));
            }
            filledCount++;
        }
        if (filledCount < MIN_CHAPTERS) {
            throw insufficient(filledCount);
        }
    }

    private List<String> trimTrailingBlanks(List<String> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return List.of();
        }
        int end = chapters.size();
        while (end > 0 && isBlank(chapters.get(end - 1))) {
            end--;
        }
        return new ArrayList<>(chapters.subList(0, end));
    }

    private int countFilled(List<String> chapters) {
        int count = 0;
        for (String chapter : chapters) {
            if (!isBlank(chapter)) {
                count++;
            }
        }
        return count;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ScriptValidationException insufficient(int filledCount) {
        return new ScriptValidationException(
                ValidationErrorCode.INSUFFICIENT_CHAPTERS,
                "请至少填写 " + MIN_CHAPTERS + " 个章节内容（当前已填写 " + filledCount + " 个）",
                MIN_CHAPTERS,
                filledCount,
                List.of());
    }

    private ScriptValidationException blankChapter(int chapterIndex, int filledCount) {
        return new ScriptValidationException(
                ValidationErrorCode.CHAPTER_GAP,
                "请先填写第 " + chapterIndex + " 章，再填写后续章节",
                MIN_CHAPTERS,
                filledCount,
                List.of(chapterIndex));
    }
}
