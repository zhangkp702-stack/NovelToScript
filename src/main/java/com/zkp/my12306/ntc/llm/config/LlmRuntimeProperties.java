package com.zkp.my12306.ntc.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm")
public class LlmRuntimeProperties {

    private Trace trace = new Trace();
    private StreamExecutor streamExecutor = new StreamExecutor();

    public Trace getTrace() {
        return trace;
    }

    public void setTrace(Trace trace) {
        this.trace = trace;
    }

    public StreamExecutor getStreamExecutor() {
        return streamExecutor;
    }

    public void setStreamExecutor(StreamExecutor streamExecutor) {
        this.streamExecutor = streamExecutor;
    }

    public static class Trace {
        private boolean enabled = true;
        private int maxErrorLength = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxErrorLength() {
            return maxErrorLength;
        }

        public void setMaxErrorLength(int maxErrorLength) {
            this.maxErrorLength = maxErrorLength;
        }
    }

    public static class StreamExecutor {
        private int coreSize = 4;
        private int maxSize = 16;
        private int queueCapacity = 200;
        private int keepAliveSeconds = 60;
        private String threadNamePrefix = "llm-stream-";
        private String rejectionPolicy = "caller-runs";

        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        public String getRejectionPolicy() {
            return rejectionPolicy;
        }

        public void setRejectionPolicy(String rejectionPolicy) {
            this.rejectionPolicy = rejectionPolicy;
        }
    }
}
