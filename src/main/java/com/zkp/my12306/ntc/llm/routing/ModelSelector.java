package com.zkp.my12306.ntc.llm.routing;

import com.zkp.my12306.ntc.llm.config.AIModelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ModelSelector {
    private static final Logger log = LoggerFactory.getLogger(ModelSelector.class);

    private final AIModelProperties properties;

    public ModelSelector(AIModelProperties properties) {
        this.properties = properties;
    }

    public List<ModelTarget> selectChatCandidates() {
        return selectChatCandidates(false);
    }

    public List<ModelTarget> selectChatCandidates(boolean deepThinking) {
        AIModelProperties.ModelGroup group = properties.getChat();
        if (group == null) {
            return List.of();
        }
        String firstChoiceModelId = resolveFirstChoiceModel(group, deepThinking);
        return selectCandidates(group, firstChoiceModelId, deepThinking);
    }

    private String resolveFirstChoiceModel(AIModelProperties.ModelGroup group, boolean deepThinking) {
        if (deepThinking) {
            String deepModel = group.getDeepThinkingModel();
            if (deepModel != null && !deepModel.isBlank()) {
                return deepModel;
            }
        }
        return group.getDefaultModel();
    }

    private List<ModelTarget> selectCandidates(
            AIModelProperties.ModelGroup group,
            String firstChoiceModelId,
            boolean deepThinking) {
        if (group.getCandidates() == null) {
            return List.of();
        }

        List<AIModelProperties.ModelCandidate> orderedCandidates =
                filterAndSortCandidates(group.getCandidates(), firstChoiceModelId, deepThinking);
        return buildAvailableTargets(orderedCandidates);
    }

    private List<AIModelProperties.ModelCandidate> filterAndSortCandidates(
            List<AIModelProperties.ModelCandidate> candidates,
            String firstChoiceModelId,
            boolean deepThinking) {
        List<AIModelProperties.ModelCandidate> enabled = candidates.stream()
                .filter(candidate -> candidate != null && !Boolean.FALSE.equals(candidate.getEnabled()))
                .filter(candidate -> !deepThinking || Boolean.TRUE.equals(candidate.getSupportsThinking()))
                .sorted(Comparator
                        .comparing((AIModelProperties.ModelCandidate candidate) ->
                                !Objects.equals(resolveId(candidate), firstChoiceModelId))
                        .thenComparing(AIModelProperties.ModelCandidate::getPriority,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AIModelProperties.ModelCandidate::getId,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        if (deepThinking && enabled.isEmpty()) {
            log.warn("深度思考模式没有可用候选模型");
        }

        return enabled;
    }

    private List<ModelTarget> buildAvailableTargets(List<AIModelProperties.ModelCandidate> candidates) {
        Map<String, AIModelProperties.ProviderConfig> providers = properties.getProviders();
        return candidates.stream()
                .map(candidate -> buildModelTarget(candidate, providers))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ModelTarget buildModelTarget(
            AIModelProperties.ModelCandidate candidate,
            Map<String, AIModelProperties.ProviderConfig> providers) {
        String modelId = resolveId(candidate);
        AIModelProperties.ProviderConfig provider = providers.get(candidate.getProvider());
        if (provider == null) {
            log.warn("Provider配置缺失: provider={}, modelId={}", candidate.getProvider(), modelId);
            return null;
        }
        validateTarget(modelId, candidate, provider);
        return new ModelTarget(modelId, candidate, provider);
    }

    private void validateTarget(
            String modelId,
            AIModelProperties.ModelCandidate candidate,
            AIModelProperties.ProviderConfig provider) {
        if (candidate.getProvider() == null || candidate.getProvider().isBlank()) {
            throw new IllegalStateException("模型配置缺少 provider：" + modelId);
        }
        if (candidate.getModel() == null || candidate.getModel().isBlank()) {
            throw new IllegalStateException("模型配置缺少 model：" + modelId);
        }
        boolean hasCandidateUrl = candidate.getUrl() != null && !candidate.getUrl().isBlank();
        boolean hasProviderUrl = provider.getUrl() != null && !provider.getUrl().isBlank();
        if (!hasCandidateUrl && !hasProviderUrl) {
            throw new IllegalStateException("模型配置缺少 url：" + modelId);
        }
    }

    private String resolveId(AIModelProperties.ModelCandidate candidate) {
        if (candidate.getId() != null && !candidate.getId().isBlank()) {
            return candidate.getId();
        }
        return String.format("%s::%s",
                Objects.toString(candidate.getProvider(), "unknown"),
                Objects.toString(candidate.getModel(), "unknown"));
    }
}
