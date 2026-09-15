package dev.vfyjxf.cloudlib.api.event.context;

import dev.vfyjxf.cloudlib.api.event.EventChannel;

public final class BubbleContext {

    private final EventChannel<?> source;

    private EventChannel<?> current;
    private Phase phase = Phase.capture;

    private boolean consumed = false;
    private boolean cancelled = false;
    private boolean interrupted = false;


    public BubbleContext(EventChannel<?> source) {
        this.source = source;
    }


    public EventChannel<?> target() {
        return source;
    }

    public EventChannel<?> current() {
        return current;
    }

    public void setCurrent(EventChannel<?> current) {
        this.current = current;
    }

    public Phase phase() {
        return phase;
    }

    public boolean capturing() {
        return phase == Phase.capture;
    }

    public boolean targeting() {
        return phase == Phase.target;
    }

    public boolean bubbling() {
        return phase == Phase.bubble;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public boolean consumed() {
        return consumed;
    }

    public void consume() {
        consumed = true;
    }

    public void interrupt() {
        interrupted = true;
    }

    public boolean interrupted() {
        return interrupted;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public void cancelAndInterrupt() {
        cancelled = true;
        interrupted = true;
    }

    public enum Phase {
        capture,
        target,
        bubble
    }
}
