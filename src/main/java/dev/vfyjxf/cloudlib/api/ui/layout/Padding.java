package dev.vfyjxf.cloudlib.api.ui.layout;

/**
 * TODO:implement padding support
 */
public class Padding {

    private int start;
    private int top;
    private int end;
    private int bottom;

    public Padding all(int all) {
        return this.all(all, all);
    }

    public Padding all(int horizontal, int vertical) {
        return this.all(horizontal, vertical, horizontal, vertical);
    }

    public Padding all(int start, int top, int end, int bottom) {
        this.start = start;
        this.top = top;
        this.end = end;
        this.bottom = bottom;

        return this;
    }

    public Padding start(int start) {
        this.start = start;

        return this;
    }

    public Padding top(int top) {
        this.top = top;

        return this;
    }

    public Padding end(int end) {
        this.end = end;

        return this;
    }

    public Padding bottom(int bottom) {
        this.bottom = bottom;

        return this;
    }

}