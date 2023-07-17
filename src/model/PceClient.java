package model;

import model.message.Message;
import model.message.NodeInfo;

import java.util.List;

public interface PceClient {

    ChannelInfo heartbeat(NodeInfo nodeInfo);
    byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx);
    CorrespondenceClient correspondence(byte nodeId);
    List<String> feed(long controllerId, Message message);

}
