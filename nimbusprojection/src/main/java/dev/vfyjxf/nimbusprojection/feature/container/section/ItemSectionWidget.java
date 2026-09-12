package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.ui.widget.ContainerGridWidget;
import dev.vfyjxf.nimbusprojection.api.section.SectionView;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetFactory;
import dev.vfyjxf.nimbusprojection.network.ContainerOpsPayload;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The item grid section — a {@link ContainerGridWidget} fed by the
 * section snapshot cache instead of its own query pipeline. Slot clicks
 * carry the section id so the server validates the address, not the
 * client's layout. Works for block and entity targets alike.
 */
public final class ItemSectionWidget {

    private ItemSectionWidget() {}

    public static final SectionWidgetFactory<ItemSectionData> factory = ItemSectionWidget::create;

    private static Widget create(SectionView<ItemSectionData> view) {
        return new ContainerGridWidget(
                () -> view.target().pos(),
                () -> stacksOf(view),
                () -> view.target().entity(),
                (slot, button, input) -> {
                    if (view.panel().channel() == null) return;
                    boolean shift = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
                    int op =
                            input.isLeftClick() && shift ? ContainerOpsPayload.extractAll : ContainerOpsPayload.extract;
                    int count = input.isRightClick() ? 1 : -1;
                    view.panel()
                            .channel()
                            .sendToServer(new ContainerOpsPayload(view.id(), op, view.target(), slot, count));
                });
    }

    private static @Nullable List<ItemStack> stacksOf(SectionView<ItemSectionData> view) {
        ItemSectionData live = view.live();
        return live != null ? live.stacks() : null;
    }
}
