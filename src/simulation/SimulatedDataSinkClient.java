package simulation;

import model.DataSinkClient;
import model.message.Message;

public class SimulatedDataSinkClient implements DataSinkClient {
    boolean connected;

    @Override
    public boolean heartbeat() {
        return connected;
    }

    @Override
    public void feed(Message message) {
        if (!connected) throw new IllegalStateException();
        System.out.printf("data received: %s\n", message);
    }
}
