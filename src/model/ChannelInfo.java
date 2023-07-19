package model;

import java.io.Serializable;

public record ChannelInfo(String code) implements Serializable {

    public static final ChannelInfo rendezvous = new ChannelInfo("c000001a1744");

    @Override
    public String toString() {
        return code;
    }
}
