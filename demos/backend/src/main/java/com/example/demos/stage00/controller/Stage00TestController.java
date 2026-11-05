package com.example.demos.stage00.controller;

import com.example.demos.stage00.api.StudentExamRecord;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
        Stage00TestController.STAGE_PATH,
        Stage00TestController.LEGACY_PATH
})
public class Stage00TestController {

    public static final String STAGE_PATH = "/api/stage/00/test";
    public static final String LEGACY_PATH = "/api/test";

    private static final Logger log = LoggerFactory.getLogger(Stage00TestController.class);

    @Autowired
    private ChatClient chatClient;

    @GetMapping("/hello")
    public Object demoHello(@RequestParam String msg) {
        return chatClient.prompt(msg).call().content();
    }

    @GetMapping("/student-scores")
    public Object extractStudentScores(@RequestParam(required = false) String text) {
        String narrative = (text != null && !text.isBlank())
                ? text
                : "小明是一个五年级的小学生，本次考试语文成绩98，数学成绩100，科学99";

        try {
            StudentExamRecord result = chatClient.prompt()
                    .system("""
                            你是教务数据提取助手。只输出符合 schema 的 json，不要 markdown，不要解释。
                            字段必须是英文 camelCase，示例：
                            {"studentName":"小明","gradeLevel":"五年级","subjectScores":[{"subject":"语文","score":98},{"subject":"数学","score":100}]}
                            未提及的字段用空字符串；分数必须是整数；科目名保留原文（如语文、数学）。
                            """)
                    .user(u -> u.text("""
                            从下面描述中提取学生姓名、年级、各科分数，输出 json：
                            {narrative}
                            """).param("narrative", narrative))
                    .options(OpenAiChatOptions.builder().temperature(0.0))
                    .call()
                    .entity(StudentExamRecord.class);
            return result;
        } catch (Exception e) {
            log.warn("K10 student-scores 解析失败: {}", e.getMessage());
            return Map.of(
                    "parseSuccess", false,
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    "inputText", narrative
            );
        }
    }
}
