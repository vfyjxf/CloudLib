package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * Host-driven interaction state: which visual is focused, which member is
 * selected inside a merged panel, and whether the overflow dock is open
 * (plus the group and page it is showing).
 */
public record Interaction(
        String focusedId, String selectedMemberId, boolean overflowOpen, String overflowGroup, int page) {

    public static Interaction none() {
        return new Interaction(null, null, false, null, 0);
    }
}
