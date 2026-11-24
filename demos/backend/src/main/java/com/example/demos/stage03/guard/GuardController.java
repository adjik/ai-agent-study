package com.example.demos.stage03.guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第 10 天 · 停止与边界的两个入口。
 *
 * <h2>为什么是两个接口</h2>
 * 一次带人工确认的运行被切成两段：
 * <ol>
 *   <li>{@code POST /guard} —— 开始跑，可能直接结束，也可能停在 {@code waiting_human} 并返回 runId</li>
 *   <li>{@code POST /guard/confirm} —— 人点「确认」后，用同一个 runId 把运行接上</li>
 * </ol>
 * 中间那一次点击不在代码里，这就是「人工介入」的成本：
 * 服务端要能暂停并记住状态，客户端要能展示确认条，两边都得配合。
 * <p>
 * Controller 只做校验和转发，三道闸全在 {@link GuardedAgent} 里。
 */
@RestController
@RequestMapping("/api/stage/03")
public class GuardController {

    private static final Logger log = LoggerFactory.getLogger(GuardController.class);

    private final GuardedAgent guardedAgent;

    public GuardController(GuardedAgent guardedAgent) {
        this.guardedAgent = guardedAgent;
    }

    /**
     * 开始一次运行。
     * 返回的 stopReason 决定下一步：final/max_steps 到此为止，waiting_human 要走确认接口。
     */
    @PostMapping("/guard")
    public GuardResponse guard(@RequestBody GuardRequest body) {
        if (body == null || !StringUtils.hasText(body.question())) {
            throw new IllegalArgumentException("question 不能为空");
        }
        /*
         * 上限卡在 8，和手写循环那页一致：每一轮都是一次真实的模型请求。
         * 演示第 1 步用 2 就能看到 max_steps；正常发货要 3 轮，4 留一轮余量。
         */
        int maxSteps = body.maxSteps() == null ? 4 : body.maxSteps();
        if (maxSteps < 1 || maxSteps > 8) {
            throw new IllegalArgumentException("maxSteps 取值 1 到 8，默认 4");
        }
        log.info("[guard] 收到请求 question={} maxSteps={}", body.question(), maxSteps);
        return guardedAgent.run(body.question().trim(), maxSteps);
    }

    /**
     * 确认并接上暂停的运行。
     * runId 必须来自上一次 {@code /guard} 的响应，服务端靠它在暂停表里找回待执行的退款。
     */
    @PostMapping("/guard/confirm")
    public GuardResponse confirm(@RequestBody GuardConfirmRequest body) {
        if (body == null || !StringUtils.hasText(body.runId())) {
            throw new IllegalArgumentException("runId 不能为空");
        }
        log.info("[guard] 收到确认 runId={}", body.runId());
        return guardedAgent.confirm(body.runId().trim());
    }
}
