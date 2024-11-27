package dev.vfyjxf.cloudlib.api.ui;

/**
 * A rectangle standardized to the screen coordinate system.
 */
public interface Area {

    Area EMPTY = of(0, 0, 0, 0);

    static Area of(int x, int y, int width, int height) {
        return new Area() {
            @Override
            public int x() {
                return x;
            }

            @Override
            public int y() {
                return y;
            }

            @Override
            public int width() {
                return width;
            }

            @Override
            public int height() {
                return height;
            }
        };
    }

    int x();

    int y();

    int width();

    int height();

}
