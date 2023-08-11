package v2.core.domain;

import v2.core.domain.message.Message;
import v2.core.domain.message.MessageType;

import java.util.Collection;

public interface CorrespondenceRegister {

    int address();
    Message pack(MessageType type, byte... data);
    Message packAndIncrement(MessageType type, byte... data);
    Collection<Integer> registerAndListLosses(Message message);

}
