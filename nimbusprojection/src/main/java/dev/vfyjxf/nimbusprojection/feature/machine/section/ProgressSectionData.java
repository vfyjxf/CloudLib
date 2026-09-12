package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A processing section's snapshot — the furnace/brewing face: how far the
 * current job is ({@code progress}/{@code total}) and how much fuel is
 * left ({@code fuel}/{@code fuelTotal}, {@code 0/0} when the machine has
 * no fuel concept). Display-only — machines take no player ops.
 */
public record ProgressSectionData(int progress, int total, int fuel, int fuelTotal) implements SectionData {

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressSectionData> streamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ProgressSectionData::progress,
            ByteBufCodecs.VAR_INT,
            ProgressSectionData::total,
            ByteBufCodecs.VAR_INT,
            ProgressSectionData::fuel,
            ByteBufCodecs.VAR_INT,
            ProgressSectionData::fuelTotal,
            ProgressSectionData::new);

    @Override
    public SectionType<?> type() {
        return SectionTypes.progress;
    }
}
