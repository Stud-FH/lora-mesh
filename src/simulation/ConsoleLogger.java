package simulation;

import model.Logger;
import model.message.NodeInfo;

public class ConsoleLogger implements Logger {

    private final SimulatedLoRaMeshClient simT;

    public ConsoleLogger(SimulatedLoRaMeshClient simT) {
        this.simT = simT;
    }

    @Override
    public void log(Logger.Severity severity, String text, NodeInfo nodeInfo) {
        if (severity.ordinal() < simT.logLevel.ordinal()) return;
//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        System.out.printf("%s%3s: %s >> %s", setColor(severity), simT.name, nodeInfo, text);
        System.out.println("\u001B[0m");
    }

    private String setColor(Logger.Severity severity) {
        return switch (severity) {
            case Debug -> "\u001B[61m";
            case Info -> "\u001B[36m";
            case Warn -> "\u001B[33m";
            case Error -> "\u001B[31m";
        };
    }

}
