package model;

import model.message.Message;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PceClient extends Module {

    ChannelInfo heartbeat(NodeInfo nodeInfo);
    byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx);
    CorrespondenceClient correspondence(byte nodeId);
    List<String> feed(long controllerId, Message message);

    @Override
    default Collection<Class<? extends Module>> providers() {
        return Set.of(PceClient.class);
    }

}
