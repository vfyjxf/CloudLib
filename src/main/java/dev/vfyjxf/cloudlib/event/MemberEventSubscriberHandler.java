package dev.vfyjxf.cloudlib.event;

import dev.vfyjxf.cloudlib.api.event.RegisterListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MemberEventSubscriberHandler {

    private static final Type annotationType = Type.getType(RegisterListener.class);

    public static void registerForMod(Module module, ModContainer container, IEventBus modBus) {
        IModFileInfo info = ModList.get().getModFileById(container.getModId());
        if (info == null) throw new IllegalStateException("Can't find mod file info of mod: " + container.getModId());
        ModFileScanData scanResult = info.getFile().getScanResult();
        var annotations = scanResult.getAnnotations()
                .stream()
                .filter(it -> annotationType.equals(it.annotationType()))
                .toList();

        try {
            for (var annotation : annotations) {
                var dist = getDist(annotation);
                if (!dist.contains(FMLEnvironment.dist)) continue;

                var clazz = Class.forName(annotation.clazz().getClassName(), true, module.getClassLoader());
                var field = clazz.getDeclaredField(annotation.memberName());
                var fieldModifier = field.getModifiers();
                if (!Modifier.isFinal(fieldModifier) || !Modifier.isStatic(fieldModifier)) {
                    throw new IllegalStateException("Field " + annotation.memberName() + " is not static or final");
                }
                field.setAccessible(true);
                Object value = field.get(null);
                boolean registerToModBus = (boolean) annotation.annotationData().getOrDefault("modBus", true);
                if (registerToModBus) modBus.register(value);
                else NeoForge.EVENT_BUS.register(value);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("unchecked")
    private static Set<Dist> getDist(ModFileScanData.AnnotationData annotationData) {
        var rawDistData = (List<ModAnnotation.EnumHolder>) annotationData.annotationData().get("dist");
        if (rawDistData == null) return EnumSet.allOf(Dist.class);
        else return rawDistData.stream()
                .map(ModAnnotation.EnumHolder::value)
                .map(Dist::valueOf)
                .collect(Collectors.toSet());
    }

}
