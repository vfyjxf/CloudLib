package dev.vfyjxf.cloudlib.api.math;

import org.jetbrains.annotations.Contract;

/**
 * Represents a position in 2D space.
 */
public record Pos(int x, int y) {

    public static final Pos ORIGIN = new Pos(0, 0);

    public Pos(MutablePos pos) {
        this(pos.x(), pos.y());
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    @Contract(" -> new")
    public Pos copy() {
        return new Pos(x(), y());
    }

    public MutablePos toMutable() {
        return new MutablePos(this);
    }

    @Contract("_,_-> new")
    public Pos translate(int x, int y) {
        return new Pos(this.x() + x, this.y() + y);
    }

    public Pos translate(Pos pos) {
        return new Pos(this.x() + pos.x(), this.y() + pos.y());
    }

    @Contract("_,_-> new")
    public Pos scale(int x, int y) {
        return new Pos(this.x() * x, this.y() * y);
    }

}
