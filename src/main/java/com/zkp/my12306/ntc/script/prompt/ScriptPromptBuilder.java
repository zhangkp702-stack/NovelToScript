package com.zkp.my12306.ntc.script.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScriptPromptBuilder {

    public static final String SCHEMA_VERSION = "1.0.0";

    public String build(String title, List<String> chapters) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是专业的小说改编编剧。请将以下小说章节转换为结构化剧本初稿。\n\n");
        builder.append("输出要求：\n");
        builder.append("- 仅输出 YAML，不要输出解释性文字\n");
        builder.append("- 不要使用 markdown 代码块包裹\n");
        builder.append("- 必须包含顶层字段：metadata、characters、scenes、notes\n");
        builder.append("- metadata 必须包含：title、source_type、language、generated_at、schema_version\n");
        builder.append("- metadata.schema_version 固定为 ").append(SCHEMA_VERSION).append('\n');
        builder.append("- metadata.source_type 固定为 novel\n");
        builder.append("- metadata.language 固定为 zh-CN\n");
        builder.append("- characters 每项必须包含：id、name、role_type、description\n");
        builder.append("- scenes 每项必须包含：scene_id、scene_title、location、time、characters、action、dialogues\n");
        builder.append("- dialogues 每项必须包含：speaker、content\n");
        builder.append("- source_summary.chapter_count 应等于输入章节数\n\n");

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
}
