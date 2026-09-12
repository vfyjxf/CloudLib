package dev.vfyjxf.cloudlib.api.ui.theme;

import dev.vfyjxf.cloudlib.internal.css.Stylesheet;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A parsed theme: an id plus the flattened, {@code @import}-resolved rule list.
 * <p>
 * Rules keep source order across the whole import chain — later declarations of
 * equal specificity win per the cascade.
 */
public record Theme(ResourceLocation id, Stylesheet sheet) {

    /** All style rules in source order (imported sheets inlined at the import point). */
    public List<dev.vfyjxf.cloudlib.internal.css.StyleRule> styleRules() {
        return sheet.rules().stream()
                .filter(r -> r instanceof dev.vfyjxf.cloudlib.internal.css.StyleRule)
                .map(r -> (dev.vfyjxf.cloudlib.internal.css.StyleRule) r)
                .toList();
    }
}
