package production;

import model.message.Message;
import model.message.NodeInfo;

public class JsonUtil {

    public static String stringify(NodeInfo data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"serialId\": ").append(data.serialId()).append(", ")
                .append("\"nodeId\": ").append(data.nodeId()).append(", ")
                .append("\"status\": ").append("\"").append(data.status()).append("\"").append(", ")
                .append("\"retx\": {");
        for (var entry : data.retx().entrySet()) sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        sb.append("}}");
        return sb.toString();
    }

    public static String fromRetxMessage(Message message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
                .append("\"nodeId\": ").append(message.getNodeId()).append(", ")
                .append("\"retx\": {");
        for (int i = 0; i+1 < message.dataLength(); i += 2) sb.append(message.data(i)).append(": ").append(message.data(i+1)).append(", ");
        sb.append("}}");
        return sb.toString();
    }
}
