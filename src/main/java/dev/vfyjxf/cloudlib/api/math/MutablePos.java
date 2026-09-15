package dev.vfyjxf.cloudlib.api.math;

public final class MutablePos {
    public int x;
    public int y;

    public MutablePos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public MutablePos(Pos pos) {
        this.x = pos.x();
        this.y = pos.y();
    }

    public void translate(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void set(MutablePos pos) {
        this.x = pos.x;
        this.y = pos.y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public MutablePos copy() {
        return new MutablePos(this.x, this.y);
    }

    public Pos toImmutable() {
        return new Pos(this.x, this.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MutablePos that = (MutablePos) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "MutablePos{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
