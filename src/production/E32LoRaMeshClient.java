package production;

import local.BashClient;
import local.Exec;
import local.FileClient;
import model.ChannelInfo;
import model.LoRaMeshClient;
import model.Logger;
import model.Module;
import model.Observer;
import model.message.Message;
import model.NodeInfo;

import java.io.File;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class E32LoRaMeshClient implements LoRaMeshClient {

    public static final Path outFilePath = Config.fsRoot.resolve("out.txt");

    public static final String startingSymbol = "x";
    public static final String terminalSymbol = ";";

    private final Queue<SendingItem> queue = new LinkedList<>();
    private final FileClient fileClient;
    private final BashClient bashClient;
    private final Logger logger;

    private ChannelInfo listeningChannel = null;
    private Observer<Message> listeningObserver = null;
    private Supplier<NodeInfo> listeningNodeInfoSupplier = null;
    private Process listeningProcess = null;
    private Scanner listeningScanner = null;

    public E32LoRaMeshClient(FileClient fileClient, BashClient bashClient, Logger logger) {
        this.fileClient = fileClient;
        this.bashClient = bashClient;
        this.logger = logger;
        Exec.repeat(this::tick, getSendingIntervalMillis());
    }

    private void tick() {

        while (listeningScanner != null && listeningScanner.hasNext("\\w*" + startingSymbol + "\\w*" + terminalSymbol)) {
            var in = listeningScanner.next("\\w*" + startingSymbol + "\\w*" + terminalSymbol);
            var startIndex = in.indexOf(startingSymbol) + startingSymbol.length();
            var endIndex =  in.indexOf(terminalSymbol);
            if (startIndex > 0) {
                logger.debug("received (garbage/discarding): " + in.substring(0, startIndex), listeningNodeInfoSupplier.get());
            }
            var raw = in.substring(startIndex, endIndex);
            if (listeningObserver != null && !listeningObserver.isExpired()) {
                logger.debug("received: " + in, listeningNodeInfoSupplier.get());
                listeningObserver.next(rawToMessage(raw));
            } else {
                logger.debug("received (discarding): " + in, listeningNodeInfoSupplier.get());
            }
        }

        if (!queue.isEmpty()) {
            var item = queue.poll();
            var raw = messageToRaw(item.message);
            logger.debug("emitting: " + raw, listeningNodeInfoSupplier.get());
            stopListening();
            try {
                fileClient.write(outFilePath, startingSymbol + raw + terminalSymbol);
                bashClient.run("bash", "-l", "-c", "e32", "-w", item.channel.code, "--in-file", outFilePath.toString());
            } catch (Exception e) {
                logger.error("exception thrown while trying to send message: " + e, item.nodeInfo);
            }
            startListening();
        }
    }

    @Override
    public void listen(ChannelInfo channel, Observer<Message> observer, Supplier<NodeInfo> nodeInfoSupplier) {
        logger.debug("now listening on channel " + channel.code, listeningNodeInfoSupplier.get());
        if (listeningObserver != null) listeningObserver.dispose();
        stopListening();
        listeningChannel = channel;
        listeningObserver = observer;
        listeningNodeInfoSupplier = nodeInfoSupplier;
        startListening();
    }

    private void stopListening() {
        if (listeningProcess == null || listeningScanner == null) return;
        logger.debug("listening pause...", listeningNodeInfoSupplier.get());
        listeningProcess.destroy();
        listeningProcess = null;
        listeningScanner.close();
        listeningScanner = null;
    }

    private void startListening() {
        if (listeningChannel == null || listeningObserver == null || listeningNodeInfoSupplier == null) return;
        logger.debug("listening continue...", listeningNodeInfoSupplier.get());
        try {
            listeningProcess = new ProcessBuilder()
                    .command("bash", "-l", "-c", "e32", "-w", listeningChannel.code)
                    .start();
        } catch (Exception e) {
            logger.error("exception thrown while starting to listen: " + e, listeningNodeInfoSupplier.get());
        }
        listeningScanner = new Scanner(listeningProcess.getInputStream());
    }

    @Override
    public void enqueue(ChannelInfo channel, Message message, NodeInfo nodeInfo) {
        logger.debug(String.format("enqueued @%s: %s", channel.code, message), nodeInfo);
        queue.add(new SendingItem(channel, message, nodeInfo));
    }

    @Override
    public long getSendingIntervalMillis() {
        return 1000; // todo
    }


    private String messageToRaw(Message message) {
        var sb = new StringBuilder();
        sb.append(String.format("%04X", message.header()));
        for (byte b : message.data) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    private Message rawToMessage(String raw) {
        int header = Integer.parseInt(raw.substring(0, 4), 16);
        raw = raw.substring(4);
        ByteBuffer buffer = ByteBuffer.allocate(raw.length() / 2);
        while (raw.length() >= 2) {
            buffer.put(Byte.parseByte(raw.substring(0, 2), 16));
            raw = raw.substring(2);
        }
        return new Message(header, buffer.array());
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(FileClient.class, BashClient.class, Logger.class);
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
