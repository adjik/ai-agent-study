package com.example.demos.common.web;

import com.example.demos.common.config.DeepSeekConfig;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DeepSeekConfig deepSeekConfig;

    public HealthController(DeepSeekConfig deepSeekConfig) {
        this.deepSeekConfig = deepSeekConfig;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        String apiKey = deepSeekConfig.getApiKey();
        return Map.of(
                "status", "UP",
                "llmProvider", "deepseek",
                "baseUrl", deepSeekConfig.getBaseUrl(),
                "model", deepSeekConfig.getModel(),
                "apiKeyConfigured", StringUtils.hasText(apiKey),
                "apiKeyPreview", maskApiKey(apiKey)
        );
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "not_set";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
