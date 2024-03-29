package v2.production.impl;

import v2.core.concurrency.Executor;
import v2.core.context.Context;
import v2.core.domain.ChannelInfo;
import v2.core.domain.LoRaMeshModule;
import v2.core.common.Observer;
import v2.core.domain.message.Message;
import v2.core.log.Logger;
import v2.shared.integration.CommandLine;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class E32LoRaMeshModule implements LoRaMeshModule {

    public static final long COOLDOWN = 1000;

    public static final String startingSymbol = "x";
    public static final String terminalSymbol = ";";
    public static final String messagePattern = "\\w*" + startingSymbol + "\\w*" + terminalSymbol;

    private final Queue<SendingItem> queue = new LinkedList<>();
    private CommandLine cmd;
    private Logger logger;
    private Executor exec;

    private ChannelInfo listeningChannel = null;
    private Observer<Message> listeningObserver = null;
    private Process listeningProcess = null;

    @Override
    public void deploy() {
        exec.schedulePeriodic(this::triggerQueue, COOLDOWN, 100);
    }

    @Override
    public void build(Context ctx) {
        logger = ctx.resolve(Logger.class);
        cmd = ctx.resolve(CommandLine.class);
        exec = ctx.resolve(Executor.class);
    }

    private void triggerQueue() {
        if (!queue.isEmpty()) {
            var item = queue.poll();
            logger.debug("trigger queue", this);
            processInput(closeListeningChannel(), listeningObserver);
            var raw = messageToRaw(item.message);
            logger.debug("emitting: " + raw, this);
            var proc = cmd.async("/usr/java.local/bin/e32", "-w", item.channel.code);
            try {
                proc.getOutputStream().write(String.format("%s%s%s\n", startingSymbol, raw, terminalSymbol).getBytes());
            } catch (Exception e) {
                logger.exception(e, this);
            }

            // TODO send EOF INSTEAD OF DESTROY
            proc.destroy();
            openListeningChannel();
        }
    }

    @Override
    public void listen(ChannelInfo channel, Observer<Message> observer) {
        processInput(closeListeningChannel(), listeningObserver);
//        if (listeningObserver != null) listeningObserver.dispose(); // FIXME
        logger.debug("now listening on channel " + channel.code, this);
        listeningChannel = channel;
        listeningObserver = observer;
        openListeningChannel();
    }

    private String closeListeningChannel() {
        if (listeningProcess == null) {
            return "";
        } else {
            logger.debug(String.format("closing channel %s", listeningChannel.code), this);
            listeningProcess.destroy();
            var result = "";
            try {
                result = new String(listeningProcess.getInputStream().readAllBytes());
                logger.debug(result, this);
            } catch (Exception e) {
                logger.exception(e, this);
            }
            listeningProcess = null;
            return result;
        }
    }

    private void openListeningChannel() {
        if (listeningChannel == null || listeningObserver == null) {
            listeningProcess = null;
        } else {
            logger.debug(String.format("listening on %s", listeningChannel.code), this);
            listeningProcess = cmd.async("/usr/java.local/bin/e32", "-w", listeningChannel.code);
        }
    }

    private void processInput(String input, Observer<Message> observer) {
//        if (input.isEmpty() || observer == null || observer.isExpired()) return; // FIXME
        Scanner sc = new Scanner(input);
        while (sc.hasNext(messagePattern)) {
            String in = sc.next(messagePattern);
            var startIndex = in.indexOf(startingSymbol) + startingSymbol.length();
            var endIndex =  in.indexOf(terminalSymbol);
            if (startIndex > 0) {
                logger.debug("received (prefix/discarding): " + in.substring(0, startIndex), this);
            }
            var raw = in.substring(startIndex, endIndex);
            logger.debug("received: " + raw, this);
            observer.next(rawToMessage(raw));
        }
    }

    @Override
    public void enqueue(ChannelInfo channel, Message message) {
        logger.debug(String.format("enqueued on channel %s: %s", channel.code, message), this);
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

    private static class SendingItem {
        public final ChannelInfo channel;
        public final Message message;

        public SendingItem(ChannelInfo channel, Message message) {
            this.channel = channel;
            this.message = message;
        }
    }
}
