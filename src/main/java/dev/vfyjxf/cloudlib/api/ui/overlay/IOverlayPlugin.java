package dev.vfyjxf.cloudlib.api.ui.overlay;

import com.google.gson.JsonObject;
import dev.vfyjxf.cloudlib.api.ui.IRenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface IOverlayPlugin {

    ResourceLocation getPluginUid();

    /**
     * @return An icon of this plugin.
     */
    IRenderableTexture getIcon();

    void init(IWidgetGroup<IWidget> mainPanel);

    default void deserialize(JsonObject tag) {

    }

    @Nullable
    default JsonObject serialize() {
        return null;
    }

}
