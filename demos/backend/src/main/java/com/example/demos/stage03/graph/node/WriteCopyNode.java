package com.example.demos.stage03.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.demos.stage03.plan.PlanningModel;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点三 / 四：写文案。{@code canShipCopy} 和 {@code cannotShipCopy} 两个节点都用这个类，
 * 构造时传不同的名字和措辞。条件边决定走到哪一个。
 * <p>
 * <b>干什么</b>：把状态里的订单和库存事实交给模型，让模型写一句给客户看的话。
 * <b>这是整张图里唯一调模型的地方</b>，而且这一轮没有工具菜单——事实已经在状态里，
 * 模型想再查也没得查。这就是「模型负责表达，Java 负责流程和判断」。
 * <p>
 * <b>从状态里读</b>：{@code orderJson}、{@code orderStatus}、{@code available}。
 * <br>
 * <b>往状态里写</b>：{@code answer}（那句话）、{@code branch}（响应里的分支名）、{@code nodes}（追加）。
 * <p>
 * 它不再查表，所以没有 {@code steps} 产出。走了哪个文案节点，从 {@code nodes} 的最后一个名字就能看出。
 */
public class WriteCopyNode implements NodeAction {

    private static final Logger log = LoggerFactory.getLogger(WriteCopyNode.class);

    /** 只发一次、不带工具的模型调用，和第 8 天规划那两步用的是同一个组件。 */
    private final PlanningModel planningModel;
    /** 节点名，会出现在响应和日志里，例如 canShipCopy。 */
    private final String nodeName;
    /** 分支名，写进状态供条件边和响应使用，例如 canShip。 */
    private final String branch;
    /** 这一次要给模型的任务说明：说「可以发」还是「不能发」。 */
    private final String systemPrompt;

    public WriteCopyNode(PlanningModel planningModel, String nodeName, String branch, String systemPrompt) {
        this.planningModel = planningModel;
        this.nodeName = nodeName;
        this.branch = branch;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 事实全部从状态里取，不从问句里猜。取不到就用显式兜底，避免提示词里出现 null
        String orderJson = state.value("orderJson").map(String::valueOf).orElse("");
        String status = state.value("orderStatus").map(String::valueOf).orElse("");
        int available = state.value("available").map(v -> (Integer) v).orElse(0);

        // 拼一段事实清单给模型。这里没有任何「请你查一下」的措辞，只让它复述事实
        String user = "订单：" + orderJson
                + "\n订单状态：" + status
                + "\n本次判断用的可用数量：" + available;

        String answer = planningModel.complete(systemPrompt, user);
        log.info("[graph] 节点 {} 写出回答={}", nodeName, answer);

        return Map.of(
                "answer", answer,
                "branch", branch,
                "nodes", List.of(nodeName));
    }
}
