package com.example.demos.common.web;

import com.example.demos.stage00.controller.Stage00DemoController;
import com.example.demos.stage00.controller.Stage00TestController;
import com.example.demos.stage01.controller.PromptExperimentController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stages")
public class StageIndexController {

    @GetMapping
    public Map<String, Object> listStages() {
        Map<String, Object> stages = new LinkedHashMap<>();

        stages.put("00", Map.of(
                "title", "环境与 LLM 入门",
                "status", "ready",
                "codePackage", "com.example.demos.stage00",
                "endpoints", List.of(
                        "GET " + Stage00DemoController.STAGE_PATH,
                        "GET " + Stage00TestController.STAGE_PATH
                )
        ));

        stages.put("01", Map.of(
                "title", "提示词工程",
                "status", "ready",
                "codePackage", "com.example.demos.stage01",
                "endpoints", List.of(
                        "GET " + PromptExperimentController.STAGE_PATH,
                        "POST " + PromptExperimentController.PATH_ROLE,
                        "POST " + PromptExperimentController.PATH_FEW_SHOT,
                        "POST " + PromptExperimentController.PATH_COT,
                        "POST " + PromptExperimentController.PATH_TEMPLATE,
                        "POST " + PromptExperimentController.PATH_SCHEMA,
                        "POST " + PromptExperimentController.PATH_FAILURE
                )
        ));

        return Map.of(
                "service", "ai-agent-demos",
                "port", 8080,
                "stages", stages
        );
    }
}
