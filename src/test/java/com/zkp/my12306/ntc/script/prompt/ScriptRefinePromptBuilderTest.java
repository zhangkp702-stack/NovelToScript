package com.zkp.my12306.ntc.script.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptRefinePromptBuilderTest {

    @Test
    void buildSystemPrompt_includesTitleChapterAndCharacters() {
        ScriptRefinePromptBuilder builder = new ScriptRefinePromptBuilder(new ScriptPromptBuilder());
        String prompt = builder.buildSystemPrompt(
                "雨夜归来",
                2,
                List.of(new CharacterPromptItem("林澈", "主角", "刑警", "冷静")));

        assertTrue(prompt.contains("雨夜归来"));
        assertTrue(prompt.contains("第 2 章"));
        assertTrue(prompt.contains("林澈"));
        assertTrue(prompt.contains("必须从「剧本标题：」开始"));
    }

    @Test
    void buildFirstUserMessage_includesScriptAndInstruction() {
        ScriptRefinePromptBuilder builder = new ScriptRefinePromptBuilder(new ScriptPromptBuilder());
        String message = builder.buildFirstUserMessage("剧本标题：《测试》\n场景一", "把对白写得更紧张");

        assertTrue(message.contains("当前剧本如下"));
        assertTrue(message.contains("剧本标题：《测试》"));
        assertTrue(message.contains("修改要求"));
        assertTrue(message.contains("把对白写得更紧张"));
    }
}
