package com.example.demos.stage03.graph.node;

/**
 * 从 dispatcher 返回的观察 JSON 里取字符串字段。
 * <p>
 * 观察只有两种形状：记录本身，或者 {@code {"error":...}}。两种都不保证某个字段在，
 * 所以取不到一律给空串，由调用方按「没查到」处理，而不是抛异常。
 * 图里只有两个查询节点会做这件事，所以放在一个包内小工具里，不引入 ObjectMapper。
 */
final class FixtureJson {

    private FixtureJson() {
    }

    static String text(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int index = json.indexOf(marker);
        if (index < 0) {
            return "";
        }
        int start = index + marker.length();
        int end = json.indexOf('"', start);
        return end < 0 ? "" : json.substring(start, end);
    }
}
