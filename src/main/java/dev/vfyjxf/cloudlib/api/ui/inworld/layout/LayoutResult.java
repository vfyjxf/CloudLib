package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;
import java.util.Map;

/**
 * The solver's output for one frame: visible panels, routed leader lines,
 * overflow dock/drawer state, per-request address bookkeeping (which tier
 * and container each request landed in), panel transitions for animation
 * and diagnostics.
 */
public record LayoutResult(
        long frameIndex,
        List<PanelPlacement> panels,
        List<LeaderLine> leaders,
        OverflowDock dock,
        OverflowDrawer drawer,
        Map<String, RequestAddress> addresses,
        List<PanelTransition> transitions,
        List<String> diagnostics,
        Map<String, SourceSnapshot> sources) {

    public LayoutResult {
        panels = List.copyOf(panels);
        leaders = List.copyOf(leaders);
        addresses = Map.copyOf(addresses);
        transitions = List.copyOf(transitions);
        diagnostics = List.copyOf(diagnostics);
        sources = Map.copyOf(sources);
    }
}
