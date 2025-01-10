package dev.vfyjxf.cloudlib.api.ui.text;

import dev.vfyjxf.cloudlib.api.ui.Tooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

//TODO:Implement RichTooltipComponent
public class RichTooltipComponent implements TooltipComponent, ClientTooltipComponent {

    private MutableList<Object> entries = Lists.mutable.empty();
    private int height;

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getWidth(Font font) {
        return 0;
    }

    public Tooltip toTooltip() {
        return RichTooltip.of(this);
    }

}
