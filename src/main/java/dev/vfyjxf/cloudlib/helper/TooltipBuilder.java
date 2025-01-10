package dev.vfyjxf.cloudlib.helper;

import dev.vfyjxf.cloudlib.api.ui.text.RichTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.List;
import java.util.Objects;

public class TooltipBuilder {

    private final MutableList<Object> entries = Lists.mutable.empty();

    public static TooltipBuilder create() {
        return new TooltipBuilder();
    }

    public static TooltipBuilder from(List<Component> components) {
        TooltipBuilder builder = new TooltipBuilder();
        components.forEach(builder::text);
        return builder;
    }

    public static TooltipBuilder of(List<String> components) {
        TooltipBuilder builder = new TooltipBuilder();
        components.forEach(builder::text);
        return builder;
    }

    public TooltipBuilder translatable(String key) {
        this.entries.add(Component.translatable(key));
        return this;
    }

    public TooltipBuilder translatable(String key, Object... args) {
        this.entries.add(Component.translatable(key, args));
        return this;
    }

    public TooltipBuilder text(String text) {
        this.entries.add(Component.literal(text));
        return this;
    }

    public TooltipBuilder text(String text, Object... args) {
        this.entries.add(Component.translatable(text, args));
        return this;
    }

    public TooltipBuilder text(Component text) {
        this.entries.add(text);
        return this;
    }

    public TooltipBuilder component(TooltipComponent component) {
        this.entries.add(component);
        return this;
    }

    public List<ClientTooltipComponent> make() {
        return entries.collect(each -> {
            if (each instanceof Component component) {
                return ClientTooltipComponent.create(component.getVisualOrderText());
            }
            if (each instanceof TooltipComponent tooltipComponent) {
                return ClientTooltipComponent.create(tooltipComponent);
            }
            return null;
        }).reject(Objects::isNull);
    }

    public RichTooltip build() {
        return RichTooltip.from(entries.collect(each -> {
            if (each instanceof Component component) {
                return RichTooltip.of(component);
            }
            if (each instanceof TooltipComponent component) {
                return RichTooltip.of(component);
            }
            return null;
        }).reject(Objects::isNull));
    }

}
