package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The hive face — bees currently inside / capacity (plus honey level for completeness). */
public record HiveSectionData(int occupied, int max, int honeyLevel) implements SectionData {

    public static final StreamCodec<RegistryFriendlyByteBuf, HiveSectionData> streamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            HiveSectionData::occupied,
            ByteBufCodecs.VAR_INT,
            HiveSectionData::max,
            ByteBufCodecs.VAR_INT,
            HiveSectionData::honeyLevel,
            HiveSectionData::new);

    @Override
    public SectionType<?> type() {
        return SectionTypes.hive;
    }
}
