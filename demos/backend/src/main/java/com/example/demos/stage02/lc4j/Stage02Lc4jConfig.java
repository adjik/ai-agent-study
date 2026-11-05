package com.example.demos.stage02.lc4j;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Stage02Lc4jConfig {

    @Bean
    OrderAssistant orderAssistant(
            Lc4jOrderTools lc4jOrderTools,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${spring.ai.openai.chat.model:deepseek-chat}") String model) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.0)
                .build();
        return AiServices.builder(OrderAssistant.class)
                .chatModel(chatModel)
                .tools(lc4jOrderTools)
                .build();
    }
}
