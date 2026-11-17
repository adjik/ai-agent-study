package com.example.demos.stage03.lc4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 第 7 天 · 只声明接口，方法体由 {@code AiServices} 生成。
 * 背后仍是「模型调用 + 工具循环」，与第 4 天手写的 while 是同一件事，只是循环握在框架手里。
 * {@link MemoryId} 决定用哪一份 {@code ChatMemory}；同一个 id 的下一句能取回上一句。
 * <p>
 * 系统提示必须写进 {@code @SystemMessage} 而不是每次调用时拼在 user 文本里：
 * LangChain4j 会把它作为独立的一条系统消息，窗口裁剪时才不会被当成普通用户句裁掉。
 */
public interface Stage03Assistant {

    /** 与 Spring 页同一套约束，方便两页对同一句问句得到可比较的答案。 */
    String SYSTEM = """
            你是电商售后助手。订单和库存只存在于工具返回里，禁止编造状态或数量。
            getOrder 只查订单，不查库存。getInventory 只查库存，不查订单。一次只调用一个工具。
            用户只问订单状态时，调用一次 getOrder，看到观察后直接写回答，不要再查库存。
            用户问能不能发货时，必须先 getOrder；下一轮再用订单观察里的 sku 和 warehouse 调用 getInventory；两段观察都在消息里之后，才写能不能发。
            用户说「刚才那张单」时，只能使用对话记忆里出现过的单号。记忆里没有单号就说不知道是哪张单，禁止改用夹具里的 ORD-1001。
            观察里的 error 是 invalid_argument 或 not_found 时，按这个错误说明，禁止说成已支付或 PAID。
            """;

    /**
     * @param conversationId 记忆 key，来自方法参数而不是提示文本。同一个值对应同一份窗口
     * @param question       这一句原文。模型看不到上一句，除非同 id 的记忆里还留着
     */
    @SystemMessage(SYSTEM)
    String chat(@MemoryId String conversationId, @UserMessage String question);
}
