package com.zkp.my12306.ntc.llm.routing;

import com.zkp.my12306.ntc.llm.client.ChatClient;
import com.zkp.my12306.ntc.llm.service.ChatResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ModelRoutingExecutor {
    public ChatResult executeChat(
            String prompt,
            List<ModelTarget> targets,
            Map<String, ChatClient> clientMap) {
        IllegalStateException lastException = null;
        for (ModelTarget target : targets) {
            ChatClient chatClient = clientMap.get(target.getProvider());
            if (chatClient == null) {
                lastException = new IllegalStateException("未找到 provider 对应客户端：" + target.getProvider() + "（model=" + target.getName() + "）");
                continue;
            }
            try {
                return chatClient.chat(prompt, target);
            } catch (Exception ex) {
                lastException = new IllegalStateException("模型调用失败：" + target.getName(), ex);
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new IllegalStateException("没有可用模型可执行");
    }
}
