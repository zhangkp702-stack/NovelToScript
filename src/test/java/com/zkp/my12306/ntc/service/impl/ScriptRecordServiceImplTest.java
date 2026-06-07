package com.zkp.my12306.ntc.service.impl;

import com.zkp.my12306.ntc.dto.ScriptRecordResponseDto;
import com.zkp.my12306.ntc.dto.ScriptSaveRequestDto;
import com.zkp.my12306.ntc.script.dao.entity.ScriptRecordDO;
import com.zkp.my12306.ntc.script.dao.mapper.ScriptRecordMapper;
import com.zkp.my12306.ntc.script.record.ScriptRecordNotFoundException;
import com.zkp.my12306.ntc.script.record.ScriptRecordValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScriptRecordServiceImplTest {

    @Mock
    private ScriptRecordMapper scriptRecordMapper;

    @InjectMocks
    private ScriptRecordServiceImpl service;

    @Test
    void save_newRecord_inserts() {
        when(scriptRecordMapper.selectOne(any())).thenReturn(null);

        ScriptSaveRequestDto request = new ScriptSaveRequestDto(
                "作品A",
                1,
                "章节内容",
                "剧本内容",
                "model-x");
        ScriptRecordResponseDto response = service.save("user1", request);

        ArgumentCaptor<ScriptRecordDO> captor = ArgumentCaptor.forClass(ScriptRecordDO.class);
        verify(scriptRecordMapper).insert(captor.capture());
        verify(scriptRecordMapper, never()).updateById(any(ScriptRecordDO.class));
        assertEquals("user1", captor.getValue().getUserId());
        assertEquals("作品A", response.workTitle());
        assertEquals("剧本内容", response.scriptContent());
    }

    @Test
    void getById_notFound_throws() {
        when(scriptRecordMapper.selectById(99L)).thenReturn(null);
        assertThrows(ScriptRecordNotFoundException.class, () -> service.getById("user1", 99L));
    }

    @Test
    void save_blankScriptContent_throws() {
        ScriptSaveRequestDto request = new ScriptSaveRequestDto("作品", 1, "章节", "  ", null);
        assertThrows(ScriptRecordValidationException.class, () -> service.save("user1", request));
    }
}
