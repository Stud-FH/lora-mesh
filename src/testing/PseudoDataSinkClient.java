package testing;

import model.ApplicationContext;
import model.DataSinkClient;
import model.Module;
import model.message.Message;

import java.util.Collection;
import java.util.Set;

public class PseudoDataSinkClient implements DataSinkClient {

    @Override
    public void useContext(ApplicationContext ctx) {

    }

    @Override
    public boolean heartbeat() {
        return false;
    }

    @Override
    public void feed(Message message) {
        throw new RuntimeException("invalid call of DataSinkClient.feed()");
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }

    @Override
    public String info() {
        return "Pseudo Data Sink";
    }
}
