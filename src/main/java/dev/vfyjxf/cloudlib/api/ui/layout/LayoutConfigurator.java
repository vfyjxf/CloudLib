package dev.vfyjxf.cloudlib.api.ui.layout;

import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.widget.WidgetGroup;
import org.appliedenergistics.yoga.YogaNode;

import java.util.function.Consumer;

/**
 * Extra Configuration for {@link Modifier} to configure {@link WidgetGroup}'s {@link WidgetGroup#children()}
 */
@FunctionalInterface
public interface LayoutConfigurator extends Consumer<YogaNode> {

    static <T extends WidgetGroup<?>> LayoutConfigurator configureEach(Modifier modifier) {
        return modifier::apply;
    }

    static <T extends WidgetGroup<?>> LayoutConfigurator configureEach(Consumer<YogaNode> configurator) {
        return node -> {
            for (YogaNode child : node.getChildren()) {
                configurator.accept(child);
            }
        };
    }

    default LayoutConfigurator then(LayoutConfigurator after) {
        return node -> {
            this.accept(node);
            after.accept(node);
        };
    }

    default void configure(WidgetGroup<?> group) {
        YogaNode node = group.yogaNode();
        this.accept(node);
    }

    void accept(YogaNode parent);
}
