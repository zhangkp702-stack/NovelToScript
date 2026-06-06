package com.zkp.my12306.ntc.script.input;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptInputValidatorTest {

    private final ScriptInputValidator validator = new ScriptInputValidator();

    @Test
    void validate_threeChapters_passes() {
        assertDoesNotThrow(() -> validator.validate(List.of("a", "b", "c")));
    }

    @Test
    void validate_twoChapters_throwsInsufficient() {
        ScriptValidationException ex = assertThrows(
                ScriptValidationException.class,
                () -> validator.validate(List.of("a", "b")));
        assertEquals(ValidationErrorCode.INSUFFICIENT_CHAPTERS, ex.getCode());
        assertTrue(ex.getMessage().contains("当前已填写 2 个"));
    }

    @Test
    void validate_gapChapter_throwsGap() {
        ScriptValidationException ex = assertThrows(
                ScriptValidationException.class,
                () -> validator.validate(List.of("a", "", "c")));
        assertEquals(ValidationErrorCode.CHAPTER_GAP, ex.getCode());
        assertEquals(List.of(2), ex.getInvalidIndexes());
    }

    @Test
    void validate_trailingBlankIgnored() {
        assertDoesNotThrow(() -> validator.validate(List.of("a", "b", "c", "")));
    }
}
