package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * The text face — a sign's written lines. {@code front} is the side the
 * text was written on; both faces ship so the widget can render whichever
 * side the viewer is actually looking at (or both in the hologram).
 */
public record TextSectionData(List<Component> front, List<Component> back) implements SectionData {

    /** Signs hold at most 4 lines a side — cap the wire shape defensively. */
    private static final int maxLines = 4;

    public static final StreamCodec<RegistryFriendlyByteBuf, TextSectionData> streamCodec = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(maxLines)),
            TextSectionData::front,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(maxLines)),
            TextSectionData::back,
            TextSectionData::new);

    @Override
    public SectionType<?> type() {
        return SectionTypes.text;
    }
}
