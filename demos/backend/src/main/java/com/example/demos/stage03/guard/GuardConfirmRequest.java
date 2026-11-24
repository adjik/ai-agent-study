package com.example.demos.stage03.guard;

/**
 * POST /api/stage/03/guard/confirm 的请求体。
 * <p>
 * 只有 runId 一个字段，这是刻意的：
 * 问句、步数、要退的单号都随第一次运行存在服务端了。
 * 如果确认时还让调用方重传问句和单号，等于又发起一次新运行——
 * 那样「确认」和「重新执行」就分不清了，确认接口也失去了「接上原来那次」的含义。
 *
 * @param runId 上一次 {@code /guard} 响应里的 runId，服务端用它到暂停表里找回待执行的退款
 */
public record GuardConfirmRequest(String runId) {}
