package testing;

import model.Module;
import model.*;
import model.message.Message;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class GuardedPceClient implements PceClient {

    private final  PceClient pce;
    private final Supplier<Boolean> guard;

    public GuardedPceClient(PceClient pce, Supplier<Boolean> guard) {
        this.pce = pce;
        this.guard = guard;
    }

    @Override
    public void build(Context ctx) {
        pce.build(ctx);
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
    public Collection<Class<? extends Module>> providers() {
        return pce.providers();
    }

    @Override
    public ChannelInfo heartbeat(NodeInfo nodeInfo) {
        return guard.get()? pce.heartbeat(nodeInfo) : null;
    }

    @Override
    public byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx) {
        if (!guard.get()) throw new RuntimeException("invalid call of PceClient.allocateNodeId()");
        return pce.allocateNodeId(serialId, mediatorId, mediatorRetx);
    }

    @Override
    public CorrespondenceRegister correspondence(byte nodeId) {
        if (!guard.get()) throw new RuntimeException("invalid call of PceClient.correspondence()");
        return pce.correspondence(nodeId);
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        if (!guard.get()) throw new RuntimeException("invalid call of PceClient.feed()");
        return pce.feed(controllerId, message);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return pce.dependencies();
    }

    @Override
    public String info() {
        return guard.get()? pce.info() : "Disabled PCE";
    }
}
