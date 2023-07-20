package production;

import model.Logger;
import model.NodeStatus;
import model.message.NodeInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;

public class HttpLogger implements Logger {

    private final String baseUrl;
    HttpClient http = Production.http;

    public HttpLogger(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void log(Severity severity, String text, NodeInfo nodeInfo) {
        try {
            var request = HttpRequest.newBuilder(new URI(baseUrl + "/log"))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"severity\": \"" + severity
                            + "\", \"text\": \"" + text
                            + "\", \"nodeSerialId\": \"" + nodeInfo.serialId
                            + "\", \"nodeId\": \"" + nodeInfo.nodeId
                            + "\", \"nodeStatus\": \"" + nodeInfo.status
                            + "\"}"))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
