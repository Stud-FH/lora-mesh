package v2.core.domain;

import v2.core.context.Module;
import v2.core.domain.message.Message;

public interface LoRaMeshClient extends Module {
    void listen(ChannelInfo channel, Observer<Message> observer);
    void enqueue(ChannelInfo channel, Message message);
}