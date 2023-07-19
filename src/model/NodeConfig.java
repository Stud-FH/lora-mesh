package model;

import java.io.Serializable;

public class NodeConfig implements Serializable {
    public static final long serialVersionUID = 1L;

    public long serialId;
    public ChannelInfo meshChannel;
    public boolean enableController;
    public boolean enableDataSink;
    public String apiUrl;
    public int helloInterval = 20;
    public int rendezvousInterval = 50;
    public int routingInterval = 200;
    public int tracingPeriod = 10;
    public int joinVolleyAmount = 10;
    public int inviteVolleyAmount = 5;
}
