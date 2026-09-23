package dev.vfyjxf.cloudlib.api.ui.inworld.stress;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldCoordinator;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.stress.StressElement.Motion;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * The seeded, fully deterministic stress driver: generates an element
 * population and a schedule of world events (exclusion churn, membership
 * churn, anchor retraction, viewport resizes, an optional giant exclusion),
 * drives the coordinator frame by frame, feeds every frame to
 * {@link FrameInvariants} and records a canonical per-frame string — the
 * determinism witness. Same spec (same seed) twice produces identical runs;
 * every random draw comes from the one seeded stream in a fixed order.
 */
final class StressScenario {

    static final double dt = 1.0 / 60.0;

    private static final int staticSettleFrames = 90;
    private static final int[][] resizeSizes = {{400, 300}, {640, 360}, {320, 240}, {1024, 640}, {280, 200},};

    /**
     * @param seed the one source of randomness
     * @param elementCount the initial population
     * @param frames how many frames to drive
     * @param screenWidth initial gui width
     * @param screenHeight initial gui height
     * @param maxLadderRungs rungs per ladder; 0 draws 2..4 randomly
     * @param staticTailFraction tail fraction of the run with all dynamics
     *        frozen (strict stability window after a settle period); 0 disables
     * @param exclusionBaseCount exclusions present from frame 0
     * @param exclusionEvents scheduled add/remove events before the frozen tail
     * @param exclusionStorm swap one exclusion for another every frame
     * @param churnEvents scheduled membership churn events (register/unregister batches)
     * @param retractEvents scheduled anchor-invalidation episodes (retract, later recover)
     * @param resizeEvents scheduled viewport resizes
     * @param singleAnchor adversarial: every element anchored at the screen center
     * @param motionOverride force one motion mode for the whole population; null mixes
     * @param budgetAwareFraction share of budget-aware elements (proactive degradation)
     * @param giantExclusionFrame frame at which a full-screen exclusion appears; -1 never
     * @param sizeScale multiplier on the drawn element sizes — the dense big
     *        scenario scales sizes up so the same 240-element over-constrained
     *        population commits fewer rects, which keeps the suite inside its
     *        time budget without giving up volume
     * @param worldOnlyFraction share of world-only elements in the population
     *        (0 draws none and consumes no stream draws, keeping existing
     *        seeds' populations identical)
     */
    record Spec(
        long seed,
        int elementCount,
        int frames,
        int screenWidth,
        int screenHeight,
        int maxLadderRungs,
        double staticTailFraction,
        int exclusionBaseCount,
        int exclusionEvents,
        boolean exclusionStorm,
        int churnEvents,
        int retractEvents,
        int resizeEvents,
        boolean singleAnchor,
        @Nullable Motion motionOverride,
        double budgetAwareFraction,
        int giantExclusionFrame,
        double sizeScale,
        double worldOnlyFraction
    ) {

        Spec {
            if (elementCount < 1 || frames < 1) {
                throw new IllegalArgumentException("elementCount and frames must be positive");
            }
            if (screenWidth < 100 || screenHeight < 100) {
                throw new IllegalArgumentException(
                    "screen must be at least 100x100: " + screenWidth + "x" + screenHeight
                );
            }
            if (staticTailFraction < 0 || staticTailFraction > 0.9) {
                throw new IllegalArgumentException("staticTailFraction must be in [0, 0.9]: " + staticTailFraction);
            }
            if (exclusionBaseCount < 0
                    || exclusionEvents < 0
                    || churnEvents < 0
                    || retractEvents < 0
                    || resizeEvents < 0) {
                throw new IllegalArgumentException("event counts must not be negative");
            }
            if (budgetAwareFraction < 0 || budgetAwareFraction > 1) {
                throw new IllegalArgumentException("budgetAwareFraction must be in [0, 1]: " + budgetAwareFraction);
            }
            if (worldOnlyFraction < 0 || worldOnlyFraction > 1) {
                throw new IllegalArgumentException("worldOnlyFraction must be in [0, 1]: " + worldOnlyFraction);
            }
        }

        static Spec of(long seed, int elementCount, int frames) {
            return new Spec(
                seed,
                elementCount,
                frames,
                400,
                300,
                0,
                0.15,
                4,
                10,
                false,
                10,
                6,
                0,
                false,
                null,
                0.25,
                -1,
                1.0,
                0.0
            );
        }

        /** Draws {@code fraction} of the population as world-only elements. */
        Spec withWorldOnly(double fraction) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                staticTailFraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                fraction
            );
        }

        Spec withSizeScale(double scale) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                staticTailFraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                scale,
                worldOnlyFraction
            );
        }

        Spec withScreen(int width, int height) {
            return new Spec(
                seed,
                elementCount,
                frames,
                width,
                height,
                maxLadderRungs,
                staticTailFraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withStaticTail(double fraction) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                fraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withStorm() {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                0,
                exclusionBaseCount,
                exclusionEvents,
                true,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withSingleAnchor() {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                staticTailFraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                true,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withMotion(Motion motion) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                0,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motion,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withExclusions(int base, int events) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                staticTailFraction,
                base,
                events,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withChurn(int churn, int retract) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                staticTailFraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churn,
                retract,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withResizes(int count) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                staticTailFraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                count,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withRungs(int rungs) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                rungs,
                staticTailFraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                giantExclusionFrame,
                sizeScale,
                worldOnlyFraction
            );
        }

        Spec withGiantExclusion(int frame) {
            return new Spec(
                seed,
                elementCount,
                frames,
                screenWidth,
                screenHeight,
                maxLadderRungs,
                staticTailFraction,
                exclusionBaseCount,
                exclusionEvents,
                exclusionStorm,
                churnEvents,
                retractEvents,
                resizeEvents,
                singleAnchor,
                motionOverride,
                budgetAwareFraction,
                frame,
                sizeScale,
                worldOnlyFraction
            );
        }
    }

    /** One driven frame: the coordinator's answer plus the generator-side truth. */
    record FrameRecord(
        int frameIndex,
        InworldCoordinator.FrameInput input,
        CoordinationResult result,
        Map<String, FloatPos> anchors,
        Set<String> registeredIds,
        boolean resized,
        boolean strictStatic,
        Set<String> worldOnlyIds
    ) {}

    /** The outcome of one run: canonical frames, cause census, and counters. */
    record RunSummary(
        Spec spec,
        List<String> canonicalFrames,
        EnumMap<CoordinationResult.RenegotiationCause, Integer> causeCounts,
        int resolvedFrames,
        int presentedFlips,
        int peakPresented,
        int lastPresented,
        int peakWorldOnlyPresented
    ) {

        int causeCount(CoordinationResult.RenegotiationCause cause) {
            return causeCounts.getOrDefault(cause, 0);
        }
    }

    static RunSummary run(Spec spec) {
        return run(spec, true);
    }

    /**
     * @param checkInvariants whether to run the {@link FrameInvariants}
     *        oracle frame by frame; the determinism witness uses
     *        {@code false} so the byte-equality property gets its own
     *        verdict instead of being masked by the (separately reported)
     *        continuity violations
     */
    static RunSummary run(Spec spec, boolean checkInvariants) {
        Random rnd = new Random(spec.seed());
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        FrameInvariants checker = new FrameInvariants(spec, InworldCoordinator.Config.defaults());

        int width = spec.screenWidth();
        int height = spec.screenHeight();
        List<Rect> exclusions = new ArrayList<>();
        for (int i = 0; i < spec.exclusionBaseCount(); i++) {
            exclusions.add(randomExclusion(rnd, width, height));
        }

        int freezeStart = spec.staticTailFraction() > 0
                ? spec.frames() - (int) (spec.frames() * spec.staticTailFraction())
                : Integer.MAX_VALUE;
        int strictStart = freezeStart == Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : Math.min(spec.frames(), freezeStart + staticSettleFrames);

        long idCounter = 0;
        Map<String, StressElement> live = new LinkedHashMap<>();
        List<StressElement> initial = new ArrayList<>(spec.elementCount());
        for (int i = 0; i < spec.elementCount(); i++) {
            int rungs = spec.maxLadderRungs() == 0 ? 2 + rnd.nextInt(3) : spec.maxLadderRungs();
            initial.add(
                StressElement.generate(
                    "e" + (idCounter++),
                    rnd,
                    width,
                    height,
                    rungs,
                    spec.motionOverride(),
                    spec.budgetAwareFraction(),
                    spec.singleAnchor(),
                    spec.sizeScale(),
                    spec.worldOnlyFraction()
                )
            );
        }
        Collections.shuffle(initial, rnd);
        for (StressElement element : initial) {
            coordinator.register(element);
            live.put(element.id(), element);
        }

        TreeMap<Integer, Boolean> exclusionEvents = new TreeMap<>();
        for (int i = 0; i < spec.exclusionEvents(); i++) {
            exclusionEvents.put(1 + rnd.nextInt(Math.max(1, dynamicSpan(freezeStart) - 2)), i % 2 == 0);
        }
        TreeMap<Integer, Boolean> churnEvents = new TreeMap<>();
        for (int i = 0; i < spec.churnEvents(); i++) {
            churnEvents.put(2 + rnd.nextInt(Math.max(1, dynamicSpan(freezeStart) - 4)), i % 2 == 0);
        }
        List<String> retractIds = new ArrayList<>(live.keySet());
        TreeMap<Integer, String> retractOff = new TreeMap<>();
        TreeMap<Integer, String> retractOn = new TreeMap<>();
        for (int i = 0; i < spec.retractEvents(); i++) {
            String id = retractIds.get(rnd.nextInt(retractIds.size()));
            int offFrame = 2 + rnd.nextInt(Math.max(1, dynamicSpan(freezeStart) - 70));
            retractOff.put(offFrame, id);
            retractOn.put(offFrame + 20 + rnd.nextInt(40), id);
        }
        TreeMap<Integer, Integer> resizes = new TreeMap<>();
        for (int i = 0; i < spec.resizeEvents(); i++) {
            resizes.put(3 + rnd.nextInt(Math.max(1, dynamicSpan(freezeStart) - 6)), i % resizeSizes.length);
        }

        EnumMap<CoordinationResult.RenegotiationCause, Integer> causes = new EnumMap<>(
            CoordinationResult.RenegotiationCause.class
        );
        List<String> canonical = new ArrayList<>(spec.frames());
        Set<String> previousPresented = null;
        int resolvedFrames = 0;
        int presentedFlips = 0;
        int peakPresented = 0;
        int peakWorldOnlyPresented = 0;

        for (int frame = 0; frame < spec.frames(); frame++) {
            boolean frozen = frame >= freezeStart;
            double now = frame * dt;
            boolean resized = false;

            if (!frozen) {
                if (frame == spec.giantExclusionFrame()) {
                    exclusions.add(new Rect(0, 0, width, height));
                }
                if (spec.exclusionStorm() && !exclusions.isEmpty()) {
                    exclusions.remove(rnd.nextInt(exclusions.size()));
                    exclusions.add(randomExclusion(rnd, width, height));
                }
                Boolean exclusionEvent = exclusionEvents.get(frame);
                if (exclusionEvent != null) {
                    if (exclusionEvent) {
                        exclusions.add(randomExclusion(rnd, width, height));
                    } else if (!exclusions.isEmpty()) {
                        exclusions.remove(rnd.nextInt(exclusions.size()));
                    }
                }
                Boolean churnEvent = churnEvents.get(frame);
                if (churnEvent != null) {
                    int batch = 2 + rnd.nextInt(5);
                    if (churnEvent) {
                        for (int i = 0; i < batch; i++) {
                            StressElement element = StressElement.generate(
                                "e" + (idCounter++),
                                rnd,
                                width,
                                height,
                                2 + rnd.nextInt(3),
                                null,
                                0.25,
                                false,
                                spec.sizeScale(),
                                spec.worldOnlyFraction()
                            );
                            coordinator.register(element);
                            live.put(element.id(), element);
                        }
                    } else {
                        for (int i = 0; i < batch && !live.isEmpty(); i++) {
                            List<String> ids = new ArrayList<>(live.keySet());
                            String removed = ids.remove(rnd.nextInt(ids.size()));
                            coordinator.unregister(removed);
                            live.remove(removed);
                        }
                    }
                }
                String off = retractOff.get(frame);
                if (off != null && live.containsKey(off)) {
                    live.get(off).anchorValid = false;
                }
                String on = retractOn.get(frame);
                if (on != null && live.containsKey(on)) {
                    live.get(on).anchorValid = true;
                }
                Integer resize = resizes.get(frame);
                if (resize != null) {
                    width = resizeSizes[resize][0];
                    height = resizeSizes[resize][1];
                    resized = true;
                }
                for (StressElement element : live.values()) {
                    element.update(now, dt, rnd, width, height);
                }
            }

            InworldCoordinator.FrameInput input = InworldCoordinator.FrameInput
                    .of(width, height, now, dt, List.copyOf(exclusions));
            CoordinationResult result = coordinator.frame(input);

            Map<String, FloatPos> anchors = new LinkedHashMap<>();
            for (StressElement element : live.values()) {
                if (element.anchorValid) {
                    anchors.put(element.id(), element.anchor());
                }
            }
            Set<String> worldOnlyIds = new LinkedHashSet<>();
            for (StressElement element : live.values()) {
                if (element.worldOnly) {
                    worldOnlyIds.add(element.id());
                }
            }
            Set<String> presented = new LinkedHashSet<>();
            int worldOnlyPresented = 0;
            for (InworldPlacement placement : result.placements()) {
                presented.add(placement.elementId());
                if (worldOnlyIds.contains(placement.elementId())) {
                    worldOnlyPresented++;
                }
            }
            peakWorldOnlyPresented = Math.max(peakWorldOnlyPresented, worldOnlyPresented);
            if (checkInvariants) {
                checker.check(
                    new FrameRecord(
                        frame,
                        input,
                        result,
                        anchors,
                        new LinkedHashSet<>(live.keySet()),
                        resized,
                        frame >= strictStart,
                        worldOnlyIds
                    )
                );
            }

            canonical.add(canonicalize(result));
            causes.merge(result.cause(), 1, Integer::sum);
            if (result.resolved()) {
                resolvedFrames++;
            }
            peakPresented = Math.max(peakPresented, presented.size());
            if (previousPresented != null) {
                for (String id : presented) {
                    if (!previousPresented.contains(id)) {
                        presentedFlips++;
                    }
                }
            }
            previousPresented = presented;
        }
        return new RunSummary(
            spec,
            canonical,
            causes,
            resolvedFrames,
            presentedFlips,
            peakPresented,
            previousPresented == null ? 0 : previousPresented.size(),
            peakWorldOnlyPresented
        );
    }

    private static int dynamicSpan(int freezeStart) {
        return freezeStart == Integer.MAX_VALUE ? 100_000 : freezeStart;
    }

    private static Rect randomExclusion(Random rnd, int width, int height) {
        int w = 24 + rnd.nextInt(140);
        int h = 16 + rnd.nextInt(84);
        int x = rnd.nextInt(Math.max(1, width - w + 1));
        int y = rnd.nextInt(Math.max(1, height - h + 1));
        return new Rect(x, y, w, h);
    }

    /** The exact-precision per-frame witness used for determinism comparison. */
    private static String canonicalize(CoordinationResult result) {
        StringBuilder text = new StringBuilder(512);
        text.append("f").append(result.frame()).append('|').append(result.epoch()).append('|').append(result.resolved())
                .append('|').append(result.cause()).append('|').append(Double.toString(result.budget().freeFraction()));
        for (CoordinationResult.ElementState state : result.elementStates()) {
            text.append("||").append(state.elementId()).append(':').append(state.phase()).append(':')
                    .append(Double.toString(state.alpha())).append(':');
            if (state.placement() == null) {
                text.append('-');
            } else {
                text.append(state.placement().variant().level()).append('@')
                        .append(Double.toString(state.placement().offsetRect().x())).append(',')
                        .append(Double.toString(state.placement().offsetRect().y())).append(',')
                        .append(Double.toString(state.placement().offsetRect().width())).append(',')
                        .append(Double.toString(state.placement().offsetRect().height())).append('@')
                        .append(state.placement().epoch()).append('@').append(state.placement().arbitrationIndex());
            }
            text.append(':');
            if (state.visualRect() == null) {
                text.append('-');
            } else {
                text.append(Double.toString(state.visualRect().x())).append(',')
                        .append(Double.toString(state.visualRect().y())).append(',')
                        .append(Double.toString(state.visualRect().width())).append(',')
                        .append(Double.toString(state.visualRect().height()));
            }
            text.append(':').append(
                state.rejection() == null ? "-" : state.rejection().reason() + "/" + state.rejection().blockerId()
            );
        }
        return text.toString();
    }
}
