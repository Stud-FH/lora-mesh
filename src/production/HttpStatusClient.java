package production;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpStatusClient {

    private enum ActionCommand {
        Reboot,
        GitPull
    }

    private final String baseUrl;
    private final long serialId;
    HttpClient http = Production.http;

    public HttpStatusClient(String baseUrl, long serialId) {
        this.baseUrl = baseUrl;
        this.serialId = serialId;
    }

    public void status() {
        try {
            var proc = new ProcessBuilder().command("ip", "a");
            var stream = proc.start().getInputStream();

            var request = HttpRequest.newBuilder(new URI(String.format
                            ("%s/status?serialId=%d", baseUrl, serialId)))
                    .POST(HttpRequest.BodyPublishers.ofString(new String(stream.readAllBytes())))
                    .setHeader("Content-Type", "application/json")
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new Exception(response.toString());
            // TODO execute command based on status answer
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
