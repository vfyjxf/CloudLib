package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The configurable fake element the coordinator tests drive: anchored at a
 * mutable screen position, proposing anchor-centered candidates of the
 * context's variant size in a fixed deterministic spread, with optional
 * single-candidate, self-managed and budget-aware behaviors plus a log of
 * everything the coordinator asked of it.
 */
final class TestElement implements InworldElement {

    private static final ContentTier[] degradeTiers = {ContentTier.full, ContentTier.compact, ContentTier.labelOnly,
            ContentTier.iconOnly, ContentTier.pip, ContentTier.directionalOnly,};

    final String id;
    final SpaceKind kind;
    final int priority;
    final boolean sticky;
    final ElementMode mode;
    final VariantLadder ladder;
    final boolean singleCandidate;
    final boolean worldOnly;
    final AvoidanceClass avoidanceClass;

    FloatPos anchor;
    boolean anchorValid = true;
    boolean budgetAware = false;

    final List<InworldVariant> ctxVariants = new ArrayList<>();
    final List<InworldVariant> usedVariants = new ArrayList<>();
    int proposeCount;
    int lastRound = -1;
    @Nullable
    SpaceBudget lastBudget;
    @Nullable
    ElementRejection lastRejectionSeen;

    private TestElement(
        String id,
        SpaceKind kind,
        int priority,
        boolean sticky,
        ElementMode mode,
        VariantLadder ladder,
        boolean singleCandidate,
        boolean worldOnly,
        AvoidanceClass avoidanceClass,
        FloatPos anchor
    ) {
        this.id = id;
        this.kind = kind;
        this.priority = priority;
        this.sticky = sticky;
        this.mode = mode;
        this.ladder = ladder;
        this.singleCandidate = singleCandidate;
        this.worldOnly = worldOnly;
        this.avoidanceClass = avoidanceClass;
        this.anchor = anchor;
    }

    /** A plain arbitrated world element with the default active ladder. */
    static TestElement arbitrated(String id, double anchorX, double anchorY, Size... sizes) {
        return new TestElement(
            id,
            SpaceKind.world,
            0,
            false,
            ElementMode.arbitrated,
            ladder(sizes),
            false,
            false,
            AvoidanceClass.standard,
            new FloatPos(anchorX, anchorY)
        );
    }

    /** A self-managed element bringing one authoritative rect. */
    static TestElement selfManaged(String id, double anchorX, double anchorY, Size size) {
        return new TestElement(
            id,
            SpaceKind.world,
            0,
            false,
            ElementMode.selfManaged,
            ladder(SpacePolicy.active, false, true, size),
            true,
            false,
            AvoidanceClass.standard,
            new FloatPos(anchorX, anchorY)
        );
    }

    /** A ladder of the given sizes with default policies: active, no nudge, clamping. */
    static VariantLadder ladder(Size... sizes) {
        return ladder(SpacePolicy.active, false, true, sizes);
    }

    /**
     * A ladder over the sizes, every rung sharing the given policy and
     * nudge/clamp flags; tiers walk the degradation order and the comfortable
     * minimum sits at 75% of each rung's area.
     */
    static VariantLadder ladder(SpacePolicy policy, boolean allowsNudge, boolean allowsClamp, Size... sizes) {
        List<InworldVariant> rungs = new ArrayList<>(sizes.length);
        for (int i = 0; i < sizes.length; i++) {
            Size size = sizes[i];
            rungs.add(
                new InworldVariant(
                    i,
                    size,
                    degradeTiers[Math.min(i, degradeTiers.length - 1)],
                    policy,
                    allowsNudge,
                    allowsClamp,
                    0.75 * size.width() * size.height()
                )
            );
        }
        return VariantLadder.of(rungs);
    }

    TestElement withPriority(int newPriority) {
        return new TestElement(
            id,
            kind,
            newPriority,
            sticky,
            mode,
            ladder,
            singleCandidate,
            worldOnly,
            avoidanceClass,
            anchor
        );
    }

