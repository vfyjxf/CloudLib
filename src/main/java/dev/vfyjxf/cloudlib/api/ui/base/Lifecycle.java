package dev.vfyjxf.cloudlib.api.ui.base;

public enum Lifecycle {
    created, initialized, mounted, unmounted, destroyed;

    public boolean created() {
        return this == created;
    }

    public boolean initialized() {
        return this != created;
    }

    public boolean mounted() {
        return this == mounted;
    }

    public boolean unmounted() {
        return this == unmounted;
    }

    public boolean destroyed() {
        return this == destroyed;
    }
}
