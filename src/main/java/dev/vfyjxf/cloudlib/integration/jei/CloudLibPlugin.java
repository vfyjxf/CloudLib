package dev.vfyjxf.cloudlib.integration.jei;

import com.google.common.base.Preconditions;
import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.annotations.NotNullByDefault;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IScreenHelper;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
@NotNullByDefault
public class CloudLibPlugin implements IModPlugin {

    public static IScreenHelper getScreenHelper() {
        Preconditions.checkState(screenHelper != null, "ScreenHelper is not initialized yet.");
        return screenHelper;
    }

    private static IScreenHelper screenHelper;

    @Override
    public ResourceLocation getPluginUid() {
        return Constants.JEI_PLUGIN_UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        screenHelper = jeiRuntime.getScreenHelper();
    }
}
