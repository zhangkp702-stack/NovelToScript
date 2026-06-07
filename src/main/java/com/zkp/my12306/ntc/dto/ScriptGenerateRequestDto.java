package com.zkp.my12306.ntc.dto;

public record ScriptGenerateRequestDto(
        String workId,
        String generationId,
        String title,
        Integer chapterNumber,
        String chapterContent) {
}
