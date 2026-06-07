package com.zkp.my12306.ntc.script.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class ScriptPromptBuilder {

    private static final String PROMPT_RESOURCE = "prompt/script_generation.md";
    private static final String TITLE_PLACEHOLDER = "{{作品标题}}";
    private static final String CHAPTER_NUMBER_PLACEHOLDER = "{{章节编号}}";
    private static final String CHAPTER_CONTENT_PLACEHOLDER = "{{章节内容}}";
    private static final String SOURCE_CHARS_PLACEHOLDER = "{{原文字数}}";
    private static final String MIN_SCRIPT_CHARS_PLACEHOLDER = "{{最少正文字数}}";
    private static final double MIN_SCRIPT_RATIO = 0.65;

    private final String templateText;

    public ScriptPromptBuilder() {
        this.templateText = loadResource(PROMPT_RESOURCE);
    }

    public String build(String title, int chapterNumber, String chapterContent) {
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
                .replace(CHAPTER_CONTENT_PLACEHOLDER, chapterBlock);
    }

    private String loadResource(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载剧本 prompt 资源: " + path, ex);
        }
    }
}
