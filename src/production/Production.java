package production;

import model.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.http.HttpClient;
import java.util.Scanner;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Production {

    public static final HttpClient http = HttpClient.newHttpClient();

    public static void main(String... args) throws Exception {

        File f1 = new File("./config/serial-id.txt");
        Scanner s1 = new Scanner(f1);
        long serialId = s1.nextLong();
        s1.close();

        File f2 = new File("./config/api-url.txt");
        Scanner s2 = new Scanner(f2);
        String apiUrl = s2.nextLine();
        s2.close();

        var meshClient = new E32LoRaMeshClient();
        var dataClient = new HttpDataClient(apiUrl);
        var pceClient = new HttpPceClient(apiUrl);
        var logger = new HttpLogger(apiUrl);

        var node = new Node(serialId, meshClient, dataClient, pceClient, logger);

        ScheduledExecutorService exec = new ScheduledThreadPoolExecutor(2);
        exec.schedule(node, 10, TimeUnit.MILLISECONDS);
        var statusClient = new HttpStatusClient(apiUrl, serialId);
        exec.scheduleAtFixedRate(statusClient::status, 0, 1, TimeUnit.HOURS);
    }
}
