package com.zkp.my12306.ntc.llm.routing;

import com.zkp.my12306.ntc.llm.config.LlmRuntimeProperties;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ModelSelector {
    private final LlmRuntimeProperties llmRuntimeProperties;

    public ModelSelector(LlmRuntimeProperties llmRuntimeProperties) {
        this.llmRuntimeProperties = llmRuntimeProperties;
    }

    public List<ModelTarget> selectTargets() {
        List<ModelTarget> targets = llmRuntimeProperties.getModels().stream()
                .filter(item -> item.getEnabled() == null || item.getEnabled())
                .sorted(Comparator.comparing(item -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority()))
                .toList();
        for (ModelTarget target : targets) {
            validateTarget(target);
        }
        return targets;
    }

    private void validateTarget(ModelTarget target) {
        String targetName = target.getName() == null || target.getName().isBlank() ? "<unknown>" : target.getName();
        if (target.getProvider() == null || target.getProvider().isBlank()) {
            throw new IllegalStateException("模型配置缺少 provider：" + targetName);
        }
        if (target.getBaseUrl() == null || target.getBaseUrl().isBlank()) {
            throw new IllegalStateException("模型配置缺少 baseUrl：" + targetName);
        }
        if (target.getModel() == null || target.getModel().isBlank()) {
            throw new IllegalStateException("模型配置缺少 model：" + targetName);
        }
    }
}
