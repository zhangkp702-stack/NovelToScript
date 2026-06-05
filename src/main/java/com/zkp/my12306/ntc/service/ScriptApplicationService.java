package com.zkp.my12306.ntc.service;

import com.zkp.my12306.ntc.dto.ScriptGenerateRequestDto;
import com.zkp.my12306.ntc.dto.ScriptGenerateResponseDto;

public interface ScriptApplicationService {

    ScriptGenerateResponseDto generateScript(ScriptGenerateRequestDto request, String currentUser);
}
