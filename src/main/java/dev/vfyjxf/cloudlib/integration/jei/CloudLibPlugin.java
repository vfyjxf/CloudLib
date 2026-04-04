package dev.vfyjxf.cloudlib.integration.jei;

import com.google.common.base.Preconditions;
import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayApi;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

@JeiPlugin
@NotNullByDefault
public class CloudLibPlugin implements IModPlugin {

    public static final ResourceLocation pluginId = ResourceLocation.fromNamespaceAndPath(Constants.modId, "jei_plugin");

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
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                return OverlayApi.instance().exclusionAreas(Minecraft.getInstance().screen).toList();
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        screenHelper = jeiRuntime.getScreenHelper();
    }
}
