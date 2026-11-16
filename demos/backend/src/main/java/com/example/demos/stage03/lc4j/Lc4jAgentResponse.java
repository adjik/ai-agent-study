package com.example.demos.stage03.lc4j;

import com.example.demos.stage03.springai.MemoryMessageView;
import java.util.List;

/**
 * LangChain4j 循环的响应。工具轨迹留在框架内部，页面只看回答和记忆窗口。
 *
 * @param answer         模型最终写出的句子。事实仍来自阶段 2 的 Store
 * @param conversationId 这次读写的记忆 key
 * @param memoryMessages 写入之后、按窗口裁过的消息。窗口只有 2 条时这里会很短
 */
public record Lc4jAgentResponse(
        String answer, String conversationId, List<MemoryMessageView> memoryMessages) {}
