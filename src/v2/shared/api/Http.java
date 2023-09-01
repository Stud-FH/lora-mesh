package v2.shared.api;

import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.log.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Http implements Module {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private Config config;

    private Logger logger;

    @Override
    public void build(Context ctx) {
        config = ctx.resolve(Config.class);
        logger = ctx.resolve(Logger.class);
    }

    public byte[] getResponseBinary(String path) {
        var uri = config.api().resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofMillis(500))
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
        var uri = config.api().resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofMillis(500))
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
        var uri = config.api().resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofMillis(500))
                    .build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("request failed");
        }
    }

    public void postResponseVoid(String path, String data) {
        var uri = config.api().resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .setHeader("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(500))
                    .build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("request failed");
        }
    }

    public void postResponseVoidDisableLogging(String path, String data) {
        var uri = config.api().resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .setHeader("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(500))
                    .build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            throw new RuntimeException("request failed");
        }
    }

    public void postResponseVoid(String path, byte[] data) {
        var uri = config.api().resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .setHeader("Content-Type", "application/octet-stream")
                    .timeout(Duration.ofMillis(500))
                    .build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) throw new Exception(response.toString());
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("request failed");
        }
    }

    public byte[] postResponseBinary(String path, byte[] data) {
        var uri = config.api().resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .setHeader("Content-Type", "application/octet-stream")
                    .timeout(Duration.ofMillis(500))
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
        var uri = config.api().resolve(path);
        try {
            var request = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .setHeader("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(500))
                    .build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            return response.body();
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("request failed");
        }
    }

    public interface Config extends Module {
        URI api();
    }
}
