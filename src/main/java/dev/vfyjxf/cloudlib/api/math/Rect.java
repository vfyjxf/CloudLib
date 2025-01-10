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

    public Rect(Pos pos, Size size) {
        this.x = pos.x;
        this.y = pos.y;
        this.width = size.width;
        this.height = size.height;
    }

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

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public Pos pos() {
        return new Pos(x, y);
    }

    public Size size() {
        return new Size(width, height);
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public Pos center() {
        return new Pos(centerX(), centerY());
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

    public boolean contains(Pos pos) {
        return contains(pos.x, pos.y);
    }

    public boolean contains(double x, double y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    public boolean contains(Rect rect) {
        return x <= rect.x && y <= rect.y && x + width >= rect.x + rect.width && y + height >= rect.y + rect.height;
    }

    public boolean intersects(Rect rect) {
        return x < rect.x + rect.width && x + width > rect.x && y < rect.y + rect.height && y + height > rect.y;
    }

    public boolean intersects(int x, int y, int width, int height) {
        return this.x < x + width && this.x + this.width > x && this.y < y + height && this.y + this.height > y;
    }


    @Contract("_ -> this")
    public Rect move(Pos pos) {
        this.x = pos.x;
        this.y = pos.y;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Rect rect = (Rect) o;
        return x == rect.x && y == rect.y && width == rect.width && height == rect.height;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return "Rect{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
