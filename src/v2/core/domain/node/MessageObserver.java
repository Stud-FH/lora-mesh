package v2.core.domain.node;

import v2.core.common.Observer;
import v2.core.domain.message.Message;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MessageObserver implements Observer<Message> {

    private boolean disposed = false;
    private final Consumer<Message> messageHandler;
    private final Supplier<Boolean> validity;

    public MessageObserver(Consumer<Message> messageHandler, Supplier<Boolean> validity) {
        this.messageHandler = messageHandler;
        this.validity = validity;
    }

    @Override
    public void next(Message message) {
        messageHandler.accept(message);
    }

    @Override
    public boolean isExpired() {
        return disposed || !validity.get();
    }

    @Override
    public void dispose() {
        disposed = true;
    }
}
