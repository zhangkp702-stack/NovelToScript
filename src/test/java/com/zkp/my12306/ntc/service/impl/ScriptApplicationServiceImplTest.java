package com.zkp.my12306.ntc.service.impl;

import com.zkp.my12306.ntc.dto.ScriptGenerateRequestDto;
import com.zkp.my12306.ntc.llm.service.ChatResult;
import com.zkp.my12306.ntc.llm.service.LLMService;
import com.zkp.my12306.ntc.script.input.ScriptInputValidator;
import com.zkp.my12306.ntc.script.input.ScriptValidationException;
import com.zkp.my12306.ntc.script.input.ValidationErrorCode;
import com.zkp.my12306.ntc.script.parse.ScriptOutputParser;
import com.zkp.my12306.ntc.script.prompt.ScriptPromptBuilder;
import com.zkp.my12306.ntc.script.validate.ScriptSchemaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
        ScriptGenerateRequestDto request = new ScriptGenerateRequestDto("title", List.of("a", "b"));
        doThrow(new ScriptValidationException(
                ValidationErrorCode.INSUFFICIENT_CHAPTERS,
                "error",
                3,
                2,
                List.of()))
                .when(inputValidator).validate(List.of("a", "b"));

        assertThrows(ScriptValidationException.class, () -> service.generateScript(request, "user1"));
        verify(llmService, never()).chat(anyString());
    }

    @Test
    void generateScript_success_callsLlm() {
        String yaml = """
                metadata:
                  title: 测试
                  source_type: novel
                  language: zh-CN
                  generated_at: "2026-06-05T12:00:00+08:00"
                  schema_version: "1.0.0"
                characters:
                  - id: char_001
                    name: 甲
                    role_type: protagonist
                    description: 主角
                scenes:
                  - scene_id: scene_001
                    scene_title: 场景
                    location: 室内
                    time: 白天
                    characters:
                      - char_001
                    action: 行动
                    dialogues:
                      - speaker: char_001
                        content: 台词
                notes:
                  adaptation_strategy: 测试
                """;
        ScriptGenerateRequestDto request = new ScriptGenerateRequestDto("title", List.of("a", "b", "c"));
        when(promptBuilder.build("title", List.of("a", "b", "c"))).thenReturn("prompt");
        when(llmService.chat("prompt")).thenReturn(new ChatResult(yaml, "ollama-local"));

        ScriptApplicationServiceImpl realService = new ScriptApplicationServiceImpl(
                llmService,
                new ScriptInputValidator(),
                promptBuilder,
                new ScriptOutputParser(),
                new ScriptSchemaValidator());

        realService.generateScript(request, "user1");
        verify(llmService).chat("prompt");
    }
}
