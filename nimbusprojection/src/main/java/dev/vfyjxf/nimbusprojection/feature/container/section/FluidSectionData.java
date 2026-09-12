package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Tank-list snapshot of an {@code IFluidHandler} — one bar per tank. */
public record FluidSectionData(List<Tank> tanks) implements SectionData {

    private static final int maxTanks = 64;

    /** One tank: the fluid it holds and its capacity in mB. */
    public record Tank(FluidStack fluid, int capacity) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidSectionData> streamCodec = StreamCodec.composite(
            StreamCodec.composite(
                            FluidStack.OPTIONAL_STREAM_CODEC,
                            Tank::fluid,
                            ByteBufCodecs.VAR_INT,
                            Tank::capacity,
                            Tank::new)
                    .apply(ByteBufCodecs.list(maxTanks)),
            FluidSectionData::tanks,
            FluidSectionData::new);

    @Override
    public SectionType<?> type() {
        return SectionTypes.fluid;
    }
}
