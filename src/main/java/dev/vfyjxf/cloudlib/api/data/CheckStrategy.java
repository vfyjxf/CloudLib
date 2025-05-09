package dev.vfyjxf.cloudlib.api.data;

import dev.vfyjxf.cloudlib.api.data.snapshot.Observable;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface CheckStrategy<T> {

    @Contract(pure = true)
    boolean matches(T previous, T current);

    default boolean notMatches(T previous, T current) {
        return !matches(previous, current);
    }

    static <T> CheckStrategy<T> neverEqual() {
        return (a, b) -> false;
    }

    static <T> CheckStrategy<T> identity() {
        return (a, b) -> a == b;
    }

    static <T> CheckStrategy<T[]> array(CheckStrategy<T> elementStrategy) {
        return (a, b) -> {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            for (int i = 0; i < a.length; i++) {
                if (!elementStrategy.matches(a[i], b[i])) return false;
            }
            return true;
        };
    }

    /**
     * @param <T> a nullable type
     * @return null safe hashcode comparison
     */
    static <T extends @Nullable Object> CheckStrategy<T> hashcode() {
        return (a, b) -> (a == null || b == null) ? a == b : a.hashCode() == b.hashCode();
    }

    /**
     * @param <T> a nullable type
     * @return null safe comparison
     */
    static <T extends @Nullable Object> CheckStrategy<T> equals() {
        return Objects::equals;
    }

    /**
     * Same as {@link #equals()} but the name is better for primitive types
     */
    static <T extends @Nullable Object> CheckStrategy<T> primitive() {
        return Objects::equals;
    }

    static <T extends Observable> CheckStrategy<T> observable() {
        return (previous, current) -> {
            if (previous == current) return true;
            if (previous == null || current == null) return false;
            return current.changed();
        };
    }

    /**
     * matches {@link ItemStack#getCount()} and {@link ItemStack#getItem()} and {@link ItemStack#getComponents()}
     */
    CheckStrategy<ItemStack> sameItemStack = ItemStack::matches;

    /**
     * matches {@link ItemStack#getItem()}
     */
    CheckStrategy<ItemStack> sameItem = ItemStack::isSameItem;

    /**
     * matches {@link ItemStack#getItem()} and {@link ItemStack#getCount()}
     */
    CheckStrategy<ItemStack> sameItemAndCount = (a, b) -> ItemStack.isSameItem(a, b) && a.getCount() == b.getCount();

    /**
     * matches {@link ItemStack#getItem()} and {@link ItemStack#getComponents()}
     */
    CheckStrategy<ItemStack> sameItemAndComponent = ItemStack::isSameItemSameComponents;

    /**
     * matches {@link FluidStack#getAmount()} and {@link FluidStack#getFluid()} and {@link FluidStack#getComponents()}
     */
    CheckStrategy<FluidStack> sameFluidStack = FluidStack::matches;

    /**
     * matches {@link FluidStack#getFluid()}
     */
    CheckStrategy<FluidStack> sameFluid = FluidStack::isSameFluid;

    /**
     * matches {@link FluidStack#getFluid()} and {@link FluidStack#getAmount()}
     */
    CheckStrategy<FluidStack> sameFluidAndAmount = (a, b) -> FluidStack.isSameFluid(a, b) && a.getAmount() == b.getAmount();

    /**
     * matches {@link FluidStack#getFluid()} and {@link FluidStack#getComponents()}
     */
    CheckStrategy<FluidStack> sameFluidAndComponent = FluidStack::isSameFluidSameComponents;

}
