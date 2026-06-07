package com.zkp.my12306.ntc.script.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.zkp.my12306.ntc.script.model.ScriptDocument;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ScriptOutputParser {

    private static final String NATURAL_SCRIPT_FORMAT = "natural_script";
    private static final Pattern FENCE_PATTERN = Pattern.compile(
            "```(?:yaml|yml|json|markdown|md)?\\s*([\\s\\S]*?)```",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public ScriptDocument parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new ScriptOutputException("LLM 返回内容为空");
        }
        String payload = extractPayload(rawContent.trim());
        if (NaturalScriptFormat.looksLikeNaturalScript(payload)) {
            return wrapNaturalScript(payload);
        }
        JsonNode root = parseStructuredPayload(payload);
        return new ScriptDocument(root);
    }

    private String extractPayload(String rawContent) {
        Matcher matcher = FENCE_PATTERN.matcher(rawContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return rawContent;
    }

    private ScriptDocument wrapNaturalScript(String payload) {
        ObjectNode root = jsonMapper.createObjectNode();
        root.put("format", NATURAL_SCRIPT_FORMAT);
        root.put("content", payload);
        return new ScriptDocument(root);
    }

    private JsonNode parseStructuredPayload(String payload) {
        try {
            return yamlMapper.readTree(payload);
        } catch (Exception yamlError) {
            try {
                return jsonMapper.readTree(payload);
            } catch (Exception jsonError) {
                throw new ScriptOutputException("无法解析 LLM 输出为自然剧本、YAML 或 JSON", yamlError);
            }
        }
    }
}
