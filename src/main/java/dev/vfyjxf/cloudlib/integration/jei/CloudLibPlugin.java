package dev.vfyjxf.cloudlib.integration.jei;

import com.google.common.base.Preconditions;
import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IScreenHelper;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
@NotNullByDefault
public class CloudLibPlugin implements IModPlugin {

    public static final ResourceLocation pluginId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "jei_plugin");

    public static IScreenHelper getScreenHelper() {
        Preconditions.checkState(screenHelper != null, "ScreenHelper is not initialized yet.");
        return screenHelper;
    }

    private static IScreenHelper screenHelper;

    @Override
    public ResourceLocation getPluginUid() {
        return pluginId;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        screenHelper = jeiRuntime.getScreenHelper();
    }
}
