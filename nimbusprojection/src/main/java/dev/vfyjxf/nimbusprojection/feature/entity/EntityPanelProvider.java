package dev.vfyjxf.nimbusprojection.feature.entity;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.nimbusprojection.NimbusConfig;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import dev.vfyjxf.nimbusprojection.api.provider.PanelProvider;
import dev.vfyjxf.nimbusprojection.api.provider.PanelSink;
import dev.vfyjxf.nimbusprojection.api.provider.ProviderContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The entity feature's provider: every eligible entity inside the
 * soft-focus cone gets a Follow panel keyed {@code entity/<id>} — a
 * glanceable tier-0 card (name + health) that expands into the world
 * hologram on the interact key and pins to the screen edge on the pin key.
 * <p>
 * Keys use the entity's session-stable network id and are
 * {@code sharedDomain} — two players looking at the same mob share the
 * panel identity, so presence relays between them.
 */
public final class EntityPanelProvider implements PanelProvider {

    /** Same ~31° cone as the container scan — wherever the interact key can reach. */
    private static final double coneCosEnter = Math.cos(Math.toRadians(31));

    /** The panel anchors just above the nameplate space. */
    private static final double headroom = 0.35;

    @Override
    public void provide(ProviderContext context, PanelSink sink) {
        if (!NimbusConfig.entitiesEnabled()) return;
        double reach = NimbusConfig.entityReach();
        Vec3 eye = context.player().getEyePosition();
        Vec3 look = context.player().getLookAngle().normalize();
        AABB box = AABB.ofSize(eye, reach * 2, reach * 2, reach * 2);
        for (Entity entity : context.level().getEntities(context.player(), box, EntityPanelProvider::eligible)) {
            Vec3 center = entity.getBoundingBox().getCenter();
            Vec3 to = center.subtract(eye);
            double dist = to.length();
            if (dist < 0.5 || dist > reach) continue;
            if (to.normalize().dot(look) < coneCosEnter) continue;
            sink.offer(PanelSpec.of(
                            keyOf(entity),
                            InworldAnchor.ofEntity(entity.getId(), new Vec3(0, entity.getBbHeight() + headroom, 0)),
                            Presentation.follow(8, -14),
                            ctx -> new EntityPanelWidget(ctx, entity.getId()))
                    .title(entity.getName())
                    .onDemand(false)
                    .floatingOnIdle(true)
                    .hints("V:expand"));
        }
    }

    /** Session-stable shared-domain key — same entity, same identity across clients. */
    public static PanelKey keyOf(Entity entity) {
        return PanelKey.of("nimbusprojection", "entity/" + entity.getId());
    }

    /**
     * Living entities plus the non-living interactables (boats, minecarts,
     * hanging entities — item frames, paintings). Never the viewer themself.
     */
    private static boolean eligible(Entity entity) {
        if (!entity.isAlive()) return false;
        return entity instanceof LivingEntity
                || entity instanceof HangingEntity
                || entity instanceof Boat
                || entity instanceof AbstractMinecart;
    }
}
