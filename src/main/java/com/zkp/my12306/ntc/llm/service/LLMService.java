package com.zkp.my12306.ntc.llm.service;

import com.zkp.my12306.ntc.llm.stream.StreamCallback;
import com.zkp.my12306.ntc.llm.stream.StreamCancellationHandle;

public interface LLMService {

    ChatResult chat(String prompt);

    StreamCancellationHandle streamChat(String prompt, StreamCallback callback);
}
