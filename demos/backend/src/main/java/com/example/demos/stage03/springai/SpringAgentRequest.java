package com.example.demos.stage03.springai;

/**
 * 聊天入口的请求。巡检接口不读这个对象。
 *
 * @param question          用户这一句。模型看不到上一句，除非 conversationId 下的记忆还在
 * @param maxSteps          循环上限，null 时用 4
 * @param maxMessages       窗口最多保留几条，null 时用 20。系统消息不占这个名额的算法在窗口实现里
 * @param rememberToolTrace true 时把每一轮的点菜和工具结果写入记忆；false 时只留用户句和最终回答
 * @param conversationId    记忆 key。空则服务端生成。点「新会话」才换；重启后旧 key 在堆里已经没了
 */
public record SpringAgentRequest(
        String question,
        Integer maxSteps,
        Integer maxMessages,
        Boolean rememberToolTrace,
        String conversationId) {}
