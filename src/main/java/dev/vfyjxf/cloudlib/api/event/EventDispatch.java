package dev.vfyjxf.cloudlib.api.event;

public enum EventDispatch {
    pass,
    handled,
    consumed;

    public static EventDispatch max(EventDispatch a, EventDispatch b) {
        return a.ordinal() > b.ordinal() ? a : b;
    }

    public boolean handled() {
        return this != pass;
    }
}
