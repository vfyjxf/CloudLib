package dev.vfyjxf.cloudlib.api.ui.layout;

public class Offset {

    private int x;
    private int y;

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public Offset x(int x) {
        this.x = x;
        return this;
    }

    public Offset y(int y) {
        this.y = y;
        return this;
    }

    public Offset reset() {
        this.x = 0;
        this.y = 0;
        return this;
    }
}
