package production;

import model.Context;
import model.DataSinkClient;
import model.Module;
import model.message.Message;

import java.util.Collection;
import java.util.Set;

public class HttpDataClient implements DataSinkClient {

    private Context ctx;

    @Override
    public void build(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean heartbeat() {
        var response = ctx.resolve(Http.class).getResponseStringDisableLogging("/data");
        return "true".equals(response);
    }

    @Override
    public void feed(Message message) {
        ctx.resolve(Http.class).postResponseVoid("/data", JsonUtil.message(message));
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Http.class);
    }

    @Override
    public String info() {
        return "Http Data Sink";
    }
}
