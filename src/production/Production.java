package production;

import local.*;
import model.ApplicationContext;
import model.Logger;
import local.Node;
import model.NodeInfo;
import testing.PseudoDataSinkClient;
import testing.PseudoPceClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.function.Supplier;

public class Production {

    public static final HttpClient http = HttpClient.newHttpClient();

    private static Node node;
    private static Logger consoleLogger;
    private static Logger fileLogger;

    public static void main(String... args) throws URISyntaxException {

        System.out.println("initializing LoRa Mesh Node...");

        if (Config.missing("api")) throw new IllegalStateException("api must be specified in config.txt");
        String apiUrl = Config.var("api");
        boolean pceDisabled = Config.var("pce").equals("disabled");
        boolean dataSinkDisabled = Config.var("data").equals("disabled");
        Logger.Severity logLevel = Logger.Severity.valueOf(Config.var("log", Logger.Severity.Debug.name()));

        ApplicationContext ctx = new ApplicationContext();

        Supplier<NodeInfo> nodeInfo = () -> node.info();
        consoleLogger = new ConsoleLogger(() -> logLevel, () -> "prd");
        var fileClient = new FileClient(consoleLogger, nodeInfo);
        fileLogger = new FileLogger(fileClient, () -> consoleLogger);
        var httpRequestClient = new HttpRequestClient(new URI(apiUrl), fileLogger, () -> node.info());
        Logger httpLogger = new HttpLogger(httpRequestClient, () -> fileLogger);
        var bashClient = new BashClient(httpLogger, nodeInfo);
        var dataClient = dataSinkDisabled? new PseudoDataSinkClient() : new HttpDataClient(httpRequestClient);
        var pceClient =  pceDisabled ? new PseudoPceClient() : new HttpPceClient(httpRequestClient);
        var meshClient = new E32LoRaMeshClient(fileClient, bashClient, httpLogger);
        var statusClient = new HttpStatusClient(httpRequestClient, bashClient, fileClient);

        node = new Node(Config.serialId, meshClient, dataClient, pceClient, httpLogger);

        Exec.run(node, 50);

        Exec.repeat(statusClient::update, 300000);
        Exec.repeat(statusClient::status, 300000, 100);

        Exec.repeat(() -> node.feedData(bashClient.run("vcgencmd", "measure_temp")), 10000, 3000);
    }

    static {

    }
}