package dev.vfyjxf.cloudlib.api.math;

import org.jetbrains.annotations.Contract;

public class FloatPos {

    public double x;
    public double y;

    public FloatPos(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Contract("_,_ -> this")
    public FloatPos set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Contract("_,_ -> this")
    public FloatPos translate(double x, double y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public FloatPos copy() {
        return new FloatPos(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FloatPos floatPos = (FloatPos) o;
        return Double.compare(x, floatPos.x) == 0 && Double.compare(y, floatPos.y) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        return result;
    }

    @Override
    public String toString() {
        return "FloatPos{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
