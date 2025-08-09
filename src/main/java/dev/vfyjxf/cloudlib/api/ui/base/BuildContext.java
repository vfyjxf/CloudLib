package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.data.AttachableDataContainer;
import dev.vfyjxf.cloudlib.api.data.DataAttachable;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.UIContext;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.NotNull;

public final class BuildContext implements DataAttachable {

    //region attachable
    private final AttachableDataContainer attachableDataContainer = new AttachableDataContainer();
    //endregion


    //region builtin context

    private final UIContext screenContext = UIContext.current();

    //endregion

    public UIContext screenContext() {
        return screenContext;
    }

    public FloatPos mousePos() {
        return screenContext.mousePos();
    }


    public Font font() {
        return screenContext.font();
    }

    public int screenWidth() {
        return screenContext.width();
    }

    public int screenHeight() {
        return screenContext.height();
    }


    @Override
    public @NotNull AttachableDataContainer attachableDataContainer() {
        return attachableDataContainer;
    }
}
