package dev.vfyjxf.nimbusprojection.feature.machine.section;

import dev.vfyjxf.nimbusprojection.api.section.SectionProvider;
import dev.vfyjxf.nimbusprojection.api.section.SectionType;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.Arrays;
import java.util.List;

/** The sign face — front and back lines, unfiltered (viewer-side). */
public final class TextSectionProvider implements SectionProvider<TextSectionData> {

    @Override
    public SectionType<TextSectionData> type() {
        return SectionTypes.text;
    }

    @Override
    public List<TextSectionData> collect(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity sign)) return List.of();
        return List.of(new TextSectionData(lines(sign.getFrontText()), lines(sign.getBackText())));
    }

    private static List<Component> lines(SignText text) {
        return Arrays.asList(text.getMessages(false));
    }
}
