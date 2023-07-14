package model;

import model.message.Message;
import model.message.MessageHeader;
import model.message.MessageType;

import java.util.HashSet;
import java.util.Set;

public class LocalCorrespondenceClient implements CorrespondenceClient {

    public static LocalCorrespondenceClient to(int nodeId) {
        return new LocalCorrespondenceClient(nodeId | MessageHeader.DOWNWARDS_BIT, 1 << MessageHeader.COUNTER_BITS);
    }

    public static LocalCorrespondenceClient from(int nodeId) {
        return new LocalCorrespondenceClient(nodeId, 1 << MessageHeader.COUNTER_BITS);
    }

    private int sendingCounter = 0;
    private int nextReceivingCounter = 0;
    private final Set<Integer> missing = new HashSet<>();

    private final int address;
    private final int counterLimit;

    private LocalCorrespondenceClient(int address, int counterLimit) {
        this.address = address;
        this.counterLimit = counterLimit;
    }

    @Override
    public Message pack(MessageType type, byte... data) {
        int header = type.getHeaderBinary()
                | (address << MessageHeader.ADDRESS_SHIFT)
                | (sendingCounter << MessageHeader.COUNTER_SHIFT);
        return new Message(header, data);
    }

    @Override
    public Message packAndIncrement(MessageType type, byte... data) {
        int header = type.getHeaderBinary()
                | (address << MessageHeader.ADDRESS_SHIFT)
                | (sendingCounter << MessageHeader.COUNTER_SHIFT);

        sendingCounter += 1;
        sendingCounter %= counterLimit;

        return new Message(header, data);
    }

    /**
     * returns the list of counter numbers that were skipped when receiving this message
     */
    public byte[] registerAndListLosses(Message message) {
        int counter = message.getCounter();

        if (counter == nextReceivingCounter) {
            nextReceivingCounter = (counter + 1) % counterLimit;
        } else if(missing.contains(counter)) {
            missing.remove(counter);
            return new byte[] {};
        } else {
            for (int i = nextReceivingCounter; i != counter; i = ((i + 1) % counterLimit)) {
                missing.add(i);
            }
            nextReceivingCounter = (counter + 1) % counterLimit;
        }
        var result = new byte[missing.size()];
        int i = 0;
        for (int b : missing) result[i++] = (byte) b;
        return result;
    }
}
