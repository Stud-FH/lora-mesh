package testing;

import model.DataSinkClient;
import model.message.Message;

public class PseudoDataSinkClient implements DataSinkClient {

    @Override
    public boolean heartbeat() {
        return false;
    }

    @Override
    public void feed(Message message) {
        throw new RuntimeException("invalid call of DataSinkClient.feed()");
    }
}
