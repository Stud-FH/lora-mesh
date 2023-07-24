package model;

import model.message.Message;
import model.message.MessageType;

public interface CorrespondenceRegister {

    Message pack(MessageType type, byte... data);
    Message packAndIncrement(MessageType type, byte... data);
    byte[] registerAndListLosses(Message message);

}
