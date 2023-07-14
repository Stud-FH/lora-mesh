package model;

import model.message.Message;

public interface DataSinkClient {

    boolean heartbeat();
    void feed(Message message);
}
