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

        File f3 = new File("./config/options.txt");
        Scanner s3 = new Scanner(f3);
        while (s3.hasNextLine()) {
            switch (s3.nextLine()) {
                case "disable pce": pceClient.disabled = true;
                break;
                case "disable data": dataClient.disabled = true;
                break;
            }
        }
        s3.close();

        var node = new Node(serialId, meshClient, dataClient, pceClient, logger);

        Exec.run(node, 50);

        var statusClient = new HttpStatusClient(apiUrl, serialId);
        Exec.repeat(statusClient::status, 3600000);

        Exec.repeat(() -> node.feedData(temperature()), 10000, 3000);
    }

    static byte[] temperature() {
        try {
            var proc = new ProcessBuilder().command("vcgencmd", "measure_temp");
            var stream = proc.start().getInputStream();
            return new String(stream.readAllBytes()).getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return "error".getBytes();
        }
    }
}
