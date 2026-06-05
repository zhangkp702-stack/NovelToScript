package com.zkp.my12306.ntc.llm.client.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkp.my12306.ntc.llm.client.AbstractOpenAIStyleChatClient;
import com.zkp.my12306.ntc.llm.config.LlmRuntimeProperties;
import com.zkp.my12306.ntc.llm.stream.StreamAsyncExecutor;
import com.zkp.my12306.ntc.llm.stream.sse.OpenAIStyleSseParser;
import org.springframework.stereotype.Component;

@Component
public class BaiLianChatClient extends AbstractOpenAIStyleChatClient {
    public BaiLianChatClient(
            ObjectMapper objectMapper,
            OpenAIStyleSseParser openAIStyleSseParser,
            StreamAsyncExecutor streamAsyncExecutor,
            LlmRuntimeProperties llmRuntimeProperties) {
        super(objectMapper, openAIStyleSseParser, streamAsyncExecutor, llmRuntimeProperties.getRouting().getConnectTimeoutMs());
    }

    @Override
    public String provider() {
        return "bailian";
    }
}
