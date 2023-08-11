package v2.core.domain.message;

import java.util.Arrays;

public class Message implements MessageHeader {

    public final int header;
    public final byte[] data;

    public Message(int header, byte... data) {
        this.header = header;
        this.data = data;
    }

    public byte[] data() {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    public byte data(int idx) {
        return data[idx];
    }

    public boolean hasData() {
        return data.length > 0;
    }

    public int dataLength() {
        return data.length;
    }

    public int header() {
        return header;
    }

    @Override
    public int getHeaderBinary() {
        return header;
    }

    @Override
    public String toString() {
        String direction = (header & DOWNWARDS_BIT) != 0 ? "to" : "from";
        String resend = (header & RESEND_BIT) != 0 ? " (resend)" : "";
        return String.format("%s#%d %s %d%s: %s", MessageType.pure(this), getCounter(), direction, getAddress(), resend, Arrays.toString(data));
    }
}
