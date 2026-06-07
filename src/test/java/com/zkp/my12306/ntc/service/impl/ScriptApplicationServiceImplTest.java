package com.zkp.my12306.ntc.service.impl;

import com.zkp.my12306.ntc.dto.ScriptGenerateRequestDto;
import com.zkp.my12306.ntc.llm.service.ChatResult;
import com.zkp.my12306.ntc.llm.service.LLMService;
import com.zkp.my12306.ntc.llm.stream.StreamCallback;
import com.zkp.my12306.ntc.llm.stream.StreamCancellationHandle;
import com.zkp.my12306.ntc.script.input.ScriptInputValidator;
import com.zkp.my12306.ntc.script.input.ScriptValidationException;
import com.zkp.my12306.ntc.script.input.ValidationErrorCode;
import com.zkp.my12306.ntc.script.parse.ScriptOutputParser;
import com.zkp.my12306.ntc.script.prompt.ScriptPromptBuilder;
import com.zkp.my12306.ntc.script.validate.ScriptSchemaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScriptApplicationServiceImplTest {

    @Mock
    private LLMService llmService;
    @Mock
    private ScriptInputValidator inputValidator;
    @Mock
    private ScriptPromptBuilder promptBuilder;
    @Mock
    private ScriptOutputParser outputParser;
    @Mock
    private ScriptSchemaValidator schemaValidator;

    @InjectMocks
    private ScriptApplicationServiceImpl service;

    @Test
    void generateScript_validationFails_doesNotCallLlm() {
        ScriptGenerateRequestDto request = new ScriptGenerateRequestDto("title", 1, "   ");
        doThrow(new ScriptValidationException(
                ValidationErrorCode.EMPTY_CHAPTER,
                "error",
                1,
                1,
                0,
                List.of(1)))
                .when(inputValidator).validate(1, "");

        assertThrows(ScriptValidationException.class, () -> service.generateScript(request, "user1"));
        verify(llmService, never()).chat(anyString());
    }

    @Test
    void generateScript_success_callsLlm() {
        String script = """
                剧本标题：《测试》
                原章节标题：第一章

                场景一：开场
                剧本正文：
                甲：台词
                """;
        ScriptGenerateRequestDto request = new ScriptGenerateRequestDto("title", 1, "第一章");
        when(promptBuilder.build("title", 1, "第一章")).thenReturn("prompt");
        when(llmService.chat("prompt")).thenReturn(new ChatResult(script, "ollama-local"));

        ScriptApplicationServiceImpl realService = new ScriptApplicationServiceImpl(
                llmService,
                new ScriptInputValidator(),
                promptBuilder,
                new ScriptOutputParser(),
                new ScriptSchemaValidator());

        realService.generateScript(request, "user1");
        verify(llmService).chat("prompt");
        verify(promptBuilder).build(eq("title"), eq(1), eq("第一章"));
    }

    @Test
    void streamGenerateScript_validationFails_doesNotCallLlm() {
        ScriptGenerateRequestDto request = new ScriptGenerateRequestDto("title", 1, "   ");
        doThrow(new ScriptValidationException(
                ValidationErrorCode.EMPTY_CHAPTER,
                "error",
                1,
                1,
                0,
                List.of(1)))
                .when(inputValidator).validate(1, "");

        assertThrows(ScriptValidationException.class,
                () -> service.streamGenerateScript(request, "user1", new SseEmitter()));
        verify(llmService, never()).streamChat(anyString(), any());
    }

    @Test
    void streamGenerateScript_success_invokesStreamCallback() {
        ScriptGenerateRequestDto request = new ScriptGenerateRequestDto("title", 1, "第一章");
        when(promptBuilder.build("title", 1, "第一章")).thenReturn("prompt");
        StreamCancellationHandle handle = mock(StreamCancellationHandle.class);
        doAnswer(invocation -> {
            StreamCallback callback = invocation.getArgument(1);
            callback.onOpen("model-x");
            callback.onToken("剧本标题：《测试》\n");
            callback.onToken("场景一：开场\n");
            callback.onComplete();
            return handle;
        }).when(llmService).streamChat(eq("prompt"), any(StreamCallback.class));

        SseEmitter emitter = new SseEmitter(5_000L);
        assertDoesNotThrow(() -> service.streamGenerateScript(request, "user1", emitter));

        ArgumentCaptor<StreamCallback> callbackCaptor = ArgumentCaptor.forClass(StreamCallback.class);
        verify(llmService).streamChat(eq("prompt"), callbackCaptor.capture());
        verify(promptBuilder).build(eq("title"), eq(1), eq("第一章"));
    }
}
