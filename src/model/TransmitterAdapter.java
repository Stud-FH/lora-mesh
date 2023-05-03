package model;

import model.message.Message;

import java.util.function.Consumer;

public interface TransmitterAdapter {

    void send(Message message);
    void setReceiveCallback(Consumer<Message> callback);

    long getSendingIntervalMillis();

    // todo enforce
    int getDataLengthLimit();

    String getTuning();
    void applyDefaultTuning();
    void tune(String config);
    String tuneStep();

}
