package v2.core.domain.message;

/**
 * <p>
 *     message structure: <br/>
 *     [typification bits] [correspondence counter] [address]
 * </p>
 * <p>
 *     typification bits: <br/>
 *     indication on how the message should be interpreted
 * </p>
 * <p>
 *     correspondence counter: <br/>
 *     continuously incrementing number to track lost messages
 * </p>
 * <p>
 *     address: <br/>
 *     [downwards bit] [0] [node id] <br/>
 *     the downwards bit indicates the direction in which the message travels: <br/>
 *     - downwards, meaning from controller to node (1) <br/>
 *     - upwards, meaning from node to controller (0) <br/>
 * </p>
 */
public interface MessageHeader {

    ////////// begin address part //////////
    int ADDRESS_BITS = 8; // CONFIGURABLE. proposed: 8
    int ADDRESS_SHIFT = 0;
    int ADDRESS_MASK = (-1 << ADDRESS_SHIFT) ^ (-1 << (ADDRESS_SHIFT + ADDRESS_BITS));

    int NODE_ID_BITS = ADDRESS_BITS - 2; // part of address
    int NODE_ID_SHIFT = ADDRESS_SHIFT;
    int NODE_ID_MASK = (-1 << NODE_ID_SHIFT) ^ (-1 << (NODE_ID_SHIFT + NODE_ID_BITS));

    int ADDRESS_MULTIPURPOSE_BIT = 1 << (ADDRESS_BITS - 2);
    int RESEND_BIT = ADDRESS_MULTIPURPOSE_BIT; // as part of message header, indicates that this message was lost before and should ignore cache
    int DELETE_BIT = ADDRESS_MULTIPURPOSE_BIT; // as part of routing data, indicates that an address should be removed from the forwarding list
    int RESOLVED_BIT = ADDRESS_MULTIPURPOSE_BIT; // as part of tracing data, indicates that a message was found
    int DOWNWARDS_BIT = 1 << (ADDRESS_BITS - 1);
    ////////// end address part //////////

    ////////// begin counter part //////////
    int COUNTER_BITS = 5; // CONFIGURABLE. proposed: 5
    int COUNTER_SHIFT = ADDRESS_SHIFT + ADDRESS_BITS;
    int COUNTER_MASK = (-1 << COUNTER_SHIFT) ^ (-1 << (COUNTER_SHIFT + COUNTER_BITS));
    ////////// end counter part //////////

    ////////// begin type part //////////
    int TYPE_BITS = 3; // CONFIGURABLE. proposed: 3 min: 3
    int TYPE_SHIFT = COUNTER_SHIFT + COUNTER_BITS;
    int TYPE_MASK = (-1 << TYPE_SHIFT) ^ (-1 << (TYPE_SHIFT + TYPE_BITS));

    int HELLO_BIT =     1 << TYPE_SHIFT;
    int ROUTING_BIT =   1 << (TYPE_SHIFT + 1);
    int TRACE_BIT =     1 << (TYPE_SHIFT + 2);
    ////////// end type part //////////

    int HEADER_BITS = TYPE_SHIFT + TYPE_BITS;
    int HEADER_MASK = ~(-1 << HEADER_BITS);

    int TRACING_MASK = ADDRESS_MASK | COUNTER_MASK;


    int getHeaderBinary();

    default byte getNodeId() {
        return (byte) ((getHeaderBinary() & NODE_ID_MASK) >>> NODE_ID_SHIFT);
    }

    default int getCounter() {
        return (getHeaderBinary() & COUNTER_MASK) >>> COUNTER_SHIFT;
    }

    default byte getRoutingAddress() {
        return (byte) ((getHeaderBinary() & (NODE_ID_MASK | DOWNWARDS_BIT)) >>> ADDRESS_SHIFT);
    }

    default int getTracingHeader(byte counter) {
        // cut out type and counter, switch direction
        return ((getHeaderBinary() & ~TYPE_MASK & ~COUNTER_MASK) ^ DOWNWARDS_BIT) | (counter << COUNTER_SHIFT);
    }

}
