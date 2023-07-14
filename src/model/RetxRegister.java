package model;

import model.message.Message;

import java.util.Map;

public interface RetxRegister extends Observer<Message> {
    void next(Message message);
    double calculateRetx(byte nodeId, String... options);
    Map<Byte, Double> calculateRetxMap(double threshold, String... options);
    boolean knows(byte nodeId);
    void dispose();
}
