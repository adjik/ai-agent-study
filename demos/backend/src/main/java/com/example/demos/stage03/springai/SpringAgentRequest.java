package com.example.demos.stage03.springai;

/**
 * 第 4 天只要问句和步数上限。会话 ID、窗口条数留到第 5 天再加。
 *
 * @param question 用户原句。这次接口不读上一问
 * @param maxSteps 请求模型的次数上限。null 时 Controller 用 4
 */
public record SpringAgentRequest(String question, Integer maxSteps) {}
