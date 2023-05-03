package simulation;

import model.*;
import model.message.Message;
import model.message.MessageHeader;
import model.message.MessageType;

import java.util.*;

public class SimulatedApi implements ApiAdapter {

    static String meshConfig = "3";
    static byte nextId = 1;

    static Set<String> redeemedJoiningCodes = new HashSet<>();

    static Map<Byte, CorrespondenceManager> correspondences = new HashMap<>();

    static Map<Byte, Set<Byte>> calculatedRouting = new HashMap<>();
    static Map<Byte, Set<Byte>> routing = new HashMap<>();
    static Map<Byte, Map<Byte, Byte>> reception = new HashMap<>();
    static Map<Byte, Long> lastControllerPing = new HashMap<>();

    SimulatedTransmitter simT;

    public SimulatedApi(SimulatedTransmitter simT) {
        this.simT = simT;
    }

    @Override
    public boolean testConnection() {
        if (!simT.apiConnected) return false;
        lastControllerPing.put(simT.node.getId(), System.currentTimeMillis());
        return true;
    }

    @Override
    public CorrespondenceManager correspondence(byte nodeId) {
        return correspondences.get(nodeId);
    }

    @Override
    public List<String> receive(byte controllerId, Message message) {
        if (!testConnection()) throw new IllegalStateException();

        synchronized(Simulation.INSTANCE) {

            List<String> jobs = new ArrayList<>();

            if (MessageType.Downwards.matches(message)) {
                return jobs;
            }

            var lost = registerAndListLosses(message);
            if (lost.length > 0) {
                StringBuilder job = new StringBuilder(String.format("%d trace ", message.getNodeId()));
                for (byte b : lost) job.append(";").append(b);
                jobs.add(job.toString());
            }

            if (message.getNodeId() == 0 || MessageType.Join.matches(message)) {
                var code = new String(message.data()).substring(1);
                if (tryRedeemJoiningCode(code)) {
                    byte assignedId = nextId();
                    jobs.add(String.format("%d invite %d %s", message.getNodeId(), assignedId, code));
                }
            } else if (MessageType.Routing.matches(message)) {
                var updates = updateRouting(message);
                StringBuilder job = new StringBuilder(String.format("%d update ", message.getNodeId()));
                for (byte b : updates) job.append(";").append(b);
                jobs.add(job.toString());
            } else if (MessageType.Data.matches(message)) {
                feedData(message.data());
            }
            return jobs;
        }
    }

    @Override
    public byte nextId() {
        if (!testConnection()) throw new IllegalStateException();
        synchronized (Simulation.INSTANCE) {
            correspondences.put(nextId, LocalCorrespondenceManager.to(nextId));
            return nextId++;
        }
    }

    Set<Byte> updateRouting(Message message) {
        synchronized (Simulation.INSTANCE) {
            reception.computeIfAbsent(message.getNodeId(), HashMap::new)
                    .putAll(NodeSnapshot.receptionMap(message.data()));
//            runFloydWarshall(); TODO
            Set<Byte> current = routing.getOrDefault(message.getNodeId(), Set.of());
            Set<Byte> calculated = calculatedRouting.getOrDefault(message.getNodeId(), Set.of());
            Set<Byte> updates = new HashSet<>(calculated.stream().filter(b -> !current.contains(b)).toList());
            current.stream().filter(b -> !calculated.contains(b)).map(b -> (byte) (b | (MessageHeader.DELETE_BIT))).forEach(updates::add);
            return updates;
        }
    }

    @Override
    public String getTuning() {
        if (!testConnection()) throw new IllegalStateException();
        return meshConfig;
    }

    void feedData(byte[] data) {
        System.out.println("data received: "+ new String(data));
    }

    boolean tryRedeemJoiningCode(String code) {
        if (!testConnection()) throw new IllegalStateException();
        synchronized (Simulation.INSTANCE) {
            return redeemedJoiningCodes.add(code);
        }
    }

    byte[] registerAndListLosses(Message message) {
        if (message.getNodeId() == 0) return new byte[0];
        synchronized (Simulation.INSTANCE) {
            return correspondences.get(message.getNodeId()).registerAndListLosses(message);
        }
    }

    void runFloydWarshall() {
        int n = reception.size();
        byte[] alias = new byte[n];
        float[][] dist = new float[n][n];
        byte[][] trace = new byte[n][n];
        Set<Byte> controller = new HashSet<>();

        // init
        long timeThreshold = System.currentTimeMillis() - 10000;
        byte next = 0;
        for (byte nodeId : reception.keySet()) {
            if (lastControllerPing.getOrDefault(nodeId, 0L) >= timeThreshold) controller.add(next);
            alias[next++] = nodeId;
        }
        for (byte i = 0; i < n; i++) {
            for (byte j = 0; j < n; j++) {
                if (i == j) {
                    dist[i][j] = 0;
                    trace[i][j] = i;
                } else {
                    dist[i][j] = 255f / reception.get(alias[i]).getOrDefault(alias[j], (byte) 0);
                    trace[i][j] = (dist[i][j] < Float.MAX_VALUE)? j : 0;
                }
            }
        }

        // find all shortest paths
        for (byte k = 0; k < n; k++) {
            for (byte i = 0; i < n; i++) {
                for (byte j = 0; j < n; j++) {
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                        trace[i][j] = trace[i][k];
                    }
                }
            }
        }

        calculatedRouting.clear();

        for (byte nodeIdx = 0; nodeIdx < n; nodeIdx++) {
            byte upwardsIdx = -1;
            byte downwardsIdx = -1;
            for (byte cIdx : controller) {
                if (cIdx == nodeIdx) {
                    upwardsIdx = downwardsIdx = nodeIdx;
                    break;
                }
                if (upwardsIdx == -1 || dist[nodeIdx][cIdx] < dist[nodeIdx][upwardsIdx]) {
                    upwardsIdx = cIdx;
                }
                if (downwardsIdx == -1 || dist[cIdx][nodeIdx] < dist[downwardsIdx][nodeIdx]) {
                    downwardsIdx = cIdx;
                }
            }

            for (int i = trace[nodeIdx][upwardsIdx]; i != upwardsIdx; i = trace[i][upwardsIdx])
                calculatedRouting.computeIfAbsent(alias[i], HashSet::new).add(alias[nodeIdx]);

            for (int i = trace[nodeIdx][downwardsIdx]; i != downwardsIdx; i = trace[i][downwardsIdx])
                calculatedRouting.computeIfAbsent(alias[i], HashSet::new).add((byte) (alias[nodeIdx] | (MessageHeader.DOWNWARDS_BIT >>> MessageHeader.ADDRESS_SHIFT)));
        }
    }
}
