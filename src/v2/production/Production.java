package v2.production;

import v2.core.context.Context;
import v2.core.domain.node.Node;
import v2.core.log.LogMultiplexer;
import v2.production.datasource.RpiTemperatureSensor;
import v2.production.impl.E32LoRaMeshModule;
import v2.production.impl.LinuxAdapter;
import v2.production.maintenance.HttpSynchronizer;
import v2.production.util.ConfigReader;
import v2.shared.api.Http;
import v2.shared.api.HttpDataSinkModuleModule;
import v2.shared.api.HttpLogger;
import v2.shared.api.HttpPceModule;
import v2.shared.impl.ConsoleLogger;
import v2.shared.impl.FileLogger;
import v2.shared.impl.SimpleExecutor;
import v2.shared.integration.CommandLine;
import v2.shared.integration.FileClient;
import v2.shared.testing.GuardedDataSinkModule;
import v2.shared.testing.GuardedPceModule;

import java.nio.file.Path;

public class Production {

    public static void main(String... args) {

        System.out.println("initializing LoRa Mesh Node v3...");
        var root = Path.of("/home/pi/lora-mesh");
        var config = new ConfigReader(root);

        String apiUrl = config.var("api", "localhost:8080");
        boolean pceDisabled = config.var("pce").equals("disabled");
        boolean dataSinkDisabled = config.var("data").equals("disabled");

        var ctx = new Context.Builder()
                .register(new LinuxAdapter())
                .register(new Node())
                .register(new LogMultiplexer(new ConsoleLogger(), new FileLogger(), new HttpLogger()))
                .register(new CommandLine())
                .register(new FileClient())
                .register(new Http())
                .register(new GuardedDataSinkModule(new HttpDataSinkModuleModule()))
                .register(new GuardedPceModule(new HttpPceModule()))
                .register(new E32LoRaMeshModule())
                .register(new HttpSynchronizer())
                .register(new RpiTemperatureSensor())
                .register(new SimpleExecutor())
                .build()
                .deploy();

        Runtime.getRuntime().addShutdownHook(new Thread(ctx::destroy));
    }
}