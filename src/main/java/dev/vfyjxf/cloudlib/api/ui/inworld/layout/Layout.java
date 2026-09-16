package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * Resolves where one UI surface lands for a frame — the contract between a
 * layout intent (face-mounted, floating, docked, …) and its computed
 * placement.
 * <p>
 * A layout is bound to its intent by whoever creates it (typically a factory
 * registered in a {@link LayoutRegistry}); {@link #resolve} then turns the
 * frame's {@link LayoutContext} into a {@link LayoutOutput}. The intent types
 * themselves are semantic-layer business — this contract never names them.
 *
 * @param <C> the context variant this layout resolves against
 */
@FunctionalInterface
public interface Layout<C extends LayoutContext> {

    /** Computes this frame's placement. */
    LayoutOutput resolve(C ctx);
}
