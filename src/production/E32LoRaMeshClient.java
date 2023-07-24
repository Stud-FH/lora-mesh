package production;

import local.CommandLine;
import local.FileClient;
import model.Module;
import model.Observer;
import model.*;
import model.Executor;
import model.message.Message;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;

public class E32LoRaMeshClient implements LoRaMeshClient {
    public static final Path outFilePath = Config.fsRoot.resolve("out.txt");

    public static final long COOLDOWN = 1000;

    public static final String startingSymbol = "x";
    public static final String terminalSymbol = ";";
    public static final String messagePattern = "\\w*" + startingSymbol + "\\w*" + terminalSymbol;

    private final Queue<SendingItem> queue = new LinkedList<>();
    private FileClient fs;
    private CommandLine cmd;
    private Logger logger;
    private Executor exec;

    private ChannelInfo listeningChannel = null;
    private Observer<Message> listeningObserver = null;
    private Process listeningProcess = null;
    private long lastSendingTime;

    @Override
    public void deploy() {
        lastSendingTime = System.currentTimeMillis();
        exec.scheduleDynamic("lora cycle", this::cycle, () -> lastSendingTime + COOLDOWN, () -> false);
    }

    @Override
    public void useContext(ApplicationContext ctx) {
        logger = ctx.resolve(Logger.class);
        cmd = ctx.resolve(CommandLine.class);
        fs = ctx.resolve(FileClient.class);
        exec = ctx.resolve(Executor.class);
    }

    private void cycle() {

        if (listeningProcess != null && listeningObserver != null && !listeningObserver.isExpired()) {
            String readable = "";
            try {
                readable = new String(listeningProcess.getInputStream().readAllBytes());
            } catch (Exception e) {
                logger.warn("could not read from input stream", this);
            }
            Scanner sc = new Scanner(readable);
            while (sc.hasNext(messagePattern)) {
                String in = sc.next(messagePattern);
                var startIndex = in.indexOf(startingSymbol) + startingSymbol.length();
                var endIndex =  in.indexOf(terminalSymbol);
                if (startIndex > 0) {
                    logger.debug("received (prefix/discarding): " + in.substring(0, startIndex), this);
                }
                var raw = in.substring(startIndex, endIndex);
                logger.debug("received: " + raw, this);
                listeningObserver.next(rawToMessage(raw));
            }
        }

        if (!queue.isEmpty()) {
            var item = queue.poll();
            var raw = messageToRaw(item.message);
            logger.debug("emitting: " + raw, this);
            stopListening();
            try {
                fs.write(outFilePath, startingSymbol + raw + terminalSymbol);
                cmd.run("bash", "-l", "-c", "e32", "-w", item.channel.code, "--in-file", outFilePath.toString());
            } catch (Exception e) {
                logger.error("exception thrown while trying to send message: " + e, this);
            }
            startListening();
            lastSendingTime = System.currentTimeMillis();
        }
    }

    @Override
    public void listen(ChannelInfo channel, Observer<Message> observer) {
        logger.debug("now listening on channel " + channel.code, this);
        if (listeningObserver != null) listeningObserver.dispose();
        stopListening();
        listeningChannel = channel;
        listeningObserver = observer;
        startListening();
    }

    private void stopListening() {
        if (listeningProcess == null) return;
        logger.debug("listening pause...", this);
        listeningProcess.destroy();
        listeningProcess = null;
    }

    private void startListening() {
        if (listeningChannel == null || listeningObserver == null) return;
        logger.debug("listening continue...", this);
        try {
            listeningProcess = new ProcessBuilder()
                    .command("bash", "-l", "-c", "e32", "-w", listeningChannel.code)
                    .start();
        } catch (Exception e) {
            logger.error("exception thrown while starting to listen: " + e, this);
        }
    }

    @Override
    public void enqueue(ChannelInfo channel, Message message) {
        logger.debug(String.format("enqueued @%s: %s", channel.code, message), this);
        queue.add(new SendingItem(channel, message));
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
    public String info() {
        return "E32";
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(FileClient.class, CommandLine.class, Logger.class, Executor.class);
    }

    private static class SendingItem {
        public final ChannelInfo channel;
        public final Message message;

        public SendingItem(ChannelInfo channel, Message message) {
            this.channel = channel;
            this.message = message;
        }
    }
}
