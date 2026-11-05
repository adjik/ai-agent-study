package com.example.demos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class DemosApplication {

    /**
     * 在 Spring 启动前向上查找 .env 文件，逐行注入 System Properties。
     */
    private static void loadDotEnv() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && current != null; i++) {
            Path candidate = current.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                try {
                    for (String line : Files.readAllLines(candidate, StandardCharsets.UTF_8)) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int sep = trimmed.indexOf('=');
                        if (sep <= 0) {
                            continue;
                        }
                        String key = trimmed.substring(0, sep).trim();
                        String value = trimmed.substring(sep + 1).trim();
                        if ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (!key.isEmpty()) {
                            System.setProperty(key, value);
                        }
                    }
                    System.out.println("[.env] 已从 " + candidate + " 加载配置");
                } catch (IOException ignored) {
                }
                return;
            }
            current = current.getParent();
        }
        System.err.println("[.env] ⚠️ 未找到 .env 文件，DEEPSEEK_API_KEY 可能未设置");
    }

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(DemosApplication.class, args);
    }
}
