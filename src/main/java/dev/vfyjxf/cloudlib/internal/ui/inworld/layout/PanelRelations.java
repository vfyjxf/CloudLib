package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;

import java.util.ArrayList;
import java.util.List;

/**
 * Directed yield rules between panels — {@code yieldTo} names individual
 * request ids, {@code yieldScope} widens the rule to every request in the
 * frame or in a chosen space.
 */
public final class PanelRelations {

    private PanelRelations() {}

    /** Does {@code a} defer to {@code b} — {@code b} keeps its place over {@code a}? */
    static boolean yields(PanelRequest a, PanelRequest b) {
        if (a.id().equals(b.id())) return false;
        return a.yieldTo().contains(b.id())
                || switch (a.yieldScope()) {
                    case all -> true;
                    case screen -> b.space() == Space.screen;
                    case world -> b.space() == Space.world;
                    case otherSpace -> a.space() != b.space();
                    case none -> false;
                }
                || a.crossSpaceAvoidance() && a.space() != b.space();
    }

    /** Must {@code a} and {@code b} never overlap on screen? */
    static boolean mustSeparate(PanelRequest a, PanelRequest b) {
        return a.space() == b.space() || yields(a, b) || yields(b, a);
    }

    /** Does {@code request} take part in any yield rule of this frame? */
    static boolean participates(PanelRequest request, List<PanelRequest> requests) {
        return requests.stream().anyMatch(other -> yields(request, other) || yields(other, request));
    }

    /**
     * Topological layers for the yield DAG: each layer may only be placed
     * once the panels it yields to are committed. Cyclic rules throw.
     * The member-level {@code waitsOn} matrix is built once — repeated
     * rounds only scan the boolean matrix, not the relation rules.
     */
    static List<List<LayoutEngine.Unit>> layers(List<LayoutEngine.Unit> units) {
        int n = units.size();
        boolean[][] waitsOn = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                for (var a : units.get(i).members()) {
                    for (var b : units.get(j).members()) {
                        if (yields(a, b)) {
                            waitsOn[i][j] = true;
                            break;
                        }
                    }
                    if (waitsOn[i][j]) break;
                }
            }
        }
        boolean[] done = new boolean[n];
        int remaining = n;
        List<List<LayoutEngine.Unit>> result = new ArrayList<>();
        while (remaining > 0) {
            List<LayoutEngine.Unit> ready = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (done[i]) continue;
                boolean waits = false;
                for (int j = 0; j < n; j++) {
                    if (!done[j] && waitsOn[i][j]) {
                        waits = true;
                        break;
                    }
                }
                if (!waits) {
                    done[i] = true;
                    ready.add(units.get(i));
                }
            }
            if (ready.isEmpty()) {
                List<String> cyclic = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    if (!done[i]) cyclic.add(units.get(i).id());
                }
                throw new IllegalArgumentException("cyclic UI yieldTo rules: " + cyclic);
            }
            result.add(List.copyOf(ready));
            remaining -= ready.size();
        }
        return result;
    }
}
