package dev.vfyjxf.nimbusprojection.feature.container.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Slot-list snapshot of an {@code IItemHandler} — the classic chest grid. */
public record ItemSectionData(List<ItemStack> stacks) implements SectionData {

    /** Never ship more slots than this — a malformed handler shouldn't spam the wire. */
    private static final int maxSlots = 512;

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemSectionData> streamCodec = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(maxSlots)),
            ItemSectionData::stacks,
            ItemSectionData::new);

    @Override
    public SectionType<?> type() {
        return SectionTypes.item;
    }
}
