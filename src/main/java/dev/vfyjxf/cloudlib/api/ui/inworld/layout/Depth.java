package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/** How a panel or leader line behaves against world depth. */
public enum Depth {
    /** Occluded by opaque world geometry. */
    test,
    /** Reads through world geometry (x-ray). */
    xray
}
