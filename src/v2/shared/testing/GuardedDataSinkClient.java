package v2.shared.testing;

import v2.core.context.Module;
import v2.core.domain.DataSinkClient;
import v2.core.context.Context;
import v2.core.domain.message.Message;

public class GuardedDataSinkClient implements DataSinkClient {

    private final DataSinkClient data;
    private Handle handle;

    public GuardedDataSinkClient(DataSinkClient data) {
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
    public void feed(Message message) {
        if (handle.dataSinkDisabled()) throw new RuntimeException("invalid call of DataSinkClient.feed()");
        data.feed(message);
    }

    public interface Handle extends Module {
        boolean dataSinkDisabled();
    }
}
