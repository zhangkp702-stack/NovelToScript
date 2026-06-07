package com.zkp.my12306.ntc.script.input;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScriptInputValidator {

    public void validate(Integer chapterNumber, String chapterContent) {
        if (chapterNumber == null || chapterNumber < 1) {
            throw invalidChapterNumber(chapterNumber);
        }
        if (isBlank(chapterContent)) {
            throw emptyChapter(chapterNumber);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ScriptValidationException emptyChapter(int chapterNumber) {
        return new ScriptValidationException(
                ValidationErrorCode.EMPTY_CHAPTER,
                "请先填写第 " + chapterNumber + " 章内容",
                1,
                0,
                List.of(chapterNumber));
    }

    private ScriptValidationException invalidChapterNumber(Integer chapterNumber) {
        int index = chapterNumber == null ? 0 : chapterNumber;
        return new ScriptValidationException(
                ValidationErrorCode.INVALID_CHAPTER_NUMBER,
                "章节编号无效",
                1,
                0,
                List.of(index));
    }
}
