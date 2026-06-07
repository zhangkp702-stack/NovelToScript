package com.zkp.my12306.ntc.script.prompt;

import com.zkp.my12306.ntc.script.prompt.CharacterPromptItem;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void build_includesCharacterSettingsWhenProvided() {
        String prompt = builder.build("旧城雨夜", 1, "第一章", List.of(
                new CharacterPromptItem("林澈", "小林", "档案管理员", "冷静克制")));

        assertTrue(prompt.contains("人物设定（跨章保持一致"));
        assertTrue(prompt.contains("- 名称：林澈（别名：小林）"));
        assertTrue(prompt.contains("身份：档案管理员"));
        assertTrue(prompt.contains("性格：冷静克制"));
    }

    @Test
    void build_withoutCharacters_usesFallbackHint() {
        String prompt = builder.build("旧城雨夜", 1, "第一章", List.of());

        assertTrue(prompt.contains("暂无预定义人物"));
    }
}
