package production;

import local.*;
import model.ApplicationContext;
import model.Executor;
import model.LogMultiplexer;
import model.Logger;
import testing.PseudoDataSinkClient;
import testing.PseudoPceClient;

import java.net.URI;
import java.net.URISyntaxException;

public class Production {

    public static void main(String... args) throws URISyntaxException {

        System.out.println("initializing LoRa Mesh Node v3...");

        if (Config.missing("api")) throw new IllegalStateException("api must be specified in config.txt");
        String apiUrl = Config.var("api");
        boolean pceDisabled = Config.var("pce").equals("disabled");
        boolean dataSinkDisabled = Config.var("data").equals("disabled");
        Logger.Severity logLevel = Logger.Severity.valueOf(Config.var("log", Logger.Severity.Debug.name()));

        var ctx = new ApplicationContext.Builder()
                .register(new Node(Config.serialId))
                .register(new LogMultiplexer(new ConsoleLogger(() -> logLevel), new FileLogger(), new HttpLogger()))
                .register(new CommandLine())
                .register(new FileClient(Config.fsRoot))
                .register(new Http(new URI(apiUrl)))
                .register(dataSinkDisabled? new PseudoDataSinkClient() : new HttpDataClient())
                .register(pceDisabled ? new PseudoPceClient() : new HttpPceClient())
                .register(new E32LoRaMeshClient())
                .register(new HttpSynchronizer())
                .register(new RpiTemperatureSensor())
                .register(new Executor())
                .build()
                .deploy();

        Runtime.getRuntime().addShutdownHook(new Thread(ctx::destroy));
    }
}