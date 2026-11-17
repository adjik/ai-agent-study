package com.example.demos.stage03.plan;

/**
 * POST /api/stage/03/plan 的请求体。
 *
 * @param question 用户这一句。三种策略用同一句，页面上只换 strategy
 * @param strategy {@code react}（边想边做）、{@code plan}（先计划再执行）、{@code reflect}（做完再评）
 * @param maxSteps 只对 react 与 reflect 有意义，表示模型调用轮次上限。null 时用 4
 */
public record PlanningRequest(String question, String strategy, Integer maxSteps) {}
