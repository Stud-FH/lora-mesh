package v2.shared.api;

import v2.core.common.Observable;
import v2.core.context.Context;
import v2.core.domain.ChannelInfo;
import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.PceModule;
import v2.core.domain.message.Message;
import v2.core.domain.node.Node;
import v2.shared.measurements.PceModuleInsights;
import v2.shared.util.JsonUtil;

import java.util.List;

public class HttpPceModule implements PceModule, PceModuleInsights {

    private Observable<Message> forwarded = new Observable<>();

    private Http http;
    private Node node;

    @Override
    public void build(Context ctx) {
        http = ctx.resolve(Http.class);
        node = ctx.resolve(Node.class);
    }

    @Override
    public ChannelInfo heartbeat() {
        try {
            String response = http.postResponseString("/pce", JsonUtil.nodeInfo(node));
            return new ChannelInfo(response);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte allocateAddress(long serialId, byte mediatorId, double mediatorRetx) {
        String response = http.postResponseString(
                String.format("/pce/address?mediatorId=%d&mediatorRetx=%,.4f", mediatorId, mediatorRetx), serialId + "");
        return Byte.parseByte(response);
    }

    @Override
    public CorrespondenceRegister correspondence(byte address) {
        return new HttpCorrespondenceRegister(address, http);
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        forwarded.next(message);
        var response = http.postResponseString(String.format("/pce/feed?controllerId=%d", controllerId), JsonUtil.message(message));
        return JsonUtil.parseStringList(response);
    }

    @Override
    public Observable<Message> forwarded() {
        return forwarded;
    }
}
