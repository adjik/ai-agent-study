package com.example.demos.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("请求参数不合法: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "参数不合法",
                "message", ex.getMessage() != null ? ex.getMessage() : "IllegalArgumentException"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleLlmException(Exception ex) {
        log.error("LLM 调用失败", ex);
        String message = rootMessage(ex);
        boolean authIssue = message.contains("API Key") || message.contains("401");
        boolean timeout = isTimeout(ex);
        HttpStatus status = authIssue ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;
        String hint = timeout
                ? "调用 DeepSeek 超过 spring.ai.openai.timeout（默认 60s）。请加大超时，或缩短 prompt / max-tokens。"
                : "请确认 DEEPSEEK_API_KEY 已配置且网络可访问 https://api.deepseek.com";

        return ResponseEntity.status(status).body(Map.of(
                "error", "DeepSeek API 调用失败",
                "message", message,
                "hint", hint
        ));
    }

    private static boolean isTimeout(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            String name = current.getClass().getSimpleName();
            String msg = current.getMessage() == null ? "" : current.getMessage();
            if (name.contains("Timeout") || msg.contains("timeout") || msg.contains("timed out")
                    || msg.contains("Error reading response")) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        String message = ex.getClass().getSimpleName();
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getClass().getSimpleName() + ": " + current.getMessage();
            }
            current = current.getCause();
        }
        return message;
    }
}
