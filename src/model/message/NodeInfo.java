package model.message;

import model.NodeStatus;

import java.util.Map;

public class NodeInfo {
    public final long serialId;
    public final NodeStatus status;
    public final int nodeId;
    public final Map<Integer, Double> retx;

    public NodeInfo(long serialId, NodeStatus status, int nodeId, Map<Integer, Double> retx) {
        this.serialId = serialId;
        this.status = status;
        this.nodeId = nodeId;
        this.retx = retx;
    }
}
