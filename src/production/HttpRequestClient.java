package production;

import model.Logger;
import model.NodeInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

public class HttpRequestClient {

    private static final HttpClient http = HttpClient.newHttpClient();

    private final URI baseUri;
    private final Logger logger;
    private final Supplier<NodeInfo> nodeInfo;

    public HttpRequestClient(URI baseUri, Logger logger, Supplier<NodeInfo> nodeInfo) {
        this.baseUri = baseUri;
        this.logger = logger;
        this.nodeInfo = nodeInfo;
    }

    public byte[] getResponseBinary(String path) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.error("error sending request: GET " + uri, nodeInfo.get());
            throw new RuntimeException("request failed");
        }
    }

    public String getResponseString(String path) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.error("error sending request: GET " + uri, nodeInfo.get());
            throw new RuntimeException("request failed");
        }
    }

    public String getResponseStringDebugOnly(String path) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.debug("error sending request: GET " + uri, nodeInfo.get());
            throw new RuntimeException("request failed");
        }
    }

    public void postResponseVoid(String path, String data) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            logger.error("error sending request: POST " + uri, nodeInfo.get());
            throw new RuntimeException("request failed");
        }
    }

    public void postResponseVoid(String path, byte[] data) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .setHeader("Content-Type", "application/octet-stream")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            logger.error("error sending request: POST " + uri, nodeInfo.get());
            throw new RuntimeException("request failed");
        }
    }

    public byte[] postResponseBinary(String path, byte[] data) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .setHeader("Content-Type", "application/octet-stream")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.error("error sending request: POST " + uri, nodeInfo.get());
            throw new RuntimeException("request failed");
        }
    }

    public String postResponseString(String path, String data) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.error("error sending request: POST " + uri, nodeInfo.get());
            throw new RuntimeException("request failed");
        }
    }

    public String postResponseStringDebugOnly(String path, String data) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.debug("error sending request: POST " + uri, nodeInfo.get());
            throw new RuntimeException("request failed");
        }
    }
}
