package v2.core.domain;

import v2.core.common.Observer;
import v2.core.domain.message.Message;

import java.util.Map;

public interface RetxRegister extends Observer<Message> {
    void next(Message message);
    double calculateRetx(int address, String... options);
    Map<Integer, Double> calculateRetx(double threshold, String... options);
    boolean knows(int address);
}
