package dev.vfyjxf.cloudlib.test.inworld;

import dev.vfyjxf.cloudlib.api.ui.floating.FloatingMiddlewares;
import dev.vfyjxf.cloudlib.api.ui.floating.FloatingPlacement;
import dev.vfyjxf.cloudlib.api.ui.Widgets;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldSink;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldUi;
import dev.vfyjxf.cloudlib.ui.inworld.InworldTheme;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.cloudlib.ui.widget.DividerWidget;
import dev.vfyjxf.cloudlib.ui.widget.ProgressBarWidget;
import dev.vfyjxf.cloudlib.ui.widget.SliderWidget;
import dev.vfyjxf.cloudlib.ui.widget.TextWidget;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.JustifyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.alignItemsFlexStart;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.columnGap;
import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.sizeOf;

/**
 * Demo provider for {@link TrackerBlockEntity}: per block it offers
 * <ul>
 *   <li>a docked stat console — synced values, a density bar, an alert-threshold
 *       slider wired through a second reversed channel, and action chips;</li>
 *   <li>a compact face controller tracking the player's side of the block —
 *       {@code V} or the "scan" chip opens the world-space scan console;</li>
 *   <li>an {@link EntityTagWidget} follow-tag per living entity in range,
 *       group-merged via {@code groupLimit(3)};</li>
 *   <li>a detached expand panel hosting {@link ScanConsoleWidget}.</li>
 * </ul>
 */
public final class TrackerPanelProvider implements InworldProvider {

    private static final int RANGE = 10;

