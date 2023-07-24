package production;

import model.Module;
import model.*;
import model.message.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class HttpPceClient implements PceClient {

    private ApplicationContext ctx;

    @Override
    public void useContext(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public ChannelInfo heartbeat(NodeInfo nodeInfo) {
        try {
            String response = ctx.resolve(Http.class).postResponseStringDebugOnly("/pce", JsonUtil.nodeInfo(nodeInfo));
            return new ChannelInfo(response);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx) {
        String response = ctx.resolve(Http.class).postResponseString(
                String.format("/pce/node-id?mediatorId=%d&mediatorRetx=%,.4f", mediatorId, mediatorRetx), serialId + "");
        return Byte.parseByte(response);
    }

    @Override
    public CorrespondenceRegister correspondence(byte nodeId) {
        return new HttpCorrespondenceRegister(nodeId, ctx.resolve(Http.class));
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        var response = ctx.resolve(Http.class).postResponseString("/pce/feed", JsonUtil.message(message));
        System.out.println(response); // TODO
        return new ArrayList<>();
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Http.class);
    }

    @Override
    public String info() {
        return "Http PCE";
    }
}
