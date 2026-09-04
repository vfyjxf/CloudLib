package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.util.Checks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link PluginLookup} that discovers plugins via NeoForge's annotation scanning
 * ({@link ModFileScanData}).
 * <p>
 * Scans for classes annotated with the specified annotation (default: {@link PluginMarker})
 * that also implement the target plugin interface.
 *
 * @param <T> the plugin type
 */
public final class AnnotationPluginLookup<T extends ModPlugin> implements PluginLookup<T> {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationPluginLookup.class);

    private final Class<T> pluginClass;
    private final Class<? extends Annotation> annotation;

    private AnnotationPluginLookup(Class<T> pluginClass, Class<? extends Annotation> annotation) {
        this.pluginClass = pluginClass;
        this.annotation = annotation;
    }

    /**
     * Creates a lookup that scans for the default {@link PluginMarker @PluginMarker} annotation.
     */
    public static <T extends ModPlugin> AnnotationPluginLookup<T> of(Class<T> pluginClass) {
        return of(pluginClass, PluginMarker.class);
    }

    /**
     * Creates a lookup that scans for a custom annotation.
     */
    public static <T extends ModPlugin> AnnotationPluginLookup<T> of(Class<T> pluginClass, Class<? extends Annotation> annotation) {
        Checks.checkNotNull(pluginClass, "pluginClass");
        Checks.checkNotNull(annotation, "annotation");
        return new AnnotationPluginLookup<>(pluginClass, annotation);
    }

    @Override
    public Collection<T> findPlugins() {
        List<T> result = new ArrayList<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            scanData.getAnnotatedBy(annotation, ElementType.TYPE)
                    .forEach(annotationData -> {
                        String className = annotationData.memberName();
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (pluginClass.isAssignableFrom(clazz)) {
                                @SuppressWarnings("unchecked")
                                T instance = (T) clazz.getConstructor().newInstance();
                                result.add(instance);
                            }
                        } catch (ClassNotFoundException e) {
                            logger.error("Plugin class not found: {}", className, e);
                        } catch (NoSuchMethodException e) {
                            logger.error("Plugin class {} must have a public no-arg constructor", className, e);
                        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                            logger.error("Failed to instantiate plugin: {}", className, e);
                        }
                    });
        }
        return result;
    }
}
