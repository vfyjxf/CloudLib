package dev.vfyjxf.cloudlib.api.module;

import dev.vfyjxf.cloudlib.event.MemberEventSubscriberHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

public abstract class ModEntryPoint {

    public ModEntryPoint(ModContainer container, IEventBus eventBus) {
        Module module = this.getClass().getModule();
        MemberEventSubscriberHandler.registerForMod(module, container, eventBus);
    }

}
