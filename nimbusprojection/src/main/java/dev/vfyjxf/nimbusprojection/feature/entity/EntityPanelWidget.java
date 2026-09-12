package dev.vfyjxf.nimbusprojection.feature.entity;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDrag;
import dev.vfyjxf.cloudlib.api.ui.inworld.WorldDragAcceptor;
import dev.vfyjxf.cloudlib.ui.hacker.HackerTheme;
import dev.vfyjxf.cloudlib.ui.widget.ColumnWidget;
import dev.vfyjxf.nimbusprojection.NimbusConfig;
import dev.vfyjxf.nimbusprojection.NimbusKeyMappings;
import dev.vfyjxf.nimbusprojection.api.panel.PanelKeySink;
import dev.vfyjxf.nimbusprojection.api.section.SectionInstance;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import dev.vfyjxf.nimbusprojection.internal.section.SectionContents;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import dev.vfyjxf.nimbusprojection.internal.section.SectionWidgets;
import dev.vfyjxf.nimbusprojection.network.ContainerOpsPayload;
import dev.vfyjxf.nimbusprojection.network.TransferPayload;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The tiered entity panel: a glanceable tier-0 card (name + health) that
 * escalates while the {@code detail} key is held — effects → equipment →
 * debug — and stacks every item section the entity exposes once engaged.
 * <p>
 * All entity data here is vanilla-synced (effects, equipment, attributes);
 * only the {@code IItemHandler} contents ride the section pipeline, so the
 * widget needs no server data channel beyond the shared section snapshot.
 * A dead/unloaded anchor renders a "signal lost" card instead of closing —
 * a pinned panel keeps showing that state until unpinned.
 */
public final class EntityPanelWidget extends WidgetGroup<Widget> implements WorldDragAcceptor, PanelKeySink {

    private static final int rowHeight = 9;
    private static final int panelWidth = 76;

    private final InworldPanelContext ctx;
    private final int entityId;
    private final EntityInfoWidget info;
    private final ColumnWidget sections;

    private int heldTier;
    private long tierAdvanceAt;

    public EntityPanelWidget(InworldPanelContext ctx, int entityId) {
        this.ctx = ctx;
        this.entityId = entityId;
        this.info = new EntityInfoWidget();
        this.sections = ColumnWidget.create(2);
        Entity entity = entity();
        if (entity != null) {
            SectionTarget target = SectionTarget.of(entity);
            for (SectionInstance<?> instance : SectionProviders.collectAll(ctx.level(), entity)) {
                Widget widget = SectionWidgets.create(ctx, target, instance);
                if (widget != null) sections.addWidget(widget);
            }
        }
        addWidget(info);
        addWidget(sections);
    }

    private @Nullable Entity entity() {
        return ctx.level().getEntity(entityId);
    }

    /**
     * The visible detail tier: base 0 dormant / 1 engaged, escalating to 3
     * while {@code detail} is held. Release resets to the base.
     */
    private int tier() {
        int base = ctx.panel().engaged() ? 1 : 0;
        boolean held = NimbusKeyMappings.detail.isDown();
        long now = ctx.level().getGameTime();
        if (held) {
            if (heldTier < 3 && now >= tierAdvanceAt) {
                heldTier++;
                tierAdvanceAt = now + NimbusConfig.detailStepTicks();
            }
            return Math.max(base, heldTier);
        }
        heldTier = base;
        tierAdvanceAt = now;
        return base;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        Entity entity = entity();
        if (entity != null) SectionContents.watch(SectionTarget.of(entity));
        info.entity = entity;
        info.tier = entity != null ? tier() : 0;
        sections.setVisible(ctx.panel().engaged() && entity != null);
        super.renderInternal(canvas, mouseX, mouseY, partialTicks);
    }

    /** A stack dropped onto this panel deposits into the entity's item handler. */
    @Override
    public boolean acceptWorldDrag(WorldDrag drag, InworldPanelContext dropCtx, double sceneX, double sceneY) {
        Entity entity = entity();
        if (entity == null || ctx.channel() == null) return false;
        ctx.channel()
                .sendToServer(new TransferPayload(
                        SectionTarget.of(drag.sourceContainer(), drag.sourceEntity()),
                        drag.sourceSlot(),
                        SectionTarget.of(entity),
                        -1,
                        drag.carried().getCount()));
        return true;
    }

    /**
     * Same quick-store gesture as block containers: {@code X} pushes the
     * held stack into the entity's item handler, {@code Shift+X} dumps the
     * main inventory — chest boats and pack animals take it.
     */
    @Override
    public EventDispatch keyPressed(InputContext input) {
        Entity entity = entity();
        if (!input.isKey(GLFW.GLFW_KEY_X) || entity == null || ctx.channel() == null) return EventDispatch.pass;
        SectionTarget target = SectionTarget.of(entity);
        if (entity.getCapability(Capabilities.ItemHandler.ENTITY) == null) return EventDispatch.pass;
        int op = input.isShiftDown() ? ContainerOpsPayload.insertAll : ContainerOpsPayload.insert;
        ctx.channel()
                .sendToServer(new ContainerOpsPayload(SectionProviders.idOf(SectionTypes.item, 0), op, target, -1, -1));
        return EventDispatch.consumed;
    }

