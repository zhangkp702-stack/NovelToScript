package com.zkp.my12306.ntc.llm.stream.sse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkp.my12306.ntc.llm.stream.StreamCallback;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class OpenAIStyleSseParser {
    private final ObjectMapper objectMapper;

    public OpenAIStyleSseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void parse(InputStream inputStream, StreamCallback callback) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder eventData = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    if (eventData.length() > 0) {
                        if (processEventData(eventData.toString(), callback)) {
                            return;
                        }
                        eventData.setLength(0);
                    }
                    continue;
                }
                if (line.startsWith(":") || line.startsWith("event:") || line.startsWith("id:") || line.startsWith("retry:")) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String dataLine = line.substring(5).trim();
                if (eventData.length() > 0) {
                    eventData.append('\n');
                }
                eventData.append(dataLine);
            }
            if (eventData.length() > 0) {
                if (processEventData(eventData.toString(), callback)) {
                    return;
                }
            }
        }
        callback.onComplete();
    }

    private boolean processEventData(String data, StreamCallback callback) throws IOException {
        if (data.isEmpty()) {
            return false;
        }
        if ("[DONE]".equals(data)) {
            callback.onComplete();
            return true;
        }
        JsonNode root = objectMapper.readTree(data);
        JsonNode choices0 = root.path("choices").path(0);
        JsonNode content = choices0.path("delta").path("content");
        if (content.isMissingNode() || content.isNull()) {
            content = choices0.path("message").path("content");
        }
        if (content.isMissingNode() || content.isNull()) {
            content = choices0.path("text");
        }
        if (!content.isMissingNode() && !content.isNull()) {
            callback.onToken(content.asText(""));
        }
        return false;
    }
}
