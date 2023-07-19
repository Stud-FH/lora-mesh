package model.message;

import model.ChannelInfo;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {

    public static byte[] serialIdToJoinData(long serialId) {
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
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.putChar(info.code().charAt(0));
        buffer.putChar(info.code().charAt(1));
        buffer.putChar(info.code().charAt(2));
        buffer.putChar(info.code().charAt(3));
        buffer.putChar(info.code().charAt(4));
        buffer.putChar(info.code().charAt(5));
        return buffer.array();
    }

    public static ChannelInfo rendezvousDataToChannelInfo(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return new ChannelInfo(buffer.toString());
    }

    public record InviteResult(byte assignedId, long serialId) {}
}
