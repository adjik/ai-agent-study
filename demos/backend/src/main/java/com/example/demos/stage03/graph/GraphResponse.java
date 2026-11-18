package com.example.demos.stage03.graph;

import com.example.demos.stage03.loop.StepRecord;
import java.util.List;

/**
 * 固定工作流的响应。
 *
 * @param orderId          本次实际查的单号：从问句里抽出来的，抽不到是兜底值 ORD-1001
 * @param nodes            走过的节点名，按顺序。只问状态时也照样有 queryInventory，因为那条边不看问句
 * @param branch           条件边的结果：canShip 或 cannotShip
 * @param answer           文案节点写出的一句话
 * @param fixtureAvailable 夹具里该订单 SKU 的真实数量。传了 availableOverride 也不会变；订单没查到时为 0
 * @param observations     两个查询节点各自看到的 JSON
 */
public record GraphResponse(
        String orderId,
        List<String> nodes,
        String branch,
        String answer,
        Integer fixtureAvailable,
        List<StepRecord> observations) {}
