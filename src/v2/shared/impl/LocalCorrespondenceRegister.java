package v2.shared.impl;

import v2.core.domain.CorrespondenceRegister;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageHeader;
import v2.core.domain.message.MessageType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LocalCorrespondenceRegister implements CorrespondenceRegister {

    public static LocalCorrespondenceRegister to(int nodeId) {
        return new LocalCorrespondenceRegister(nodeId | MessageHeader.DOWNWARDS_BIT);
    }

    public static LocalCorrespondenceRegister from(int nodeId) {
        return new LocalCorrespondenceRegister(nodeId);
    }

    private static final int counterLimit = 1 << MessageHeader.COUNTER_BITS;

    private int sendingCounter = 0;
    private int nextReceivingCounter = 0;
    private final Set<Integer> missing = new HashSet<>();

    private final int address;

    private LocalCorrespondenceRegister(int address) {
        this.address = address;
    }

    public int address() {
        return address;
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
    public Collection<Integer> registerAndListLosses(Message message) {
        int counter = message.getCounter();

        if (counter == nextReceivingCounter) {
            nextReceivingCounter = (counter + 1) % counterLimit;
        } else if(missing.contains(counter)) {
            missing.remove(counter);
            return Collections.emptyList();
        } else {
            for (int i = nextReceivingCounter; i != counter; i = ((i + 1) % counterLimit)) {
                missing.add(i);
            }
            nextReceivingCounter = (counter + 1) % counterLimit;
        }
        return missing;
    }
}
