package com.example.demos.stage01.api;

import java.util.List;

/**
 * 工单分类结构化出参（DTO）。{@code ChatClient.entity(TicketTriage.class)} 按这些字段反序列化。
 * 与 {@code demos/01-hello-llm/README.md} 对齐。
 */
public class TicketTriage {

    /** PERF / BUG / DATA / UNKNOWN */
    private String category;
    /** P0 / P1 / P2 / P3 */
    private String severity;
    /** 下一步，最多几条 */
    private List<String> nextSteps;
    /** 必须引用用户原文；引用不上就空字符串，不要编造 */
    private String evidence;
    /** high / medium / low */
    private String confidence;

    public TicketTriage() {
    }

    public TicketTriage(String category, String severity, List<String> nextSteps,
                        String evidence, String confidence) {
        this.category = category;
        this.severity = severity;
        this.nextSteps = nextSteps;
        this.evidence = evidence;
        this.confidence = confidence;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public List<String> getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(List<String> nextSteps) {
        this.nextSteps = nextSteps;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
}
