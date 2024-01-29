package dev.vfyjxf.cloudlib.data.lang;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class LangKeyProvider extends LanguageProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    private final MutableList<ILangProvider> providers = Lists.mutable.empty();

    public LangKeyProvider(String modid, PackOutput output) {
        super(output, modid, "en_us");
        Type annotationType = Type.getType(LangProvider.class);
        for (ModFileScanData data : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : data.getAnnotations()) {
                if (Objects.equals(annotation.annotationType(), annotationType)) {
                    try {
                        var clazz = Class.forName(annotation.memberName());
                        //if clazz impl ILangProvider
                        if (ILangProvider.class.isAssignableFrom(clazz)) {
                            ILangProvider provider = (ILangProvider) clazz.getDeclaredConstructor().newInstance();
                            providers.add(provider);
                        }
                    } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                             IllegalAccessException |
                             InstantiationException e) {
                        LOGGER.error("Failed to load lang provider: {}", annotation.memberName(), e);
                    }
                }
            }
        }
    }

    @Override
    protected void addTranslations() {
        LangBuilder.builders.forEach(builder -> builder.getDefines().forEach(it -> this.add(it.key(), it.value())));
        for (ILangProvider provider : providers) {
            provider.addTranslations(this);
        }
    }
}
