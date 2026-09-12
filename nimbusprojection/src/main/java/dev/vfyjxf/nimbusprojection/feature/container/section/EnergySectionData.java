package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Stored/capacity pair of an {@code IEnergyStorage} — the FE bar. */
public record EnergySectionData(int stored, int capacity) implements SectionData {

    public static final StreamCodec<RegistryFriendlyByteBuf, EnergySectionData> streamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EnergySectionData::stored,
            ByteBufCodecs.VAR_INT,
            EnergySectionData::capacity,
            EnergySectionData::new);

    @Override
    public SectionType<?> type() {
        return SectionTypes.energy;
    }
}
