package com.zkp.my12306.ntc.script.parse;

import com.zkp.my12306.ntc.script.model.ScriptDocument;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptOutputParserTest {

    private final ScriptOutputParser parser = new ScriptOutputParser();

    @Test
    void parse_yamlFile_returnsDocument() throws Exception {
        String yaml = new ClassPathResource("script/sample_valid_script.yaml")
                .getContentAsString(StandardCharsets.UTF_8);

        ScriptDocument document = parser.parse(yaml);

        assertEquals("旧城雨夜", document.root().path("metadata").path("title").asText());
    }

    @Test
    void parse_fencedYaml_stripsFence() {
        String raw = """
                说明文字
                ```yaml
                metadata:
                  title: 测试
                  source_type: novel
                  language: zh-CN
                  generated_at: "2026-06-05T12:00:00+08:00"
                  schema_version: "1.0.0"
                characters: []
                scenes: []
                notes: {}
                ```
                """;

        ScriptDocument document = parser.parse(raw);

        assertEquals("测试", document.root().path("metadata").path("title").asText());
    }

    @Test
    void parse_emptyContent_throws() {
        ScriptOutputException ex = assertThrows(ScriptOutputException.class, () -> parser.parse("  "));
        assertTrue(ex.getMessage().contains("为空"));
    }
}
