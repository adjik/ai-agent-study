package com.example.demos.stage03.plan;

import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

/**
 * 只发一次、不带工具菜单的模型调用。第 8 天的两个地方用它：
 * 先计划再执行时写步骤清单，做完再评时检查草稿。
 * <p>
 * 不挂工具是关键：规划那一次请求里还不能查表，模型写下的清单只是根据问句猜的；
 * 检查那一次也不必再查库，库存已经在草稿的上下文里。
 */
@Component
public class PlanningModel {

    private final ChatModel chatModel;
    /** 没有 toolCallbacks。模型这一轮想点菜也点不了，只能写文字。 */
    private final OpenAiChatOptions options;

    public PlanningModel(ChatModel chatModel) {
        this.chatModel = chatModel;
        /*
         * 模型名从 ChatModel 的默认选项里抄，避免这一页和 /spring-ai 打到不同模型，
         * 否则同一句问句在两页上的措辞差异会被误读成「策略不同」。
         */
        String model = chatModel.getDefaultOptions() == null ? null : chatModel.getDefaultOptions().getModel();
        this.options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.0)
                .build();
    }

    /**
     * 一次不带工具的问答，返回正文。
     * <p>
     * 调用方给两段文字：{@code system} 说清这一次要什么格式，{@code user} 放问句或草稿。
     * 返回前 trim，因为模型常带前后空行，直接拼进下一次提示会让页面上的 draft / review 多出空白。
     * 没有 choices 时抛异常，由 ApiExceptionHandler 收成 500，页面 alert 显示原因。
     */
    public String complete(String system, String user) {
        List<Message> messages = List.of(new SystemMessage(system), new UserMessage(user));
        ChatResponse response = chatModel.call(new Prompt(messages, options));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("ChatModel 没有返回 choices");
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text.trim();
    }
}
