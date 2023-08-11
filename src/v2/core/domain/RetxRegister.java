package v2.core.domain;

import v2.core.domain.message.Message;

import java.util.Map;

public interface RetxRegister extends Observer<Message> {
    void next(Message message);
    double calculateRetx(byte address, String... options);
    Map<Byte, Double> calculateRetxMap(double threshold, String... options);
    boolean knows(byte address);
    void dispose();
}
