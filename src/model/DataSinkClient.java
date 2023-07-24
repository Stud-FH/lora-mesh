package model;

import model.message.Message;

import java.util.Collection;
import java.util.Set;

public interface DataSinkClient extends Module {

    boolean heartbeat();
    void feed(Message message);

    @Override
    default Collection<Class<? extends Module>> providers() {
        return Set.of(DataSinkClient.class);
    }
}
