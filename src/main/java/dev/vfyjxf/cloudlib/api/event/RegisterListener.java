package dev.vfyjxf.cloudlib.api.event;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Register a static field object to a {@link net.neoforged.bus.api.IEventBus}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegisterListener {

    /**
     * Specify targets to load this event subscriber on. Can be used to avoid loading Client specific events on a dedicated server, for example.
     *
     * @return an array of Dist to load this event subscriber on
     */
    Dist[] dist() default {Dist.CLIENT, Dist.DEDICATED_SERVER};

    boolean modBus() default true;

}
