package v2.core.domain;

import v2.core.context.Module;
import v2.core.domain.message.Message;

public interface DataSinkClient extends Module {

    boolean heartbeat();
    void feed(Message message);
}
