package v2.core.domain.node;

import v2.core.domain.message.Message;
import v2.core.domain.message.MessageHeader;

public class MessageCache {

    private final int[] headers;
    private final byte[][] data;

    private int pointer;

    public MessageCache(int capacity) {
        headers = new int[capacity];
        data = new byte[capacity][];
        pointer = 0;
        for (int i = 0; i < capacity; i++) headers[i] = -1;
    }

    public void store(Message m) {
        // remove previous
        int idx = findIndex(m.header(), MessageHeader.HEADER_MASK);
        if (idx != -1) {
            headers[idx] = -1;
        }
        // store
        headers[pointer] = m.header();
        data[pointer] = m.data();
        pointer = (pointer + 1) % headers.length;
    }

    public boolean contains(Message message) {
        int idx = findIndex(message.header(), MessageHeader.HEADER_MASK);
        return idx != -1;
    }

    public Message restore(int tracingHeader) {
        int idx = findIndex(tracingHeader, MessageHeader.TRACING_MASK);
        return idx == -1? null : new Message(headers[idx] | MessageHeader.RESEND_BIT, data[idx]);
    }

    private int findIndex(int header, int matchingMask) {
        for (int i = 0; i < headers.length; i++)
            if ((headers[i] & matchingMask) == header)
                return i;
        return -1;
    }

}
