package com.zkp.my12306.ntc.script.validate;

import com.fasterxml.jackson.databind.JsonNode;
import com.zkp.my12306.ntc.script.model.ScriptDocument;
import com.zkp.my12306.ntc.script.parse.NaturalScriptFormat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScriptSchemaValidator {

    private static final String NATURAL_SCRIPT_FORMAT = "natural_script";

    public void validate(ScriptDocument document) {
        JsonNode root = document.root();
        if (isNaturalScript(root)) {
            validateNaturalScript(root);
            return;
        }
        List<String> errors = collectStructuredErrors(root);
        if (!errors.isEmpty()) {
            throw new ScriptSchemaValidationException(String.join("; ", errors));
        }
    }

    private boolean isNaturalScript(JsonNode root) {
        return root != null
                && root.isObject()
                && NATURAL_SCRIPT_FORMAT.equals(root.path("format").asText());
    }

    private void validateNaturalScript(JsonNode root) {
        String content = root.path("content").asText("");
        String error = NaturalScriptFormat.validateStructure(content);
        if (error != null) {
            throw new ScriptSchemaValidationException(error);
        }
    }

    List<String> collectStructuredErrors(JsonNode root) {
        List<String> errors = new ArrayList<>();
        if (root == null || !root.isObject()) {
            errors.add("根节点必须是对象");
            return errors;
        }
        requireObject(root, "metadata", errors);
        requireArray(root, "characters", errors);
        requireArray(root, "scenes", errors);
        requireObject(root, "notes", errors);

        JsonNode metadata = root.get("metadata");
        if (metadata != null && metadata.isObject()) {
            requireText(metadata, "title", errors);
            requireText(metadata, "source_type", errors);
            requireText(metadata, "language", errors);
            requireText(metadata, "generated_at", errors);
            requireText(metadata, "schema_version", errors);
        }

        JsonNode characters = root.get("characters");
        if (characters != null && characters.isArray()) {
            for (int i = 0; i < characters.size(); i++) {
                JsonNode item = characters.get(i);
                String prefix = "characters[" + i + "]";
                if (!item.isObject()) {
                    errors.add(prefix + " 必须是对象");
                    continue;
                }
                requireText(item, "id", errors, prefix);
                requireText(item, "name", errors, prefix);
                requireText(item, "role_type", errors, prefix);
                requireText(item, "description", errors, prefix);
            }
        }

        JsonNode scenes = root.get("scenes");
        if (scenes != null && scenes.isArray()) {
            for (int i = 0; i < scenes.size(); i++) {
                JsonNode item = scenes.get(i);
                String prefix = "scenes[" + i + "]";
                if (!item.isObject()) {
                    errors.add(prefix + " 必须是对象");
                    continue;
                }
                requireText(item, "scene_id", errors, prefix);
                requireText(item, "scene_title", errors, prefix);
                requireText(item, "location", errors, prefix);
                requireText(item, "time", errors, prefix);
                requireArray(item, "characters", errors, prefix);
                requireText(item, "action", errors, prefix);
                requireArray(item, "dialogues", errors, prefix);

                JsonNode dialogues = item.get("dialogues");
                if (dialogues != null && dialogues.isArray()) {
                    for (int j = 0; j < dialogues.size(); j++) {
                        JsonNode dialogue = dialogues.get(j);
                        String dialoguePrefix = prefix + ".dialogues[" + j + "]";
                        if (!dialogue.isObject()) {
                            errors.add(dialoguePrefix + " 必须是对象");
                            continue;
                        }
                        requireText(dialogue, "speaker", errors, dialoguePrefix);
                        requireText(dialogue, "content", errors, dialoguePrefix);
                    }
                }
            }
        }
        return errors;
    }

    private void requireObject(JsonNode parent, String field, List<String> errors) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            errors.add("缺少必填字段 " + field);
            return;
        }
        if (!node.isObject()) {
            errors.add(field + " 必须是对象");
        }
    }

    private void requireArray(JsonNode parent, String field, List<String> errors) {
        requireArray(parent, field, errors, null);
    }

    private void requireArray(JsonNode parent, String field, List<String> errors, String prefix) {
        JsonNode node = parent.get(field);
        String label = prefix == null ? field : prefix + "." + field;
        if (node == null || node.isNull()) {
            errors.add("缺少必填字段 " + label);
            return;
        }
        if (!node.isArray()) {
            errors.add(label + " 必须是数组");
        }
    }

    private void requireText(JsonNode parent, String field, List<String> errors) {
        requireText(parent, field, errors, null);
    }

    private void requireText(JsonNode parent, String field, List<String> errors, String prefix) {
        JsonNode node = parent.get(field);
        String label = prefix == null ? field : prefix + "." + field;
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            errors.add("缺少必填字段 " + label);
        }
    }
}
