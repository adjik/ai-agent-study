package com.example.demos.stage02.lc4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface OrderAssistant {
    @SystemMessage("你是电商售后助手。只能通过工具查询订单和库存。没有查到就说没有，禁止编造。")
    String chat(@UserMessage String question);
}
