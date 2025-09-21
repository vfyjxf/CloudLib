package dev.vfyjxf.cloudlib.api.registry.util;

import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.util.Locations;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.collections.api.list.MutableList;


/**
 * A register for registering simple object.
 */
public class SimpleRegister<T> {

    public static <T> SimpleRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new SimpleRegister<>(registryKey, namespace);
    }

    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;

    private final MutableList<Pair<ResourceLocation, T>> entries = MutableLists.empty();

    private SimpleRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    //region entry

    public T register(ResourceLocation location, T entry) {
        this.entries.add(Pair.of(location, entry));
        return entry;
    }

    public T register(String id, T entry) {
        this.entries.add(Pair.of(Locations.create(namespace, id), entry));
        return entry;
    }

    //endregion

    public void submitToBus(IEventBus modBus) {
        modBus.addListener(this::onRegisterEvent);
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void onRegisterEvent(RegisterEvent event) {
        if (entries.isEmpty() || event.getRegistryKey() != registryKey) return;
        Registry<T> registry = (Registry<T>) event.getRegistry();
        for (var entry : entries) {
            Registry.register(registry, entry.getLeft(), entry.getRight());
        }
    }

}
