package model;

import java.io.Serializable;

public class ChannelInfo implements Serializable {

    public static final ChannelInfo rendezvous = new ChannelInfo("c000001a1744");

    public final String code;

    public ChannelInfo(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
