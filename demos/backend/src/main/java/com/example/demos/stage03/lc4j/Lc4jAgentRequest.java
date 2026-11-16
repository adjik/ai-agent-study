package com.example.demos.stage03.lc4j;

/**
 * POST /api/stage/03/langchain4j 的请求体。
 *
 * @param question       这一句。模型自己不保存会话，上一句靠 conversationId 从记忆里取回
 * @param conversationId 记忆 key。空则服务端生成。点「新会话」才换
 * @param maxMessages    窗口最多保留几条，null 时用 20。同一个 id 改这个值，下一次读写就按新窗口裁
 */
public record Lc4jAgentRequest(String question, String conversationId, Integer maxMessages) {}
