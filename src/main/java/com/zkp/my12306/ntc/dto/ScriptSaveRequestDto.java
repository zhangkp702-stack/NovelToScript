package com.zkp.my12306.ntc.dto;

public record ScriptSaveRequestDto(
        String workTitle,
        Integer chapterNumber,
        String chapterContent,
        String scriptContent,
        String modelName) {
}
