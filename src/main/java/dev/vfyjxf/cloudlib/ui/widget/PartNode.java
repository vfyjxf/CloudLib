package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * A flowed named part of a compound widget — the flex sibling of
 * {@link WidgetPart}.
 * <p>
 * {@link WidgetPart} is a {@code position: absolute} box the owner places by
 * hand, which is what a bar's track/fill/mark are. A <em>header</em>'s title and
 * its rule, or a key/value row's two columns, are the opposite: they should be
 * sized by their own content and arranged by taffy. Both are still parts, so
 * both have to answer to the owner's selector surface:
 * <ul>
 *   <li>{@link #styleTag()} reports the owner's tag, so the documented compound
 *       form {@code section-header::part(title)} matches a child that is really
 *       a {@code TitleNode};</li>
 *   <li>{@link #styleStates()} mirrors the owner's states, so
 *       {@code section-header:hover::part(rule)} follows the compound;</li>
 *   <li>the part is {@code ::part(name)}-tagged and never takes the pointer.</li>
 * </ul>
 * The stylesheet address is the only thing this class touches — layout is left
 * entirely to the owner's flex settings.
 */
class PartNode extends Widget {

    private final Widget owner;

    /**
     * @param owner the compound widget this part belongs to
     * @param name  the {@code ::part(name)} name, or null for a repeated child a
     *              theme addresses by class or tag instead
     */
    PartNode(Widget owner, @Nullable String name) {
        this.owner = owner;
        stylePart(name);
        setInteractive(false);
        useStyle(UIStyle.of(UIStyles.flexShrink(0f)));
    }

    // region selector surface

    @Override
    public String styleTag() {
        return owner.styleTag();
    }

    @Override
    public Set<String> styleStates() {
        Set<String> states = new HashSet<>(owner.styleStates());
        if (hovered()) {
            states.add("hovered");
            states.add("hover");
        }
        if (hasFocus()) {
            states.add("focused");
        }
        return states;
    }

    // endregion

    // region rendering

    /** The themed background when the theme paints this part, else the owner's code texture. */
    protected @Nullable VisualTexture themedBackground(@Nullable VisualTexture fallback) {
        VisualTexture themed = style().visualContext().background();
        if (themed != null && !themed.isEmpty()) {
            return themed;
        }
        return fallback;
    }

    // endregion
}
