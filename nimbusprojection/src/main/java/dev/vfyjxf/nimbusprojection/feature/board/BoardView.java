package dev.vfyjxf.nimbusprojection.feature.board;

import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import dev.vfyjxf.nimbusprojection.api.sync.SharedPanelView;
import dev.vfyjxf.nimbusprojection.api.sync.SharedViewContext;
import net.minecraft.network.chat.Component;

/**
 * The client materializer for {@link BoardFeature#view} — every watching
 * client runs this once when the shared panel spawns, and the spec's
 * channel handler feeds later {@link BoardPayload} pushes into the widget.
 */
public final class BoardView implements SharedPanelView<BoardPayload> {

    @Override
    public PanelSpec open(SharedViewContext<BoardPayload> ctx) {
        BoardPayload initial = ctx.payload() != null ? ctx.payload() : new BoardPayload(0, "");
        // the channel handler fires before/while content materializes, so the
        // widget reference is filled by the content factory
        var widget = new BoardWidgetRef();
        return PanelSpec.of(ctx.key(), ctx.anchor(), ctx.presentation(), pctx -> {
                    widget.w = new BoardWidget(pctx, initial);
                    return widget.w;
                })
                .title(Component.literal("board"))
                .onDemand(false)
                .channel((pctx, payload) -> {
                    if (payload instanceof BoardPayload update && widget.w != null) widget.w.update(update);
                });
    }

    private static final class BoardWidgetRef {
        BoardWidget w;
    }
}
