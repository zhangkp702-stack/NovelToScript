package com.zkp.my12306.ntc.dto;

public record ScriptWorkSummaryDto(
        String workTitle,
        String displayTitle,
        Integer chapterCount,
        String lastUpdatedAt) {
}
