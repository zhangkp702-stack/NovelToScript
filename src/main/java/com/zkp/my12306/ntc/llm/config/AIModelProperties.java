package com.zkp.my12306.ntc.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIModelProperties {

    private Map<String, ProviderConfig> providers = new HashMap<>();
    private ModelGroup chat = new ModelGroup();
    private Selection selection = new Selection();
    private Stream stream = new Stream();
    private StreamExecutor streamExecutor = new StreamExecutor();

    @Data
    public static class ModelGroup {
        private String defaultModel;
        private String deepThinkingModel;
        private List<ModelCandidate> candidates = new ArrayList<>();
    }

    @Data
    public static class ModelCandidate {
        private String id;
        private String provider;
        private String model;
        private String url;
        private Integer priority = 100;
        private Boolean enabled = true;
        private Boolean supportsThinking = false;
    }

    @Data
    public static class ProviderConfig {
        private String url;
        private String apiKey;
        private Map<String, String> endpoints = new HashMap<>();
    }

    @Data
    public static class Selection {
        private Integer failureThreshold = 2;
        private Long openDurationMs = 30000L;
        private Integer firstTokenTimeoutMs = 60000;
        private Integer connectTimeoutMs = 10000;
        private Integer requestTimeoutMs = 120000;
    }

    @Data
    public static class Stream {
        private Integer messageChunkSize = 5;
    }

    @Data
    public static class StreamExecutor {
        private int coreSize = 4;
        private int maxSize = 16;
        private int queueCapacity = 200;
        private int keepAliveSeconds = 60;
        private String threadNamePrefix = "llm-stream-";
        private String rejectionPolicy = "caller-runs";
    }
}
