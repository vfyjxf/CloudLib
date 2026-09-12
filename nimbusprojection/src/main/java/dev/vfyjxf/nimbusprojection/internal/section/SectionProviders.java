package dev.vfyjxf.nimbusprojection.internal.section;

import dev.vfyjxf.nimbusprojection.api.section.EntitySectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionInstance;
import dev.vfyjxf.nimbusprojection.api.section.SectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionRegister;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The common-side section registry — providers in registration order,
 * codecs keyed by type. {@link #collectAll} runs every provider and
 * assigns each result its server-addressable id; it is called on the
 * server for snapshots and on the client for panel structure. Block and
 * entity targets each have their own provider list; the codec registry
 * is shared.
 */
public final class SectionProviders {

    private static final List<SectionProvider<?>> providers = new CopyOnWriteArrayList<>();
    private static final List<EntitySectionProvider<?>> entityProviders = new CopyOnWriteArrayList<>();
    private static final Map<SectionType<?>, StreamCodec<RegistryFriendlyByteBuf, ? extends SectionData>> codecs =
            new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SectionType<?>> byId = new ConcurrentHashMap<>();

    /** The {@link SectionRegister} handed to plugins — folds codec + provider into one call. */
    public static final SectionRegister register = new SectionRegister() {
        @Override
        public <D extends SectionData> void register(
                SectionType<D> type, StreamCodec<RegistryFriendlyByteBuf, D> codec, SectionProvider<D> provider) {
            codecs.put(type, codec);
            byId.put(type.id(), type);
            providers.add(provider);
        }

        @Override
        public <D extends SectionData> void registerEntity(
                SectionType<D> type, StreamCodec<RegistryFriendlyByteBuf, D> codec, EntitySectionProvider<D> provider) {
            codecs.put(type, codec);
            byId.put(type.id(), type);
            entityProviders.add(provider);
        }
    };

    private SectionProviders() {}

    /** {@code "type/index"} — the id ops payloads address and widgets subscribe to. */
    public static String idOf(SectionType<?> type, int index) {
        return type.id() + "/" + index;
    }

    /** Every section at a block position, across all providers, in registration order. */
    public static List<SectionInstance<?>> collectAll(Level level, BlockPos pos) {
        List<SectionInstance<?>> out = new ArrayList<>();
        for (SectionProvider<?> provider : providers) {
            collectInto(provider, level, pos, out);
        }
        return out;
    }

    /** Every section on an entity — entity providers only. */
    public static List<SectionInstance<?>> collectAll(Level level, Entity entity) {
        List<SectionInstance<?>> out = new ArrayList<>();
        for (EntitySectionProvider<?> provider : entityProviders) {
            collectInto(provider, level, entity, out);
        }
        return out;
    }

    /** Snapshot path — resolves the target's shape and dispatches to the matching list. */
    public static List<SectionInstance<?>> collectAll(Level level, SectionTarget target) {
        if (target.pos() != null) return collectAll(level, target.pos());
        Entity entity = target.resolveEntity(level);
        return entity != null ? collectAll(level, entity) : List.of();
    }

    /** The type a section id belongs to — null for an unknown kind or a malformed id. */
    public static @Nullable SectionType<?> typeOf(String sectionId) {
        int slash = sectionId.lastIndexOf('/');
        if (slash < 0) return null;
        ResourceLocation id = ResourceLocation.tryParse(sectionId.substring(0, slash));
        return id != null ? byId.get(id) : null;
    }

    /** Wire codec lookup by type id — the decode side of a snapshot entry. */
    public static @Nullable StreamCodec<RegistryFriendlyByteBuf, ? extends SectionData> codecOf(
            ResourceLocation typeId) {
        SectionType<?> type = byId.get(typeId);
        return type != null ? codecs.get(type) : null;
    }

    @SuppressWarnings("unchecked")
    private static <D extends SectionData> void collectInto(
            SectionProvider<?> provider, Level level, BlockPos pos, List<SectionInstance<?>> out) {
        SectionProvider<D> typed = (SectionProvider<D>) provider;
        List<D> found = typed.collect(level, pos);
        for (int i = 0; i < found.size(); i++) {
            out.add(new SectionInstance<>(idOf(typed.type(), i), typed.type(), found.get(i)));
        }
    }

    @SuppressWarnings("unchecked")
    private static <D extends SectionData> void collectInto(
            EntitySectionProvider<?> provider, Level level, Entity entity, List<SectionInstance<?>> out) {
        EntitySectionProvider<D> typed = (EntitySectionProvider<D>) provider;
        List<D> found = typed.collect(level, entity);
        for (int i = 0; i < found.size(); i++) {
            out.add(new SectionInstance<>(idOf(typed.type(), i), typed.type(), found.get(i)));
        }
    }
}
