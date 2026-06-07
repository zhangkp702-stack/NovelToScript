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

    public static final String SCHEMA_VERSION = "1.0.0";
    private static final String SCHEMA_RESOURCE = "schema/script_schema.yaml";
    private static final String SAMPLE_RESOURCE = "schema/sample_valid_script.yaml";

    private final String schemaText;
    private final String sampleText;

    public ScriptPromptBuilder() {
        this.schemaText = loadResource(SCHEMA_RESOURCE);
        this.sampleText = loadResource(SAMPLE_RESOURCE);
    }

    public String build(String title, List<String> chapters) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是专业的小说改编编剧。请严格按下方 Schema 与示例，将小说章节转换为结构化剧本初稿。\n\n");
        builder.append("输出要求：\n");
        builder.append("- 仅输出 YAML，不要输出解释性文字\n");
        builder.append("- 不要使用 markdown 代码块包裹\n");
        builder.append("- metadata.schema_version 固定为 ").append(SCHEMA_VERSION).append('\n');
        builder.append("- metadata.source_type 固定为 novel\n");
        builder.append("- metadata.language 固定为 zh-CN\n");
        builder.append("- source_summary.chapter_count 应等于输入章节数\n\n");

        builder.append("【Schema 规范】\n");
        builder.append(schemaText).append("\n\n");

        builder.append("【输出示例】\n");
        builder.append(sampleText).append("\n\n");

        builder.append("【改编任务】\n");
        if (title != null && !title.isBlank()) {
            builder.append("小说标题：").append(title.trim()).append('\n');
        }
        builder.append("章节数量：").append(chapters.size()).append('\n');
        for (int i = 0; i < chapters.size(); i++) {
            builder.append("第").append(i + 1).append("章：\n");
            builder.append(chapters.get(i).trim()).append("\n\n");
        }
        return builder.toString();
    }

    String schemaText() {
        return schemaText;
    }

    String sampleText() {
        return sampleText;
    }

    private String loadResource(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("无法加载剧本 prompt 资源: " + path, ex);
        }
    }
}
