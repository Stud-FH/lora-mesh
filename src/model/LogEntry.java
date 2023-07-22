package model;

import model.message.NodeInfo;

public class LogEntry {
    public Logger.Severity severity;
    public NodeInfo nodeInfo;
    public byte[] data;
}
