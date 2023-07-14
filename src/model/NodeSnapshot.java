package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NodeSnapshot {

    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static NodeSnapshot complete(Node node) {
        return new NodeSnapshot(
                node.getNodeId(),
                node.getStatus(),
                node.getMeshChannel(),
                node.getRoutingRegistry(),
                node.calculateReception());
    }

    public static NodeSnapshot fromUpwardsRoutingUpdate(byte nodeId, ChannelInfo meshChannel, byte[] receptionData) {
        Map<Byte, Double> reception = new HashMap<>();
        for (int i = 0; i + 1 < receptionData.length; i += 2) reception.put(receptionData[i], receptionData[i + 1] / 256.0);

        return new NodeSnapshot(
                nodeId,
                NodeStatus.Node,
                meshChannel,
                null,
                reception);
    }

    public static byte[] receptionData(Node node) {
        Collection<Map.Entry<Byte, Double>> entries = node.calculateReception().entrySet();
        byte[] data = new byte[entries.size() * 2];
        int i = 0;
        for (var entry : entries) {
            data[i++] = entry.getKey();
            data[i++] = (byte) (entry.getValue() * 256);
        }
        return data;
    }

    public static Map<Byte, Byte> receptionMap(byte[] data) {
        Map<Byte, Byte> map = new HashMap<>();
        for (int i = 0; i + 1 < data.length; i++) {
            map.put(data[i], data[i+1]);
        }
        return map;
    }

    public static byte[] routingRegistryData(Set<Byte> routingRegistry) {
        byte[] data = new byte[routingRegistry.size()];
        int i = 0;
        for (byte b : routingRegistry) data[i++] = b;
        return data;
    }

    public final long threadId;
    public final LocalDateTime timestamp;
    public final byte id;
    public final NodeStatus status;
    public final ChannelInfo meshChannel;
    public final Set<Byte> routingRegistry;
    public final Map<Byte, Double> reception;

    private NodeSnapshot(byte id, NodeStatus status, ChannelInfo meshChannel, Set<Byte> routingRegistry, Map<Byte, Double> reception) {
        this.threadId = Thread.currentThread().threadId();
        this.timestamp = LocalDateTime.now();
        this.id = id;
        this.status = status;
        this.meshChannel = meshChannel;
        this.routingRegistry = routingRegistry == null? null : Collections.unmodifiableSet(routingRegistry);
        this.reception = reception == null? null : Collections.unmodifiableMap(reception);
    }

    @Override
    public String toString() {
        return String.format("[ %s | pid=%d\t| node~%d(%s) @%s ]",
                timestamp.format(format), threadId, id, status, meshChannel);
    }
}
