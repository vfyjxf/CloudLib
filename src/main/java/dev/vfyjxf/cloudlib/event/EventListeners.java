package dev.vfyjxf.cloudlib.event;


import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Consumer;

/**
 * Lambda event registration for forge.
 */
public final class EventListeners {

    private EventListeners() {

    }

    public static <T extends Event> void register(Class<T> type, Consumer<T> listener) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, listener);
    }

    public static <T extends Event> void register(Class<T> type, EventPriority priority, Consumer<T> listener) {
        NeoForge.EVENT_BUS.addListener(priority, false, type, listener);
    }

    public static void unregister(Object listener) {
        NeoForge.EVENT_BUS.unregister(listener);
    }


}
