package v2.shared.testing;

import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.ChannelInfo;
import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.PceModule;
import v2.core.domain.message.Message;

import java.util.List;

public class GuardedPceModule implements PceModule {

    private final PceModule pce;
    private Handle handle;

    public GuardedPceModule(PceModule pce) {
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
    public ChannelInfo heartbeat() {
        return handle.pceDisabled()? null : pce.heartbeat();
    }

    @Override
    public int allocateAddress(long sid, byte mediatorId, double mediatorRetx) {
        if (handle.pceDisabled()) throw new IllegalStateException("disabled");
        return pce.allocateAddress(sid, mediatorId, mediatorRetx);
    }

    @Override
    public CorrespondenceRegister correspondence(int address) {
        if (handle.pceDisabled()) throw new IllegalStateException("disabled");
        return pce.correspondence(address);
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        if (handle.pceDisabled()) throw new IllegalStateException("disabled");
        return pce.feed(controllerId, message);
    }

    public interface Handle extends Module {
        boolean pceDisabled();
    }
}
