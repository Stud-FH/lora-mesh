package v2.core.domain;

import v2.core.context.Module;
import v2.core.domain.message.Message;
import v2.core.domain.node.NodeInfo;

import java.util.List;

public interface PceClient extends Module {

    ChannelInfo heartbeat(NodeInfo nodeInfo);
    byte allocateAddress(long sid, byte mediatorId, double mediatorRetx);
    CorrespondenceRegister correspondence(byte address);
    List<String> feed(long controllerId, Message message);

}
