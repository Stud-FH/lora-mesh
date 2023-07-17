package production;

import model.DataSinkClient;
import model.message.Message;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpDataClient implements DataSinkClient {

    private final String baseUrl;
    HttpClient http = Production.http;

    public HttpDataClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean heartbeat() {
        try {
            var request = HttpRequest.newBuilder(new URI(baseUrl + "/data"))
                    .GET()
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return "true".equals(response.body());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void feed(Message message) {
        try {
            var request = HttpRequest.newBuilder(new URI(baseUrl + "/data"))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"header\": " + message.header()
                                    + ", \"data\": \"" + message.dataAsString()
                                    + "\"}"))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // test
    public static void main(String... args) {
        var client = new HttpDataClient("http://localhost:8080");
        var result = client.heartbeat();
        System.out.println(result);
        client.feed(new Message(1234, "test".getBytes()));

    }
}
