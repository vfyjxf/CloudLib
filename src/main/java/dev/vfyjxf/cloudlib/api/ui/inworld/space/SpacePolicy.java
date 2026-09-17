package dev.vfyjxf.cloudlib.api.ui.inworld.space;

/**
 * How an element participates in space resolution.
 * <ul>
 *   <li>{@link #active} — pushes others out of its way and is pushed in turn</li>
 *   <li>{@link #passive} — never pushes, only gets pushed</li>
 *   <li>{@link #fixed} — exempt from occlusion/avoidance resolution by default
 *       (declared layouts that must not be renegotiated)</li>
 *   <li>{@link #ghost} — participates in nothing: no collision, no avoidance,
 *       invisible to the coordinator</li>
 * </ul>
 */
public enum SpacePolicy {
    active,
    passive,
    fixed,
    ghost;

    /** Whether this element displaces others when it resolves overlaps. */
    public boolean pushesOthers() {
        return this == active;
    }

    /** Whether other elements may push this one when resolving overlaps. */
    public boolean pushable() {
        return this == active || this == passive;
    }

    /** Whether the element is visible to space resolution at all. */
    public boolean participates() {
        return this != ghost;
    }

    /**
     * Whether the element is skipped when computing what occludes what.
     * {@link #fixed} defaults to exempt (G9: fixed layouts stay put);
     * {@link #ghost} trivially so.
     */
    public boolean occlusionExempt() {
        return this == fixed || this == ghost;
    }
}
