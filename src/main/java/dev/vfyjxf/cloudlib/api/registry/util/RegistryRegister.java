package dev.vfyjxf.cloudlib.api.registry.util;

import net.minecraft.core.Registry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class RegistryRegister {

    private final MutableList<Registry<?>> registries = Lists.mutable.empty();

    public <T> Registry<T> register(Registry<T> registry) {
        registries.add(registry);
        return registry;
    }

    public <T> Registry<T> register(RegistryBuilder<T> builder) {
        Registry<T> registry = builder.create();
        return register(registry);
    }

    @SubscribeEvent
    public void register(NewRegistryEvent event) {
        for (Registry<?> registry : registries) {
            event.register(registry);
        }
    }

}
