package production;

import model.ChannelInfo;
import model.CorrespondenceClient;
import model.NodeStatus;
import model.PceClient;
import model.message.Message;
import model.message.NodeInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HttpPceClient implements PceClient {

    private final String baseUrl;
    HttpClient http = Production.http;

    public HttpPceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public ChannelInfo heartbeat(NodeInfo nodeInfo) {
        try {
            var request = HttpRequest.newBuilder(new URI(baseUrl + "/pce"))
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(nodeInfo)))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return new ChannelInfo(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx) {
        try {
            var request = HttpRequest.newBuilder(new URI(String.format
                            ("%s/node-id?serialId=%d&mediatorId=%d&mediatorRetx=%,.4f",
                                    baseUrl, serialId, mediatorId, mediatorRetx)))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return Byte.parseByte(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public CorrespondenceClient correspondence(byte nodeId) {
        return new HttpCorrespondenceClient(baseUrl, nodeId);
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        try {
            var request = HttpRequest.newBuilder(new URI(String.format
                            ("%s/feed?controllerSerialId=%d", baseUrl, controllerId)))
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.fromRetxMessage(message)))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            // TODO
            System.out.println(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
