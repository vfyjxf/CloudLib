package dev.vfyjxf.cloudlib.api.math;

import org.jetbrains.annotations.Contract;

/**
 * Represents a rectangle.
 */
public class Rect {

    public int x;
    public int y;
    public int width;
    public int height;

    public Rect(Pos pos, int width, int height) {
        this.x = pos.x;
        this.y = pos.y;
        this.width = width;
        this.height = height;
    }

    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Rect copy() {
        return new Rect(x, y, width, height);
    }

    public boolean contains(int x, int y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    public boolean contains(double x, double y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    @Contract("_ -> this")
    public Rect move(Pos pos) {
        this.x = pos.x;
        this.y = pos.y;
        return this;
    }
}