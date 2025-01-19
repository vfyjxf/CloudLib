package dev.vfyjxf.cloudlib.utils;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public final class FluidHandlers {

    public static FluidStack insert(@Nullable IFluidHandler handler, FluidStack stack, IFluidHandler.FluidAction simulate) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private FluidHandlers() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
