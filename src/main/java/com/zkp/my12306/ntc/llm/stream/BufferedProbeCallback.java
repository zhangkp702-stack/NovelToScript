package com.zkp.my12306.ntc.llm.stream;

import java.util.ArrayList;
import java.util.List;

public class BufferedProbeCallback implements StreamCallback {
    private final StreamCallback downstream;
    private final List<String> tokenBuffer = new ArrayList<>();
    private boolean promoted = false;
    private boolean openSent = false;
    private String modelName;
    private boolean completedBeforePromote = false;
    private Throwable errorBeforePromote;

    public BufferedProbeCallback(StreamCallback downstream) {
        this.downstream = downstream;
    }

    @Override
    public synchronized void onOpen(String modelName) {
        this.modelName = modelName;
        if (promoted && !openSent) {
            downstream.onOpen(modelName);
            openSent = true;
        }
    }

    @Override
    public synchronized void onToken(String token) {
        if (!promoted) {
            tokenBuffer.add(token);
            return;
        }
        ensureOpenSent();
        downstream.onToken(token);
    }

    @Override
    public synchronized void onComplete() {
        if (!promoted) {
            completedBeforePromote = true;
            return;
        }
        ensureOpenSent();
        downstream.onComplete();
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        if (!promoted) {
            errorBeforePromote = throwable;
            return;
        }
        ensureOpenSent();
        downstream.onError(throwable);
    }

    public synchronized void promoteAndFlush() {
        if (promoted) {
            return;
        }
        promoted = true;
        ensureOpenSent();
        for (String token : tokenBuffer) {
            downstream.onToken(token);
        }
        tokenBuffer.clear();
        if (completedBeforePromote) {
            downstream.onComplete();
        }
    }

    public synchronized void clearBuffer() {
        tokenBuffer.clear();
        completedBeforePromote = false;
        errorBeforePromote = null;
    }

    public synchronized Throwable getErrorBeforePromote() {
        return errorBeforePromote;
    }

    private void ensureOpenSent() {
        if (!openSent && modelName != null && !modelName.isBlank()) {
            downstream.onOpen(modelName);
            openSent = true;
        }
    }
}
