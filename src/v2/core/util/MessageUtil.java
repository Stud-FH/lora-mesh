package v2.core.util;

import v2.core.domain.ChannelInfo;
import v2.core.domain.message.MessageHeader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageUtil {

    public static byte[] sidToJoinData(long serialId) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + 1);
        buffer.put((byte) 0);
        buffer.putLong(serialId);
        return buffer.array();
    }

    public static int tracingHeader(int address, int counter) {
        int tracingHeader = 0;
        tracingHeader |= address << MessageHeader.ADDRESS_SHIFT;
        tracingHeader |= counter << MessageHeader.COUNTER_SHIFT;
        return tracingHeader;
    }

    public static Collection<Integer> countersToTracingHeaders(int address, Collection<Integer> counters) {
        return counters.stream().map(counter -> tracingHeader(address, counter)).collect(Collectors.toList());
    }

    public static Collection<Integer> helloDataToTracingHeaders(byte[] data) {
        Collection<Integer> result = new ArrayList<>();
        for (int i = 0; i + 1 < data.length; i += 2) {
            result.add(tracingHeader(data[i], data[i+1]));
        }
        return result;
    }

    public static InviteResult inviteDataToInviteResult(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return new InviteResult(buffer.get(), buffer.getLong());
    }

    public static Map<Byte, Byte> retxDataToRetxMap(byte[] data) {
        Map<Byte, Byte> map = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            byte key = buffer.get();
            if (buffer.hasRemaining()) {
                map.put(key, buffer.get());
            }
        }
        return map;
    }

    public static byte[] channelInfoToRendezvousData(ChannelInfo info) {
        return info.code.getBytes();
    }

    public static ChannelInfo rendezvousDataToChannelInfo(byte[] data) {
        return new ChannelInfo(new String(data));
    }

    public static class InviteResult {
        public final byte assignedId;
        public final long serialId;

        public InviteResult(byte assignedId, long serialId) {
            this.assignedId = assignedId;
            this.serialId = serialId;
        }
    }
}
