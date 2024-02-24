package dev.vfyjxf.cloudlib.api.ui;

/**
 * A rectangle standardized to the screen coordinate system.
 */
public interface IArea {

    IArea EMPTY = of(0, 0, 0, 0);

    static IArea of(int x, int y, int width, int height) {
        return new IArea() {
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
