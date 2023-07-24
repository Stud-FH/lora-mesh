package model;

import model.message.Message;

import java.util.Collection;
import java.util.Set;

public interface LoRaMeshClient extends Module {
    void listen(ChannelInfo channel, Observer<Message> observer);
    void enqueue(ChannelInfo channel, Message message);

    @Override
    default Collection<Class<? extends Module>> providers() {
        return Set.of(LoRaMeshClient.class);
    }
}