package v2.core.domain.node;

import java.util.Map;

// TODO remove
public class NodeInfo {

    public final long id;
    public final NodeStatus status;
    public final int address;
    public final Map<Integer, Double> retx;

    public NodeInfo(long id, NodeStatus status, int address, Map<Integer, Double> retx) {
        this.id = id;
        this.status = status;
        this.address = address;
        this.retx = retx;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "id=" + id +
                ", status=" + status +
                ", address=" + address +
                ", retx=" + retx +
                '}';
    }
}
