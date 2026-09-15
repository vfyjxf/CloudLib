package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * The world interaction contract of a panel. A {@linkplain #physical()
 * physical} panel collides with solid obstacles and is depth-tested; a
 * {@linkplain #virtual() virtual} one floats through geometry and draws
 * x-ray style.
 */
public record Material(boolean requiresFreeWorldSpace, Depth depth) {

    public static Material physical() {
        return new Material(true, Depth.test);
    }

    public static Material virtual() {
        return new Material(false, Depth.xray);
    }
}
