package com.zkp.my12306.ntc.llm.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AIModelPropertiesBindingTest {

    @Autowired
    private AIModelProperties aiModelProperties;

    @Test
    void shouldBindProvidersAndChatCandidatesFromApplicationYaml() {
        assertNotNull(aiModelProperties.getProviders());
        assertEquals(5, aiModelProperties.getProviders().size());
        assertNotNull(aiModelProperties.getProviders().get("ollama"));
        assertNotNull(aiModelProperties.getProviders().get("openai"));
        assertNotNull(aiModelProperties.getProviders().get("bailian"));
        assertEquals("http://127.0.0.1:11434", aiModelProperties.getProviders().get("ollama").getUrl());
        assertEquals("/v1/chat/completions",
                aiModelProperties.getProviders().get("ollama").getEndpoints().get("chat"));
        assertEquals("/compatible-mode/v1/chat/completions",
                aiModelProperties.getProviders().get("bailian").getEndpoints().get("chat"));

        assertNotNull(aiModelProperties.getChat());
        assertEquals("siliconflow-primary", aiModelProperties.getChat().getDefaultModel());
        assertEquals(6, aiModelProperties.getChat().getCandidates().size());

        AIModelProperties.ModelCandidate ollama = aiModelProperties.getChat().getCandidates().stream()
                .filter(candidate -> "ollama-local".equals(candidate.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("ollama", ollama.getProvider());
        assertEquals("qwen2.5:7b", ollama.getModel());
        assertFalse(ollama.getEnabled());

        AIModelProperties.ModelCandidate siliconflow = aiModelProperties.getChat().getCandidates().stream()
                .filter(candidate -> "siliconflow-primary".equals(candidate.getId()))
                .findFirst()
                .orElseThrow();
        assertTrue(siliconflow.getEnabled());

        AIModelProperties.ModelCandidate openai = aiModelProperties.getChat().getCandidates().stream()
                .filter(candidate -> "openai-gpt4o-mini".equals(candidate.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("openai", openai.getProvider());
        assertFalse(openai.getEnabled());

        AIModelProperties.ModelCandidate bailian = aiModelProperties.getChat().getCandidates().stream()
                .filter(candidate -> "bailian-qwen-plus".equals(candidate.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("bailian", bailian.getProvider());
        assertFalse(bailian.getEnabled());

        assertEquals(2, aiModelProperties.getSelection().getFailureThreshold());
        assertEquals(30000L, aiModelProperties.getSelection().getOpenDurationMs());
        assertEquals(60000, aiModelProperties.getSelection().getFirstTokenTimeoutMs());
        assertEquals(10000, aiModelProperties.getSelection().getConnectTimeoutMs());
        assertEquals(300000, aiModelProperties.getSelection().getRequestTimeoutMs());
        assertEquals(8192, aiModelProperties.getGeneration().getMaxTokens());
        assertEquals(0.3, aiModelProperties.getGeneration().getTemperature());
        assertEquals(0.25, aiModelProperties.getGeneration().getFrequencyPenalty());
        assertEquals(0.1, aiModelProperties.getGeneration().getPresencePenalty());
        assertEquals(4, aiModelProperties.getStreamExecutor().getCoreSize());
    }
}
