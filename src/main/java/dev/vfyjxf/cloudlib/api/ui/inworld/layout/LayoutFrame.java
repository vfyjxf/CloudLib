package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything the solver sees for one frame: the clock, the camera
 * (projector plus its world-space basis), HUD regions to avoid, world
 * obstacles, samplable sources, the panel requests and host interaction.
 */
public record LayoutFrame(
        SampleContext clock,
        Projector camera,
        PanelBasis viewBasis,
        List<HudRegion> hud,
        WorldObstacles world,
        Map<String, SourceProvider> sources,
        List<PanelRequest> requests,
        Interaction interaction,
        Config config) {

    public LayoutFrame {
        hud = List.copyOf(hud);
        sources = Map.copyOf(sources);
        requests = List.copyOf(requests);
        Objects.requireNonNull(interaction);
        Objects.requireNonNull(config);
    }
}
