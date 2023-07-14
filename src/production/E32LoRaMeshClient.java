package production;

import model.LoRaMeshClient;
import model.ChannelInfo;
import model.Observer;
import model.message.Message;

public class E32LoRaMeshClient implements LoRaMeshClient {

    @Override
    public void listen(ChannelInfo lmc, Observer<Message> observer) {

    }

    @Override
    public void enqueue(ChannelInfo lmc, Message message) {

    }

    @Override
    public long getSendingIntervalMillis() {
        return 1000; // todo
    }
}
