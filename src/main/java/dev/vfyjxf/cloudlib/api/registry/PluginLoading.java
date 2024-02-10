package dev.vfyjxf.cloudlib.api.registry;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Objects;

public final class PluginLoading {

    private static final Logger LOGGER = LogManager.getLogger();

    public static <T> Collection<T> load(Class<T> pluginInterface) {
        MutableList<T> plugins = new FastList<>();
        Type annotationType = Type.getType(RegisterPlugin.class);
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            Iterable<ModFileScanData.AnnotationData> annotations = scanData.getAnnotations();
            for (ModFileScanData.AnnotationData a : annotations) {
                if (Objects.equals(a.annotationType(), annotationType)) {
                    String memberName = a.memberName();
                    T plugin = getPlugin(memberName, pluginInterface);
                    if (plugin != null) plugins.add(plugin);
                }
            }
        }
        return plugins;
    }

    @Nullable
    private static <T> T getPlugin(String className, Class<T> type) {
        try {
            Class<?> asmClass = Class.forName(className);
            if (!type.isAssignableFrom(asmClass)) return null;
            Class<? extends T> asmInstanceClass = asmClass.asSubclass(type);
            Constructor<? extends T> constructor = asmInstanceClass.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.error("Failed to load plugin: {}", className, e);
        }
        return null;
    }

}
