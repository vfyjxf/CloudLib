package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.nimbusprojection.internal.NimbusPalette;
import dev.vfyjxf.nimbusprojection.NimbusConfig;
import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionView;
import dev.vfyjxf.nimbusprojection.api.section.SectionWidgetFactory;
import dev.vfyjxf.nimbusprojection.feature.container.section.ItemSectionData;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import dev.vfyjxf.nimbusprojection.feature.machine.JeiBridge;
import dev.vfyjxf.nimbusprojection.internal.section.SectionContents;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The processing face: a progress bar plus a fuel bar under it. Clicking
 * the section opens JEI for the resolved smelt output (or the input's
 * uses when no recipe resolves) — the spec's recipe-lookup affordance.
 */
public final class ProgressSectionWidget extends Widget {

    public static final SectionWidgetFactory<ProgressSectionData> factory =
            view -> NimbusConfig.machineProgress() ? new ProgressSectionWidget(view) : null;

    private static final int width = 9 * 18;
    private static final int barHeight = 8;
    private static final int height = barHeight * 2 + 4;
    private static final int barBg = 0x33221B10;
    private static final int barBorder = 0x66F2C04D;
    private static final int progressFill = 0xCC4CAF50;
    private static final int fuelFill = 0xCCD84315;

    private static final List<RecipeType<? extends net.minecraft.world.item.crafting.Recipe<SingleRecipeInput>>>
            cookingTypes =
                    List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.BLASTING, RecipeType.CAMPFIRE_COOKING);

    private final SectionView<ProgressSectionData> view;

    private ProgressSectionWidget(SectionView<ProgressSectionData> view) {
        this.view = view;
        onMount((scene, context, handle) ->
                scene.layoutTree().setMeasureFunc(nodeId(), (style, space) -> new FloatSize(width, height)));
        onMouseClick((input, clickCount, context) -> {
            if (input.isLeftClick() && clickCount == 1) {
                showRecipes();
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });
    }

    private void showRecipes() {
        if (!JeiBridge.available()) return;
        ItemStack input = firstInput();
        if (input.isEmpty()) return;
        ItemStack output = smeltOutput(view.panel().level(), input);
        if (!output.isEmpty()) JeiBridge.showRecipes(output);
        else JeiBridge.showUses(input);
    }

    /** The companion item section's slot 0 — the machine's input by convention. */
    private ItemStack firstInput() {
        SectionData data = SectionContents.latest(view.target(), SectionProviders.idOf(SectionTypes.item, 0));
        return data instanceof ItemSectionData items && !items.stacks().isEmpty()
                ? items.stacks().get(0)
                : ItemStack.EMPTY;
    }

    /** Client recipe manager — resolve what this input smelts into. */
    private static ItemStack smeltOutput(Level level, ItemStack input) {
        SingleRecipeInput in = new SingleRecipeInput(input);
        for (var type : cookingTypes) {
            var found = level.getRecipeManager().getRecipeFor(type, in, level);
            if (found.isPresent()) {
                return found.get().value().assemble(in, level.registryAccess());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        ProgressSectionData live = view.live();
        if (live == null) {
            canvas.text("···", 4, 4, NimbusPalette.textDim);
            return;
        }
        bar(canvas, 0, live.progress(), live.total(), progressFill, live.progress() + "/" + live.total() + "t");
        bar(canvas, barHeight + 4, live.fuel(), live.fuelTotal(), fuelFill, "fuel " + live.fuel());
    }

    private void bar(SceneCanvas canvas, int y, int value, int max, int color, String label) {
        canvas.strokeRect(0, y, width, barHeight, barBorder);
        if (max > 0 && value > 0) {
            int w = Math.max(1, Math.round((width - 2) * (value / (float) max)));
            canvas.fill(1, y + 1, w, barHeight - 2, color);
        }
        canvas.text(label, 4, y, 0xFFFFFFFF);
    }
}
