package com.zkp.my12306.ntc.dto;

public record ScriptRecordResponseDto(
        Long id,
        String workTitle,
        Integer chapterNumber,
        String chapterContent,
        String scriptContent,
        String modelName,
        String createdAt,
        String updatedAt) {
}
