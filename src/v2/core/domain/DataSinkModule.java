package v2.core.domain;

import v2.core.context.Module;
import v2.core.domain.message.Message;

import java.util.Collection;

public interface DataSinkModule extends Module {

    boolean heartbeat();
    Collection<Integer> feed(Message message);
}
