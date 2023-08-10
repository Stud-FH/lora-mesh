package testing;

import model.Context;
import model.DataSinkClient;
import model.Module;
import model.message.Message;

import java.util.Collection;
import java.util.function.Supplier;

public class GuardedDataSinkClient implements DataSinkClient {

    private final DataSinkClient data;
    private final Supplier<Boolean> guard;

    public GuardedDataSinkClient(DataSinkClient data, Supplier<Boolean> guard) {
        this.data = data;
        this.guard = guard;
    }

    @Override
    public void build(Context ctx) {
        data.build(ctx);
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
    public Collection<Class<? extends Module>> providers() {
        return data.providers();
    }

    @Override
    public boolean heartbeat() {
        return guard.get() && data.heartbeat();
    }

    @Override
    public void feed(Message message) {
        if (!guard.get()) throw new RuntimeException("invalid call of DataSinkClient.feed()");
        data.feed(message);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return data.dependencies();
    }

    @Override
    public String info() {
        return guard.get()? data.info() : "Disabled Data Sink";
    }
}
