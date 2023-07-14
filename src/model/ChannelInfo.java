package model;

public record ChannelInfo(short frequency, short dataRate, short spreadingFactor) {

    public static final short RENDEZVOUS_FREQUENCY = (short) 0xC000;
    public static final short RENDEZVOUS_DATA_RATE = (short) 0x001A;
    public static final short RENDEZVOUS_SPREADING_FACTOR = (short) 0x0644;
    public static final ChannelInfo rendezvous = new ChannelInfo(RENDEZVOUS_FREQUENCY, RENDEZVOUS_DATA_RATE, RENDEZVOUS_SPREADING_FACTOR);
}
