package dev.vfyjxf.cloudlib.api.text;

/**
 * Vertical alignment of an inline object (image, item, block, entity, widget...)
 * within a text line.
 */
public enum VerticalAlign {
    /**
     * Sits the object on the text baseline (its bottom edge aligns with the
     * baseline of the surrounding text).
     */
    baseline,
    /**
     * Aligns the object's top edge with the line top.
     */
    top,
    /**
     * Centers the object vertically within the line.
     */
    middle,
    /**
     * Aligns the object's bottom edge with the line bottom.
     */
    bottom
}
