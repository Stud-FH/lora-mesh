package v2.core.test;

import org.junit.jupiter.api.Test;
import v2.core.domain.message.MessageType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTypeTest {

    @Test
    void testConflicts() {
        Set<Integer> headers = new HashSet<>();

        for (MessageType t : MessageType.values()) {
            assertTrue(headers.add(t.getHeaderBinary()));
        }
    }
}