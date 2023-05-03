package model;

import model.message.Message;
import model.message.MessageHeader;

import java.util.HashMap;
import java.util.Map;

public class HelloCounter {

    protected static final int COUNTER_LIMIT = 1 << MessageHeader.COUNTER_BITS;

    protected static final int NODE_LIMIT = 1 << MessageHeader.NODE_ID_BITS;

    private final int[] received = new int[NODE_LIMIT];
    private final int[] missed = new int[NODE_LIMIT];
    private final int[] lastReceived = new int[NODE_LIMIT];
    private final byte[] lastReset = new byte[NODE_LIMIT];

    public boolean knows(byte nodeId) {
        return received[nodeId] != 0 || lastReceived[nodeId] != 0;
    }

    public void count(Message hello) {
        int nodeId = hello.getNodeId();
        int counter = hello.getCounter();

        if (received[nodeId] == 0) {
            lastReceived[nodeId] = counter;
            received[nodeId] = 1;
            missed[nodeId] = 0;
        } else {
            received[nodeId]++;
            missed[nodeId] += (counter - 1 -lastReceived[nodeId] + COUNTER_LIMIT) % COUNTER_LIMIT;
            lastReceived[nodeId] = counter;
        }
    }

    // TODO dying node not recognized!

    public Map<Byte, Byte> calculateReception(boolean reset) {
        Map<Byte, Byte> measured = new HashMap<>();
        for (byte i = 1; i < NODE_LIMIT; i++) {
            if (received[i] == 0) {
                if (lastReset[i] != 0) measured.put(i, lastReset[i]);
                continue;
            }
            double ratio = 1.0 * received[i] / (received[i] + missed[i]);
            byte calculated = (byte) (lastReset[i] == 0? 255 * ratio : 0.5 * lastReset[i] + 127.5 * ratio);
            measured.put(i, calculated);

            if (reset) {
                lastReset[i] = calculated;
                received[i] = missed[i] = 0;
            }
        }
        return measured;
    }

}
