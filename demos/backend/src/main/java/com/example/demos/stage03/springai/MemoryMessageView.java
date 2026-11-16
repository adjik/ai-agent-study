package com.example.demos.stage03.springai;

/**
 * 记忆里的一条，给页面看。不直接序列化 Spring 的 {@code Message}，避免把协议对象整棵交出去。
 *
 * @param role 消息角色：user、assistant、tool
 * @param text 用户句、最终回答，或工具名加观察。窗口裁掉的旧消息不会出现在这里
 */
public record MemoryMessageView(String role, String text) {}
