package v2.shared.util;

import v2.core.domain.message.Message;
import v2.core.domain.node.Node;
import v2.shared.api.domain.LogEntry;

import java.util.*;
import java.util.stream.Collectors;

public class JsonUtil {

    public static String nodeInfo(Node node) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append(key("id")).append(integer(node.id())).append(", ")
                .append(key("address")).append(integer(node.address())).append(", ")
                .append(key("status")).append(string(node.status())).append(", ")
                .append(key("retx")).append(retxMap(node.retx()))
                .append("}");
        return sb.toString();
    }

    public static String message(Message data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append(key("header")).append(integer(data.header)).append(", ")
                .append(key("data")).append(bytes(data.data))
                .append("}");
        return sb.toString();
    }

    public static String logEntry(LogEntry data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append(key("severity")).append(string(data.severity)).append(", ")
                .append(key("moduleInfo")).append(string(data.moduleInfo)).append(", ")
                .append(key("data")).append(bytes(data.data))
                .append("}");
        return sb.toString();
    }

    public static String key(String name) {
        return String.format("\"%s\": ", name);
    }

    public static String integer(long n) {
        return String.format("\"%d\"", n);
    }

    public static String string(Object o) {
        return String.format("\"%s\"", o.toString());
    }

    public static String bytes(byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (data.length > 0) {
            for (var b : data) sb.append(b).append(", ");
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("]");
        return sb.toString();
    }

    public static String retxMap(Map<Integer, Double> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (!data.isEmpty()) {
            for (var entry : data.entrySet()) sb.append(String.format("%d: %f, ", entry.getKey(), entry.getValue()));
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("}");
        return sb.toString();
    }

    public static List<String> parseStringList(String str) {
        if (str.equals("[]")) return new ArrayList<>();
        return Arrays.asList(str.substring(2, str.length() -2).split("\"\\s*,\\s*\""));
    }

    public static byte[] parseByteArray(String str) {
        if (str.equals("[]")) return new byte[]{};
        var split = str.substring(1, str.length() -1).split("\\s*,\\s*");
        byte[] result = new byte[split.length];
        for (int i = 0; i < split.length; i++) {
            result[i] = Byte.parseByte(split[i]);
        }
        return result;
    }

    public static List<Byte> parseByteList(String str) {
        if (str.equals("[]")) return Collections.emptyList();
        return Arrays.stream(str.substring(1, str.length() -1)
                .split("\\s*,\\s*"))
                .map(Byte::parseByte)
                .collect(Collectors.toList());
    }

    public static List<Integer> parseIntList(String str) {
        if (str.equals("[]")) return Collections.emptyList();
        return Arrays.stream(str.substring(1, str.length() -1)
                        .split("\\s*,\\s*"))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
