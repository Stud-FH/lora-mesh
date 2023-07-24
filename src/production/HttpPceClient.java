package production;

import model.ChannelInfo;
import model.CorrespondenceClient;
import model.PceClient;
import model.message.Message;
import model.NodeInfo;

import java.util.ArrayList;
import java.util.List;

public class HttpPceClient implements PceClient {

    private final HttpRequestClient httpRequestClient;

    public HttpPceClient(HttpRequestClient httpRequestClient) {
        this.httpRequestClient = httpRequestClient;
    }

    @Override
    public ChannelInfo heartbeat(NodeInfo nodeInfo) {
        try {
            String response = httpRequestClient.postResponseStringDebugOnly("/pce", JsonUtil.nodeInfo(nodeInfo));
            return new ChannelInfo(response);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx) {
        String response = httpRequestClient.postResponseString(
                String.format("/pce/node-id?mediatorId=%d&mediatorRetx=%,.4f", mediatorId, mediatorRetx), serialId + "");
        return Byte.parseByte(response);
    }

    @Override
    public CorrespondenceClient correspondence(byte nodeId) {
        return new HttpCorrespondenceClient(nodeId, httpRequestClient);
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        var response = httpRequestClient.postResponseString("/pce/feed", JsonUtil.message(message));
        System.out.println(response); // TODO
        return new ArrayList<>();
    }
}
