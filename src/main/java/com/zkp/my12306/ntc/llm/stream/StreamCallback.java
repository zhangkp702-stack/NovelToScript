package com.zkp.my12306.ntc.llm.stream;

public interface StreamCallback {

    default void onOpen(String modelName) {
    }

    default void onToken(String token) {
    }

    default void onComplete() {
    }

    default void onError(Throwable throwable) {
    }
}
