package v2.core.domain.message;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public enum MessageType implements MessageHeader {
    Any(0, 0),
    Upwards(DOWNWARDS_BIT, 0),
    Downwards(DOWNWARDS_BIT, DOWNWARDS_BIT),

    Resend(RESEND_BIT, RESEND_BIT),

    /**
     * Link java.test. Recipients can measure link reliability based on the amount of Hellos received.
     * If node-id = 0, a hello message is to be interpreted as an attempt to join the mesh.
     */
    Hello(TYPE_MASK, HELLO_BIT),

    /**
     * A node wants to join the mesh.
     * The neighbour of that node asks the controller to provide a new id.
     * All nodes on the answer's path should add the joining node's id to their routing registry.
     */
    Join(TYPE_MASK, HELLO_BIT, ROUTING_BIT),
    UpwardsJoin(TYPE_MASK | DOWNWARDS_BIT, HELLO_BIT, ROUTING_BIT),
    DownwardsJoin(TYPE_MASK | DOWNWARDS_BIT, HELLO_BIT, ROUTING_BIT, DOWNWARDS_BIT),

    /**
     * Upwards: Link reliabilities measured by a node.
     * Downwards: Routing registry updates for a node.
     */
    Routing(TYPE_MASK, ROUTING_BIT),
    UpwardsRouting(TYPE_MASK | DOWNWARDS_BIT, ROUTING_BIT),
    DownwardsRouting(TYPE_MASK | DOWNWARDS_BIT, ROUTING_BIT, DOWNWARDS_BIT),

    /**
     * Both directions: A message got lost somewhere on its path and needs to be resent.
     */
    Trace(TYPE_MASK, TRACE_BIT),
    Resolved(RESOLVED_BIT, RESOLVED_BIT),

    /**
     * Data collected from a node, sent upwards. Interpretation depends on application.
     */
    Data(TYPE_MASK),
    ;

    private static final int ALL_TYPES_BITMASK = calculateAllTypesBitmask();

    private final int bitmask;
    private final int binary;
    MessageType(int bitmask, int... headerBits) {
        this.bitmask = bitmask;
        int tmp = 0;
        for (int bit : headerBits) {
            tmp |= bit;
        }
        this.binary = tmp;
    }

    @Override
    public int getHeaderBinary() {
        return binary;
    }

    public boolean matches(Message message) {
        return matches(message.header());
    }

    public boolean matches(int header) {
        return (header & bitmask) == binary;
    }

    public Collection<MessageType> children() {
        return Arrays.stream(values()).filter(t -> (t.binary & bitmask) == binary).collect(Collectors.toList());
    }

    /**
     * does not include direction or resend
     * @return Hello, Join, Routing, Trace, Lost, or Data
     */
    public static MessageType pure(Message message) {
        int type = message.header() & TYPE_MASK;
        for (MessageType t : values()) {
            if (t.bitmask == TYPE_MASK && t.binary == type) return t;
        }
        throw new IllegalArgumentException("unmatchable message type: " + type + " (message: " + message + ")");
    }

    private static int calculateAllTypesBitmask() {
        int tmp = 0;
        for (MessageType t : values()) {
            tmp |= t.bitmask;
        }
        return tmp;
    }
}
