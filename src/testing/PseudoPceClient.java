package testing;

import model.ChannelInfo;
import model.CorrespondenceClient;
import model.PceClient;
import model.message.Message;
import model.NodeInfo;

import java.util.List;

public class PseudoPceClient implements PceClient {
    @Override
    public ChannelInfo heartbeat(NodeInfo nodeInfo) {
        return null;
    }

    @Override
    public byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx) {
        throw new RuntimeException("invalid call of PceClient.allocateNodeId()");
    }

    @Override
    public CorrespondenceClient correspondence(byte nodeId) {
        throw new RuntimeException("invalid call of PceClient.correspondence()");
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        throw new RuntimeException("invalid call of PceClient.feed()");
    }
}
