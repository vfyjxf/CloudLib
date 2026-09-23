package dev.vfyjxf.cloudlib.api.ui.inworld.stress;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ContentTier;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementProposal;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldElement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldVariant;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.PlacementCandidate;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ProposeContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.RejectionReason;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceKind;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.VariantLadder;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.WorldAabb;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The stress-test element: a deterministic, randomly parameterized
 * {@link InworldElement} whose anchor follows one of five motion modes and
 * whose contract surface (kind, priority, stickiness, mode, ladder, policies,
 * nudge/clamp flags) is drawn from the scenario's seeded generator. Proposals
 * are anchor-centered candidate spreads of the context's variant — the same
 * shape the coordinator unit tests use, so behavior differences come from the
 * coordinator, not from exotic elements.
 */
final class StressElement implements InworldElement {

    /** The anchor motion modes the generator mixes. */
    enum Motion {
        staticAnchor, linearDrift, orbit, teleport, jitter
    }

    private static final ContentTier[] degradeTiers = ContentTier.values();
    private static final double tierComfortFactor = 0.75;

    final String id;
    final SpaceKind kind;
    final int priority;
    final boolean sticky;
    final ElementMode mode;
    final VariantLadder ladder;
    final boolean singleCandidate;
    final boolean budgetAware;
    final boolean worldOnly;
    final Motion motion;

    private final FloatPos base;
    private final double velocityX;
    private final double velocityY;
    private final double orbitRadius;
    private final double orbitOmega;
    private final double orbitPhase;
    private final double teleportInterval;
    private final double jitterAmplitude;

    private double teleportTimer;
    private FloatPos home;
    private FloatPos anchor;
    boolean anchorValid = true;

    private StressElement(
        String id,
        SpaceKind kind,
        int priority,
        boolean sticky,
        ElementMode mode,
        VariantLadder ladder,
        boolean singleCandidate,
        boolean budgetAware,
        boolean worldOnly,
        Motion motion,
        FloatPos base,
        double velocityX,
        double velocityY,
        double orbitRadius,
        double orbitOmega,
        double orbitPhase,
        double teleportInterval,
        double jitterAmplitude
    ) {
        this.id = id;
        this.kind = kind;
        this.priority = priority;
        this.sticky = sticky;
        this.mode = mode;
        this.ladder = ladder;
        this.singleCandidate = singleCandidate;
        this.budgetAware = budgetAware;
        this.worldOnly = worldOnly;
        this.motion = motion;
        this.base = base;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.orbitRadius = orbitRadius;
        this.orbitOmega = orbitOmega;
        this.orbitPhase = orbitPhase;
        this.teleportInterval = teleportInterval;
        this.jitterAmplitude = jitterAmplitude;
        this.home = base;
        this.anchor = base;
    }

