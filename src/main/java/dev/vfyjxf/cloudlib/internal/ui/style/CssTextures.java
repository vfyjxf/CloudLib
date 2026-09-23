package dev.vfyjxf.cloudlib.internal.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.texture.BatchableTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.BorderTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.BuiltInTextures;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.CompositeTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.GradientTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.ImageTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.NineSliceTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.RoundedRectTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.SpriteTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.TiledTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.TintedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.TransformedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Texture function values — the theme extension vocabulary:
 * <ul>
 *   <li>{@code nine-slice(loc,border[,w,h])} — border is one number or
 *       {@code top right bottom left}</li>
 *   <li>{@code sprite(loc,w,h)} / {@code sprite(loc,x,y,w,h)} — a region of the texture
 *       file (the five-argument form offsets the region inside the sheet)</li>
 *   <li>{@code sprite(loc)} — a GUI sprite atlas entry (vanilla semantics — use it for
 *       {@code gui/sprites/**})</li>
 *   <li>{@code tiled(loc,w,h)}, {@code color(argb)},
 *       {@code linear-gradient(c1,c2[,vertical])}, {@code border-texture(color,thickness)}</li>
 *   <li>{@code sdf(<color>[, radius|r r r r[, stroke[, borderColor]]])} and its alias
 *       {@code rect(...)} — a rounded rectangle; the four-radius form keeps LDLib2's order
 *       bottom-left, bottom-right, top-right, top-left</li>
 *   <li>{@code group(<texture>, <texture>, …)} — the layers composited in order, first at
 *       the bottom</li>
 *   <li>{@code built-in(<ns:NAME>)} — a sprite from the ported LDLib2 tables
 *       ({@link BuiltInTextures})</li>
 *   <li>{@code empty} / {@code none} — draws nothing</li>
 * </ul>
 * Any texture function may be followed by the modifier chain
 * {@code scale(<n>|<w>,<h>)}, {@code translate(<x>,<y>)}, {@code color(#hex)} — each
 * applied to the texture the function produced ({@link TransformedTexture},
 * {@link TintedTexture}).
 * <p>
 * The whole declaration value is consumed: the first token is the texture function, every
 * later token must be a modifier.
 */
public final class CssTextures {

    private CssTextures() {}

    // region entry points

    /**
     * Parses a texture declaration — the texture function plus its modifier chain, or a
     * bare {@code none}/{@code empty}. Returns {@code null} for anything else, which the
     * caller reports as an invalid value.
     */
    public static @Nullable VisualTexture parse(List<ComponentValue> values) {
        List<ComponentValue> tokens = flatten(values);
        if (tokens.isEmpty()) {
            return null;
        }
        VisualTexture texture = main(tokens.get(0));
        if (texture == null) {
            return null;
        }
        float scaleX = 1f;
        float scaleY = 1f;
        float offsetX = 0f;
        float offsetY = 0f;
        boolean scaled = false;
        boolean translated = false;
        Integer tint = null;
        for (int i = 1; i < tokens.size(); i++) {
            if (!(tokens.get(i) instanceof ComponentValue.Function fn)) {
                return null; // a stray token where a modifier belongs
            }
            List<List<ComponentValue>> args = segments(fn.args());
            switch (fn.name().toLowerCase(Locale.ROOT)) {
                case "scale" -> {
                    Float sx = scalar(args, 0);
                    Float sy = args.size() > 1 ? scalar(args, 1) : sx;
                    if (sx == null || sy == null || args.size() > 2) {
                        return null;
                    }
                    scaleX = sx;
                    scaleY = sy;
                    scaled = true;
                }
                case "translate" -> {
                    Float x = scalar(args, 0);
                    Float y = scalar(args, 1);
                    if (x == null || y == null || args.size() != 2) {
                        return null;
                    }
                    offsetX = x;
                    offsetY = y;
                    translated = true;
                }
                case "color" -> {
                    Integer c = args.size() == 1 ? color(args.get(0)) : null;
                    if (c == null) {
                        return null;
                    }
                    tint = c;
                }
                default -> {
                    // unknown modifiers are ignored — forward compatible with newer themes
                }
            }
        }
        if (scaled || translated) {
            texture = new TransformedTexture(texture, scaleX, scaleY, offsetX, offsetY);
        }
        return tint != null ? new TintedTexture(texture, tint) : texture;
    }

    /**
     * Serializes a texture back to declaration text — the inverse of {@link #parse} for the
     * value shapes a texture function can produce. Best effort: a texture built through the
     * java api that has no css spelling (an atlas {@link SpriteTexture}, a diagonal gradient,
     * a nine-slice bound to a sheet region) raises {@link IllegalArgumentException} — pass
     * such a texture to the texture consumers directly instead of through css text.
     */
    public static String write(VisualTexture texture) {
        if (texture.isEmpty()) {
            return "empty";
        }
        if (texture instanceof ColorTexture color) {
            return "color(" + hex(color.color()) + ")";
        }
        if (texture instanceof TintedTexture tinted) {
            // the parser's order: the transform sits inside the tint
            return write(tinted.texture()) + " color(" + hex(tinted.tint()) + ")";
        }
        if (texture instanceof TransformedTexture transform) {
            return write(transform.texture()) + transformText(transform);
        }
        if (texture instanceof GradientTexture gradient) {
            return gradientText(gradient);
        }
        if (texture instanceof NineSliceTexture slice) {
            return nineSliceText(slice);
        }
        if (texture instanceof ImageTexture image) {
            return imageText(image);
        }
        if (texture instanceof TiledTexture tiled) {
            if (tiled.atlasSprite()) {
                throw new IllegalArgumentException("no css form for an atlas-sprite tiled texture");
            }
            return "tiled(\"" + resource(tiled.texture()) + "\", " + tiled.tileWidth() + ", " + tiled.tileHeight()
                    + ")";
        }
        if (texture instanceof BorderTexture border) {
            if (!border.isUniformColor() || !border.isUniformThickness()) {
                throw new IllegalArgumentException("no css form for a per-side border-texture");
            }
            return "border-texture(" + hex(border.colorTop()) + ", " + border.thicknessTop() + ")";
        }
        if (texture instanceof RoundedRectTexture rect) {
            return roundedRectText(rect);
        }
        if (texture instanceof CompositeTexture group) {
            if (group.layerCount() == 0) {
                throw new IllegalArgumentException("no css form for an empty group");
            }
            StringBuilder out = new StringBuilder("group(");
            boolean first = true;
            for (BatchableTexture layer : group.layers()) {
                if (!first) out.append(", ");
                first = false;
                out.append(write(layer));
            }
            return out.append(')').toString();
        }
        throw new IllegalArgumentException("no css form for " + texture.getClass().getSimpleName());
    }

    private static String transformText(TransformedTexture transform) {
        StringBuilder out = new StringBuilder();
        if (transform.scaleX() != 1f || transform.scaleY() != 1f) {
            out.append(" scale(").append(number(transform.scaleX()));
            if (transform.scaleY() != transform.scaleX()) {
                out.append(", ").append(number(transform.scaleY()));
            }
            out.append(')');
        }
        if (transform.offsetX() != 0f || transform.offsetY() != 0f) {
            out.append(" translate(").append(number(transform.offsetX())).append(", ")
                    .append(number(transform.offsetY())).append(')');
        }
        return out.toString();
    }

    /** Only the axis-aligned two-stop gradients have a css spelling — the diagonal form does not. */
    private static String gradientText(GradientTexture gradient) {
        int topLeft = gradient.colorTopLeft();
        int topRight = gradient.colorTopRight();
        int bottomLeft = gradient.colorBottomLeft();
        int bottomRight = gradient.colorBottomRight();
        if (topLeft == topRight && bottomLeft == bottomRight && topLeft != bottomLeft) {
            return "linear-gradient(" + hex(topLeft) + ", " + hex(bottomLeft) + ", vertical)";
        }
        if (topLeft == bottomLeft && topRight == bottomRight && topLeft != topRight) {
            return "linear-gradient(" + hex(topLeft) + ", " + hex(topRight) + ")";
        }
        throw new IllegalArgumentException("no css form for a diagonal gradient");
    }

    private static String nineSliceText(NineSliceTexture slice) {
        if (slice.atlasSprite() || slice.textureWidth() != slice.width() || slice.textureHeight() != slice.height()) {
            throw new IllegalArgumentException("no css form for a nine-slice bound to a sheet region or atlas sprite");
        }
        return "nine-slice(\"" + resource(slice.location()) + "\", " + slice.top() + " " + slice.right() + " "
                + slice.bottom() + " " + slice.left() + ", " + slice.width() + ", " + slice.height() + ")";
    }

    private static String imageText(ImageTexture image) {
        if (image.atlasSprite()) {
            throw new IllegalArgumentException("no css form for an atlas sprite");
        }
        String location = "\"" + resource(image.location()) + "\"";
        if (image.u() == 0
                && image.v() == 0
                && image.textureWidth() == image.width()
                && image.textureHeight() == image.height()) {
            return "sprite(" + location + ", " + image.width() + ", " + image.height() + ")";
        }
        if (image.textureWidth() == 0 && image.textureHeight() == 0) {
            return "sprite(" + location + ", " + image.u() + ", " + image.v() + ", " + image.width() + ", "
                    + image.height() + ")";
        }
        throw new IllegalArgumentException("no css form for a sprite region with an explicit sheet size");
    }

    /** The four-radii form is written in the parser's LDLib2 order. */
    private static String roundedRectText(RoundedRectTexture rect) {
        int topLeft = rect.radiusTopLeft();
        int topRight = rect.radiusTopRight();
        int bottomLeft = rect.radiusBottomLeft();
        int bottomRight = rect.radiusBottomRight();
        String radius = topLeft == topRight && topLeft == bottomLeft && topLeft == bottomRight
                ? Integer.toString(topLeft)
                : bottomLeft + " " + bottomRight + " " + topRight + " " + topLeft;
        return "sdf(" + hex(rect.fillColor()) + ", " + radius + ", " + rect.borderThickness() + ", "
                + hex(rect.borderColor()) + ")";
    }

    /** {@code #RRGGBB} for an opaque color, {@code #RRGGBBAA} otherwise — the css hex order. */
    private static String hex(int argb) {
        int alpha = argb >>> 24;
        return alpha == 0xFF ? "#%06X".formatted(argb & 0xFFFFFF) : "#%06X%02X".formatted(argb & 0xFFFFFF, alpha);
    }

    /** The inverse of {@link #fileLocation} — a raw file back to the short texture id a theme writes. */
    private static String resource(ResourceLocation location) {
        String path = location.getPath();
        if (path.startsWith("textures/") && path.endsWith(".png")) {
            path = path.substring("textures/".length(), path.length() - ".png".length());
        }
        return location.getNamespace() + ":" + path;
    }

    private static String number(float value) {
        return value == Math.floor(value) && !Float.isInfinite(value)
                ? Long.toString((long) value)
                : Float.toString(value);
    }

    /** The resource location of a texture argument — string, url or bare ident. */
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

    /**
     * Splits a token sequence on its top-level commas — the argument shape of the
     * multi-texture functions ({@code group(...)}) and of the id/property values that
     * carry a texture ({@code scrollbar-style(...)}). Whitespace is kept, so a segment may
     * span several tokens ({@code 3 5 3 5}).
     */
    public static List<List<ComponentValue>> segments(List<ComponentValue> tokens) {
        List<List<ComponentValue>> out = new ArrayList<>();
        List<ComponentValue> current = new ArrayList<>();
        for (ComponentValue token : tokens) {
            if (token instanceof ComponentValue.Delim comma && comma.value() == ',') {
                out.add(List.copyOf(current));
                current.clear();
            } else {
                current.add(token);
            }
        }
        if (!current.isEmpty()) {
            out.add(List.copyOf(current));
        }
        return out;
    }

    // endregion

    // region tokens

    /** Declaration values without whitespace tokens. */
    private static List<ComponentValue> flatten(List<ComponentValue> values) {
        return values.stream().filter(c -> c != ComponentValue.Whitespace.instance).toList();
    }

    /** A function's arguments without whitespace or top-level commas. */
    private static List<ComponentValue> args(List<ComponentValue> raw) {
        return raw.stream().filter(c -> c != ComponentValue.Whitespace.instance)
                .filter(c -> !(c instanceof ComponentValue.Delim d && d.value() == ',')).toList();
    }

    /** An id's tokens as text — {@code built-in(ore:BTN_DEFAULT)} arrives as ident, ':', ident. */
    private static @Nullable String text(List<ComponentValue> tokens) {
        if (tokens.size() == 1 && tokens.get(0) instanceof ComponentValue.StringValue string) {
            return string.value();
        }
        String id = Tokens.serialize(tokens);
        return id.isBlank() ? null : id;
    }

    /**
     * Expands a short texture id ({@code cloudlib:gui/panel/dark}) into the
     * resource path the texture system actually loads
     * ({@code cloudlib:textures/gui/panel/dark.png}) — file-backed texture fns
     * bind raw files, so the {@code textures/} prefix + {@code .png} suffix are
     * supplied here rather than making theme authors write them.
     */
    private static @Nullable ResourceLocation fileLocation(ComponentValue v) {
        ResourceLocation loc = location(v);
        return loc == null ? null : fileLocation(loc);
    }

    /** The file-backed form of an already-resolved texture location. */
    private static ResourceLocation fileLocation(ResourceLocation loc) {
        return loc.withPath(p -> "textures/" + p + ".png");
    }

    private static @Nullable Float num(ComponentValue v) {
        if (v instanceof ComponentValue.NumericValue n) {
            return (float) n.value();
        }
        return null;
    }

    /** The value of a scale/translate argument — a plain number or a {@code px} length. */
    private static @Nullable Float length(ComponentValue v) {
        if (v instanceof ComponentValue.NumericValue n) {
            return switch (n.kind()) {
                case number -> (float) n.value();
                case dimension -> n.unit().equalsIgnoreCase("px") ? (float) n.value() : null;
                case percentage -> null;
            };
        }
        return null;
    }

    /** The single token of a segment — the shapes {@code scale(2)} and {@code color(#fff)} take. */
    private static @Nullable ComponentValue only(List<ComponentValue> segment) {
        List<ComponentValue> flat = flatten(segment);
        return flat.size() == 1 ? flat.get(0) : null;
    }

    /** The single number of the {@code index}-th comma segment, or {@code null}. */
    private static @Nullable Float scalar(List<List<ComponentValue>> segments, int index) {
        if (index >= segments.size()) {
            return null;
        }
        ComponentValue token = only(segments.get(index));
        return token == null ? null : length(token);
    }

    /** The single color of a segment, or {@code null}. */
    private static @Nullable Integer color(List<ComponentValue> segment) {
        ComponentValue token = only(segment);
        return token == null ? null : CssValues.color(token);
    }

    /** Every numeric token of a segment — the shapes a border/radius list takes. */
    private static @Nullable List<Float> numbers(List<ComponentValue> segment) {
        List<Float> out = new ArrayList<>();
        for (ComponentValue token : flatten(segment)) {
            Float number = num(token);
            if (number == null) {
                return null;
            }
            out.add(number);
        }
        return out.isEmpty() ? null : out;
    }

    // endregion

    // region functions

    /** The texture function itself — modifiers are consumed by {@link #parse}. */
    private static @Nullable VisualTexture main(ComponentValue token) {
        if (token instanceof ComponentValue.Ident id) {
            return switch (id.value().toLowerCase(Locale.ROOT)) {
                case "none", "empty" -> VisualTexture.empty;
                default -> null;
            };
        }
        if (!(token instanceof ComponentValue.Function fn)) {
            return null;
        }
        return switch (fn.name().toLowerCase(Locale.ROOT)) {
            case "none", "empty" -> VisualTexture.empty;
            case "nine-slice" -> nineSlice(segments(fn.args()));
            case "sprite" -> sprite(args(fn.args()));
            case "tiled" -> tiled(args(fn.args()));
            case "color" -> solid(args(fn.args()));
            case "linear-gradient" -> gradient(args(fn.args()));
            case "border-texture" -> borderTexture(args(fn.args()));
            case "sdf", "rect" -> roundedRect(segments(fn.args()));
            case "group" -> group(segments(fn.args()));
            case "built-in" -> builtIn(args(fn.args()));
            default -> null;
        };
    }

    /**
     * {@code nine-slice(loc, border[, w, h])} — the border is one number or the
     * {@code top right bottom left} sequence, {@code w}/{@code h} the source cell size
     * (default 18x18, matching the bundled assets).
     */
    private static @Nullable VisualTexture nineSlice(List<List<ComponentValue>> segments) {
        if (segments.size() != 2 && segments.size() != 4) return null;
        ComponentValue loc = only(segments.get(0));
        ResourceLocation location = loc == null ? null : fileLocation(loc);
        List<Float> border = numbers(segments.get(1));
        if (location == null || border == null) return null;
        int left;
        int right;
        int top;
        int bottom;
        if (border.size() == 1) {
            left = right = top = bottom = border.get(0).intValue();
        } else if (border.size() == 4) {
            top = border.get(0).intValue();
            right = border.get(1).intValue();
            bottom = border.get(2).intValue();
            left = border.get(3).intValue();
        } else {
            return null;
        }
        if (segments.size() == 4) {
            Float w = scalar(segments, 2);
            Float h = scalar(segments, 3);
            if (w == null || h == null) return null;
            return NineSliceTexture.of(location, w.intValue(), h.intValue(), left, right, top, bottom);
        }
        return NineSliceTexture.of(location, 18, 18, left, right, top, bottom);
    }

    /**
     * {@code sprite(loc)} — a GUI atlas entry; {@code sprite(loc, w, h)} — the whole file
     * drawn at that size; {@code sprite(loc, x, y, w, h)} — a region of the file, the
     * sheet's size being read from the file itself.
     */
    private static @Nullable VisualTexture sprite(List<ComponentValue> args) {
        if (args.isEmpty()) return null;
        ResourceLocation loc = location(args.get(0));
        if (loc == null) return null;
        if (args.size() == 1) {
            return SpriteTexture.fromGuiSprite(loc); // intrinsic size from the atlas
        }
        if (args.size() == 3) {
            Float w = num(args.get(1));
            Float h = num(args.get(2));
            if (w == null || h == null) return null;
            // explicit dims = a file-backed blit of that region, not an atlas sprite
            return ImageTexture.of(fileLocation(loc), w.intValue(), h.intValue());
        }
        if (args.size() == 5) {
            Float x = num(args.get(1));
            Float y = num(args.get(2));
            Float w = num(args.get(3));
            Float h = num(args.get(4));
            if (x == null || y == null || w == null || h == null) return null;
            // five-arg form = the region's UV offset inside the sheet
            return ImageTexture.region(fileLocation(loc), x.intValue(), y.intValue(), w.intValue(), h.intValue());
        }
        return null;
    }

    private static @Nullable VisualTexture tiled(List<ComponentValue> args) {
        if (args.size() < 3) return null;
        ResourceLocation loc = fileLocation(args.get(0));
        Float w = num(args.get(1));
        Float h = num(args.get(2));
        if (loc == null || w == null || h == null) return null;
        return TiledTexture.of(loc, w.intValue(), h.intValue());
    }

    /** {@code color(<color>)} — a flat fill. */
    private static @Nullable VisualTexture solid(List<ComponentValue> args) {
        Integer c = args.isEmpty() ? null : CssValues.color(args.get(0));
        return c != null ? new ColorTexture(c) : null;
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

    /**
     * {@code sdf(<color>[, <radius>|<r> <r> <r> <r>[, <stroke>[, <borderColor>]]])} and its
     * alias {@code rect(...)} — LDLib2's SDF rounded rectangle, drawn with
     * {@link RoundedRectTexture}. The four-radius form keeps LDLib2's order:
     * bottom-left, bottom-right, top-right, top-left. The stroke defaults to 0 and its ink
     * to opaque black (LDLib2's {@code SDFRectTexture} default).
     */
    private static @Nullable VisualTexture roundedRect(List<List<ComponentValue>> segments) {
        if (segments.isEmpty() || segments.size() > 4) return null;
        Integer fill = color(segments.get(0));
        if (fill == null) return null;
        float bottomLeft = 0f;
        float bottomRight = 0f;
        float topRight = 0f;
        float topLeft = 0f;
        if (segments.size() > 1) {
            List<Float> radii = numbers(segments.get(1));
            if (radii == null || (radii.size() != 1 && radii.size() != 4)) return null;
            if (radii.size() == 1) {
                bottomLeft = bottomRight = topRight = topLeft = radii.get(0);
            } else {
                bottomLeft = radii.get(0);
                bottomRight = radii.get(1);
                topRight = radii.get(2);
                topLeft = radii.get(3);
            }
        }
        int stroke = 0;
        int borderColor = 0xFF000000;
        if (segments.size() > 2) {
            Float thickness = scalar(segments, 2);
            if (thickness == null) return null;
            stroke = thickness.intValue();
        }
        if (segments.size() > 3) {
            Integer ink = color(segments.get(3));
            if (ink == null) return null;
            borderColor = ink;
        }
        return new RoundedRectTexture(
            fill,
            (int) topLeft,
            (int) topRight,
            (int) bottomLeft,
            (int) bottomRight,
            borderColor,
            stroke,
            8
        );
    }

    /**
     * {@code group(<texture>, <texture>, …)} — the layers in order, the first drawn at the
     * bottom. Layers that cannot batch ({@code empty}, the animation textures) are dropped;
     * a group without a single drawable layer is invalid.
     */
    private static @Nullable VisualTexture group(List<List<ComponentValue>> segments) {
        List<BatchableTexture> layers = new ArrayList<>(segments.size());
        for (List<ComponentValue> segment : segments) {
            if (parse(segment) instanceof BatchableTexture layer) {
                layers.add(layer);
            }
        }
        return layers.isEmpty() ? null : CompositeTexture.of(layers);
    }

    /** {@code built-in(<ns:NAME>)} — a ported LDLib2 sprite, see {@link BuiltInTextures}. */
    private static @Nullable VisualTexture builtIn(List<ComponentValue> args) {
        String id = text(args);
        return id == null ? null : BuiltInTextures.get(id);
    }

    // endregion
}
