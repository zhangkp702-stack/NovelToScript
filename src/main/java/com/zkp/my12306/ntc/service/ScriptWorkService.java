package com.zkp.my12306.ntc.service;

import com.zkp.my12306.ntc.dto.ScriptWorkCreateRequestDto;
import com.zkp.my12306.ntc.dto.ScriptWorkResponseDto;
import com.zkp.my12306.ntc.dto.ScriptWorkSummaryDto;
import com.zkp.my12306.ntc.script.dao.entity.ScriptWorkDO;

import java.util.List;

public interface ScriptWorkService {

    ScriptWorkResponseDto createWork(String currentUser, ScriptWorkCreateRequestDto request);

    String resolveWorkId(String currentUser, String workId, String title);

    ScriptWorkDO requireOwnedWork(String currentUser, String workId);

    List<ScriptWorkSummaryDto> listWorks(String currentUser);

    void deleteWork(String currentUser, String workId);

    void deleteWorkByTitle(String currentUser, String workTitle);

    void touchWork(String workId);

    ScriptWorkResponseDto updateTitle(String currentUser, String workId, String title);
}
