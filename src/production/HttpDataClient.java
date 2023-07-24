package production;

import model.ApplicationContext;
import model.DataSinkClient;
import model.Module;
import model.message.Message;

import java.util.Collection;
import java.util.Set;

public class HttpDataClient implements DataSinkClient {

    private ApplicationContext ctx;

    @Override
    public void useContext(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean heartbeat() {
        var response = ctx.resolve(Http.class).getResponseStringDebugOnly("/data");
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
