package com.zkp.my12306.ntc.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkp.my12306.ntc.llm.routing.ModelTarget;
import com.zkp.my12306.ntc.llm.service.ChatResult;
import com.zkp.my12306.ntc.llm.stream.StreamAsyncExecutor;
import com.zkp.my12306.ntc.llm.stream.StreamCallback;
import com.zkp.my12306.ntc.llm.stream.StreamCancellationHandle;
import com.zkp.my12306.ntc.llm.stream.StreamCancellationHandles;
import com.zkp.my12306.ntc.llm.stream.sse.OpenAIStyleSseParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractOpenAIStyleChatClient implements ChatClient {
    private final ObjectMapper objectMapper;
    private final OpenAIStyleSseParser openAIStyleSseParser;
    private final StreamAsyncExecutor streamAsyncExecutor;
    private final HttpClient httpClient;

    protected AbstractOpenAIStyleChatClient(
            ObjectMapper objectMapper,
            OpenAIStyleSseParser openAIStyleSseParser,
            StreamAsyncExecutor streamAsyncExecutor,
            int connectTimeoutMs) {
        this.objectMapper = objectMapper;
        this.openAIStyleSseParser = openAIStyleSseParser;
        this.streamAsyncExecutor = streamAsyncExecutor;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    @Override
    public ChatResult chat(String prompt, ModelTarget modelTarget) {
        try {
            String body = buildRequestBody(prompt, modelTarget, false);
            HttpRequest request = buildRequest(modelTarget, body, modelTarget.getTimeoutMs());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("模型请求失败，provider=" + provider() + ", status=" + response.statusCode());
            }
            return parseChatResponse(response.body(), modelTarget.getName());
        } catch (Exception ex) {
            throw new IllegalStateException("模型调用失败，provider=" + provider(), ex);
        }
    }

    @Override
    public StreamCancellationHandle streamChat(String prompt, ModelTarget modelTarget, StreamCallback callback) {
        try {
            callback.onOpen(modelTarget.getName());
            String body = buildRequestBody(prompt, modelTarget, true);
            HttpRequest request = buildRequest(modelTarget, body, modelTarget.getTimeoutMs());
            AtomicBoolean cancelled = new AtomicBoolean(false);
            AtomicReference<CompletableFuture<?>> requestFutureRef = new AtomicReference<>();
            AtomicReference<CompletableFuture<?>> parseFutureRef = new AtomicReference<>();
            AtomicReference<InputStream> bodyStreamRef = new AtomicReference<>();
            CompletableFuture<HttpResponse<InputStream>> requestFuture =
                    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
            requestFutureRef.set(requestFuture);
            CompletableFuture<Void> parseFuture = requestFuture.thenCompose(response -> {
                if (cancelled.get()) {
                    return CompletableFuture.completedFuture(null);
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    callback.onError(new IllegalStateException("流式请求失败，status=" + response.statusCode()));
                    return CompletableFuture.completedFuture(null);
                }
                InputStream stream = response.body();
                bodyStreamRef.set(stream);
                CompletableFuture<Void> asyncParse = streamAsyncExecutor.execute(() -> parseStream(stream, callback));
                parseFutureRef.set(asyncParse);
                return asyncParse;
            });
            parseFutureRef.set(parseFuture);
            return StreamCancellationHandles.of(() -> {
                cancelled.set(true);
                CompletableFuture<?> rf = requestFutureRef.get();
                if (rf != null) {
                    rf.cancel(true);
                }
                CompletableFuture<?> pf = parseFutureRef.get();
                if (pf != null) {
                    pf.cancel(true);
                }
                InputStream stream = bodyStreamRef.getAndSet(null);
                if (stream != null) {
                    closeQuietly(stream);
                }
            });
        } catch (Exception ex) {
            callback.onError(ex);
            return StreamCancellationHandles.noOp();
        }
    }

    private HttpRequest buildRequest(ModelTarget modelTarget, String body, Integer timeoutMs) {
        int safeTimeout = timeoutMs == null || timeoutMs <= 0 ? 120000 : timeoutMs;
        String path = modelTarget.getChatCompletionsPath() == null || modelTarget.getChatCompletionsPath().isBlank()
                ? "/v1/chat/completions" : modelTarget.getChatCompletionsPath();
        return HttpRequest.newBuilder()
                .uri(URI.create(trimSlash(modelTarget.getBaseUrl()) + path))
                .timeout(Duration.ofMillis(safeTimeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + modelTarget.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private String trimSlash(String input) {
        if (input == null) {
            return "";
        }
        return input.endsWith("/") ? input.substring(0, input.length() - 1) : input;
    }

    private String buildRequestBody(String prompt, ModelTarget modelTarget, boolean stream) throws IOException {
        Map<String, Object> payload = Map.of(
                "model", modelTarget.getModel(),
                "stream", stream,
                "messages", List.of(Map.of("role", "user", "content", prompt)));
        return objectMapper.writeValueAsString(payload);
    }

    private ChatResult parseChatResponse(String responseBody, String modelName) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        return new ChatResult(content, modelName);
    }

    private void parseStream(InputStream body, StreamCallback callback) {
        try {
            openAIStyleSseParser.parse(body, callback);
        } catch (Exception ex) {
            callback.onError(ex);
        } finally {
            closeQuietly(body);
        }
    }

    private void closeQuietly(InputStream body) {
        if (body == null) {
            return;
        }
        try {
            body.close();
        } catch (IOException ignore) {
        }
    }
}
