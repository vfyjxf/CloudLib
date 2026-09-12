package dev.vfyjxf.nimbusprojection.feature.machine;

import dev.vfyjxf.nimbusprojection.Constants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

/**
 * The JEI endpoint: JEI discovers this via {@link JeiPlugin} only when
 * loaded and hands us the runtime — the bridge keeps it for the machine
 * sections' recipe lookup.
 */
@JeiPlugin
public final class NimbusJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Constants.namespace, "jei_bridge");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiBridge.install(jeiRuntime);
    }
}
