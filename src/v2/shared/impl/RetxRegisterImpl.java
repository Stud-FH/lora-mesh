package v2.shared.impl;

import v2.core.domain.RetxRegister;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageHeader;
import v2.core.domain.message.MessageType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class RetxRegisterImpl implements RetxRegister {

    public static final String HISTORY_BREAKPOINT_OPTION = "history-breakpoint";
    protected static final int COUNTER_LIMIT = 1 << MessageHeader.COUNTER_BITS;

    private boolean expired = false;
    private final Map<Byte, Entry> perAddress = new HashMap<>();

    public boolean knows(byte address) {
        return perAddress.containsKey(address);
    }

    @Override
    public void dispose() {
        expired = true;
        perAddress.clear();
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public void next(Message message) {
        if (expired) throw new IllegalStateException();
        if (!MessageType.Hello.matches(message)) return;
        int counter = message.getCounter();
        var entry = perAddress.computeIfAbsent(message.getNodeAddress(), address -> new Entry(counter));
        entry.currentReceivedCounter++;
        entry.currentMissedCounter += counter - entry.expectedCounter;
        entry.expectedCounter = (counter + COUNTER_LIMIT) % COUNTER_LIMIT;
    }

    @Override
    public double calculateRetx(byte address, String... options) {
        if (expired) throw new IllegalStateException();
        var entry = perAddress.getOrDefault(address, null);
        if (entry == null) return 0;

        double currentlyMeasured = entry.currentReceivedCounter / (entry.currentReceivedCounter + entry.currentMissedCounter);
        double result = currentlyMeasured;
        double exp = 0.5;
        for (double historical : entry.history) {
            result = ((1-exp)*result) + (exp*historical);
            exp /= 2;
        }
        if (Arrays.asList(options).contains(HISTORY_BREAKPOINT_OPTION)) {
            entry.history.add(currentlyMeasured);
            entry.currentMissedCounter = entry.currentReceivedCounter = 0;
        }
        return result;
    }

    @Override
    public Map<Byte, Double> calculateRetxMap(double threshold, String... options) {
        Map<Byte, Double> result = new HashMap<>();
        perAddress.keySet().forEach(address -> {
            double retx = calculateRetx(address, options);
            if (retx >= threshold) result.put(address, retx);
        });
        return result;
    }

    private static class Entry {
        int expectedCounter;
        double currentReceivedCounter = 0;
        double currentMissedCounter = 0;
        LinkedList<Double> history = new LinkedList<>();

        Entry(int initialCounter) {
            this.expectedCounter = initialCounter;
        }
    }

}
