package model;

import model.message.Message;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public interface LoRaMeshClient extends Module {
    void listen(ChannelInfo channel, Observer<Message> observer, Supplier<NodeInfo> nodeInfoSupplier);
    void enqueue(ChannelInfo channel, Message message, NodeInfo nodeInfo);

    long getSendingIntervalMillis();

    @Override
    default Collection<Class<? extends Module>> providers() {
        return Set.of(LoRaMeshClient.class);
    }
}