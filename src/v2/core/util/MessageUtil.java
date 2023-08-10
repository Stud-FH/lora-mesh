package v2.core.util;

import v2.core.domain.ChannelInfo;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {

    public static byte[] sidToJoinData(long serialId) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + 1);
        buffer.put((byte) 0);
        buffer.putLong(serialId);
        return buffer.array();
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
