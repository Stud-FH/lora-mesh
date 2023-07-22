package model;

import model.message.Message;
import model.message.NodeInfo;

import java.util.function.Supplier;

public interface LoRaMeshClient {
    void listen(ChannelInfo channel, Observer<Message> observer, Supplier<NodeInfo> nodeInfoSupplier);
    void enqueue(ChannelInfo channel, Message message, NodeInfo nodeInfo);

    long getSendingIntervalMillis();

}