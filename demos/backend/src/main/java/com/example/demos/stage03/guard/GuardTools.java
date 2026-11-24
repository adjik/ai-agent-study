package com.example.demos.stage03.guard;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 工具菜单的载体，只为让模型知道「有这三个函数、参数长什么样」。
 *
 * <h2>为什么方法体是占位</h2>
 * 今天的循环<b>不调</b> {@code ToolCallingManager}，每一次点菜都由 {@link GuardedAgent} 自己决定
 * 要不要真的执行——退款要先挂起等确认，查询才直接做。如果这些方法体里有真实逻辑，
 * 一旦哪天有人误接了自动执行，退款就会绕过确认直接跑掉。
 * <p>
 * 所以这里的方法体只返回一句占位。菜单（名字、说明、参数 schema）是它们唯一有价值的部分。
 *
 * <h2>菜单里少了一个方法</h2>
 * <b>没有「改库存」。</b>手册第 2 步问「把 SKU-88 的库存改成 999」，
 * 模型翻遍菜单也找不到能写的函数，于是什么也不会发生——这就是「菜单边界」：
 * 能力清单是安全边界的第一层，比任何提示词都硬。
 */
@Component
public class GuardTools {

    /**
     * 查询订单。说明里写明「只能查订单，不能查库存数量」，
     * 让模型在发货场景下知道要分开点两把工具，而不是指望它一次查全。
     */
    @Tool(description = """
            按订单号查询订单。orderId 必须形如 ORD-数字。只能查订单，不能查库存数量。
            """)
    public String getOrder(@ToolParam(description = "订单号，例如 ORD-1001") String orderId) {
        // 占位：真正执行在 GuardedAgent.run 里的 dispatcher.dispatch
        return "{\"note\":\"executed by GuardedAgent\"}";
    }

    /**
     * 查询库存。说明里补一句「不能修改库存」，
     * 让模型被要求改库存时能自己意识到「没有这个能力」，而不是编一个函数名出来。
     */
    @Tool(description = """
            按 SKU 和仓库查询可用库存。只能查库存数量，不能查订单状态，也不能修改库存。
            """)
    public String getInventory(
            @ToolParam(description = "SKU，例如 SKU-88") String sku,
            @ToolParam(description = "仓库编码，例如 WH-A") String warehouse) {
        // 占位：真正执行在 GuardedAgent.run 里的 dispatcher.dispatch
        return "{\"note\":\"executed by GuardedAgent\"}";
    }

    /**
     * 退款。菜单里唯一的写操作，也是今天三道闸里第三道要拦的东西。
     * <p>
     * 说明里写明「必须等用户确认」，但<b>不能只靠这句话</b>——
     * 真正的拦截在 {@link GuardedAgent#run} 的 {@code if (TOOL_REFUND.equals(call.name()))} 分支，
     * 那里只挂起、不执行。
     */
    @Tool(description = """
            给订单退款。这是写操作，调用后订单状态会变成已退款，必须等用户确认。
            """)
    public String refundOrder(@ToolParam(description = "订单号，例如 ORD-1001") String orderId) {
        // 占位：永远不会被直接调用。真正的执行在 GuardedAgent.confirm
        return "{\"note\":\"never executed directly\"}";
    }
}
