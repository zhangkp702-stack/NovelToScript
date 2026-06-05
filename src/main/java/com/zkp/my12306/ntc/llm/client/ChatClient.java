package com.zkp.my12306.ntc.llm.client;

import com.zkp.my12306.ntc.llm.routing.ModelTarget;
import com.zkp.my12306.ntc.llm.service.ChatResult;
import com.zkp.my12306.ntc.llm.stream.StreamCallback;
import com.zkp.my12306.ntc.llm.stream.StreamCancellationHandle;

public interface ChatClient {

    String provider();

    ChatResult chat(String prompt, ModelTarget modelTarget);

    StreamCancellationHandle streamChat(String prompt, ModelTarget modelTarget, StreamCallback callback);
}
