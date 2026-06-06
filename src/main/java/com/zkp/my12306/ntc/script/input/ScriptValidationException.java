package com.zkp.my12306.ntc.script.input;

import java.util.List;

public class ScriptValidationException extends RuntimeException {

    private final ValidationErrorCode code;
    private final int minChapters;
    private final int filledCount;
    private final List<Integer> invalidIndexes;

    public ScriptValidationException(
            ValidationErrorCode code,
            String message,
            int minChapters,
            int filledCount,
            List<Integer> invalidIndexes) {
        super(message);
        this.code = code;
        this.minChapters = minChapters;
        this.filledCount = filledCount;
        this.invalidIndexes = List.copyOf(invalidIndexes);
    }

    public ValidationErrorCode getCode() {
        return code;
    }

    public int getMinChapters() {
        return minChapters;
    }

    public int getFilledCount() {
        return filledCount;
    }

    public List<Integer> getInvalidIndexes() {
        return invalidIndexes;
    }
}