    TestElement withSticky() {
        return new TestElement(
            id,
            kind,
            priority,
            true,
            mode,
            ladder,
            singleCandidate,
            worldOnly,
            avoidanceClass,
            anchor
        );
    }

    TestElement withKind(SpaceKind newKind) {
        return new TestElement(
            id,
            newKind,
            priority,
            sticky,
            mode,
            ladder,
            singleCandidate,
            worldOnly,
            avoidanceClass,
            anchor
        );
    }

    TestElement withLadder(VariantLadder newLadder) {
        return new TestElement(
            id,
            kind,
            priority,
            sticky,
            mode,
            newLadder,
            singleCandidate,
            worldOnly,
            avoidanceClass,
            anchor
        );
    }

    TestElement withSingleCandidate() {
        return new TestElement(id, kind, priority, sticky, mode, ladder, true, worldOnly, avoidanceClass, anchor);
    }

    /**
     * A world-only element: proposes world candidates with no projection —
     * the box sits around the anchor's world position, the screen half is
     * never claimed.
     */
    TestElement withWorldOnly() {
        return new TestElement(id, kind, priority, sticky, mode, ladder, singleCandidate, true, avoidanceClass, anchor);
    }

    /**
     * A rigid element: declares the rigid yield class — the coordinator
     * grants its first screen candidate directly every frame.
     */
    TestElement withRigid() {
        return new TestElement(
            id,
            kind,
            priority,
            sticky,
            mode,
            ladder,
            singleCandidate,
            worldOnly,
            AvoidanceClass.rigid,
            anchor
        );
    }

    void moveTo(double x, double y) {
        anchor = new FloatPos(x, y);
    }

    void shift(double dx, double dy) {
        anchor = new FloatPos(anchor.x() + dx, anchor.y() + dy);
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
    public AvoidanceClass avoidanceClass() {
        return avoidanceClass;
    }

    @Override
    public ElementProposal propose(ProposeContext context) {
        proposeCount++;
        lastRound = context.round();
        lastBudget = context.budget();
        lastRejectionSeen = context.lastRejection();
        ctxVariants.add(context.variant());
        if (!anchorValid) {
            usedVariants.add(context.variant());
            return ElementProposal.retract(context.variant());
        }
        if (worldOnly) {
            usedVariants.add(context.variant());
            Size size = context.variant().requestedSize();
            return ElementProposal.worldOnly(
                context.variant(),
                List.of(
                    PlacementCandidate
                            .world(WorldAabb.around(anchor.x(), 64.0, anchor.y(), size.width(), 8.0, size.height()))
                )
            );
        }
        InworldVariant variant = context.variant();
        if (budgetAware && context.round() == 0 && context.budget().capacityFor(variant.requestedArea()) == 0) {
            InworldVariant degraded = ladder.degrade(variant, RejectionReason.insufficientArea);
            if (degraded != null) {
                variant = degraded;
            }
        }
        usedVariants.add(variant);
        Size size = variant.requestedSize();
        boolean oneCandidate = singleCandidate || mode == ElementMode.selfManaged;
        double[][] offsets = oneCandidate
                ? new double[][]{{0, 0}}
                : new double[][]{{0, 0}, {size.width() + 8, 0}, {-(size.width() + 8), 0}, {0, size.height() + 8},
                        {0, -(size.height() + 8)},};
        List<PlacementCandidate> candidates = new ArrayList<>(offsets.length);
        for (double[] offset : offsets) {
            double cx = anchor.x() + offset[0];
            double cy = anchor.y() + offset[1];
            candidates.add(
                PlacementCandidate.dual(
                    WorldAabb.around(cx, 64.0, cy, size.width(), 8.0, size.height()),
                    FloatRect.around(new FloatPos(cx, cy), size.width(), size.height())
                )
            );
        }
        return ElementProposal.of(variant, anchor, candidates);
    }
}
