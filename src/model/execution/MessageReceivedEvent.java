package model.execution;

import model.message.Message;

public class MessageReceivedEvent extends Event{

    public final Message message;

    public MessageReceivedEvent(Message message) {
        super("received " + message.toString());
        this.message = message;
    }
}
