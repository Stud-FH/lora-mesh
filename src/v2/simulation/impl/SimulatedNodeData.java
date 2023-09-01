package v2.simulation.impl;

import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.node.NodeStatus;
import v2.shared.impl.LocalCorrespondenceRegister;

import java.util.*;

public class SimulatedNodeData {

    public long id;

    public int address;

    public NodeStatus status;

    public long lastUpdated;

    public Map<Integer, Double> retx = new HashMap<>();

    public int sendingCounter = 0;

    public int nextReceivingCounter = 0;

    public Set<Integer> routing = new HashSet<>();

    public Set<Integer> missingMessages = new HashSet<>();

    public List<String> statusKeys = new ArrayList<>();

    public CorrespondenceRegister correspondence = null;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<Integer, Double> getRetx() {
        return retx;
    }

    public void setRetx(Map<Integer, Double> retx) {
        this.retx = retx;
    }

    public int getSendingCounter() {
        return sendingCounter;
    }

    public void setSendingCounter(int sendingCounter) {
        this.sendingCounter = sendingCounter;
    }

    public int getNextReceivingCounter() {
        return nextReceivingCounter;
    }

    public void setNextReceivingCounter(int nextReceivingCounter) {
        this.nextReceivingCounter = nextReceivingCounter;
    }

    public Set<Integer> getRouting() {
        return routing;
    }

    public void setRouting(Set<Integer> routing) {
        this.routing = routing;
    }

    public Set<Integer> getMissingMessages() {
        return missingMessages;
    }

    public void setMissingMessages(Set<Integer> missingMessages) {
        this.missingMessages = missingMessages;
    }

    public List<String> getStatusKeys() {
        return statusKeys;
    }

    public void setStatusKeys(List<String> statusKeys) {
        this.statusKeys = statusKeys;
    }

    public CorrespondenceRegister getCorrespondence() {
        return correspondence;
    }

    public void setCorrespondence(CorrespondenceRegister correspondence) {
        this.correspondence = correspondence;
    }
}
