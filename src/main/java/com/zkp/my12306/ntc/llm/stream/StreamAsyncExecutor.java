package com.zkp.my12306.ntc.llm.stream;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class StreamAsyncExecutor {
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public CompletableFuture<Void> execute(Runnable task) {
        return CompletableFuture.runAsync(task, executorService);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }
}
