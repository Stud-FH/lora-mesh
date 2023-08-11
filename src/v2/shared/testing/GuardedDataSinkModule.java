package v2.shared.testing;

import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.DataSinkModule;
import v2.core.domain.message.Message;

import java.util.Collection;

public class GuardedDataSinkModule implements DataSinkModule {

    private final DataSinkModule data;
    private Handle handle;

    public GuardedDataSinkModule(DataSinkModule data) {
        this.data = data;
    }

    @Override
    public void build(Context ctx) {
        data.build(ctx);
        handle = ctx.resolve(Handle.class);
    }

    @Override
    public void deploy() {
        data.deploy();
    }

    @Override
    public void destroy() {
        data.destroy();
    }

    @Override
    public boolean heartbeat() {
        return !handle.dataSinkDisabled() && data.heartbeat();
    }

    @Override
    public Collection<Integer> feed(Message message) {
        if (handle.dataSinkDisabled()) throw new IllegalStateException("disabled");
        return data.feed(message);
    }

    public interface Handle extends Module {
        boolean dataSinkDisabled();
    }
}
