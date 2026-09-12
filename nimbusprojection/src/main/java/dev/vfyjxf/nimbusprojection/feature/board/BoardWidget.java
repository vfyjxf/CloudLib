package dev.vfyjxf.nimbusprojection.feature.board;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelChannel;
import dev.vfyjxf.cloudlib.ui.widget.ButtonWidget;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.LabelWidget;
import dev.vfyjxf.cloudlib.ui.widget.RowWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The shared board's client materialization: a counter every watcher sees,
 * plus +/- ops that travel back over the panel channel — clicks are
 * requests, the server-validated {@link BoardPayload} is what actually
 * updates the label.
 */
public final class BoardWidget extends WidgetGroup<Widget> {

    private final LabelWidget count;
    private final LabelWidget lastBy;
    private final @Nullable PanelChannel channel;

    public BoardWidget(InworldPanelContext ctx, BoardPayload initial) {
        this.channel = ctx.channel();
        ColumnWidget column = ColumnWidget.create(3);
        column.addWidget(LabelWidget.of(Component.literal("shared board")));

        count = LabelWidget.of(Component.literal("0"));
        lastBy = LabelWidget.of(Component.literal("—"));
        column.addWidget(count);
        column.addWidget(lastBy);

        RowWidget buttons = RowWidget.create(3);
        buttons.addWidget(ButtonWidget.of("−", () -> send(-1)));
        buttons.addWidget(ButtonWidget.of("+", () -> send(+1)));
        column.addWidget(buttons);

        addWidget(column);
        update(initial);
    }

    /** Applies a server-pushed state — the only way the counter moves. */
    public void update(BoardPayload payload) {
        count.setText(Component.literal("count  " + payload.count()));
        lastBy.setText(Component.literal("last   " + (payload.lastBy().isEmpty() ? "—" : payload.lastBy())));
    }

    private void send(int delta) {
        if (channel != null) channel.sendToServer(new BoardOpPayload(delta));
    }
}
