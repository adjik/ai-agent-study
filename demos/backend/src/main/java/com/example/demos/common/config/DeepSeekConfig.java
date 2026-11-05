package com.example.demos.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class DeepSeekConfig implements ApplicationRunner {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.model:}")
    private String model;

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("""
                    未配置 DeepSeek API Key，无法发起真实 LLM 调用。
                    请在项目根目录 .env 中设置 DEEPSEEK_API_KEY，或执行：
                      export DEEPSEEK_API_KEY=sk-xxxx
                    获取地址：https://platform.deepseek.com/api_keys
                    """);
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }
}
