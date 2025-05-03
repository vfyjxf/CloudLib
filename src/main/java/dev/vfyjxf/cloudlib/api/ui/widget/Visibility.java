package dev.vfyjxf.cloudlib.api.ui.widget;

public enum Visibility {

    VISIBLE,
    INVISIBLE,
    GONE;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
