package v2.core.domain;

import v2.core.context.Module;
import v2.core.domain.message.Message;

import java.util.List;

public interface PceModule extends Module {

    ChannelInfo heartbeat();
    byte allocateAddress(long sid, byte mediatorId, double mediatorRetx);
    CorrespondenceRegister correspondence(byte address);
    List<String> feed(long controllerId, Message message);

}
