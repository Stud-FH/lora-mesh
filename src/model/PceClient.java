package model;

import model.message.Message;

import java.util.List;

public interface PceClient {

    ChannelInfo heartbeat();
    byte allocateNodeId();
    CorrespondenceClient correspondence(byte nodeId);
    List<String> receive(byte nodeId, Message message);

}
