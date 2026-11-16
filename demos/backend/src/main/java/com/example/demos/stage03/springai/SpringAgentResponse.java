package com.example.demos.stage03.springai;

import com.example.demos.stage03.loop.FixtureSummary;
import com.example.demos.stage03.loop.StepRecord;
import java.util.List;

/**
 * Spring AI 循环的响应。前四个字段和手写循环对齐，后面是会话记忆。
 *
 * @param conversationId   这次读写的记忆 key。模型自己不保存会话，下一句靠这个 id 把上一句取回来
 * @param memoryMessages   写入之后、按窗口裁过的消息。重启进程后，同一个 id 再取就是空列表
 * @param memoryRepository 仓库实现类名。本阶段是内存实现，跟 JVM 进程走，不是 Redis
 * @param trigger          聊天请求为 null。巡检固定是 {@code schedule}，表示没有人打字
 */
public record SpringAgentResponse(
        String answer,
        String stopReason,
        List<StepRecord> steps,
        FixtureSummary fixture,
        String conversationId,
        List<MemoryMessageView> memoryMessages,
        String memoryRepository,
        String trigger) {}
