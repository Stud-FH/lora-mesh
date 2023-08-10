package production;

import local.*;
import model.*;
import testing.GuardedDataSinkClient;
import testing.GuardedPceClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class Production {

    public static void main(String... args) throws URISyntaxException {

        System.out.println("initializing LoRa Mesh Node v3...");
        var root = Path.of("/home/pi/lora-mesh");
        var config = new ConfigReader(root);

        String apiUrl = config.var("api", "localhost:8080");
        boolean pceDisabled = config.var("pce").equals("disabled");
        boolean dataSinkDisabled = config.var("data").equals("disabled");
        Logger.Severity logLevel = Logger.Severity.valueOf(config.var("log", Logger.Severity.Debug.name()));

        var ctx = new Context.Builder()
                .register(new LinuxAdapter())
                .register(new Node(config.serialId()))
                .register(new LogMultiplexer(new ConsoleLogger(() -> logLevel), new FileLogger(), new HttpLogger()))
                .register(new CommandLine())
                .register(new FileClient(root))
                .register(new Http(new URI(apiUrl)))
                .register(new GuardedDataSinkClient(new HttpDataClient(), () -> !dataSinkDisabled))
                .register(new GuardedPceClient(new HttpPceClient(), () -> !pceDisabled))
                .register(new E32LoRaMeshClient())
                .register(new HttpSynchronizer(config.serialId()))
                .register(new RpiTemperatureSensor())
                .register(new SimpleExecutor())
                .build()
                .deploy();

        Runtime.getRuntime().addShutdownHook(new Thread(ctx::destroy));
    }
}