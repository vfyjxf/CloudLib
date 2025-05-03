package dev.vfyjxf.cloudlib.api.ui;

public enum LifecycleStage {
    CONSTRUCT,
    INIT,
    LIVING,
    DESTROYED;

    public boolean hasNext() {
        return this != DESTROYED;
    }

    public LifecycleStage nextStage() {
        if (this == DESTROYED) {
            throw new IllegalStateException("Cannot move to next stage from DESTROYED!");
        }
        return LifecycleStage.values()[ordinal() + 1];
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
