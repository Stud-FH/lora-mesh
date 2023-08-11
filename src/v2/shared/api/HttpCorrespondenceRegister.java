package v2.shared.api;

import v2.core.context.Module;
import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageHeader;
import v2.core.domain.message.MessageType;
import v2.core.log.Logger;
import v2.shared.util.JsonUtil;

import java.util.Collection;

public class HttpCorrespondenceRegister implements CorrespondenceRegister {

    private final byte nodeId;
    private final int address;
    private final Http http;

    public HttpCorrespondenceRegister(byte nodeId, Http http) {
        this.nodeId = nodeId;
        this.address = nodeId << MessageHeader.ADDRESS_SHIFT;
        this.http = http;
    }

    public int address() {
        return address;
    }

    @Override
    public Message pack(MessageType type, byte... data) {
        var response = http.getResponseString(String.format("/correspondence/out/%d", nodeId));
        int counter = Integer.parseInt(response);
        int header = type.getHeaderBinary()
                | address
                | MessageHeader.DOWNWARDS_BIT
                | (counter << MessageHeader.COUNTER_SHIFT);
        return new Message(header, data);
    }

    @Override
    public Message packAndIncrement(MessageType type, byte... data) {
        var response = http.postResponseString(String.format("/correspondence/out/%d", nodeId), "");
        int counter = Integer.parseInt(response);
        int header = type.getHeaderBinary()
                | address
                | MessageHeader.DOWNWARDS_BIT
                | (counter << MessageHeader.COUNTER_SHIFT);
        return new Message(header, data);
    }

    @Override
    public Collection<Integer> registerAndListLosses(Message message) {
        var response = http.postResponseString("/correspondence/in", message.header + "");
        return JsonUtil.parseIntList(response);
    }
}
