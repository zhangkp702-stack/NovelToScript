package com.zkp.my12306.ntc.script.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptInputValidatorTest {

    private final ScriptInputValidator validator = new ScriptInputValidator();

    @Test
    void validate_singleChapter_passes() {
        assertDoesNotThrow(() -> validator.validate(1, "第一章内容"));
    }

    @Test
    void validate_emptyChapter_throwsEmptyChapter() {
        ScriptValidationException ex = assertThrows(
                ScriptValidationException.class,
                () -> validator.validate(2, "  "));
        assertEquals(ValidationErrorCode.EMPTY_CHAPTER, ex.getCode());
        assertEquals(2, ex.getInvalidIndexes().get(0));
    }

    @Test
    void validate_invalidChapterNumber_throwsInvalidNumber() {
        ScriptValidationException ex = assertThrows(
                ScriptValidationException.class,
                () -> validator.validate(0, "内容"));
        assertEquals(ValidationErrorCode.INVALID_CHAPTER_NUMBER, ex.getCode());
    }
}