    /**
     * Draws one element from the generator's random stream. Every draw
     * consumes the stream in a fixed order, so the same seed produces the
     * same population. The {@code worldOnlyFraction} draw is consumed only
     * when the fraction is positive — existing specs' streams stay identical.
     */
    static StressElement generate(
        String id,
        Random rnd,
        int screenWidth,
        int screenHeight,
        int maxLadderRungs,
        @Nullable Motion motionOverride,
        double budgetAwareFraction,
        boolean singleAnchor,
        double sizeScale,
        double worldOnlyFraction
    ) {
        FloatPos base = new FloatPos(
            16 + rnd.nextDouble() * (screenWidth - 32),
            16 + rnd.nextDouble() * (screenHeight - 32)
        );
        if (singleAnchor) {
            base = new FloatPos(screenWidth * 0.5, screenHeight * 0.5);
        }
        SpaceKind kind = drawKind(rnd);
        int priority = rnd.nextInt(11);
        boolean sticky = rnd.nextDouble() < 0.25;
        ElementMode mode = rnd.nextDouble() < 0.12 ? ElementMode.selfManaged : ElementMode.arbitrated;
        boolean singleCandidate = mode == ElementMode.selfManaged || rnd.nextDouble() < 0.2;
        SpacePolicy policy = drawPolicy(rnd);
        VariantLadder ladder = ladderFor(rnd, policy, Math.max(1, maxLadderRungs), sizeScale);
        boolean budgetAware = rnd.nextDouble() < budgetAwareFraction;
        boolean worldOnly = worldOnlyFraction > 0 && rnd.nextDouble() < worldOnlyFraction;
        Motion motion = motionOverride != null ? motionOverride : drawMotion(rnd);

        double velocityX = 0;
        double velocityY = 0;
        double orbitRadius = 0;
        double orbitOmega = 0;
        double orbitPhase = 0;
        double teleportInterval = 0;
        double jitterAmplitude = 0;
        switch (motion) {
            case linearDrift -> {
                double speed = 10 + rnd.nextDouble() * 50;
                double heading = rnd.nextDouble() * 2 * Math.PI;
                velocityX = Math.cos(heading) * speed;
                velocityY = Math.sin(heading) * speed;
            }
            case orbit -> {
                orbitRadius = 20 + rnd.nextDouble() * 100;
                orbitOmega = (rnd.nextBoolean() ? 1 : -1) * (0.2 + rnd.nextDouble() * 1.0);
                orbitPhase = rnd.nextDouble() * 2 * Math.PI;
            }
            case teleport -> teleportInterval = 1.0 + rnd.nextDouble() * 2.0;
            case jitter -> jitterAmplitude = 0.5;
            default -> {}
        }
        return new StressElement(
            id,
            kind,
            priority,
            sticky,
            mode,
            ladder,
            singleCandidate,
            budgetAware,
            worldOnly,
            motion,
            base,
            velocityX,
            velocityY,
            orbitRadius,
            orbitOmega,
            orbitPhase,
            teleportInterval,
            jitterAmplitude
        );
    }

    private static int clampSize(int value) {
        return Math.max(12, Math.min(600, value));
    }

    private static SpaceKind drawKind(Random rnd) {
        double roll = rnd.nextDouble();
        if (roll < 0.5) {
            return SpaceKind.world;
        }
        return roll < 0.7 ? SpaceKind.tracked : SpaceKind.panel;
    }

    private static SpacePolicy drawPolicy(Random rnd) {
        double roll = rnd.nextDouble();
        if (roll < 0.55) {
            return SpacePolicy.active;
        }
        if (roll < 0.75) {
            return SpacePolicy.passive;
        }
        return roll < 0.9 ? SpacePolicy.fixed : SpacePolicy.ghost;
    }

    private static Motion drawMotion(Random rnd) {
        double roll = rnd.nextDouble();
        if (roll < 0.2) {
            return Motion.staticAnchor;
        }
        if (roll < 0.45) {
            return Motion.linearDrift;
        }
        if (roll < 0.65) {
            return Motion.orbit;
        }
        return roll < 0.8 ? Motion.teleport : Motion.jitter;
    }

    /** A monotone ladder of at most {@code rungs} rungs over a random base size. */
    private static VariantLadder ladderFor(Random rnd, SpacePolicy policy, int rungs, double sizeScale) {
        int width = clampSize((int) ((24 + rnd.nextInt(140)) * sizeScale));
        int height = clampSize((int) ((14 + rnd.nextInt(66)) * sizeScale));
        boolean allowsNudge = rnd.nextDouble() < 0.5;
        boolean allowsClamp = rnd.nextDouble() < 0.75;
        List<InworldVariant> variants = new ArrayList<>(rungs);
        int level = 0;
        while (level < rungs) {
            int w = Math.max(12, width);
            int h = Math.max(10, height);
            if (level > 0) {
                Size previous = variants.get(level - 1).requestedSize();
                if (w * h >= previous.width() * previous.height()) {
                    break;
                }
            }
            variants.add(
                new InworldVariant(
                    level,
                    new Size(w, h),
                    degradeTiers[Math.min(level, degradeTiers.length - 1)],
                    policy,
                    allowsNudge,
                    allowsClamp,
                    tierComfortFactor * w * h
                )
            );
            width = (int) (width * 0.6);
            height = (int) (height * 0.65);
            level++;
        }
        if (variants.isEmpty()) {
            variants.add(
                new InworldVariant(
                    0,
                    new Size(24, 14),
                    ContentTier.full,
                    policy,
                    allowsNudge,
                    allowsClamp,
                    tierComfortFactor * 24 * 14
                )
            );
        }
        return VariantLadder.of(variants);
    }

