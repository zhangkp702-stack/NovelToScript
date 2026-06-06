package com.zkp.my12306.ntc.service.impl;

import com.zkp.my12306.ntc.dto.ScriptGenerateRequestDto;
import com.zkp.my12306.ntc.dto.ScriptGenerateResponseDto;
import com.zkp.my12306.ntc.llm.service.ChatResult;
import com.zkp.my12306.ntc.llm.service.LLMService;
import com.zkp.my12306.ntc.llm.trace.TraceRoot;
import com.zkp.my12306.ntc.service.ScriptApplicationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScriptApplicationServiceImpl implements ScriptApplicationService {
    private static final int MIN_CHAPTERS = 3;
    private final LLMService llmService;

    public ScriptApplicationServiceImpl(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    @TraceRoot(name = "scriptGenerate")
    public ScriptGenerateResponseDto generateScript(ScriptGenerateRequestDto request, String currentUser) {
        validateInput(request);
        String prompt = buildPrompt(request.title(), request.chapters());
        ChatResult llmResponse = llmService.chat(prompt);
        return new ScriptGenerateResponseDto(llmResponse.content(), llmResponse.modelName());
    }

    private void validateInput(ScriptGenerateRequestDto request) {
        if (request == null || request.chapters() == null || request.chapters().size() < MIN_CHAPTERS) {
            throw new IllegalArgumentException("至少提交3个章节内容");
        }
        boolean hasBlank = request.chapters().stream().anyMatch(item -> item == null || item.isBlank());
        if (hasBlank) {
            throw new IllegalArgumentException("章节内容不能为空");
        }
    }

    private String buildPrompt(String title, List<String> chapters) {
        StringBuilder builder = new StringBuilder();
        if (title != null && !title.isBlank()) {
            builder.append("标题：").append(title.trim()).append("\n");
        }
        builder.append("请将以下小说内容转换成剧本格式：\n");
        for (int i = 0; i < chapters.size(); i++) {
            builder.append("第").append(i + 1).append("章：\n").append(chapters.get(i)).append("\n");
        }
        return builder.toString();
    }
}
