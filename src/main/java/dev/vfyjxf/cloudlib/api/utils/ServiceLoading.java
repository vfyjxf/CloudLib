package dev.vfyjxf.cloudlib.api.utils;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * Another spi system using neoforge {@link net.neoforged.neoforgespi.language.ModFileScanData} system.
 * <p>
 * Only supports mod classes(class in mod jar).
 * <p>
 * <b>Note:</b> Service Class must be annotated with {@link ModService} and it must hava a default constructor.
 */
public class ServiceLoading {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Load service instances from mods without caching.
     *
     * @param serviceInterface service interface
     * @param <T>              service interface type
     * @return a list of service instances
     */
    public static <T> MutableList<T> load(Class<T> serviceInterface) {
        MutableList<T> services = new FastList<>();
        Type annotationType = Type.getType(ModService.class);
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();
            for (ModFileScanData.AnnotationData a : annotations) {
                if (Objects.equals(a.annotationType(), annotationType)) {
                    String memberName = a.memberName();
                    T plugin = createServiceInstance(memberName, serviceInterface);
                    if (plugin != null) services.add(plugin);
                }
            }
        }
        return services;
    }

    @Nullable
    private static <T> T createServiceInstance(String className, Class<T> type) {
        try {
            Class<?> asmClass = Class.forName(className);
            if (!type.isAssignableFrom(asmClass)) return null;
            Class<? extends T> asmInstanceClass = asmClass.asSubclass(type);
            Constructor<? extends T> constructor = asmInstanceClass.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.error("Failed to load service: {}", className, e);
        }
        return null;
    }

}