    /** Advances the anchor one frame according to the motion mode. */
    void update(double nowSeconds, double dtSeconds, Random rnd, int screenWidth, int screenHeight) {
        switch (motion) {
            case staticAnchor -> anchor = base;
            case linearDrift ->
                anchor = new FloatPos(base.x() + velocityX * nowSeconds, base.y() + velocityY * nowSeconds);
            case orbit -> {
                double angle = orbitOmega * nowSeconds + orbitPhase;
                anchor = new FloatPos(
                    base.x() + orbitRadius * Math.cos(angle),
                    base.y() + orbitRadius * Math.sin(angle)
                );
            }
            case teleport -> {
                teleportTimer += dtSeconds;
                if (teleportTimer >= teleportInterval) {
                    teleportTimer = 0;
                    home = new FloatPos(
                        16 + rnd.nextDouble() * (screenWidth - 32),
                        16 + rnd.nextDouble() * (screenHeight - 32)
                    );
                }
                anchor = home;
            }
            case jitter -> anchor = new FloatPos(
                base.x() + (rnd.nextDouble() * 2 - 1) * jitterAmplitude,
                base.y() + (rnd.nextDouble() * 2 - 1) * jitterAmplitude
            );
        }
    }

    /** The current anchor (the position the element proposes around). */
    FloatPos anchor() {
        return anchor;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SpaceKind spaceKind() {
        return kind;
    }

    @Override
    public VariantLadder ladder() {
        return ladder;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean sticky() {
        return sticky;
    }

    @Override
    public ElementMode mode() {
        return mode;
    }

    @Override
    public boolean worldOnly() {
        return worldOnly;
    }

    @Override
    public ElementProposal propose(ProposeContext context) {
        if (!anchorValid) {
            return ElementProposal.retract(context.variant());
        }
        InworldVariant variant = context.variant();
        if (worldOnly) {
            // pure world geometry around the anchor — no projection, no
            // screen candidates; the coordinator grants it unconditionally
            Size size = variant.requestedSize();
            return ElementProposal.worldOnly(
                context.variant(),
                List.of(
                    PlacementCandidate
                            .world(WorldAabb.around(anchor.x(), 64.0, anchor.y(), size.width(), 8.0, size.height()))
                )
            );
        }
        if (budgetAware && context.round() == 0 && context.budget().capacityFor(variant.requestedArea()) == 0) {
            InworldVariant degraded = ladder.degrade(variant, RejectionReason.insufficientArea);
            if (degraded != null) {
                variant = degraded;
            }
        }
        Size size = variant.requestedSize();
        double[][] offsets = singleCandidate
                ? new double[][]{{0, 0}}
                : new double[][]{{0, 0}, {size.width() + 8, 0}, {-(size.width() + 8), 0}, {0, size.height() + 8},
                        {0, -(size.height() + 8)},};
        List<PlacementCandidate> candidates = new ArrayList<>(offsets.length);
        for (double[] offset : offsets) {
            double centerX = anchor.x() + offset[0];
            double centerY = anchor.y() + offset[1];
            candidates.add(
                PlacementCandidate.dual(
                    WorldAabb.around(centerX, 64.0, centerY, size.width(), 8.0, size.height()),
                    FloatRect.around(new FloatPos(centerX, centerY), size.width(), size.height())
                )
            );
        }
        return ElementProposal.of(variant, anchor, candidates);
    }
}
