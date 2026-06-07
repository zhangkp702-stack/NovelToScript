package com.zkp.my12306.ntc.script.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptPromptBuilderTest {

    private final ScriptPromptBuilder builder = new ScriptPromptBuilder();

    @Test
    void build_includesPromptTemplateAndSingleChapter() {
        String prompt = builder.build("旧城雨夜", 2, "第二章小说内容");

        assertTrue(prompt.contains("专业影视剧本改编编剧"));
        assertTrue(prompt.contains("不是总结小说"));
        assertTrue(prompt.contains("作品标题：旧城雨夜"));
        assertTrue(prompt.contains("章节编号：2"));
        assertTrue(prompt.contains("原文字数约：7"));
        assertTrue(prompt.contains("剧本正文最少：400 字"));
        assertTrue(prompt.contains("【第 2 章】"));
        assertTrue(prompt.contains("第二章小说内容"));
        assertTrue(prompt.contains("本次只改编第 2 章"));
    }

    @Test
    void build_withoutTitle_usesFallbackLabel() {
        String prompt = builder.build(null, 1, "第一章");

        assertTrue(prompt.contains("作品标题：未提供"));
    }

    @Test
    void build_scalesMinimumScriptLengthWithSourceText() {
        String longChapter = "章".repeat(2000);
        String prompt = builder.build("长篇", 1, longChapter);

        assertTrue(prompt.contains("原文字数约：2000"));
        assertTrue(prompt.contains("剧本正文最少：1300 字"));
    }
}
