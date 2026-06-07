package com.zkp.my12306.ntc.script.stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamDegenerationGuardTest {

    private static final String LONG_PREFIX = "林澈来到市档案馆，准备领取母亲的旧资料。".repeat(130);

    @Test
    void detectDegeneration_flagsReplacementChar() {
        assertTrue(StreamDegenerationGuard.detectDegeneration("正常文本\uFFFD继续"));
    }

    @Test
    void detectDegeneration_flagsPhraseRepeat() {
        assertTrue(StreamDegenerationGuard.detectDegeneration(LONG_PREFIX + "问题问题问题"));
    }

    @Test
    void detectDegeneration_flagsConsecutiveChars() {
        assertTrue(StreamDegenerationGuard.detectDegeneration(LONG_PREFIX + "信信信"));
    }

    @Test
    void detectDegeneration_ignoresNormalScript() {
        String normal = """
                林澈：这封邮件怎么回事？
                许知遥：（冷静）你母亲十五年前的事，你想知道吗？
                旁白：登记表最下行写着「林晚舟」，十五年前。
                """;
        assertFalse(StreamDegenerationGuard.detectDegeneration(normal));
    }
}
