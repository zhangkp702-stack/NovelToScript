package com.zkp.my12306.ntc.script.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.zkp.my12306.ntc.script.model.ScriptDocument;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ScriptOutputParser {

    private static final Pattern FENCE_PATTERN = Pattern.compile(
            "```(?:yaml|yml|json)?\\s*([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public ScriptDocument parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new ScriptOutputException("LLM 返回内容为空");
        }
        String payload = extractPayload(rawContent.trim());
        JsonNode root = parsePayload(payload);
        return new ScriptDocument(root);
    }

    private String extractPayload(String rawContent) {
        Matcher matcher = FENCE_PATTERN.matcher(rawContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return rawContent;
    }

    private JsonNode parsePayload(String payload) {
        try {
            return yamlMapper.readTree(payload);
        } catch (Exception yamlError) {
            try {
                return jsonMapper.readTree(payload);
            } catch (Exception jsonError) {
                throw new ScriptOutputException("无法解析 LLM 输出为 YAML 或 JSON", yamlError);
            }
        }
    }
}
