package model.execution;

public class Event {

    public final String desc;

    public Event(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return desc;
    }
}
