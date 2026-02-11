package dev.vfyjxf.cloudlib.api.ui.scroll;

/**
 * Specifies which axes a scrollable container supports scrolling on.
 */
public enum ScrollDirection {

    /**
     * Only vertical scrolling.
     */
    VERTICAL,

    /**
     * Only horizontal scrolling.
     */
    HORIZONTAL,

    /**
     * Both horizontal and vertical scrolling.
     */
    BOTH;

    public boolean allowsVertical() {
        return this == VERTICAL || this == BOTH;
    }

    public boolean allowsHorizontal() {
        return this == HORIZONTAL || this == BOTH;
    }
}
