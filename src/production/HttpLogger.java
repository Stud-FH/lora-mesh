package production;

import model.LogEntry;
import model.Logger;
import model.message.NodeInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpLogger implements Logger {

    private final String baseUrl;
    HttpClient http = Production.http;

    public HttpLogger(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void log(Severity severity, String text, NodeInfo nodeInfo) {
        var data = new LogEntry();
        data.severity = severity;
        data.nodeInfo = nodeInfo;
        data.data = text.getBytes();
        try {
            var request = HttpRequest.newBuilder(new URI(baseUrl + "/log"))
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.logEntry(data)))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
