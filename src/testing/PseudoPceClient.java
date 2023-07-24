package testing;

import model.*;
import model.Module;
import model.message.Message;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class PseudoPceClient implements PceClient {

    @Override
    public void useContext(ApplicationContext ctx) {

    }
    @Override
    public ChannelInfo heartbeat(NodeInfo nodeInfo) {
        return null;
    }

    @Override
    public byte allocateNodeId(long serialId, byte mediatorId, double mediatorRetx) {
        throw new RuntimeException("invalid call of PceClient.allocateNodeId()");
    }

    @Override
    public CorrespondenceRegister correspondence(byte nodeId) {
        throw new RuntimeException("invalid call of PceClient.correspondence()");
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        throw new RuntimeException("invalid call of PceClient.feed()");
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }

    @Override
    public String info() {
        return "Pseudo PCE";
    }
}
