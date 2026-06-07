package com.zkp.my12306.ntc.script.record;

public class ScriptRecordNotFoundException extends RuntimeException {

    public ScriptRecordNotFoundException(Long id) {
        super("剧本记录不存在：" + id);
    }
}
