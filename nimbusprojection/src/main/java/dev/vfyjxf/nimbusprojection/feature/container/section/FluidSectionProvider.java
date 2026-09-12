package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/** The unsided {@code IFluidHandler} — one section listing every tank. */
public final class FluidSectionProvider implements SectionProvider<FluidSectionData> {

    private static final int maxTanks = 64;

    @Override
    public SectionType<FluidSectionData> type() {
        return SectionTypes.fluid;
    }

    @Override
    public List<FluidSectionData> collect(Level level, BlockPos pos) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null || handler.getTanks() <= 0) return List.of();
        int count = Math.min(handler.getTanks(), maxTanks);
        List<FluidSectionData.Tank> tanks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tanks.add(new FluidSectionData.Tank(handler.getFluidInTank(i).copy(), handler.getTankCapacity(i)));
        }
        return List.of(new FluidSectionData(tanks));
    }
}
