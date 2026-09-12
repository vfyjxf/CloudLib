package dev.vfyjxf.nimbusprojection.feature.machine;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lazy handle on JEI's runtime — populated by {@link NimbusJeiPlugin} when
 * JEI is loaded. Section widgets route "show recipes" affordances through
 * {@link #showRecipes}/{@link #showUses}; both no-op without JEI so the
 * feature degrades to plain readouts.
 */
public final class JeiBridge {

    private static @Nullable IJeiRuntime runtime;

    private JeiBridge() {}

    static void install(IJeiRuntime rt) {
        runtime = rt;
    }

    public static boolean available() {
        return runtime != null;
    }

    /** JEI "U" — recipes that produce this stack. */
    public static void showRecipes(ItemStack stack) {
        show(stack, RecipeIngredientRole.OUTPUT);
    }

    /** JEI "R" — recipes that consume this stack. */
    public static void showUses(ItemStack stack) {
        show(stack, RecipeIngredientRole.INPUT);
    }

    private static void show(ItemStack stack, RecipeIngredientRole role) {
        IJeiRuntime rt = runtime;
        if (rt == null || stack.isEmpty()) return;
        IFocus<ItemStack> focus =
                rt.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
        rt.getRecipesGui().show(List.of(focus));
    }
}
