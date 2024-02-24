package dev.vfyjxf.cloudlib.integration.internal.ui;

import dev.vfyjxf.cloudlib.api.annotations.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.ui.IArea;
import dev.vfyjxf.cloudlib.api.ui.IGuiAreas;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.gui.recipes.RecipesGui;

import java.util.Collection;

@NotNullByDefault
public class JeiGuiHandlerAdapter implements IGuiAreas<RecipesGui> {


    @Override
    public IArea mainArea(RecipesGui screen) {
        IGuiProperties properties = screen.getProperties();
        if (properties == null) return null;
        return IArea.of(properties.getGuiLeft(), properties.getGuiTop(), properties.getGuiXSize(), properties.getGuiYSize());
    }

    @Override
    public Collection<IArea> extraAreas(RecipesGui screen) {
        return null;
    }
}
