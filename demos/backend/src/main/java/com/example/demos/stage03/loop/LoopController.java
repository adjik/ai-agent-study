package com.example.demos.stage03.loop;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第 2 天 · 手写循环的 HTTP 入口，POST /api/stage/03/loop。
 * <p>
 * Controller 不做查表、不拼 messages。它只做三件事：拒绝空问句、把缺省的 maxSteps 补成 4、
 * 把已经裁过的上限交给 {@link HandWrittenAgentLoop#run}。循环、工具协议和 steps 都在那个类里。
 * <p>
 * 对照阶段 2 的 POST /api/stage/02/chat：那边一次 {@code ChatClient.call()} 在服务端把 while 跑完，
 * 响应里只有最终句子和 toolsCalled，没有第几轮。这个接口把每一轮交回浏览器。
 */
@RestController
@RequestMapping("/api/stage/03")
public class LoopController {

    private final HandWrittenAgentLoop agentLoop;

    public LoopController(HandWrittenAgentLoop agentLoop) {
        this.agentLoop = agentLoop;
    }

    @PostMapping("/loop")
    public LoopResponse loop(@RequestBody LoopRequest body) {
        if (body == null || !StringUtils.hasText(body.question())) {
            // IllegalArgumentException 由 ApiExceptionHandler 收成 400，页面 alert 显示这句话。
            throw new IllegalArgumentException("question 不能为空");
        }
        /*
         * 手册页面默认 4：问状态用 2 轮，问发货用 3 轮，4 留一轮余量。
         * 上限卡在 8，是因为每一轮都是一次真实的 DeepSeek 请求，防止输入框被改成很大的数后连续空打。
         */
        int maxSteps = body.maxSteps() == null ? 4 : body.maxSteps();
        if (maxSteps < 1 || maxSteps > 8) {
            throw new IllegalArgumentException("maxSteps 取值 1 到 8，默认 4");
        }
        return agentLoop.run(body.question().trim(), maxSteps);
    }
}
