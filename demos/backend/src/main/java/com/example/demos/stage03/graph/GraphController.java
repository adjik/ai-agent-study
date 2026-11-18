package com.example.demos.stage03.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第 9 天 · POST /api/stage/03/graph。
 * 这一页没有 strategy，也没有 maxSteps：图上没有「模型决定要不要继续」这一环，
 * 走几个节点由边写死，所以请求只有问句和一个可选的库存覆盖值。
 */
@RestController
@RequestMapping("/api/stage/03")
public class GraphController {

    private static final Logger log = LoggerFactory.getLogger(GraphController.class);

    private final ShipGraph shipGraph;

    public GraphController(ShipGraph shipGraph) {
        this.shipGraph = shipGraph;
    }

    @PostMapping("/graph")
    public GraphResponse graph(@RequestBody GraphRequest body) {
        if (body == null || !StringUtils.hasText(body.question())) {
            throw new IllegalArgumentException("question 不能为空");
        }
        Integer override = body.availableOverride();
        // 负库存没有意义，早一点拒绝，别让条件边去处理一个奇怪的数字
        if (override != null && override < 0) {
            throw new IllegalArgumentException("availableOverride 不能为负数");
        }

        log.info("[graph] 收到请求 question={} availableOverride={}", body.question(), override);
        GraphResponse response = shipGraph.run(body.question().trim(), override);
        log.info("[graph] 返回 nodes={} branch={}", response.nodes(), response.branch());
        return response;
    }
}
