package com.zkp.my12306.ntc.service.impl;

import com.zkp.my12306.ntc.dto.ScriptGenerateRequestDto;
import com.zkp.my12306.ntc.dto.ScriptGenerateResponseDto;
import com.zkp.my12306.ntc.llm.service.ChatResult;
import com.zkp.my12306.ntc.llm.service.LLMService;
import com.zkp.my12306.ntc.llm.trace.TraceRoot;
import com.zkp.my12306.ntc.script.input.ScriptInputValidator;
import com.zkp.my12306.ntc.script.model.ScriptDocument;
import com.zkp.my12306.ntc.script.parse.ScriptOutputParser;
import com.zkp.my12306.ntc.script.prompt.ScriptPromptBuilder;
import com.zkp.my12306.ntc.script.validate.ScriptSchemaValidator;
import com.zkp.my12306.ntc.service.ScriptApplicationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScriptApplicationServiceImpl implements ScriptApplicationService {

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
    @TraceRoot(name = "scriptGenerate")
    public ScriptGenerateResponseDto generateScript(ScriptGenerateRequestDto request, String currentUser) {
        List<String> chapters = normalizeChapters(request);
        inputValidator.validate(chapters);
        String prompt = promptBuilder.build(request == null ? null : request.title(), chapters);
        ChatResult llmResponse = llmService.chat(prompt);
        ScriptDocument document = outputParser.parse(llmResponse.content());
        schemaValidator.validate(document);
        return new ScriptGenerateResponseDto(
                llmResponse.modelName(),
                document.toMap(),
                llmResponse.content());
    }

    private List<String> normalizeChapters(ScriptGenerateRequestDto request) {
        if (request == null || request.chapters() == null) {
            return List.of();
        }
        return new ArrayList<>(request.chapters());
    }
}
