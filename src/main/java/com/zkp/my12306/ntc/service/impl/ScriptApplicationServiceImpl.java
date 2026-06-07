package com.zkp.my12306.ntc.service.impl;

import com.zkp.my12306.ntc.dto.ScriptGenerateRequestDto;
import com.zkp.my12306.ntc.dto.ScriptGenerateResponseDto;
import com.zkp.my12306.ntc.llm.service.ChatResult;
import com.zkp.my12306.ntc.llm.service.LLMService;
import com.zkp.my12306.ntc.llm.stream.StreamCallback;
import com.zkp.my12306.ntc.llm.stream.StreamCancellationHandle;
import com.zkp.my12306.ntc.llm.trace.TraceRoot;
import com.zkp.my12306.ntc.script.input.ScriptInputValidator;
import com.zkp.my12306.ntc.script.model.ScriptDocument;
import com.zkp.my12306.ntc.script.parse.NaturalScriptFormat;
import com.zkp.my12306.ntc.script.parse.ScriptOutputParser;
import com.zkp.my12306.ntc.script.prompt.ScriptPromptBuilder;
import com.zkp.my12306.ntc.script.stream.StreamDegenerationGuard;
import com.zkp.my12306.ntc.script.validate.ScriptSchemaValidator;
import com.zkp.my12306.ntc.service.ScriptApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ScriptApplicationServiceImpl implements ScriptApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ScriptApplicationServiceImpl.class);
    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final LLMService llmService;
    private final ScriptInputValidator inputValidator;
    private final ScriptPromptBuilder promptBuilder;
    private final ScriptOutputParser outputParser;
    private final ScriptSchemaValidator schemaValidator;

    public ScriptApplicationServiceImpl(
            LLMService llmService,
            ScriptInputValidator inputValidator,
            ScriptPromptBuilder promptBuilder,
            ScriptOutputParser outputParser,
            ScriptSchemaValidator schemaValidator) {
        this.llmService = llmService;
        this.inputValidator = inputValidator;
        this.promptBuilder = promptBuilder;
        this.outputParser = outputParser;
        this.schemaValidator = schemaValidator;
    }

    @Override
    public void validateGenerateRequest(ScriptGenerateRequestDto request) {
        validateChapterRequest(request);
    }

    @Override
    @TraceRoot(name = "scriptGenerate")
    public ScriptGenerateResponseDto generateScript(ScriptGenerateRequestDto request, String currentUser) {
        ChapterRequest chapterRequest = validateChapterRequest(request);
        String prompt = promptBuilder.build(
                chapterRequest.title(),
                chapterRequest.chapterNumber(),
                chapterRequest.chapterContent());
        ChatResult llmResponse = llmService.chat(prompt);
        ScriptDocument document = outputParser.parse(llmResponse.content());
        schemaValidator.validate(document);
        return new ScriptGenerateResponseDto(
                llmResponse.modelName(),
                document.toMap(),
                llmResponse.content());
    }

    @Override
    @TraceRoot(name = "scriptGenerateStream")
    public void streamGenerateScript(ScriptGenerateRequestDto request, String currentUser, SseEmitter emitter) {
        ChapterRequest chapterRequest = validateChapterRequest(request);
        String prompt = promptBuilder.build(
                chapterRequest.title(),
                chapterRequest.chapterNumber(),
                chapterRequest.chapterContent());
        log.info("开始流式生成剧本: user={}, chapter={}, title={}, contentLength={}",
                currentUser,
                chapterRequest.chapterNumber(),
                chapterRequest.title() == null || chapterRequest.title().isBlank() ? "未命名作品" : chapterRequest.title(),
                chapterRequest.chapterContent().length());

        AtomicReference<StreamCancellationHandle> handleRef = new AtomicReference<>();
        AtomicBoolean streamFinished = new AtomicBoolean(false);
        StringBuilder accumulated = new StringBuilder();

        StreamCallback emitterCallback = new StreamCallback() {
            @Override
            public void onOpen(String modelName) {
                log.info("流式生成已建立连接: user={}, chapter={}, model={}",
                        currentUser, chapterRequest.chapterNumber(), modelName);
                sendSseEvent(emitter, "open", modelName == null ? "" : modelName);
            }

            @Override
            public void onToken(String token) {
                if (token != null) {
                    accumulated.append(token);
                }
                sendSseEvent(emitter, "token", token == null ? "" : token);
            }

            @Override
            public void onComplete() {
                if (!streamFinished.compareAndSet(false, true)) {
                    return;
                }
                log.info("流式生成完成: user={}, chapter={}, outputLength={}",
                        currentUser,
                        chapterRequest.chapterNumber(),
                        accumulated.length());
                emitStructureWarningIfNeeded(emitter, accumulated.toString());
                sendSseEvent(emitter, "done", "");
                emitter.complete();
            }

            @Override
            public void onError(Throwable throwable) {
                if (!streamFinished.compareAndSet(false, true)) {
                    return;
                }
                sendSseEvent(emitter, "error", resolveStreamErrorMessage(throwable));
                emitter.completeWithError(throwable);
            }
        };
        StreamDegenerationGuard guardedCallback = new StreamDegenerationGuard(
                emitterCallback,
                () -> cancelStream(handleRef.get(), null));
        StreamCancellationHandle handle = llmService.streamChat(prompt, guardedCallback);
        handleRef.set(handle);

        emitter.onTimeout(() -> {
            if (streamFinished.compareAndSet(false, true)) {
                cancelStream(handleRef.get(), emitter);
            }
        });
        emitter.onCompletion(() -> {
            if (!streamFinished.get()) {
                cancelStream(handleRef.get(), null);
            }
        });
    }

    private void cancelStream(StreamCancellationHandle handle, SseEmitter emitter) {
        if (handle != null && !handle.isCancelled()) {
            handle.cancel();
        }
        if (emitter != null) {
            emitter.complete();
        }
    }

    private void emitStructureWarningIfNeeded(SseEmitter emitter, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        try {
            if (NaturalScriptFormat.looksLikeNaturalScript(content)) {
                String error = NaturalScriptFormat.validateStructure(content);
                if (error != null) {
                    sendSseEvent(emitter, "warn", error);
                }
            }
        } catch (RuntimeException ignored) {
            // 轻量校验失败不影响已生成内容的交付
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data, TEXT_PLAIN_UTF8));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private String resolveStreamErrorMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? "生成失败，请稍后重试" : message;
    }

    private ChapterRequest validateChapterRequest(ScriptGenerateRequestDto request) {
        int chapterNumber = request == null || request.chapterNumber() == null ? 1 : request.chapterNumber();
        String content = request == null || request.chapterContent() == null ? "" : request.chapterContent().trim();
        inputValidator.validate(chapterNumber, content);
        return new ChapterRequest(request == null ? null : request.title(), chapterNumber, content);
    }

    private record ChapterRequest(String title, int chapterNumber, String chapterContent) {
    }
}
