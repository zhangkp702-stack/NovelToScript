package com.zkp.my12306.ntc.script.validate;

import com.zkp.my12306.ntc.script.model.ScriptDocument;
import com.zkp.my12306.ntc.script.parse.ScriptOutputParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptSchemaValidatorTest {

    private final ScriptOutputParser parser = new ScriptOutputParser();
    private final ScriptSchemaValidator validator = new ScriptSchemaValidator();

    @Test
    void validate_currentPromptFormat_passes() {
        String script = """
                剧本标题：《雨夜归来》
                原章节标题：第一章

                场景一：凌晨来信
                场景头：内景，卧室，凌晨

                剧本正文：
                旁白：凌晨三点，林川被手机惊醒。
                """;
        ScriptDocument document = parser.parse(script);

        assertDoesNotThrow(() -> validator.validate(document));
    }

    @Test
    void validate_naturalScriptMissingScene_fails() {
        String script = """
                剧本标题：《雨夜归来》
                原章节标题：第一章
                """;
        ScriptDocument document = parser.parse(script);

        ScriptSchemaValidationException ex = assertThrows(
                ScriptSchemaValidationException.class,
                () -> validator.validate(document));
        assertTrue(ex.getMessage().contains("场景"));
    }

    @Test
    void validate_sampleDocument_passes() throws Exception {
        String yaml = new ClassPathResource("script/sample_valid_script.yaml")
                .getContentAsString(StandardCharsets.UTF_8);
        ScriptDocument document = parser.parse(yaml);

        assertDoesNotThrow(() -> validator.validate(document));
    }

    @Test
    void validate_missingMetadataTitle_fails() {
        String yaml = """
                metadata:
                  source_type: novel
                  language: zh-CN
                  generated_at: "2026-06-05T12:00:00+08:00"
                  schema_version: "1.0.0"
                characters: []
                scenes: []
                notes: {}
                """;
        ScriptDocument document = parser.parse(yaml);

        ScriptSchemaValidationException ex = assertThrows(
                ScriptSchemaValidationException.class,
                () -> validator.validate(document));
        assertTrue(ex.getMessage().contains("title"));
    }
}
