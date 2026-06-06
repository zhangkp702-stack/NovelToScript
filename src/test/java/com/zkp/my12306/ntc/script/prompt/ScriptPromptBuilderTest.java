package com.zkp.my12306.ntc.script.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptPromptBuilderTest {

    private final ScriptPromptBuilder builder = new ScriptPromptBuilder();

    @Test
    void build_includesSchemaRequirementsAndChapters() {
        String prompt = builder.build("旧城雨夜", List.of("第一章内容", "第二章内容", "第三章内容"));

        assertTrue(prompt.contains("metadata.schema_version 固定为 1.0.0"));
        assertTrue(prompt.contains("小说标题：旧城雨夜"));
        assertTrue(prompt.contains("章节数量：3"));
        assertTrue(prompt.contains("第1章："));
        assertTrue(prompt.contains("第一章内容"));
    }
}
