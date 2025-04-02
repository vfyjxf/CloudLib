package dev.vfyjxf.cloudlib.api.data;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * C functional interface to check if two objects are equal
 *
 * @param <T> the type to check
 */
@FunctionalInterface
public interface EqualsChecker<T> {

    boolean check(T a, T b);

    static <T> EqualsChecker<T> identity() {
        return (a, b) -> a == b;
    }

    /**
     * @param <T> a nullable type
     * @return null safe hashcode comparison
     */
    static <T extends @Nullable Object> EqualsChecker<T> hashcode() {
        return (a, b) -> (a == null || b == null) ? a == b : a.hashCode() == b.hashCode();
    }

    /**
     * @param <T> a nullable type
     * @return null safe comparison
     */
    static <T extends @Nullable Object> EqualsChecker<T> equals() {
        return Objects::equals;
    }

    /**
     * Same as {@link #equals()} but the name is better for primitive types
     */
    static <T extends @Nullable Object> EqualsChecker<T> primitive() {
        return Objects::equals;
    }

    /**
     * matches {@link ItemStack#getCount()} and {@link ItemStack#getItem()} and {@link ItemStack#getComponents()}
     */
    EqualsChecker<ItemStack> sameItemStack = ItemStack::matches;

    /**
     * matches {@link ItemStack#getItem()}
     */
    EqualsChecker<ItemStack> sameItem = ItemStack::isSameItem;

    /**
     * matches {@link ItemStack#getItem()} and {@link ItemStack#getCount()}
     */
    EqualsChecker<ItemStack> sameItemAndCount = (a, b) -> ItemStack.isSameItem(a, b) && a.getCount() == b.getCount();

    /**
     * matches {@link ItemStack#getItem()} and {@link ItemStack#getComponents()}
     */
    EqualsChecker<ItemStack> sameItemAndComponent = ItemStack::isSameItemSameComponents;

    /**
     * matches {@link FluidStack#getAmount()} and {@link FluidStack#getFluid()} and {@link FluidStack#getComponents()}
     */
    EqualsChecker<FluidStack> sameFluidStack = FluidStack::matches;

    /**
     * matches {@link FluidStack#getFluid()}
     */
    EqualsChecker<FluidStack> sameFluid = FluidStack::isSameFluid;

    /**
     * matches {@link FluidStack#getFluid()} and {@link FluidStack#getAmount()}
     */
    EqualsChecker<FluidStack> sameFluidAndAmount = (a, b) -> FluidStack.isSameFluid(a, b) && a.getAmount() == b.getAmount();

    /**
     * matches {@link FluidStack#getFluid()} and {@link FluidStack#getComponents()}
     */
    EqualsChecker<FluidStack> sameFluidAndComponent = FluidStack::isSameFluidSameComponents;

}
