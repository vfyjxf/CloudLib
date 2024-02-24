package CloudLib.extensions.java.lang.Class;

import dev.vfyjxf.cloudlib.api.annotations.NotNullByDefault;
import dev.vfyjxf.cloudlib.utils.Singletons;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import org.jetbrains.annotations.Nullable;

@Extension
@NotNullByDefault
public class ClassExtension {

    public static <T> T getInstance(@This Class<T> self) {
        return Singletons.get(self);
    }

    @Nullable
    public static <T> T getInstanceNullable(@This Class<T> self) {
        return Singletons.getNullable(self);
    }

    public static <T> void attachInstance(@This Class<T> self, T instance) {
        Singletons.attachInstance(self, instance);
    }

}