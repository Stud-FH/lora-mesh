package production;

import model.CorrespondenceClient;
import model.message.Message;
import model.message.MessageHeader;
import model.message.MessageType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpCorrespondenceClient implements CorrespondenceClient {

    private final String baseUrl;
    private final byte nodeId;
    HttpClient http = Production.http;
    boolean valid = true;

    public HttpCorrespondenceClient(String baseUrl, byte nodeId) {
        this.baseUrl = baseUrl;
        this.nodeId = nodeId;
    }

    @Override
    public Message pack(MessageType type, byte... data) {
        if (!valid) throw new IllegalStateException("HttpCorrespondenceClient can only be used once");
        valid = false;
        int counter = getCounter(false);
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
        int counter = getCounter(true);
        int header = type.getHeaderBinary()
                | (nodeId << MessageHeader.ADDRESS_SHIFT)
                | MessageHeader.DOWNWARDS_BIT
                | (counter << MessageHeader.COUNTER_SHIFT);
        return new Message(header, data);
    }

    private int getCounter(boolean increment) {
        try {
            var request = HttpRequest.newBuilder(new URI(String.format
                            ("%s/correspondence/out?nodeId=%d?increment=%s", baseUrl, nodeId, increment)))
                    .GET()
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return Integer.parseInt(response.body());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] registerAndListLosses(Message message) {
        if (!valid) throw new IllegalStateException("HttpCorrespondenceClient can only be used once");
        valid = false;

        try {
            var request = HttpRequest.newBuilder(new URI(String.format
                            ("%s/correspondence/in?nodeId=%d?counter=%d", baseUrl, nodeId, message.getCounter())))
                    .GET()
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            System.out.println(response.body());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return new byte[0];
    }
}
