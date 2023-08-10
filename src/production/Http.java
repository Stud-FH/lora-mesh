package production;

import model.Context;
import model.Logger;
import model.Module;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Set;

public class Http implements Module {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final URI baseUri;

    private Logger logger;

    public Http(URI baseUri) {
        this.baseUri = baseUri;
    }

    @Override
    public void build(Context ctx) {
        this.logger = ctx.resolve(Logger.class);
    }

    public byte[] getResponseBinary(String path) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("request failed");
        }
    }

    public String getResponseString(String path) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("request failed");
        }
    }

    public String getResponseStringDisableLogging(String path) {
        var uri = baseUri.resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
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
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            logger.exception(e, this);
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
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            logger.exception(e, this);
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
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.exception(e, this);
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
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("request failed");
        }
    }

    @Override
    public String info() {
        return String.format("Http @ %s", baseUri);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Logger.class);
    }

}
