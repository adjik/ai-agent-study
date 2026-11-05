package com.example.demos.stage01.api;

/**
 * 提示词实验请求体。
 * <p>
 * {@code ticket}：发给 {@code .user()} 的内容（必填）。
 * {@code system}：发给 {@code .system()} 的角色（可选；空则不写 system，仅 /role 使用）。
 * {@code maxTokens}：截断实验可选，限制输出长度（仅 /failure 使用）。
 */
public class PromptTicketRequest {

    /** user 消息：工单或任务文本。 */
    private String ticket;

    /** system 消息：角色设定。空或不传 = 无角色。 */
    private String system;

    public PromptTicketRequest() {
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    /** 截断实验：故意限制输出长度。仅 /failure 使用，例如 40。 */
    private Integer maxTokens;

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}
