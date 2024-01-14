package dev.vfyjxf.cloudlib.math;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class Point {

    public static final Point ZERO = new Point(0, 0);

    public int x;
    public int y;

    public Point() {
        this(0, 0);
    }

    public Point(Point p) {
        this(p.x, p.y);
    }

    public Point(FloatingPoint p) {
        this(p.x, p.y);
    }

    public Point(double x, double y) {
        this((int) x, (int) y);
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public FloatingPoint getFloatingLocation() {
        return new FloatingPoint(x, y);
    }

    public Point getLocation() {
        return new Point(x, y);
    }


    public Point copy() {
        return getLocation();
    }

    public void setLocation(double x, double y) {
        this.x = (int) Math.floor(x + 0.5);
        this.y = (int) Math.floor(y + 0.5);
    }

    public void move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void translate(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point pt) {
            return (x == pt.x) && (y == pt.y);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this);
    }
}