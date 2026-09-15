package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen occupancy is camera independent — when every request is a screen
 * panel and the static inputs match the cached frame, the previous rects
 * are reused verbatim and only poses/leaders are re-resolved.
 */
final class ScreenReuse {

    private ScreenReuse() {}

    static @Nullable LayoutSearch.Allocation tryReuse(
            LayoutFrame frame,
            LayoutState state,
            List<LayoutEngine.Unit> units,
            Map<String, SourceSnapshot> sources,
            StableFrame key,
            LeaderSolver leaders,
            LayoutSearch.Work work) {
        StableFrame old = state.cachedInput;
        LayoutResult previous = state.cachedResult;
        if (key == null
                || old == null
                || previous == null
                || !state.recovery.isEmpty()
                || state.debounce.pending()
                || frame.requests().stream().anyMatch(r -> r.space() != Space.screen)
                || frame.requests().stream().anyMatch(r -> PanelRelations.participates(r, frame.requests()))
                || !key.requests().equals(old.requests())
                || !key.config().equals(old.config())
                || !key.hud().equals(old.hud())
                || !key.interaction().equals(old.interaction())
                || key.width() != old.width()
                || key.height() != old.height()
                || previous.panels().size()
                        < Math.min(units.size(), frame.config().maxPanels())) {
            return null;
        }
        for (var entry : sources.entrySet()) {
            SourceSnapshot before = old.sources().get(entry.getKey());
            SourceSnapshot after = entry.getValue();
            if (before == null
                    || before.generation() != after.generation()
                    || before.present() != after.present()
                    || !before.dimension().equals(after.dimension())) {
                return null;
            }
        }
        Map<String, LayoutEngine.Unit> byId = new HashMap<>();
        for (LayoutEngine.Unit unit : units) byId.put(unit.id(), unit);
        List<PanelPlacement> panels = new ArrayList<>();
        for (PanelPlacement before : previous.panels()) {
            LayoutEngine.Unit unit = byId.get(before.visualId());
            if (unit == null) return null;
            List<WorldPlacement.Placement> intent =
                    WorldPlacement.propose(frame, unit.active(), unit.source(), before.pose(), before.tier());
            if (intent.isEmpty()) return null;
            Pose pose = intent.get(0).pose();
            panels.add(new PanelPlacement(
                    unit.id(),
                    unit.members(),
                    unit.active(),
                    unit.source(),
                    before.slot(),
                    before.tier(),
                    Space.screen,
                    pose,
                    before.worldWidth(),
                    before.worldHeight(),
                    before.screenRect(),
                    before.polygon(),
                    before.score(),
                    unit.merged()));
        }
        List<List<GuiVec>> exclusions = new ArrayList<>();
        for (HudRegion hud : frame.hud()) exclusions.add(hud.polygon());
        if (previous.dock().rect() != null)
            exclusions.add(previous.dock().rect().polygon());
        if (previous.drawer() != null) exclusions.add(previous.drawer().rect().polygon());
        LayoutSearch.Plan plan = LayoutSearch.routeExisting(frame, state, panels, exclusions, leaders, work);
        return plan == null
                ? null
                : new LayoutSearch.Allocation(
                        plan,
                        previous.dock().rect(),
                        previous.drawer() == null ? null : previous.drawer().rect(),
                        exclusions);
    }
}
