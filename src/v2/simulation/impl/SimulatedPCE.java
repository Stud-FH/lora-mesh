package v2.simulation.impl;

import v2.core.common.BasicObservable;
import v2.core.common.Observable;
import v2.core.context.Context;
import v2.core.domain.ChannelInfo;
import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.PceModule;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageHeader;
import v2.core.domain.message.MessageType;
import v2.core.domain.node.NodeStatus;
import v2.core.log.Logger;
import v2.shared.impl.LocalCorrespondenceRegister;
import v2.shared.measurements.PceModuleInsights;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class SimulatedPCE implements PceModule,  PceModuleInsights {

    private final BasicObservable<Message> forwarded = new BasicObservable<>();

    private Logger logger;

    @Override
    public void build(Context ctx) {
        logger = ctx.resolve(Logger.class);
    }

    private final ChannelInfo meshChannel = new ChannelInfo("sim-channel");

    private final Map<Long, SimulatedNodeData> repo = new HashMap<>();

    private SimulatedNodeData getById(long id) {
        return repo.computeIfAbsent(id, key -> {
            var created = new SimulatedNodeData();
            created.id = id;
            created.lastUpdated = System.currentTimeMillis();
            created.status = NodeStatus.Down;
            created.address = -1;
            return created;
        });
    }

    private SimulatedNodeData getByAddress(int address) {
        return repo.values().stream().filter(n -> n.address == address).findAny().orElseThrow(IllegalArgumentException::new);
    }

    private boolean existsByAddress(int address) {
        return repo.values().stream().anyMatch(n -> n.address == address);
    }

    @Override
    public ChannelInfo heartbeat() {
        return meshChannel;
    }

    @Override
    public int allocateAddress(long id, byte mediatorId, double mediatorRetx) {
        var node = getById(id);
        if (node.address > 0) {
            logger.info(String.format("resolved address %d of node %d", node.address, id), this);
            return node.address;
        }
        logger.info("allocating address for node: "+id, this);
        int allocatedAddress = 1;
        while (existsByAddress(allocatedAddress)) allocatedAddress++;
        if (allocatedAddress > MessageHeader.MESH_ADDRESS_LIMIT) throw new IllegalStateException("node ids exhausted");
        node.address = allocatedAddress;
        node.status =NodeStatus.Joining;
        node.lastUpdated = System.currentTimeMillis();
        node.correspondence = LocalCorrespondenceRegister.to(allocatedAddress);
        logger.info(String.format("assigning address %d to node %d", allocatedAddress, id), this);
        return allocatedAddress;
    }

    @Override
    public CorrespondenceRegister correspondence(int address) {
        return getByAddress(address).correspondence;
    }

    @Override
    public List<String> feed(long controllerId, Message message) {
        forwarded.next(message);
        logger.info("message feed: "+message.header + Arrays.toString(message.data), this);

        int address = message.getAddress();

        List<String> controllerCommands = new ArrayList<>();
        var lost = registerAndListLosses(message);
        if (lost.size() > 0) {
            StringBuilder job = new StringBuilder(String.format("%d trace", address));
            for (int i : lost) job.append(" ").append(i);
            controllerCommands.add(job.toString());
        }

        if (address == 0 || MessageType.Join.matches(message)) {
            long id = idFromJoinData(message.data);
            int allocateAddress = allocateAddress(id, (byte) -1, -1);
            controllerCommands.add(String.format("%d invite %d %d", address, allocateAddress, id));
        } else if (MessageType.UpwardsRouting.matches(message)) {
            var updates = updateRouting(message, controllerId);
            StringBuilder job = new StringBuilder(String.format("%d update", address));
            for (int i : updates) job.append(" ").append(i);
            controllerCommands.add(job.toString());
        }

        return controllerCommands;
    }

    public Collection<Integer> registerAndListLosses(Message message) {
        int address = message.getNodeAddress();
        var node = getByAddress(address);
        return node.correspondence.registerAndListLosses(message);
    }

    private synchronized Set<Integer> updateRouting(Message message, long controllerId) {
        var node = getByAddress(message.getNodeAddress());
        if (node.id == controllerId) {
            node.status = (NodeStatus.Controller);
        } else {
            node.status = (NodeStatus.Node);
        }
        node.setLastUpdated(System.currentTimeMillis());
        node.getRetx().putAll(retx(message.data));

        Set<Integer> current = node.getRouting();
        Set<Integer> calculated = runFloydWarshall(node);
        Set<Integer> updates = new HashSet<>();
        updates.addAll(calculated.stream().filter(i -> !current.contains(i)).collect(Collectors.toList()));
        updates.addAll(current.stream().filter(i -> !calculated.contains(i)).map(i -> i | MessageHeader.DELETE_BIT).collect(Collectors.toList()));
        return updates;
    }

    private Set<Integer> runFloydWarshall(SimulatedNodeData nodeToUpdate) {
        var all = liveNodes();
        var controllers = all.stream().filter(n -> n.getStatus() == NodeStatus.Controller).collect(Collectors.toList());

        if (controllers.isEmpty()) {
            logger.warn("no controller found", this);
            return Set.of();
        }

        Map<Integer, SimulatedNodeData> byAddress = new HashMap<>();
        Map<SimulatedNodeData, Map<SimulatedNodeData, Double>> distance = new HashMap<>();
        Map<SimulatedNodeData, Map<SimulatedNodeData, SimulatedNodeData>> trace = new HashMap<>();
        Map<SimulatedNodeData, Set<SimulatedNodeData>> uplinkRouting = new HashMap<>();
        Map<SimulatedNodeData, Set<SimulatedNodeData>> downlinkRouting = new HashMap<>();

        all.forEach(node -> byAddress.put(node.getAddress(), node));

        for (var node : all) {
            distance.put(node, initialDistanceMap(node, byAddress));
            trace.put(node, initialTraceMap(node, byAddress));
            uplinkRouting.put(node, new HashSet<>());
            downlinkRouting.put(node, new HashSet<>());
        }

        // find all shortest paths
        for (SimulatedNodeData k : all) {
            for (SimulatedNodeData u : all) {
                for (SimulatedNodeData v : all) {
                    var uk = distance.get(u).getOrDefault(k, Double.MAX_VALUE);
                    var kv = distance.get(k).getOrDefault(v, Double.MAX_VALUE);
                    var uv = distance.get(u).getOrDefault(v, Double.MAX_VALUE);
                    if (uk + kv < uv) {
                        distance.get(u).put(v, uk + kv);
                        trace.get(u).put(v, k);
                    }
                }
            }
        }


        for (SimulatedNodeData u : all) {
            SimulatedNodeData uplinkCtl = null;
            SimulatedNodeData downlinkCtl = null;
            for (SimulatedNodeData ctl : controllers) {
                if (ctl == u) {
                    uplinkCtl = downlinkCtl = u;
                    break;
                }
                if (uplinkCtl == null || distance.get(u).get(ctl) < distance.get(u).get(uplinkCtl)) {
                    uplinkCtl = ctl;
                }
                if (downlinkCtl == null || distance.get(ctl).get(u) < distance.get(downlinkCtl).get(u)) {
                    downlinkCtl = ctl;
                }
            }
            fillRouting(u, uplinkCtl, trace, uplinkRouting);
            fillRouting(downlinkCtl, u, trace, downlinkRouting);
        }

        Set<Integer> calculatedRouting = new HashSet<>();
        calculatedRouting.addAll(uplinkRouting.get(nodeToUpdate).stream().map(SimulatedNodeData::getAddress).collect(Collectors.toSet()));
        calculatedRouting.addAll(downlinkRouting.get(nodeToUpdate).stream().map(SimulatedNodeData::getAddress).map(i -> i | MessageHeader.DOWNWARDS_BIT).collect(Collectors.toSet()));
        return calculatedRouting;
    }

    private  Map<SimulatedNodeData, Double> initialDistanceMap(SimulatedNodeData node, Map<Integer, SimulatedNodeData> byAddress) {
        Map<SimulatedNodeData, Double> map = new HashMap<>();
        map.put(node, 0.0);
        node.getRetx().forEach((key, value) -> map.put(byAddress.get(key), 1.0 / value));
        return map;
    }

    private  Map<SimulatedNodeData, SimulatedNodeData> initialTraceMap(SimulatedNodeData node, Map<Integer, SimulatedNodeData> byAddress) {
        Map<SimulatedNodeData, SimulatedNodeData> map = new HashMap<>();
        node.getRetx().keySet().stream().map(byAddress::get).forEach((neighbour) -> map.put(neighbour, neighbour));
        map.put(node, node);
        return map;
    }

    private void fillRouting(SimulatedNodeData start, SimulatedNodeData end, Map<SimulatedNodeData, Map<SimulatedNodeData, SimulatedNodeData>> trace, Map<SimulatedNodeData, Set<SimulatedNodeData>> routing) {
        if (start == null) return;
        for (SimulatedNodeData v = trace.get(start).get(end); v != null && v != end; v = trace.get(v).get(end)) {
            routing.get(v).add(start);
        }
    }

    @Override
    public Observable<Message> forwarded() {
        return forwarded;
    }

    public Collection<SimulatedNodeData> liveNodes() {
        long threshold = System.currentTimeMillis() - 500000;
        return repo.values().stream().filter(n -> n.lastUpdated >= threshold).collect(Collectors.toList());
    }

    private static long idFromJoinData(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        double mediatorRetx = buf.get() / 256.0;
        return buf.getLong();
    }

    private static Map<Integer, Double> retx(byte[] data) {
        Map<Integer, Double> retx = new HashMap<>();
        for (int i = 0; i + 1 < data.length; i+=2) {
            retx.put((int) data[i], 256.0 / data[i+1]);
        }
        return retx;
    }
}
