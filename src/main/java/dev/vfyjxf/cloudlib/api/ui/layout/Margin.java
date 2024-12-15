package dev.vfyjxf.cloudlib.api.ui.layout;

public class Margin {
    public int start;
    public int top;
    public int end;
    public int bottom;

    public Margin all(int all) {
        return this.all(all, all);
    }

    public Margin all(int horizontal, int vertical) {
        return this.all(horizontal, vertical, horizontal, vertical);
    }

    public Margin all(int start, int top, int end, int bottom) {
        this.start = start;
        this.top = top;
        this.end = end;
        this.bottom = bottom;
        return this;
    }

    public Margin start(int start) {
        this.start = start;
        return this;
    }

    public Margin top(int top) {
        this.top = top;
        return this;
    }

    public Margin end(int end) {
        this.end = end;
        return this;
    }

    public Margin bottom(int bottom) {
        this.bottom = bottom;
        return this;
    }

    public Margin reset() {
        this.start = 0;
        this.top = 0;
        this.end = 0;
        this.bottom = 0;
        return this;
    }

    public int vertical() {
        return this.top + this.bottom;
    }

    public int horizontal() {
        return this.start + this.end;
    }

    @Override
    public String toString() {
        return "{" +
                "left=" + start +
                ", top=" + top +
                ", right=" + end +
                ", bottom=" + bottom +
                ", vertical=" + vertical() +
                ", horizontal=" + horizontal() +
                '}';
    }
}