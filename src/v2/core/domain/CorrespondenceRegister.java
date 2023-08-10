package v2.core.domain;

import v2.core.domain.message.Message;
import v2.core.domain.message.MessageType;

public interface CorrespondenceRegister {

    Message pack(MessageType type, byte... data);
    Message packAndIncrement(MessageType type, byte... data);
    byte[] registerAndListLosses(Message message);

}
