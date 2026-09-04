package dev.vfyjxf.cloudlib.api.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Default annotation for marking a class as a plugin, used by {@link AnnotationPluginLookup}.
 * <p>
 * The annotated class must implement a {@link ModPlugin} subinterface
 * and have a public no-arg constructor. The plugin ID is provided by
 * {@link ModPlugin#pluginId()}, not by annotation attributes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginMarker {
}
