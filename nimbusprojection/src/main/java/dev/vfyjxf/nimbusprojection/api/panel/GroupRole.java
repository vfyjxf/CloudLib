package dev.vfyjxf.nimbusprojection.api.panel;

/**
 * A panel's role inside a {@link PanelGroup} container.
 */
public enum GroupRole {

    /**
     * The group's lead member: engagement on the container's affordance opens
     * this panel first. A group has exactly one primary — the first declared
     * wins, further claimants demote to {@link #SECONDARY}.
     */
    PRIMARY,

    /**
     * Standby member: presents only after the group expands (group chrome
     * chip / keynav), layered behind or beside the primary.
     */
    SECONDARY,

    /**
     * Always-on member: visible without engagement — a mini status strip on
     * the anchor. An {@code onDemand(false)} spec in an implicit group
     * resolves to this role automatically.
     */
    AMBIENT,

}
