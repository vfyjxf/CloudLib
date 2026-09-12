package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime handle of a live in-world panel.
 */
public interface InworldPanel {

    /** The identity key from the panel's spec. */
    PanelKey key();

    InworldAnchor anchor();

    Presentation presentation();

    /** The chrome root widget (frame + content). */
    Widget widget();

    /** The widget created by the spec's content factory. */
    Widget content();

    /** Whether this panel currently owns the scene's in-world focus. */
    boolean focused();

    boolean hovered();

    /**
     * Whether the panel is currently engaged — the interact-key expansion
     * state. Content widgets may adapt their density to it (summary strip
     * while idle, full surface while engaged).
     */
    default boolean engaged() {
        return false;
    }

    boolean visible();

    void setVisible(boolean visible);

    /** Detaches the panel. Provider-driven panels reappear when re-offered. */
    void close();

    /** Current screen-space bounds of the panel, or null when not on screen. */
    default @Nullable Pos screenPos() {
        return null;
    }

    default @Nullable Size screenSize() {
        return null;
    }

    //region convenience

    /** The anchor's block position when block-bound, else null. */
    default @Nullable BlockPos blockPos() {
        return anchor().blockPos();
    }

    /** The block entity at the anchor's position, if any. */
    default @Nullable BlockEntity blockEntity(ClientLevel level) {
        BlockPos pos = blockPos();
        return pos == null ? null : level.getBlockEntity(pos);
    }

    /** Typed block entity access for panel builders. */
    @SuppressWarnings("unchecked")
    default <T extends BlockEntity> @Nullable T blockEntity(ClientLevel level, Class<T> type) {
        BlockEntity be = blockEntity(level);
        return type.isInstance(be) ? (T) be : null;
    }

    //endregion
}
