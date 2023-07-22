package production;

import model.LogEntry;
import model.message.Message;
import model.message.NodeInfo;

import java.util.Map;

public class JsonUtil {

    public static String nodeInfo(NodeInfo data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append(key("serialId")).append(integer(data.serialId)).append(", ")
                .append(key("nodeId")).append(integer(data.nodeId)).append(", ")
                .append(key("status")).append(string(data.status)).append(", ")
                .append(key("retx")).append(retxMap(data.retx))
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
                .append(key("nodeInfo")).append(nodeInfo(data.nodeInfo)).append(", ")
                .append(key("data")).append(bytes(data.data))
                .append("}");
        return sb.toString();
    }

    public static String retxMessage(Message message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append(key("nodeId")).append(message.getNodeId()).append(", ")
                .append(key("retx")).append("{");
        if (message.data.length >= 2) {
            for (int i = 0; i+1 < message.dataLength(); i += 2) sb.append(message.data(i)).append(": ").append(message.data(i+1)).append(", ");
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("}}");
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
}
