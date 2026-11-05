package com.example.demos.stage02.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ToolCallLog {
    private static final ThreadLocal<List<Map<String, Object>>> HOLDER =
            ThreadLocal.withInitial(ArrayList::new);

    private ToolCallLog() {}

    public static void clear() {
        HOLDER.get().clear();
    }

    public static void record(String name, String args, boolean success, long elapsedMs) {
        HOLDER.get().add(Map.of(
                "name", name,
                "args", args == null ? "" : args,
                "success", success,
                "elapsedMs", elapsedMs));
    }

    public static List<Map<String, Object>> snapshot() {
        return List.copyOf(HOLDER.get());
    }
}
