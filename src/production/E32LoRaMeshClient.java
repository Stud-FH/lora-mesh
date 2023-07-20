package production;

import model.LoRaMeshClient;
import model.ChannelInfo;
import model.Logger;
import model.Observer;
import model.execution.Exec;
import model.message.Message;
import model.message.NodeInfo;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.function.Supplier;

public class E32LoRaMeshClient implements LoRaMeshClient {

    public static final String startingSymbol = "<<";
    public static final String terminalSymbol = ">>";

    private final Queue<SendingItem> queue = new LinkedList<>();
    private final Logger logger;

    private ChannelInfo listeningChannel = null;
    private Observer<Message> listeningObserver = null;
    private Supplier<NodeInfo> listeningNodeInfoSupplier = null;
    private Process listeningProcess = null;
    private Scanner listeningScanner = null;
    private final File sendingFile = new File("sending.out");

    public E32LoRaMeshClient(Logger logger) {
        this.logger = logger;
        Exec.repeat(this::tick, getSendingIntervalMillis());
    }

    private void tick() {

        while (listeningScanner != null && listeningScanner.hasNext("\\w*" + startingSymbol + "\\w*" + terminalSymbol)) {
            var in = listeningScanner.next("\\w*" + startingSymbol + "\\w*" + terminalSymbol);
            var startIndex = in.indexOf(startingSymbol) + startingSymbol.length();
            var endIndex =  in.indexOf(terminalSymbol);
            var raw = in.substring(startIndex, endIndex);
            if (listeningObserver != null && !listeningObserver.isExpired()) {
                listeningObserver.next(rawToMessage(raw));
            }
        }

        if (!queue.isEmpty()) {
            var item = queue.poll();
            stopListening();
            try {
                var writer = new FileWriter(sendingFile);
                writer.write(startingSymbol + messageToRaw(item.message) + terminalSymbol);
                writer.close();
                var proc = new ProcessBuilder()
                        .command(String.format("e32 -w %s --in-file sending.out", item.channel));
                proc.start().destroy();
            } catch (Exception e) {
                logger.error("exception thrown while trying to send message: " + e, item.nodeInfo);
            }
            startListening();
        }
    }

    @Override
    public void listen(ChannelInfo channel, Observer<Message> observer, Supplier<NodeInfo> nodeInfoSupplier) {
        if (listeningObserver != null) listeningObserver.dispose();
        stopListening();
        listeningChannel = channel;
        listeningObserver = observer;
        listeningNodeInfoSupplier = nodeInfoSupplier;
        startListening();
    }

    private void stopListening() {
        if (listeningProcess == null || listeningScanner == null ) return;
        listeningProcess.destroy();
        listeningProcess = null;
        listeningScanner.close();
        listeningScanner = null;
    }

    private void startListening() {
        if (listeningChannel == null || listeningObserver == null || listeningNodeInfoSupplier == null) return;
        try {
            listeningProcess = new ProcessBuilder()
                    .command(String.format("e32 -w %s --in-file sending.out", listeningChannel))
                    .start();
        } catch (Exception e) {
            logger.error("exception thrown while starting to listen: " + e, listeningNodeInfoSupplier.get());
        }
        listeningScanner = new Scanner(listeningProcess.getInputStream());
    }

    @Override
    public void enqueue(ChannelInfo channel, Message message, NodeInfo nodeInfo) {
        queue.add(new SendingItem(channel, message, nodeInfo));
    }

    @Override
    public long getSendingIntervalMillis() {
        return 1000; // todo
    }


    private String messageToRaw(Message message) {
        return String.format("%04X%s", message.header(), message.dataAsString());
    }

    private Message rawToMessage(String raw) {
        return new Message(Integer.parseInt(raw.substring(0, 4), 16), raw.substring(4).getBytes());
    }

    private static class SendingItem {
        public final ChannelInfo channel;
        public final Message message;
        public final NodeInfo nodeInfo;

        public SendingItem(ChannelInfo channel, Message message, NodeInfo nodeInfo) {
            this.channel = channel;
            this.message = message;
            this.nodeInfo = nodeInfo;
        }
    }
}
