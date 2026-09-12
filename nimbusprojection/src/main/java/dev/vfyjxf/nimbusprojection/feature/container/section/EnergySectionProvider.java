package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/** The unsided {@code IEnergyStorage} — one section, one bar. */
public final class EnergySectionProvider implements SectionProvider<EnergySectionData> {

    @Override
    public SectionType<EnergySectionData> type() {
        return SectionTypes.energy;
    }

    @Override
    public List<EnergySectionData> collect(Level level, BlockPos pos) {
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (storage == null) return List.of();
        return List.of(new EnergySectionData(storage.getEnergyStored(), storage.getMaxEnergyStored()));
    }
}
