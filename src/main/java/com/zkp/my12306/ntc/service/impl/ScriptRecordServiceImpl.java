package com.zkp.my12306.ntc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zkp.my12306.ntc.dto.ScriptRecordResponseDto;
import com.zkp.my12306.ntc.dto.ScriptSaveRequestDto;
import com.zkp.my12306.ntc.llm.trace.TraceIdGenerator;
import com.zkp.my12306.ntc.script.dao.entity.ScriptRecordDO;
import com.zkp.my12306.ntc.script.dao.mapper.ScriptRecordMapper;
import com.zkp.my12306.ntc.script.record.ScriptRecordAccessDeniedException;
import com.zkp.my12306.ntc.script.record.ScriptRecordNotFoundException;
import com.zkp.my12306.ntc.script.record.ScriptRecordValidationException;
import com.zkp.my12306.ntc.service.ScriptRecordService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class ScriptRecordServiceImpl implements ScriptRecordService {

    private final ScriptRecordMapper scriptRecordMapper;

    public ScriptRecordServiceImpl(ScriptRecordMapper scriptRecordMapper) {
        this.scriptRecordMapper = scriptRecordMapper;
    }

    @Override
    public ScriptRecordResponseDto save(String currentUser, ScriptSaveRequestDto request) {
        validateSaveRequest(currentUser, request);
        String userId = currentUser.trim();
        String workTitle = normalizeWorkTitle(request.workTitle());
        int chapterNumber = request.chapterNumber();
        String chapterContent = request.chapterContent().trim();
        String scriptContent = request.scriptContent().trim();
        LocalDateTime now = LocalDateTime.now();

        ScriptRecordDO existing = scriptRecordMapper.selectOne(Wrappers.lambdaQuery(ScriptRecordDO.class)
                .eq(ScriptRecordDO::getUserId, userId)
                .eq(ScriptRecordDO::getWorkTitle, workTitle)
                .eq(ScriptRecordDO::getChapterNumber, chapterNumber)
                .last("LIMIT 1"));

        if (existing == null) {
            ScriptRecordDO record = new ScriptRecordDO();
            record.setId(TraceIdGenerator.nextId());
            record.setUserId(userId);
            record.setWorkTitle(workTitle);
            record.setChapterNumber(chapterNumber);
            record.setChapterContent(chapterContent);
            record.setChapterContentHash(hashContent(chapterContent));
            record.setScriptContent(scriptContent);
            record.setModelName(normalizeModelName(request.modelName()));
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setDeleted(0);
            scriptRecordMapper.insert(record);
            return toResponse(record);
        }

        existing.setChapterContent(chapterContent);
        existing.setChapterContentHash(hashContent(chapterContent));
        existing.setScriptContent(scriptContent);
        existing.setModelName(normalizeModelName(request.modelName()));
        existing.setUpdateTime(now);
        scriptRecordMapper.updateById(existing);
        return toResponse(existing);
    }

    @Override
    public List<ScriptRecordResponseDto> listByWorkTitle(String currentUser, String workTitle) {
        if (currentUser == null || currentUser.isBlank()) {
            throw new ScriptRecordValidationException("用户未登录");
        }
        String userId = currentUser.trim();
        String normalizedTitle = normalizeWorkTitle(workTitle);
        List<ScriptRecordDO> records = scriptRecordMapper.selectList(Wrappers.lambdaQuery(ScriptRecordDO.class)
                .eq(ScriptRecordDO::getUserId, userId)
                .eq(ScriptRecordDO::getWorkTitle, normalizedTitle)
                .orderByAsc(ScriptRecordDO::getChapterNumber));
        return records.stream().map(this::toResponse).toList();
    }

    @Override
    public ScriptRecordResponseDto getById(String currentUser, Long id) {
        if (currentUser == null || currentUser.isBlank()) {
            throw new ScriptRecordValidationException("用户未登录");
        }
        if (id == null || id < 1) {
            throw new ScriptRecordValidationException("记录 ID 无效");
        }
        ScriptRecordDO record = scriptRecordMapper.selectById(id);
        if (record == null) {
            throw new ScriptRecordNotFoundException(id);
        }
        if (!currentUser.trim().equals(record.getUserId())) {
            throw new ScriptRecordAccessDeniedException(id);
        }
        return toResponse(record);
    }

    private void validateSaveRequest(String currentUser, ScriptSaveRequestDto request) {
        if (currentUser == null || currentUser.isBlank()) {
            throw new ScriptRecordValidationException("用户未登录");
        }
        if (request == null) {
            throw new ScriptRecordValidationException("请求体不能为空");
        }
        if (request.chapterNumber() == null || request.chapterNumber() < 1) {
            throw new ScriptRecordValidationException("章节编号无效");
        }
        if (request.chapterContent() == null || request.chapterContent().isBlank()) {
            throw new ScriptRecordValidationException("章节内容不能为空");
        }
        if (request.scriptContent() == null || request.scriptContent().isBlank()) {
            throw new ScriptRecordValidationException("剧本内容不能为空");
        }
    }

    private String normalizeWorkTitle(String workTitle) {
        if (workTitle == null) {
            return "";
        }
        return workTitle.trim();
    }

    private String normalizeModelName(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return null;
        }
        return modelName.trim();
    }

    private ScriptRecordResponseDto toResponse(ScriptRecordDO record) {
        return new ScriptRecordResponseDto(
                record.getId(),
                record.getWorkTitle(),
                record.getChapterNumber(),
                record.getChapterContent(),
                record.getScriptContent(),
                record.getModelName(),
                formatDateTime(record.getCreateTime()),
                formatDateTime(record.getUpdateTime()));
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }
}
