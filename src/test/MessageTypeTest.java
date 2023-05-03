package test;

import model.message.MessageType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MessageTypeTest {

    @Test
    void testConflicts() {
        Set<Integer> headers = new HashSet<>();

        for (MessageType t : MessageType.values()) {
            assertTrue(headers.add(t.getHeaderBinary()));
        }
    }
}