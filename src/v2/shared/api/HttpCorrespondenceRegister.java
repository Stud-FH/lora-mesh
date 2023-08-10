package v2.shared.api;

import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageHeader;
import v2.core.domain.message.MessageType;

public class HttpCorrespondenceRegister implements CorrespondenceRegister {

    private final byte nodeId;
    private final Http http;
    boolean valid = true;

    public HttpCorrespondenceRegister(byte nodeId, Http http) {
        this.nodeId = nodeId;
        this.http = http;
    }

    @Override
    public Message pack(MessageType type, byte... data) {
        if (!valid) throw new IllegalStateException("HttpCorrespondenceRegister can only be used once");
        valid = false;
        var response = http.getResponseString(String.format("/correspondence/out/%d", nodeId));
        int counter = Integer.parseInt(response);
        int header = type.getHeaderBinary()
                | (nodeId << MessageHeader.ADDRESS_SHIFT)
                | MessageHeader.DOWNWARDS_BIT
                | (counter << MessageHeader.COUNTER_SHIFT);
        return new Message(header, data);
    }

    @Override
    public Message packAndIncrement(MessageType type, byte... data) {
        if (!valid) throw new IllegalStateException("HttpCorrespondenceRegister can only be used once");
        valid = false;
        var response = http.postResponseString(String.format("/correspondence/out/%d", nodeId), "");
        int counter = Integer.parseInt(response);
        int header = type.getHeaderBinary()
                | (nodeId << MessageHeader.ADDRESS_SHIFT)
                | MessageHeader.DOWNWARDS_BIT
                | (counter << MessageHeader.COUNTER_SHIFT);
        return new Message(header, data);
    }

    @Override
    public byte[] registerAndListLosses(Message message) {
        if (!valid) throw new IllegalStateException("HttpCorrespondenceRegister can only be used once");
        valid = false;

        var response = http.postResponseString("/correspondence/in", message.header + "");
        // TODO
        System.out.println(response);

        return new byte[]{};
    }
}