    /**
     * The tiered info block: name + vitals always, deeper rows gated by the
     * escalated tier. Renders "signal lost" while the anchor is dead.
     */
    private final class EntityInfoWidget extends Widget {

        private @Nullable Entity entity;
        private int tier;

        {
            onMount((scene, context, handle) -> scene.layoutTree()
                    .setMeasureFunc(nodeId(), (style, space) -> new FloatSize(panelWidth, measuredHeight())));
        }

        private int measuredHeight() {
            int h = 18; // name + health bar
            if (tier >= 1) h += flagsRows() * rowHeight;
            if (tier >= 2) h += 16; // equipment row
            if (tier >= 3) h += rowHeight;
            return h;
        }

        private int flagsRows() {
            if (!(entity instanceof LivingEntity living)) return 0;
            int rows = Math.min(living.getActiveEffects().size(), NimbusConfig.maxEffectsShown());
            if (entity instanceof TamableAnimal || entity instanceof AgeableMob || entity instanceof AbstractHorse) {
                rows++;
            }
            return rows;
        }

        @Override
        protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
            if (entity == null) {
                canvas.text("signal lost", 2, 4, HackerTheme.textDim);
                return;
            }
            var font = Minecraft.getInstance().font;
            canvas.text(font.plainSubstrByWidth(entity.getName().getString(), panelWidth - 4), 2, 2, HackerTheme.text);
            int y = rowHeight + 2;
            if (entity instanceof LivingEntity living) {
                renderVitals(canvas, living, y);
                y += 8;
                if (tier >= 1) y = renderEffectsAndFlags(canvas, living, y);
                if (tier >= 2) y = renderEquipment(canvas, living, y);
                if (tier >= 3) renderDebug(canvas, y);
            } else {
                renderNonLiving(canvas, y);
            }
        }

        /** Health bar + armor — the Jade tier-0 row. */
        private void renderVitals(SceneCanvas canvas, LivingEntity living, int y) {
            float health = living.getHealth();
            float max = Math.max(living.getMaxHealth(), 0.001f);
            int bar = Math.round((panelWidth - 8) * Math.min(health / max, 1f));
            canvas.fill(2, y, panelWidth - 8, 4, 0x55302620);
            canvas.fill(2, y, bar, 4, health / max > 0.3f ? 0xFF4CAF50 : 0xFFD84315);
            canvas.text(String.format("%.1f/%d", health, (int) max), 4, y + 6, HackerTheme.textDim);
            int armor = living.getArmorValue();
            if (armor > 0) {
                String label = "⛨" + armor;
                canvas.text(label, panelWidth - canvas.font().width(label) - 2, y + 6, HackerTheme.textDim);
            }
        }

        private int renderEffectsAndFlags(SceneCanvas canvas, LivingEntity living, int y) {
            int shown = 0;
            int limit = NimbusConfig.maxEffectsShown();
            for (MobEffectInstance effect : living.getActiveEffects()) {
                if (shown >= limit) break;
                canvas.text(
                        effect.getEffect().value().getDisplayName().getString() + " " + effect.getDuration() / 20 + "s",
                        2,
                        y,
                        HackerTheme.textDim);
                y += rowHeight;
                shown++;
            }
            String flags = flags(living);
            if (!flags.isEmpty()) {
                canvas.text(flags, 2, y, HackerTheme.textDim);
                y += rowHeight;
            }
            return y;
        }

        private String flags(Entity entity) {
            StringBuilder sb = new StringBuilder();
            if (entity instanceof TamableAnimal tamed && tamed.isTame()) sb.append("tamed");
            else if (entity instanceof AbstractHorse horse && horse.isTamed()) sb.append("tamed");
            if (entity instanceof AgeableMob ageable && ageable.isBaby()) appendFlag(sb, "baby");
            return sb.toString();
        }

        private void appendFlag(StringBuilder sb, String flag) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(flag);
        }

        /** Held + worn items as a single icon row. */
        private int renderEquipment(SceneCanvas canvas, LivingEntity living, int y) {
            int x = 2;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = living.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                canvas.renderItemIcon(stack, x, y);
                x += 14;
            }
            return y + (x > 2 ? 16 : 0);
        }

        /** Type id / AI target / profession — the debug tier. */
        private void renderDebug(SceneCanvas canvas, int y) {
            StringBuilder line = new StringBuilder(
                    BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath());
            if (entity instanceof Mob mob && mob.getTarget() != null) {
                line.append(" → ").append(mob.getTarget().getName().getString());
            }
            if (entity instanceof Villager villager) {
                line.append(" · ")
                        .append(BuiltInRegistries.VILLAGER_PROFESSION
                                .getKey(villager.getVillagerData().getProfession())
                                .getPath());
            }
            canvas.text(canvas.font().plainSubstrByWidth(line.toString(), panelWidth - 4), 2, y, HackerTheme.textDim);
        }

        /** Item frames show the framed item; other non-living get type + count info. */
        private void renderNonLiving(SceneCanvas canvas, int y) {
            if (entity instanceof ItemFrame frame && !frame.getItem().isEmpty()) {
                canvas.renderItemIcon(frame.getItem(), 2, y);
                canvas.text(
                        canvas.font()
                                .plainSubstrByWidth(
                                        frame.getItem().getHoverName().getString(), panelWidth - 22),
                        20,
                        y + 4,
                        HackerTheme.textDim);
            }
        }
    }
}
