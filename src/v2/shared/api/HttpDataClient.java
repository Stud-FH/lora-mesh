package v2.shared.api;

import v2.core.domain.DataSinkClient;
import v2.core.context.Context;
import v2.core.domain.message.Message;
import v2.shared.util.JsonUtil;

public class HttpDataClient implements DataSinkClient {

    private Http http;

    @Override
    public void build(Context ctx) {
        http = ctx.resolve(Http.class);
    }

    @Override
    public boolean heartbeat() {
        var response = http.getResponseStringDisableLogging("/data");
        return "true".equals(response);
    }

    @Override
    public void feed(Message message) {
        http.postResponseVoid("/data", JsonUtil.message(message));
    }
}
