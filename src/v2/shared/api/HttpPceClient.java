package v2.shared.api;

import v2.core.domain.ChannelInfo;
import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.node.NodeInfo;
import v2.core.domain.PceClient;
import v2.core.context.Context;
import v2.core.domain.message.Message;
import v2.shared.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public class HttpPceClient implements PceClient {

    private Http http;

    @Override
    public void build(Context ctx) {
        http = ctx.resolve(Http.class);
    }

    @Override
    public ChannelInfo heartbeat(NodeInfo nodeInfo) {
        try {
            String response = http.postResponseString("/pce", JsonUtil.nodeInfo(nodeInfo));
            return new ChannelInfo(response);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx) {
        String response = http.postResponseString(
                String.format("/pce/node-id?mediatorId=%d&mediatorRetx=%,.4f", mediatorId, mediatorRetx), serialId + "");
        return Byte.parseByte(response);
    }

    @Override
    public CorrespondenceRegister correspondence(byte nodeId) {
        return new HttpCorrespondenceRegister(nodeId, http);
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        var response = http.postResponseString("/pce/feed", JsonUtil.message(message));
        System.out.println(response); // TODO
        return new ArrayList<>();
    }

    @Override
    public String info() {
        return "Http PCE";
    }
}
