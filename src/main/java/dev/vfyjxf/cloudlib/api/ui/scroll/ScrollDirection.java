package dev.vfyjxf.cloudlib.api.ui.scroll;

/**
 * Specifies which axes a scrollable container supports scrolling on.
 */
public enum ScrollDirection {

    /**
     * Only vertical scrolling.
     */
    vertical,

    /**
     * Only horizontal scrolling.
     */
    horizontal,

    /**
     * Both horizontal and vertical scrolling.
     */
    both;

    public boolean allowsVertical() {
        return this == vertical || this == both;
    }

    public boolean allowsHorizontal() {
        return this == horizontal || this == both;
    }
}
