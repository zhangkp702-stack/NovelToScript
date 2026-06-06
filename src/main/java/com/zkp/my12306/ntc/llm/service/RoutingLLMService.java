package com.zkp.my12306.ntc.llm.service;

import com.zkp.my12306.ntc.llm.client.ChatClient;
import com.zkp.my12306.ntc.llm.config.LlmRuntimeProperties;
import com.zkp.my12306.ntc.llm.routing.ModelRoutingExecutor;
import com.zkp.my12306.ntc.llm.routing.ModelSelector;
import com.zkp.my12306.ntc.llm.routing.ModelTarget;
import com.zkp.my12306.ntc.llm.stream.BufferedProbeCallback;
import com.zkp.my12306.ntc.llm.stream.ProbeStreamBridge;
import com.zkp.my12306.ntc.llm.stream.StreamAsyncExecutor;
import com.zkp.my12306.ntc.llm.stream.StreamCallback;
import com.zkp.my12306.ntc.llm.stream.StreamCancellationHandle;
import com.zkp.my12306.ntc.llm.stream.StreamCancellationHandles;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RoutingLLMService implements LLMService {
    private final ModelSelector modelSelector;
    private final ModelRoutingExecutor modelRoutingExecutor;
    private final LlmRuntimeProperties llmRuntimeProperties;
    private final Map<String, ChatClient> chatClientMap;
    private final StreamAsyncExecutor streamAsyncExecutor;

    public RoutingLLMService(
            ModelSelector modelSelector,
            ModelRoutingExecutor modelRoutingExecutor,
            LlmRuntimeProperties llmRuntimeProperties,
            List<ChatClient> chatClients,
            StreamAsyncExecutor streamAsyncExecutor) {
        this.modelSelector = modelSelector;
        this.modelRoutingExecutor = modelRoutingExecutor;
        this.llmRuntimeProperties = llmRuntimeProperties;
        this.streamAsyncExecutor = streamAsyncExecutor;
        this.chatClientMap = new ConcurrentHashMap<>();
        for (ChatClient chatClient : chatClients) {
            this.chatClientMap.put(chatClient.provider(), chatClient);
        }
    }

    @Override
    public ChatResult chat(String prompt) {
        List<ModelTarget> targets = modelSelector.selectTargets();
        if (targets.isEmpty()) {
            throw new IllegalStateException("未找到可用的大模型配置");
        }
        return modelRoutingExecutor.executeChat(prompt, targets, chatClientMap);
    }

    @Override
    public StreamCancellationHandle streamChat(String prompt, StreamCallback callback) {
        List<ModelTarget> targets = modelSelector.selectTargets();
        if (targets.isEmpty()) {
            throw new IllegalStateException("未找到可用的大模型配置");
        }
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<StreamCancellationHandle> activeDelegate = new AtomicReference<>(StreamCancellationHandles.noOp());
        streamAsyncExecutor.execute(() -> {
            RuntimeException lastException = null;
            for (ModelTarget target : targets) {
                if (cancelled.get()) {
                    return;
                }
                ChatClient chatClient = chatClientMap.get(target.getProvider());
                if (chatClient == null) {
                    continue;
                }
                AtomicBoolean attemptActive = new AtomicBoolean(true);
                BufferedProbeCallback bufferedCallback = null;
                try {
                    StreamCallback guardedCallback = new StreamCallback() {
                        @Override
                        public void onOpen(String modelName) {
                            if (!cancelled.get() && attemptActive.get()) {
                                callback.onOpen(modelName);
                            }
                        }

                        @Override
                        public void onToken(String token) {
                            if (!cancelled.get() && attemptActive.get()) {
                                callback.onToken(token);
                            }
                        }

                        @Override
                        public void onComplete() {
                            if (!cancelled.get() && attemptActive.get()) {
                                callback.onComplete();
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (!cancelled.get() && attemptActive.get()) {
                                callback.onError(throwable);
                            }
                        }
                    };
                    bufferedCallback = new BufferedProbeCallback(guardedCallback);
                    ProbeStreamBridge bridge = new ProbeStreamBridge(bufferedCallback);
                    StreamCancellationHandle delegate = chatClient.streamChat(prompt, target, bridge);
                    activeDelegate.set(delegate);
                    long timeoutMs = llmRuntimeProperties.getRouting().getFirstTokenTimeoutMs();
                    ProbeStreamBridge.FirstEvent firstEvent = bridge.awaitFirstEvent(timeoutMs);
                    if (firstEvent.type() == ProbeStreamBridge.FirstEventType.TOKEN
                            || firstEvent.type() == ProbeStreamBridge.FirstEventType.COMPLETE) {
                        bufferedCallback.promoteAndFlush();
                        return;
                    }
                    attemptActive.set(false);
                    delegate.cancel();
                    bufferedCallback.clearBuffer();
                    if (firstEvent.type() == ProbeStreamBridge.FirstEventType.ERROR) {
                        Throwable error = bufferedCallback.getErrorBeforePromote();
                        Throwable cause = error == null ? firstEvent.throwable() : error;
                        lastException = new IllegalStateException("流式模型调用失败：" + target.getName(), cause);
                    } else {
                        lastException = new IllegalStateException("首包超时，已切换备用模型：" + target.getName());
                    }
                } catch (Exception ex) {
                    attemptActive.set(false);
                    if (bufferedCallback != null) {
                        bufferedCallback.clearBuffer();
                    }
                    lastException = new IllegalStateException("流式模型调用失败：" + target.getName(), ex);
                }
            }
            if (!cancelled.get()) {
                callback.onError(lastException == null ? new IllegalStateException("流式调用无可用模型") : lastException);
            }
        });
        return StreamCancellationHandles.of(() -> {
            cancelled.set(true);
            StreamCancellationHandle delegate = activeDelegate.get();
            if (delegate != null) {
                delegate.cancel();
            }
        });
    }
}
