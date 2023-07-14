package production;

import model.ChannelInfo;
import model.CorrespondenceClient;
import model.PceClient;
import model.message.Message;

import java.util.List;

public class HttpPceClient implements PceClient {

    @Override
    public ChannelInfo heartbeat() {
        return null;
    }

    @Override
    public byte allocateNodeId() {
        return 0;
    }

    @Override
    public CorrespondenceClient correspondence(byte nodeId) {
        return null;
    }

    @Override
    public List<String> receive(byte nodeId, Message message) {
        return null;
    }
}
