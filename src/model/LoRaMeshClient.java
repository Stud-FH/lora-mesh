package model;

import model.message.Message;

public interface LoRaMeshClient {
    void listen(ChannelInfo lmc, Observer<Message> observer);
    void enqueue(ChannelInfo lmc, Message message);

    long getSendingIntervalMillis();
}
