package production;

import model.Node;
import model.execution.Exec;

import java.io.File;
import java.net.http.HttpClient;
import java.util.Scanner;

public class Production {

    public static final HttpClient http = HttpClient.newHttpClient();

    public static void main(String... args) throws Exception {

        File projectRoot = new File(".");
        System.out.println("project root: " + projectRoot.getAbsolutePath());


        File f1 = new File("./config/serial-id.txt");
        Scanner s1 = new Scanner(f1);
        long serialId = s1.nextLong();
        s1.close();

        File f2 = new File("./config/api-url.txt");
        Scanner s2 = new Scanner(f2);
        String apiUrl = s2.nextLine();
        s2.close();

        var dataClient = new HttpDataClient(apiUrl);
        var pceClient = new HttpPceClient(apiUrl);
        var logger = new HttpLogger(apiUrl);
        var meshClient = new E32LoRaMeshClient(logger);

        var node = new Node(serialId, meshClient, dataClient, pceClient, logger);

        Exec.run(node, 50);

        var statusClient = new HttpStatusClient(apiUrl, serialId);
        Exec.repeat(statusClient::status, 3600000);
    }
}
