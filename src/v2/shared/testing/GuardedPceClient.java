package v2.shared.testing;

import v2.core.context.Module;
import v2.core.domain.ChannelInfo;
import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.node.NodeInfo;
import v2.core.domain.PceClient;
import v2.core.context.Context;
import v2.core.domain.message.Message;

import java.util.List;
import java.util.function.Supplier;

public class GuardedPceClient implements PceClient {

    private final  PceClient pce;
    private Handle handle;

    public GuardedPceClient(PceClient pce) {
        this.pce = pce;
    }

    @Override
    public void build(Context ctx) {
        pce.build(ctx);
        handle = ctx.resolve(Handle.class);
    }

    @Override
    public void deploy() {
        pce.deploy();
    }

    @Override
    public void destroy() {
        pce.destroy();
    }

    @Override
    public ChannelInfo heartbeat(NodeInfo nodeInfo) {
        return handle.pceDisabled()? null : pce.heartbeat(nodeInfo);
    }

    @Override
    public byte allocateNodeId(long sid, byte mediatorId, double mediatorRetx) {
        if (handle.pceDisabled()) throw new RuntimeException("invalid call of PceClient.allocateNodeId()");
        return pce.allocateNodeId(sid, mediatorId, mediatorRetx);
    }

    @Override
    public CorrespondenceRegister correspondence(byte nodeId) {
        if (handle.pceDisabled()) throw new RuntimeException("invalid call of PceClient.correspondence()");
        return pce.correspondence(nodeId);
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        if (handle.pceDisabled()) throw new RuntimeException("invalid call of PceClient.feed()");
        return pce.feed(controllerId, message);
    }

    public interface Handle extends Module {
        boolean pceDisabled();
    }
}
