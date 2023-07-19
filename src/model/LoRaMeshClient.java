package model;

import model.message.Message;
import model.message.NodeInfo;

import java.io.IOException;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface LoRaMeshClient {
    void listen(ChannelInfo channel, Observer<Message> observer, Supplier<NodeInfo> nodeInfoSupplier);
    void enqueue(ChannelInfo channel, Message message, NodeInfo nodeInfo);

    long getSendingIntervalMillis();

}