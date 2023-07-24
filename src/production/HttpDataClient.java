package production;

import model.DataSinkClient;
import model.message.Message;

public class HttpDataClient implements DataSinkClient {

    private final HttpRequestClient httpRequestClient;

    public HttpDataClient(HttpRequestClient httpRequestClient) {
        this.httpRequestClient = httpRequestClient;
    }

    @Override
    public boolean heartbeat() {
        var response = httpRequestClient.getResponseStringDebugOnly("/data");
        return "true".equals(response);
    }

    @Override
    public void feed(Message message) {
        httpRequestClient.postResponseVoid("/data", JsonUtil.message(message));
    }
}
