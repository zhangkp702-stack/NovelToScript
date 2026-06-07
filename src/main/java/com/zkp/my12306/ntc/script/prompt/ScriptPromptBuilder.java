package com.zkp.my12306.ntc.script.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ScriptPromptBuilder {

    private static final String PROMPT_RESOURCE = "prompt/script_generation.md";
    private static final String TITLE_PLACEHOLDER = "{{作品标题}}";
    private static final String CHAPTER_NUMBER_PLACEHOLDER = "{{章节编号}}";
    private static final String CHAPTER_CONTENT_PLACEHOLDER = "{{章节内容}}";
    private static final String SOURCE_CHARS_PLACEHOLDER = "{{原文字数}}";
    private static final String MIN_SCRIPT_CHARS_PLACEHOLDER = "{{最少正文字数}}";
    private static final String CHARACTER_SETTINGS_PLACEHOLDER = "{{人物设定}}";
    private static final double MIN_SCRIPT_RATIO = 0.65;

    private final String templateText;

    public ScriptPromptBuilder() {
        this.templateText = loadResource(PROMPT_RESOURCE);
    }

    public String build(String title, int chapterNumber, String chapterContent) {
        return build(title, chapterNumber, chapterContent, List.of());
    }

    public String build(String title, int chapterNumber, String chapterContent, List<CharacterPromptItem> characters) {
        String resolvedTitle = title == null || title.isBlank() ? "未提供" : title.trim();
        String normalizedContent = chapterContent == null ? "" : chapterContent.trim();
        int sourceChars = normalizedContent.length();
        int minScriptChars = Math.max(400, (int) Math.round(sourceChars * MIN_SCRIPT_RATIO));
        String chapterBlock = "【第 " + chapterNumber + " 章】\n" + normalizedContent;
        return templateText
                .replace(TITLE_PLACEHOLDER, resolvedTitle)
                .replace(CHAPTER_NUMBER_PLACEHOLDER, String.valueOf(chapterNumber))
                .replace(SOURCE_CHARS_PLACEHOLDER, String.valueOf(sourceChars))
                .replace(MIN_SCRIPT_CHARS_PLACEHOLDER, String.valueOf(minScriptChars))
                .replace(CHARACTER_SETTINGS_PLACEHOLDER, formatCharacterSettings(characters))
                .replace(CHAPTER_CONTENT_PLACEHOLDER, chapterBlock);
    }

    public String formatCharacterSettings(List<CharacterPromptItem> characters) {
        if (characters == null || characters.isEmpty()) {
            return "（暂无预定义人物。按原文识别角色；若本章出现新角色，须在出场人物中清晰标注其身份。）";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < characters.size(); i++) {
            CharacterPromptItem character = characters.get(i);
            builder.append("- 名称：").append(character.name());
            if (character.displayName() != null && !character.displayName().isBlank()
                    && !character.displayName().trim().equals(character.name())) {
                builder.append("（别名：").append(character.displayName().trim()).append("）");
            }
            if (character.description() != null && !character.description().isBlank()) {
                builder.append("\n  身份：").append(character.description().trim());
            }
            if (character.personality() != null && !character.personality().isBlank()) {
                builder.append("\n  性格：").append(character.personality().trim());
            }
            if (i < characters.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private String loadResource(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载剧本 prompt 资源: " + path, ex);
        }
    }
}