    @Override
    public void provide(InworldContext context, InworldSink sink) {
        var level = context.level();
        BlockPos center = context.player().blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-RANGE, -5, -RANGE),
                center.offset(RANGE, 5, RANGE))) {
            if (!(level.getBlockEntity(pos) instanceof TrackerBlockEntity be)) continue;
            BlockPos p = pos.immutable();

            sink.offer(InworldPanelSpec
                    .of("tracker/main/" + p,
                            InworldAnchor.of(p, new Vec3(0.5, 1.2, 0.5)),
                            InworldPlacement.dock(),
                            TrackerPanelProvider::mainContent)
                    .title(Component.literal("TRACKER//" + shortPos(p)))
                    .hints("V:scan", "LMB:press", "R:inspect")
                    .action(TrackerPanelProvider::toggleExpand));

            //compact face controller tracks the player horizontally
            Direction side = sideToward(context.player(), p);
            sink.offer(InworldPanelSpec
                    .of("tracker/face/" + p,
                            InworldAnchor.of(p),
                            InworldPlacement.face(side, 0.5, 0.5, 96),
                            TrackerPanelProvider::faceContent)
                    .action(TrackerPanelProvider::toggleExpand));

            //Witness-style trace puzzle floating left of the anchor — hold V and
            //drag a path from the start circle to the exit stub
            sink.offer(InworldPanelSpec
                    .of("tracker/maze/" + p,
                            InworldAnchor.of(p, new Vec3(0.5, 1.5, 0.5)),
                            InworldPlacement.floating(
                                    FloatingPlacement.leftStart,
                                    FloatingMiddlewares.offset(14),
                                    FloatingMiddlewares.flip(),
                                    FloatingMiddlewares.shift(4),
                                    FloatingMiddlewares.hide()),
                            ctx -> {
                                TrackerBlockEntity tbe = ctx.blockEntity(TrackerBlockEntity.class);
                                return new TracePuzzleWidget(ctx,
                                        () -> { if (tbe != null) tbe.sendSolved(); },
                                        () -> tbe != null ? tbe.solves().get() : 0);
                            })
                    .title(Component.literal("MAZE//" + shortPos(p)))
                    .hints("hold V:trace")
                    .action(TrackerPanelProvider::toggleExpand));

            //sigil pad floating right — freehand rune strokes recognized on
            //release and dispatched to the server over a reversed channel
            sink.offer(InworldPanelSpec
                    .of("tracker/sigil/" + p,
                            InworldAnchor.of(p, new Vec3(0.5, 1.5, 0.5)),
                            InworldPlacement.floating(
                                    FloatingPlacement.rightStart,
                                    FloatingMiddlewares.offset(14),
                                    FloatingMiddlewares.flip(),
                                    FloatingMiddlewares.shift(4),
                                    FloatingMiddlewares.hide()),
                            ctx -> {
                                TrackerBlockEntity tbe = ctx.blockEntity(TrackerBlockEntity.class);
                                return new GlyphPadWidget(ctx,
                                        id -> { if (tbe != null) tbe.sendGlyph(id); });
                            })
                    .title(Component.literal("SIGIL//" + shortPos(p)))
                    .hints("hold V:draw")
                    .action(TrackerPanelProvider::toggleExpand));

            //a follow-tag per living entity in range of the tracker
            var box = AABB.ofSize(Vec3.atCenterOf(p), TrackerBlockEntity.RANGE * 2.0, 12, TrackerBlockEntity.RANGE * 2.0);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (entity == context.player()) continue;
                sink.offer(InworldPanelSpec
                        .of("tracker/ent/" + entity.getId(),
                                InworldAnchor.ofEntity(entity.getId(), new Vec3(0, entity.getBbHeight() + 0.35, 0)),
                                InworldPlacement.follow(0, 0),
                                c -> new EntityTagWidget(entity))
                        .interactive(false)
                        .groupLimit(3)
                        .maxDistance(TrackerBlockEntity.RANGE * 2));
            }
        }
    }

    //region content

    private static Widget mainContent(InworldPanelContext ctx) {
        TrackerBlockEntity be = ctx.blockEntity(TrackerBlockEntity.class);
        ColumnWidget column = ColumnWidget.create(3);
        column.useStyle(alignItemsFlexStart());
        if (be == null) {
            column.addWidget(TextWidget.of("NO LINK").setColor(InworldTheme.TEXT_DIM));
            return column;
        }

        //row 1: entity count + crowd-density bar
        WidgetGroup<Widget> entsRow = Widgets.row(JustifyContent.FLEX_START, AlignItems.CENTER);
        entsRow.useStyle(columnGap(3));
        var ents = TextWidget.of("ents " + be.entities().get()).setColor(InworldTheme.TEXT);
        be.entities().onChange(v -> ents.setText("ents " + v));
        entsRow.addWidget(ents);
        var density = ProgressBarWidget.create(
                () -> Math.min(1, be.entities().get() / (double) TrackerBlockEntity.MAX_THRESHOLD));
        density.setColors(0xFF081018, InworldTheme.ACCENT);
        density.useStyle(sizeOf(44, 5));
        entsRow.addWidget(density);
        column.addWidget(entsRow);

        var nearest = TextWidget.of("nearest " + be.nearest().get()).setColor(InworldTheme.TEXT_DIM);
        be.nearest().onChange(v -> nearest.setText("nearest " + v));
        column.addWidget(nearest);

        //row 3: ping count + armed lamp (armed when the live count reaches the threshold)
        WidgetGroup<Widget> stateRow = Widgets.row(JustifyContent.FLEX_START, AlignItems.CENTER);
        stateRow.useStyle(columnGap(4));
        var pings = TextWidget.of("pings " + be.pings().get()).setColor(InworldTheme.TEXT_DIM);
        be.pings().onChange(v -> pings.setText("pings " + v));
        stateRow.addWidget(pings);
        var armed = TextWidget.of("·ok").setColor(InworldTheme.TEXT_DIM);
        armed.setTickable(true);
        armed.onTick(() -> {
            boolean tripped = be.entities().get() >= be.threshold().get() && be.entities().get() > 0;
            armed.setText(tripped ? "▲ARM" : "·ok");
            armed.setColor(tripped ? 0xFFE06666 : InworldTheme.TEXT_DIM);
        });
        stateRow.addWidget(armed);
        column.addWidget(stateRow);

        var divider = DividerWidget.horizontal();
        divider.setColor(InworldTheme.TITLE_RULE);
        divider.useStyle(sizeOf(112, 3));
        column.addWidget(divider);

        //threshold row: slider drives the second reversed channel; server clamps
        WidgetGroup<Widget> thrRow = Widgets.row(JustifyContent.FLEX_START, AlignItems.CENTER);
        thrRow.useStyle(columnGap(3));
        thrRow.addWidget(TextWidget.of("thr").setColor(InworldTheme.TEXT_DIM));
        var thrValue = TextWidget.of(be.threshold().get() + "").setColor(InworldTheme.ACCENT);
        be.threshold().onChange(v -> thrValue.setText(v + ""));
        var slider = SliderWidget.create(0, TrackerBlockEntity.MAX_THRESHOLD, be.threshold().get());
        slider.setStep(1);
        slider.setThumbSize(5);
        slider.setColors(0xFF081018, InworldTheme.ACCENT_DIM, InworldTheme.ACCENT);
        slider.useStyle(sizeOf(52, 9));
        slider.onValueChanged(v -> {
            be.sendThreshold((int) Math.round(v));
            thrValue.setText((int) Math.round(v) + "");
        });
        thrRow.addWidget(slider);
        thrRow.addWidget(thrValue);
        column.addWidget(thrRow);

        WidgetGroup<Widget> actions = Widgets.row(JustifyContent.FLEX_START, AlignItems.FLEX_START);
        actions.useStyle(columnGap(2));
        actions.addWidget(ChipWidget.of("ping", () -> be.sendAction(TrackerBlockEntity.ACTION_PING)));
        var alert = ChipWidget.of(alertLabel(be), () -> be.sendAction(TrackerBlockEntity.ACTION_TOGGLE_ALERT));
        be.alert().onChange(v -> alert.setLabel(alertLabel(be)));
        actions.addWidget(alert);
        actions.addWidget(ChipWidget.of("scan", () -> toggleExpand(ctx)));
        column.addWidget(actions);

        return column;
    }

    /**
     * Compact face controller: one row — entity count + action chips. The big
     * detail view lives in the expand panel opened by the "scan" chip.
     */
    private static Widget faceContent(InworldPanelContext ctx) {
        TrackerBlockEntity be = ctx.blockEntity(TrackerBlockEntity.class);
        WidgetGroup<Widget> row = Widgets.row(JustifyContent.FLEX_START, AlignItems.CENTER);
        row.useStyle(columnGap(3));
        if (be == null) {
            row.addWidget(TextWidget.of("◈ --").setColor(InworldTheme.TEXT_DIM));
            return row;
        }
        var count = TextWidget.of("◈ " + be.entities().get()).setColor(InworldTheme.ACCENT);
        be.entities().onChange(v -> count.setText("◈ " + v));
        row.addWidget(count);
        row.addWidget(ChipWidget.of("ping", () -> be.sendAction(TrackerBlockEntity.ACTION_PING)));
        row.addWidget(ChipWidget.of("scan", () -> toggleExpand(ctx)));
        return row;
    }

    /**
     * The "scan" chip toggles a detached expand panel: it auto-places into a
     * free world-space area near the anchor and plays the open animation.
     */
    private static void toggleExpand(InworldPanelContext ctx) {
        BlockPos p = ctx.panel().blockPos();
        if (p == null) return;
        Object key = "tracker/expand/" + p;
        if (InworldUi.instance().panel(key) != null) {
            InworldUi.instance().close(key);
            return;
        }
        InworldUi.show(InworldPanelSpec
                .of(key,
                        InworldAnchor.of(p, new Vec3(0.5, 0.9, 0.5)),
                        InworldPlacement.expand(),
                        ScanConsoleWidget::new)
                .title(Component.literal("SCAN//" + shortPos(p)))
                .openAnimation(true)
                .hints("LMB:press", "scan:close"));
    }

    //endregion

    private static String alertLabel(TrackerBlockEntity be) {
        return be.alert().get() ? "alert*" : "alert";
    }

    /** The block face oriented toward the player's position (fallback: where they look). */
    private static Direction sideToward(Player player, BlockPos pos) {
        Vec3 delta = player.position().subtract(Vec3.atCenterOf(pos));
        if (delta.x * delta.x + delta.z * delta.z < 0.05) {
            return Direction.fromYRot(player.getYRot()).getOpposite();
        }
        return Direction.getNearest(delta.x, 0, delta.z);
    }

    private static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
