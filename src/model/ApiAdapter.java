package model;

import model.message.Message;

import java.util.List;

public interface ApiAdapter {

    boolean testConnection();

    byte nextId();
    String getTuning();
    CorrespondenceManager correspondence(byte nodeId);
    List<String> receive(byte nodeId, Message message);

}
