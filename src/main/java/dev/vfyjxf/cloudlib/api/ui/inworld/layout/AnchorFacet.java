package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.Objects;

/**
 * The anchor facet (§7 plan B): what this element is anchored to — the
 * declaration of where its motion reference comes from. The facet declares
 * the <em>kind</em> and its binding parameters; the per-frame resolution to
 * a screen position (and world box) is the pipeline's ANCHOR stage, fed by
 * the frame's {@code LayoutEnvironment} (entity and world anchors are
 * resolved by the driving adapter — camera-tracked anchors resolve purely
 * from the screen size).
 * <p>
 * The kind also decides the element's {@code SpaceKind}: world anchors
 * (entity, blockFace, position) are world, {@code cameraTracked} is tracked,
 * {@code none} is panel.
 */
public sealed interface AnchorFacet {

    /** The anchor kinds and their validation-relevant families. */
    enum Kind {
        entity,
        blockFace,
        position,
        cameraTracked,
        none
    }

    /** This anchor's kind. */
    Kind kind();

    /** Whether this anchor lives in world space (entity, blockFace, position). */
    default boolean worldAnchored() {
        return kind() == Kind.entity || kind() == Kind.blockFace || kind() == Kind.position;
    }

    /**
     * Anchored to an entity (nameplates, follow panels): the motion
     * reference is the entity's render-interpolated position; the anchor is
     * gone while the entity is unloaded (retract → linger, §3.6).
     *
     * @param entityId the anchor entity's id, non-empty
     * @param verticalOffsetBlocks the vertical lift above the entity's
     *        origin, in blocks
     */
    record Entity(String entityId, double verticalOffsetBlocks) implements AnchorFacet {

        public Entity {
            Objects.requireNonNull(entityId, "entityId");
            if (entityId.isEmpty()) {
                throw new IllegalArgumentException("entityId must not be empty");
            }
            if (!Double.isFinite(verticalOffsetBlocks)) {
                throw new IllegalArgumentException("verticalOffsetBlocks must be finite: " + verticalOffsetBlocks);
            }
        }

        @Override
        public Kind kind() {
            return Kind.entity;
        }
    }

    /**
     * Anchored to a block face (machine panels): carries the face's block
     * position and outward normal; the granted quad hugs the face with the
     * declared inset (T7's edge margin).
     *
     * @param x the block's x coordinate
     * @param y the block's y coordinate
     * @param z the block's z coordinate
     * @param normal the outward face normal
     * @param insetPixels the inset from the face's edges, in gui pixels
     */
    record BlockFace(double x, double y, double z, Normal normal, double insetPixels) implements AnchorFacet {

        public BlockFace {
            Objects.requireNonNull(normal, "normal");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("block position must be finite: " + x + "," + y + "," + z);
            }
            if (!Double.isFinite(insetPixels) || insetPixels < 0) {
                throw new IllegalArgumentException("insetPixels must be finite and non-negative: " + insetPixels);
            }
        }

        @Override
        public Kind kind() {
            return Kind.blockFace;
        }
    }

    /**
     * Anchored to a fixed world position (ground projections, holograms,
     * waypoints, pings).
     *
     * @param x the world x
     * @param y the world y
     * @param z the world z
     */
    record Position(double x, double y, double z) implements AnchorFacet {

        public Position {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("position must be finite: " + x + "," + y + "," + z);
            }
        }

        @Override
        public Kind kind() {
            return Kind.position;
        }
    }

    /**
     * Anchored to a screen-fraction position (camera-tracked UI): resolves
     * purely to {@code (u·screenWidth, v·screenHeight)} — no adapter needed.
     *
     * @param u the horizontal screen fraction, in {@code [0, 1]}
     * @param v the vertical screen fraction, in {@code [0, 1]}
     */
    record CameraTracked(double u, double v) implements AnchorFacet {

        public CameraTracked {
            if (!Double.isFinite(u) || u < 0 || u > 1) {
                throw new IllegalArgumentException("u must be in [0, 1]: " + u);
            }
            if (!Double.isFinite(v) || v < 0 || v > 1) {
                throw new IllegalArgumentException("v must be in [0, 1]: " + v);
            }
        }

        @Override
        public Kind kind() {
            return Kind.cameraTracked;
        }
    }

    /** No anchor: a screen-space panel whose placement comes from its candidates. */
    record None() implements AnchorFacet {

        @Override
        public Kind kind() {
            return Kind.none;
        }
    }

    /** {@link #entity(String, double)} at the entity's origin. */
    static Entity entity(String entityId) {
        return new Entity(entityId, 0.0);
    }

    /** An entity anchor with a vertical lift. */
    static Entity entity(String entityId, double verticalOffsetBlocks) {
        return new Entity(entityId, verticalOffsetBlocks);
    }

    /** A block-face anchor. */
    static BlockFace blockFace(double x, double y, double z, Normal normal) {
        return new BlockFace(x, y, z, normal, 0.0);
    }

    /** A world-position anchor. */
    static Position position(double x, double y, double z) {
        return new Position(x, y, z);
    }

    /** A camera-tracked anchor at screen fractions {@code (u, v)}. */
    static CameraTracked cameraTracked(double u, double v) {
        return new CameraTracked(u, v);
    }

    /** The no-anchor singleton. */
    static None none() {
        return new None();
    }

    /** The six outward block-face normals. */
    enum Normal {
        north(0, 0, -1),
        south(0, 0, 1),
        west(-1, 0, 0),
        east(1, 0, 0),
        up(0, 1, 0),
        down(0, -1, 0);

        private final double dx;
        private final double dy;
        private final double dz;

        Normal(double dx, double dy, double dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        /** The normal's world x component. */
        public double dx() {
            return dx;
        }

        /** The normal's world y component. */
        public double dy() {
            return dy;
        }

        /** The normal's world z component. */
        public double dz() {
            return dz;
        }
    }
}
