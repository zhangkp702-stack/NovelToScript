package com.zkp.my12306.ntc.llm.config;

import com.zkp.my12306.ntc.llm.routing.ModelTarget;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "llm")
public class LlmRuntimeProperties {
    private Routing routing = new Routing();
    private List<ModelTarget> models = new ArrayList<>();

    public Routing getRouting() {
        return routing;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
    }

    public List<ModelTarget> getModels() {
        return models;
    }

    public void setModels(List<ModelTarget> models) {
        this.models = models;
    }

    public static class Routing {
        private int firstTokenTimeoutMs = 60000;
        private int connectTimeoutMs = 10000;

        public int getFirstTokenTimeoutMs() {
            return firstTokenTimeoutMs;
        }

        public void setFirstTokenTimeoutMs(int firstTokenTimeoutMs) {
            this.firstTokenTimeoutMs = firstTokenTimeoutMs;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }
    }
}
