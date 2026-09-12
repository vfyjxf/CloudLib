package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.ui.texture.BorderTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.GradientTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.NineSliceTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.SpriteTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.TiledTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Texture function values — the theme extension vocabulary:
 * {@code nine-slice(loc,border[,w,h])}, {@code sprite(loc[,w,h])},
 * {@code tiled(loc,w,h)}, {@code color(argb)}, {@code linear-gradient(c1,c2[,vertical])},
 * {@code border-texture(color,thickness)}.
 */
public final class CssTextures {

    private CssTextures() {}

    /** Parses a single component value into a {@link VisualTexture}. */
    public static @Nullable VisualTexture texture(@Nullable ComponentValue v) {
        if (!(v instanceof ComponentValue.Function fn)) {
            return null;
        }
        List<ComponentValue> args = fn.args().stream()
                .filter(c -> c != ComponentValue.Whitespace.instance
                        && !(c instanceof ComponentValue.Delim d && d.value() == ','))
                .toList();
        return switch (fn.name().toLowerCase(Locale.ROOT)) {
            case "nine-slice" -> nineSlice(args);
            case "sprite" -> sprite(args);
            case "tiled" -> tiled(args);
            case "color" -> {
                Integer c = args.isEmpty() ? null : CssValues.color(args.get(0));
                yield c != null ? new ColorTexture(c) : null;
            }
            case "linear-gradient" -> gradient(args);
            case "border-texture" -> borderTexture(args);
            default -> null;
        };
    }

    public static @Nullable ResourceLocation location(ComponentValue v) {
        if (v instanceof ComponentValue.StringValue s) {
            return ResourceLocation.tryParse(s.value());
        }
        if (v instanceof ComponentValue.UrlValue u) {
            return ResourceLocation.tryParse(u.value());
        }
        if (v instanceof ComponentValue.Ident id) {
            return ResourceLocation.tryParse(id.value());
        }
        return null;
    }

    private static @Nullable Float num(ComponentValue v) {
        if (v instanceof ComponentValue.NumericValue n) {
            return (float) n.value();
        }
        return null;
    }

    // region functions

    private static @Nullable VisualTexture nineSlice(List<ComponentValue> args) {
        if (args.isEmpty()) return null;
        ResourceLocation loc = location(args.get(0));
        if (loc == null) return null;
        // nine-slice(loc, border) | nine-slice(loc, border, w, h)
        Float border = args.size() > 1 ? num(args.get(1)) : null;
        if (border == null) return null;
        if (args.size() >= 4) {
            Float w = num(args.get(2));
            Float h = num(args.get(3));
            if (w == null || h == null) return null;
            return NineSliceTexture.of(loc, w.intValue(), h.intValue(), border.intValue());
        }
        // default: 18x18 cell with given border (matches the bundled assets)
        return NineSliceTexture.of(loc, 18, 18, border.intValue());
    }

    private static @Nullable VisualTexture sprite(List<ComponentValue> args) {
        if (args.isEmpty()) return null;
        ResourceLocation loc = location(args.get(0));
        if (loc == null) return null;
        if (args.size() == 1) {
            return SpriteTexture.fromGuiSprite(loc); // intrinsic size from the atlas
        }
        Float w = num(args.get(1));
        Float h = args.size() > 2 ? num(args.get(2)) : null;
        if (w == null || h == null) return null;
        return SpriteTexture.fromGuiSprite(loc, w.intValue(), h.intValue());
    }

    private static @Nullable VisualTexture tiled(List<ComponentValue> args) {
        if (args.size() < 3) return null;
        ResourceLocation loc = location(args.get(0));
        Float w = num(args.get(1));
        Float h = num(args.get(2));
        if (loc == null || w == null || h == null) return null;
        return TiledTexture.sprite(loc, w.intValue(), h.intValue());
    }

    private static @Nullable VisualTexture gradient(List<ComponentValue> args) {
        // linear-gradient(c1, c2 [, vertical|horizontal])
        if (args.size() < 2) return null;
        Integer c1 = CssValues.color(args.get(0));
        Integer c2 = CssValues.color(args.get(1));
        if (c1 == null || c2 == null) return null;
        boolean vertical = args.size() > 2
                && args.get(2) instanceof ComponentValue.Ident id
                && id.value().equalsIgnoreCase("vertical");
        return vertical ? GradientTexture.vertical(c1, c2) : GradientTexture.horizontal(c1, c2);
    }

    private static @Nullable VisualTexture borderTexture(List<ComponentValue> args) {
        if (args.size() < 2) return null;
        Integer c = CssValues.color(args.get(0));
        Float thickness = num(args.get(1));
        if (c == null || thickness == null) return null;
        return BorderTexture.of(c, thickness.intValue());
    }

    // endregion
}
