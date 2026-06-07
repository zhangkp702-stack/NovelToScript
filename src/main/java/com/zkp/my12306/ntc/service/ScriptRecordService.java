package com.zkp.my12306.ntc.service;

import com.zkp.my12306.ntc.dto.ScriptRecordResponseDto;
import com.zkp.my12306.ntc.dto.ScriptSaveRequestDto;

import java.util.List;

public interface ScriptRecordService {

    ScriptRecordResponseDto save(String currentUser, ScriptSaveRequestDto request);

    List<ScriptRecordResponseDto> listByWorkTitle(String currentUser, String workTitle);

    ScriptRecordResponseDto getById(String currentUser, Long id);
}
