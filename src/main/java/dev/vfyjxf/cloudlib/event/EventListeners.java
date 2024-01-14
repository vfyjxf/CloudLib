package dev.vfyjxf.cloudlib.event;


import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.function.Consumer;

/**
 * Lambda event registration for forge.
 */
public final class EventListeners {

    private EventListeners() {

    }

    public static <T extends Event> void register(Class<T> type, Consumer<T> listener) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, listener);
    }


}
