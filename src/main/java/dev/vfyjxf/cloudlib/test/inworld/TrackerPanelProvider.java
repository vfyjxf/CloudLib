package dev.vfyjxf.cloudlib.test.inworld;

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

/**
 * Demo provider for {@link TrackerBlockEntity}: per block it offers
 * <ul>
 *   <li>a floating stat panel above the block (entity count, nearest, ping
 *       count, action chips driving the reversed channel);</li>
 *   <li>a face panel on the side facing the player — the placement is
 *       re-offered each pass, so the console face tracks the player;</li>
 *   <li>a follow-tag on every living entity in range, anchored to the entity
 *       itself and re-created/removed as entities come and go.</li>
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
                    .hints("LMB:press", "R:inspect"));

            //compact face controller tracks the player horizontally
            Direction side = sideToward(context.player(), p);
            sink.offer(InworldPanelSpec
                    .of("tracker/face/" + p,
                            InworldAnchor.of(p),
                            InworldPlacement.face(side, 0.5, 0.5, 64),
                            TrackerPanelProvider::faceContent));

            //a follow-tag per living entity in range of the tracker
            var box = AABB.ofSize(Vec3.atCenterOf(p), TrackerBlockEntity.RANGE * 2.0, 12, TrackerBlockEntity.RANGE * 2.0);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (entity == context.player()) continue;
                sink.offer(InworldPanelSpec
                        .of("tracker/ent/" + entity.getId(),
                                InworldAnchor.ofEntity(entity.getId(), new Vec3(0, entity.getBbHeight() + 0.35, 0)),
                                InworldPlacement.follow(0, 0),
                                c -> entityTag(entity))
                        .interactive(false)
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

        var entities = TextWidget.of("ents " + be.entities().get()).setColor(InworldTheme.TEXT);
        be.entities().onChange(v -> entities.setText("ents " + v));
        column.addWidget(entities);

        var nearest = TextWidget.of("nearest " + be.nearest().get()).setColor(InworldTheme.TEXT_DIM);
        be.nearest().onChange(v -> nearest.setText("nearest " + v));
        column.addWidget(nearest);

        var pings = TextWidget.of("pings " + be.pings().get()).setColor(InworldTheme.TEXT_DIM);
        be.pings().onChange(v -> pings.setText("pings " + v));
        column.addWidget(pings);

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
     * free screen area near the anchor and plays the open animation.
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
                        c -> new RadarWidget())
                .title(Component.literal("SCAN//" + shortPos(p)))
                .openAnimation(true)
                .hints("LMB:press", "scan:close"));
    }

    private static Widget entityTag(LivingEntity entity) {
        var text = TextWidget.of(tagText(entity)).setColor(InworldTheme.ACCENT);
        text.setTickable(true);
        text.onTick(() -> text.setText(tagText(entity)));
        return text;
    }

    //endregion

    private static String tagText(LivingEntity entity) {
        return "◇ " + entity.getName().getString() + " " + (int) Math.ceil(entity.getHealth()) + "hp";
    }

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
