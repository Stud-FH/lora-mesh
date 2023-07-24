package production;

import model.CorrespondenceClient;
import model.message.Message;
import model.message.MessageHeader;
import model.message.MessageType;

public class HttpCorrespondenceClient implements CorrespondenceClient {

    private final byte nodeId;
    private final HttpRequestClient httpRequestClient;
    boolean valid = true;

    public HttpCorrespondenceClient(byte nodeId, HttpRequestClient httpRequestClient) {
        this.nodeId = nodeId;
        this.httpRequestClient = httpRequestClient;
    }

    @Override
    public Message pack(MessageType type, byte... data) {
        if (!valid) throw new IllegalStateException("HttpCorrespondenceClient can only be used once");
        valid = false;
        var response = httpRequestClient.getResponseString(String.format("/correspondence/out/%d", nodeId));
        int counter = Integer.parseInt(response);
        int header = type.getHeaderBinary()
                | (nodeId << MessageHeader.ADDRESS_SHIFT)
                | MessageHeader.DOWNWARDS_BIT
                | (counter << MessageHeader.COUNTER_SHIFT);
        return new Message(header, data);
    }

    @Override
    public Message packAndIncrement(MessageType type, byte... data) {
        if (!valid) throw new IllegalStateException("HttpCorrespondenceClient can only be used once");
        valid = false;
        var response = httpRequestClient.postResponseString(String.format("/correspondence/out/%d", nodeId), "");
        int counter = Integer.parseInt(response);
        int header = type.getHeaderBinary()
                | (nodeId << MessageHeader.ADDRESS_SHIFT)
                | MessageHeader.DOWNWARDS_BIT
                | (counter << MessageHeader.COUNTER_SHIFT);
        return new Message(header, data);
    }

    @Override
    public byte[] registerAndListLosses(Message message) {
        if (!valid) throw new IllegalStateException("HttpCorrespondenceClient can only be used once");
        valid = false;

        var response = httpRequestClient.postResponseString("/correspondence/in", message.header + "");
        // TODO
        System.out.println(response);

        return new byte[]{};
    }
}
