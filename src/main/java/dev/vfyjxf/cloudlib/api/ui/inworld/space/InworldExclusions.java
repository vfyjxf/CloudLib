package dev.vfyjxf.cloudlib.api.ui.inworld.space;

import dev.vfyjxf.cloudlib.api.math.Rect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The global registry of {@link ExclusionProvider exclusion-area providers} —
 * the single public avoidance hook for third-party UIs. Registered providers
 * are evaluated every frame by the coordinator's space-build phase and merged
 * into the layout space's occupancy, which is all it takes to be avoided.
 * <p>
 * {@link #collect} aggregates with deterministic normalization: each
 * rectangle is clipped to the viewport and empty ones dropped, pairs whose
 * union is itself a rectangle (containment, or edge-aligned overlap/touch)
 * merge into their bounding box — pairs whose union is not a rectangle stay
 * separate, since a bounding box there would exclude space nobody occupies —
 * and the result is sorted by y, x, then size.
 */
public final class InworldExclusions {

    private static final List<ExclusionProvider> providers = new CopyOnWriteArrayList<>();

    private InworldExclusions() {}

    /**
     * Registers a provider. Idempotent: an already-registered provider is not
     * registered twice.
     */
    public static void register(ExclusionProvider provider) {
        if (!providers.contains(provider)) {
            providers.add(provider);
        }
    }

    /**
     * Unregisters a provider.
     *
     * @return whether the provider was registered
     */
    public static boolean unregister(ExclusionProvider provider) {
        return providers.remove(provider);
    }

    /** The registered providers in registration order. */
    public static List<ExclusionProvider> providers() {
        return List.copyOf(providers);
    }

    /**
     * Evaluates every provider and merges their rectangles as described in
     * the class docs. Providers must not throw; a throwing provider fails the
     * frame.
     */
    public static List<Rect> collect(ExclusionContext context) {
        Rect viewport = context.viewport();
        List<Rect> rects = new ArrayList<>();
        for (ExclusionProvider provider : providers) {
            for (Rect rect : provider.exclusionAreas(context)) {
                if (rect == null) {
                    continue;
                }
                Rect clipped = viewport.intersection(rect);
                if (clipped.width() > 0 && clipped.height() > 0) {
                    rects.add(clipped);
                }
            }
        }
        mergeRectangleUnions(rects);
        rects.sort(Comparator.comparingInt(Rect::y)
                .thenComparingInt(Rect::x)
                .thenComparingInt(Rect::height)
                .thenComparingInt(Rect::width));
        return List.copyOf(rects);
    }

    /** Repeatedly merges pairs whose union is exactly a rectangle, until stable. */
    private static void mergeRectangleUnions(List<Rect> rects) {
        boolean changed = true;
        while (changed) {
            changed = false;
            mergePass:
            for (int i = 0; i < rects.size(); i++) {
                for (int j = i + 1; j < rects.size(); j++) {
                    Rect first = rects.get(i);
                    Rect second = rects.get(j);
                    long unionArea = area(first) + area(second) - intersectionArea(first, second);
                    if (area(boundingBox(first, second)) == unionArea) {
                        rects.set(i, boundingBox(first, second));
                        rects.remove(j);
                        changed = true;
                        break mergePass;
                    }
                }
            }
        }
    }

    private static Rect boundingBox(Rect a, Rect b) {
        int x = Math.min(a.x(), b.x());
        int y = Math.min(a.y(), b.y());
        int right = Math.max(a.right(), b.right());
        int bottom = Math.max(a.bottom(), b.bottom());
        return new Rect(x, y, right - x, bottom - y);
    }

    private static long area(Rect rect) {
        return (long) rect.width() * rect.height();
    }

    private static long intersectionArea(Rect a, Rect b) {
        Rect intersection = a.intersection(b);
        return (long) intersection.width() * intersection.height();
    }
}
