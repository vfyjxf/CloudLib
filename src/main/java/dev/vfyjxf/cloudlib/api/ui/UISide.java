package dev.vfyjxf.cloudlib.api.ui;

public enum UISide {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM;

    public boolean isHorizontal() {
        return this == LEFT || this == RIGHT;
    }

    public boolean isVertical() {
        return this == TOP || this == BOTTOM;
    }

}
