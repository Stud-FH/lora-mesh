package production;

import local.BashClient;
import local.FileClient;

public class HttpStatusClient {

    private final HttpRequestClient httpRequestClient;
    private final BashClient bashClient;
    private final FileClient fileClient;

    public HttpStatusClient(HttpRequestClient httpRequestClient, BashClient bashClient, FileClient fileClient) {
        this.httpRequestClient = httpRequestClient;
        this.bashClient = bashClient;
        this.fileClient = fileClient;
    }

    public void status() {
        var data = bashClient.run("ip", "a");
        var response = httpRequestClient.postResponseBinary(String.format("/status/%d", Config.serialId), data);
        fileClient.write("config.txt", response);
    }

    public void update() {
        long lastModified = fileClient.lastModified("node.jar");
        byte[] binary = httpRequestClient.getResponseBinary(String.format("/status?lm=%d", lastModified));
        if (binary.length > 0) {
            fileClient.write("node.jar", binary);
        }
    }
}
