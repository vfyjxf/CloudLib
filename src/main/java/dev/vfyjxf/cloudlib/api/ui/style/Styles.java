package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleApply;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleKey;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleParser;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleRegistry;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleScope;
import dev.vfyjxf.cloudlib.api.ui.style.key.StyleValue;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TextAlign;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The builtin style vocabulary — every supported css property as a
 * {@link StyleKey} constant.
 * <p>
 * The registry is <b>longhand-only</b>: box shorthands ({@code padding},
 * {@code margin}, {@code inset}, {@code border-width}, …) live in the
 * shorthand table and expand into the per-edge keys at declaration-collection
 * time, exactly like css.
 * <p>
 * Constants self-register into {@link StyleRegistry} on class init; the plugin
 * bootstrap calls {@link #init()} to force that init before any css parses.
 */
public final class Styles {

    private Styles() {}

    /** Forces {@code <clinit>} — every constant self-registers. */
    public static void init() {}

    /** Resolves a css property name (aliases applied) to its key. */
    public static @Nullable StyleKey<?> byId(String cssName) {
        return StyleRegistry.get().byId(cssName);
    }

    // region registration helpers

    private static <T> StyleKey<T> key(
            String id, Class<T> type, StyleScope scope, StyleParser<T> parser, StyleApply<T> applier) {
        return StyleRegistry.get().register(new StyleKey<>(id, type, scope, parser, applier));
    }

    private static <T> StyleKey<T> key(
            String id,
            Class<T> type,
            StyleScope scope,
            boolean inherited,
            StyleParser<T> parser,
            StyleApply<T> applier) {
        return StyleRegistry.get()
                .register(
                        new StyleKey<>(id, type, scope, inherited, null, parser, applier, java.util.Objects::toString));
    }

    private static <E extends Enum<E>> StyleKey<E> enumKey(
            String id, Class<E> type, Map<String, E> aliases, StyleApply<E> applier) {
        return key(
                id,
                type,
                StyleScope.layout,
                dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.parser(type, aliases),
                applier);
    }

    private static <E extends Enum<E>> StyleKey<E> enumKey(
            String id, Class<E> type, Map<String, E> aliases, boolean inherited, StyleApply<E> applier) {
        return key(
                id,
                type,
                StyleScope.layout,
                inherited,
                dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.parser(type, aliases),
                applier);
    }

    private static StyleKey<LengthPercentage> lpKey(String id, StyleApply<LengthPercentage> applier) {
        return key(
                id,
                LengthPercentage.class,
                StyleScope.layout,
                (values, ctx) -> {
                    ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                    LengthPercentage r =
                            v == null ? null : dev.vfyjxf.cloudlib.internal.ui.style.CssValues.lengthPercentage(v);
                    if (r == null) ctx.warn("invalid <length-percentage> for " + id);
                    return r;
                },
                applier);
    }

    private static StyleKey<LengthPercentageAuto> lpaKey(String id, StyleApply<LengthPercentageAuto> applier) {
        return key(
                id,
                LengthPercentageAuto.class,
                StyleScope.layout,
                (values, ctx) -> {
                    ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                    LengthPercentageAuto r =
                            v == null ? null : dev.vfyjxf.cloudlib.internal.ui.style.CssValues.lengthAuto(v);
                    if (r == null) ctx.warn("invalid <length-percentage>|auto for " + id);
                    return r;
                },
                applier);
    }

    private static StyleKey<TaffyDimension> dimKey(String id, StyleApply<TaffyDimension> applier) {
        return key(
                id,
                TaffyDimension.class,
                StyleScope.layout,
                (values, ctx) -> {
                    ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                    TaffyDimension r = v == null ? null : dev.vfyjxf.cloudlib.internal.ui.style.CssValues.dimension(v);
                    if (r == null) ctx.warn("invalid <dimension> for " + id);
                    return r;
                },
                applier);
    }

    private static StyleKey<Float> floatKey(
            String id, StyleScope scope, dev.vfyjxf.cloudlib.api.ui.style.key.StyleApply<Float> applier) {
        return key(id, Float.class, scope, dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.floatParser(id), applier);
    }

    private static StyleKey<Integer> intKey(
            String id, StyleScope scope, dev.vfyjxf.cloudlib.api.ui.style.key.StyleApply<Integer> applier) {
        return key(id, Integer.class, scope, dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.intParser(id), applier);
    }

    private static StyleKey<Boolean> boolKey(String id, StyleApply<Boolean> applier) {
        return key(
                id,
                Boolean.class,
                StyleScope.layout,
                dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.boolParser(id),
                applier);
    }

    private static StyleKey<Integer> colorKey(String id, boolean inherited, StyleApply<Integer> applier) {
        return key(
                id,
                Integer.class,
                StyleScope.visual,
                inherited,
                (values, ctx) -> {
                    ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                    Integer c = v == null ? null : dev.vfyjxf.cloudlib.internal.ui.style.CssValues.color(v);
                    if (c == null) ctx.warn("invalid <color> for " + id);
                    return c;
                },
                applier);
    }

    private static StyleKey<VisualTexture> textureKey(String id, StyleApply<VisualTexture> applier) {
        return key(
                id,
                VisualTexture.class,
                StyleScope.visual,
                (values, ctx) -> {
                    ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                    VisualTexture t = v == null ? null : dev.vfyjxf.cloudlib.internal.ui.style.CssTextures.texture(v);
                    if (t == null) ctx.warn("invalid texture for " + id);
                    return t;
                },
                applier);
    }

    // endregion

    // region layout — box

    public static final StyleKey<TaffyDisplay> display = enumKey(
            "display",
            TaffyDisplay.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.display,
            (ctx, v) -> ctx.layoutStyle().display = v);

    public static final StyleKey<TaffyPosition> position = enumKey(
            "position",
            TaffyPosition.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.position,
            (ctx, v) -> ctx.layoutStyle().position = v);

    public static final StyleKey<Overflow> overflowX = enumKey(
            "overflow-x",
            Overflow.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.overflow,
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.point(s -> s.overflow, true));

    public static final StyleKey<Overflow> overflowY = enumKey(
            "overflow-y",
            Overflow.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.overflow,
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.point(s -> s.overflow, false));

    public static final StyleKey<Float> scrollbarWidth =
            floatKey("scrollbar-width", StyleScope.layout, (ctx, v) -> ctx.layoutStyle().scrollbarWidth = v);

    public static final StyleKey<BoxSizing> boxSizing = enumKey(
            "box-sizing",
            BoxSizing.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.boxSizing,
            (ctx, v) -> ctx.layoutStyle().boxSizing = v);

    public static final StyleKey<TaffyDirection> direction = enumKey(
            "direction",
            TaffyDirection.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.direction,
            true,
            (ctx, v) -> ctx.layoutStyle().direction = v);

    public static final StyleKey<Boolean> itemIsTable =
            boolKey("item-is-table", (ctx, v) -> ctx.layoutStyle().itemIsTable = v);

    public static final StyleKey<Boolean> itemIsReplaced =
            boolKey("item-is-replaced", (ctx, v) -> ctx.layoutStyle().itemIsReplaced = v);

    // endregion

    // region layout — sizing

    public static final StyleKey<TaffyDimension> width =
            dimKey("width", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.size(s -> s.size, true));

    public static final StyleKey<TaffyDimension> height =
            dimKey("height", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.size(s -> s.size, false));

    public static final StyleKey<TaffyDimension> minWidth =
            dimKey("min-width", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.size(s -> s.minSize, true));

    public static final StyleKey<TaffyDimension> minHeight =
            dimKey("min-height", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.size(s -> s.minSize, false));

    public static final StyleKey<TaffyDimension> maxWidth =
            dimKey("max-width", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.size(s -> s.maxSize, true));

    public static final StyleKey<TaffyDimension> maxHeight =
            dimKey("max-height", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.size(s -> s.maxSize, false));

    public static final StyleKey<Float> aspectRatio = key(
            "aspect-ratio",
            Float.class,
            StyleScope.layout,
            (values, ctx) -> {
                Float r = dev.vfyjxf.cloudlib.internal.ui.style.CssValues.aspectRatio(values);
                if (r == null) ctx.warn("invalid aspect-ratio");
                return r;
            },
            (ctx, v) -> ctx.layoutStyle().aspectRatio = v);

    // endregion

    // region layout — edge boxes (longhand only)

    public static final StyleKey<LengthPercentageAuto> insetTop =
            lpaKey("inset-top", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.inset, Edge.top));
    public static final StyleKey<LengthPercentageAuto> insetRight =
            lpaKey("inset-right", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.inset, Edge.right));
    public static final StyleKey<LengthPercentageAuto> insetBottom =
            lpaKey("inset-bottom", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.inset, Edge.bottom));
    public static final StyleKey<LengthPercentageAuto> insetLeft =
            lpaKey("inset-left", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.inset, Edge.left));

    public static final StyleKey<LengthPercentageAuto> marginTop =
            lpaKey("margin-top", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.margin, Edge.top));
    public static final StyleKey<LengthPercentageAuto> marginRight =
            lpaKey("margin-right", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.margin, Edge.right));
    public static final StyleKey<LengthPercentageAuto> marginBottom = lpaKey(
            "margin-bottom", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.margin, Edge.bottom));
    public static final StyleKey<LengthPercentageAuto> marginLeft =
            lpaKey("margin-left", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.margin, Edge.left));

    public static final StyleKey<LengthPercentage> paddingTop =
            lpKey("padding-top", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.padding, Edge.top));
    public static final StyleKey<LengthPercentage> paddingRight =
            lpKey("padding-right", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.padding, Edge.right));
    public static final StyleKey<LengthPercentage> paddingBottom = lpKey(
            "padding-bottom", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.padding, Edge.bottom));
    public static final StyleKey<LengthPercentage> paddingLeft =
            lpKey("padding-left", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.padding, Edge.left));

    public static final StyleKey<LengthPercentage> borderTopWidth =
            lpKey("border-top-width", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.border, Edge.top));
    public static final StyleKey<LengthPercentage> borderRightWidth = lpKey(
            "border-right-width", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.border, Edge.right));
    public static final StyleKey<LengthPercentage> borderBottomWidth = lpKey(
            "border-bottom-width", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.border, Edge.bottom));
    public static final StyleKey<LengthPercentage> borderLeftWidth = lpKey(
            "border-left-width", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.edge(s -> s.border, Edge.left));

    // endregion

    // region layout — flex

    public static final StyleKey<FlexDirection> flexDirection = enumKey(
            "flex-direction",
            FlexDirection.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flexDirection,
            (ctx, v) -> ctx.layoutStyle().flexDirection = v);

    public static final StyleKey<FlexWrap> flexWrap = enumKey(
            "flex-wrap",
            FlexWrap.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flexWrap,
            (ctx, v) -> ctx.layoutStyle().flexWrap = v);

    public static final StyleKey<Float> flexGrow =
            floatKey("flex-grow", StyleScope.layout, (ctx, v) -> ctx.layoutStyle().flexGrow = v);

    public static final StyleKey<Float> flexShrink =
            floatKey("flex-shrink", StyleScope.layout, (ctx, v) -> ctx.layoutStyle().flexShrink = v);

    public static final StyleKey<TaffyDimension> flexBasis =
            dimKey("flex-basis", (ctx, v) -> ctx.layoutStyle().flexBasis = v);

    // endregion

    // region layout — alignment

    public static final StyleKey<AlignItems> alignItems = enumKey(
            "align-items",
            AlignItems.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.alignItems,
            (ctx, v) -> ctx.layoutStyle().alignItems = v);

    public static final StyleKey<AlignItems> alignSelf = enumKey(
            "align-self",
            AlignItems.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.alignItems,
            (ctx, v) -> ctx.layoutStyle().alignSelf = v);

    public static final StyleKey<AlignItems> justifyItems = enumKey(
            "justify-items",
            AlignItems.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.alignItems,
            (ctx, v) -> ctx.layoutStyle().justifyItems = v);

    public static final StyleKey<AlignItems> justifySelf = enumKey(
            "justify-self",
            AlignItems.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.alignItems,
            (ctx, v) -> ctx.layoutStyle().justifySelf = v);

    public static final StyleKey<AlignContent> alignContent = enumKey(
            "align-content",
            AlignContent.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.alignContent,
            (ctx, v) -> ctx.layoutStyle().alignContent = v);

    public static final StyleKey<JustifyContent> justifyContent = enumKey(
            "justify-content",
            JustifyContent.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.justifyContent,
            (ctx, v) -> ctx.layoutStyle().justifyContent =
                    dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.toAlignContent(v));

    public static final StyleKey<TextAlign> textAlign = enumKey(
            "text-align",
            TextAlign.class,
            dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.textAlign,
            true,
            (ctx, v) -> ctx.layoutStyle().textAlign = v);

    // endregion

    // region layout — gap

    /** css {@code row-gap} → the vertical spacing (taffy {@code gap.height}). */
    public static final StyleKey<LengthPercentage> rowGap =
            lpKey("row-gap", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.size(s -> s.gap, false));

    /** css {@code column-gap} → the horizontal spacing (taffy {@code gap.width}). */
    public static final StyleKey<LengthPercentage> columnGap =
            lpKey("column-gap", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.size(s -> s.gap, true));

    // endregion

    // region layout — grid

    public static final StyleKey<GridAutoFlow> gridAutoFlow = key(
            "grid-auto-flow",
            GridAutoFlow.class,
            StyleScope.layout,
            (values, ctx) -> {
                ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                if (v instanceof ComponentValue.Ident id) {
                    GridAutoFlow flow =
                            switch (id.value().toLowerCase(java.util.Locale.ROOT)) {
                                case "row" -> GridAutoFlow.ROW;
                                case "column" -> GridAutoFlow.COLUMN;
                                case "dense", "row-dense" -> GridAutoFlow.ROW_DENSE;
                                case "column-dense" -> GridAutoFlow.COLUMN_DENSE;
                                default -> null;
                            };
                    if (flow != null) return flow;
                }
                ctx.warn("invalid grid-auto-flow");
                return null;
            },
            (ctx, v) -> ctx.layoutStyle().gridAutoFlow = v);

    public static final StyleKey<GridPlacement> gridRowStart = key(
            "grid-row-start",
            GridPlacement.class,
            StyleScope.layout,
            (values, ctx) -> dev.vfyjxf.cloudlib.internal.ui.style.CssValues.placement(values),
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.line(s -> s.gridRow, true));

    public static final StyleKey<GridPlacement> gridRowEnd = key(
            "grid-row-end",
            GridPlacement.class,
            StyleScope.layout,
            (values, ctx) -> dev.vfyjxf.cloudlib.internal.ui.style.CssValues.placement(values),
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.line(s -> s.gridRow, false));

    public static final StyleKey<GridPlacement> gridColumnStart = key(
            "grid-column-start",
            GridPlacement.class,
            StyleScope.layout,
            (values, ctx) -> dev.vfyjxf.cloudlib.internal.ui.style.CssValues.placement(values),
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.line(s -> s.gridColumn, true));

    public static final StyleKey<GridPlacement> gridColumnEnd = key(
            "grid-column-end",
            GridPlacement.class,
            StyleScope.layout,
            (values, ctx) -> dev.vfyjxf.cloudlib.internal.ui.style.CssValues.placement(values),
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.line(s -> s.gridColumn, false));

    // java-dsl-only grid keys — no css parser yet (track grammar lands with grid themes)

    @SuppressWarnings("unchecked")
    private static <T> StyleKey<List<T>> listKey(String id, StyleApply<List<T>> applier) {
        return key(
                id,
                (Class<List<T>>) (Class<?>) List.class,
                StyleScope.layout,
                (values, ctx) -> {
                    ctx.warn(id + " is not css-parseable yet");
                    return null;
                },
                applier);
    }

    public static final StyleKey<List<dev.vfyjxf.taffy.style.TrackSizingFunction>> gridTemplateRows =
            listKey("grid-template-rows", (ctx, v) -> ctx.layoutStyle().gridTemplateRows = v);

    public static final StyleKey<List<dev.vfyjxf.taffy.style.TrackSizingFunction>> gridTemplateColumns =
            listKey("grid-template-columns", (ctx, v) -> ctx.layoutStyle().gridTemplateColumns = v);

    public static final StyleKey<List<dev.vfyjxf.taffy.style.GridTemplateComponent>> gridTemplateRowsWithRepeat =
            listKey("grid-template-rows-with-repeat", (ctx, v) -> ctx.layoutStyle().gridTemplateRowsWithRepeat = v);

    public static final StyleKey<List<dev.vfyjxf.taffy.style.GridTemplateComponent>> gridTemplateColumnsWithRepeat =
            listKey(
                    "grid-template-columns-with-repeat",
                    (ctx, v) -> ctx.layoutStyle().gridTemplateColumnsWithRepeat = v);

    public static final StyleKey<List<dev.vfyjxf.taffy.style.GridTemplateArea>> gridTemplateAreas =
            listKey("grid-template-areas", (ctx, v) -> ctx.layoutStyle().gridTemplateAreas = v);

    public static final StyleKey<List<dev.vfyjxf.taffy.style.NamedGridLine>> gridTemplateColumnNames =
            listKey("grid-template-column-names", (ctx, v) -> ctx.layoutStyle().gridTemplateColumnNames = v);

    public static final StyleKey<List<dev.vfyjxf.taffy.style.NamedGridLine>> gridTemplateRowNames =
            listKey("grid-template-row-names", (ctx, v) -> ctx.layoutStyle().gridTemplateRowNames = v);

    public static final StyleKey<List<dev.vfyjxf.taffy.style.TrackSizingFunction>> gridAutoRows =
            listKey("grid-auto-rows", (ctx, v) -> ctx.layoutStyle().gridAutoRows = v);

    public static final StyleKey<List<dev.vfyjxf.taffy.style.TrackSizingFunction>> gridAutoColumns =
            listKey("grid-auto-columns", (ctx, v) -> ctx.layoutStyle().gridAutoColumns = v);

    // endregion

    // region visual

    /**
     * {@code background} — a texture function ({@code nine-slice(...)},
     * {@code sprite(...)}, …) or a bare {@code <color>} wrapped in
     * {@link dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture}.
     */
    public static final StyleKey<VisualTexture> background = key(
            "background",
            VisualTexture.class,
            StyleScope.visual,
            (values, ctx) -> {
                ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                if (v == null) return null;
                VisualTexture t = dev.vfyjxf.cloudlib.internal.ui.style.CssTextures.texture(v);
                if (t != null) return t;
                Integer c = dev.vfyjxf.cloudlib.internal.ui.style.CssValues.color(v);
                return c != null ? new dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture(c) : null;
            },
            (ctx, v) -> ctx.visualContext().setBackground(v));

    /** {@code background-color} — always a color texture. */
    public static final StyleKey<Integer> backgroundColor =
            colorKey("background-color", false, (ctx, v) -> ctx.visualContext()
                    .setBackground(new dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture(v)));

    public static final StyleKey<VisualTexture> icon =
            textureKey("icon", (ctx, v) -> ctx.visualContext().setIcon(v));

    /**
     * {@code color} — the inherited text color. Aliases: {@code text-color},
     * {@code textcolor}.
     */
    public static final StyleKey<Integer> color =
            colorKey("color", true, (ctx, v) -> ctx.visualContext().textColor(v));

    public static final StyleKey<Float> opacity = floatKey(
            "opacity", StyleScope.custom, dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.customProp("opacity"));

    public static final StyleKey<Integer> zIndex =
            intKey("z-index", StyleScope.visual, (ctx, v) -> ctx.visualContext().setZIndex(v));

    public static final StyleKey<Shadow> boxShadow = key(
            "box-shadow",
            Shadow.class,
            StyleScope.visual,
            (values, ctx) -> {
                Shadow s = dev.vfyjxf.cloudlib.internal.ui.style.CssValues.shadow(values);
                if (s == null) ctx.warn("invalid box-shadow");
                return s;
            },
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.shadow);

    /** Visual border color — pairs with {@link #borderThickness}. */
    public static final StyleKey<Integer> borderColor =
            colorKey("border-color", false, dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.borderColor);

    /**
     * Visual border thickness (the drawn outline width) — distinct from the
     * taffy {@code border} layout rect.
     */
    public static final StyleKey<Float> borderThickness = floatKey(
            "border-thickness", StyleScope.visual, dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.borderWidth);

    /**
     * Whether text drawn by the widget renders with a drop shadow — inherited so
     * a themed panel silences shadows on every label inside it. The value lives
     * in the context map; widgets read it via {@code style().get(textShadow)}.
     */
    public static final StyleKey<Boolean> textShadow = StyleRegistry.get()
            .register(new StyleKey<>(
                    "text-shadow",
                    Boolean.class,
                    StyleScope.visual,
                    true,
                    null,
                    dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.boolParser("text-shadow"),
                    (ctx, v) -> {},
                    Object::toString));

    /** The scene compositing layer the widget renders into. */
    public static final StyleKey<SceneLayer> sceneLayer = key(
            "scene-layer",
            SceneLayer.class,
            StyleScope.custom,
            (values, ctx) -> {
                ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                if (v instanceof ComponentValue.Ident id) {
                    try {
                        return SceneLayer.valueOf(id.value().toUpperCase(java.util.Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        ctx.warn("unknown scene-layer " + id.value());
                        return null;
                    }
                }
                return null;
            },
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.sceneLayer);

    /** Scrollbar texture/size data — stored as a visual custom property. */
    public static final StyleKey<ScrollbarStyleData> scrollbarStyle = key(
            "scrollbar-style",
            ScrollbarStyleData.class,
            StyleScope.custom,
            (values, ctx) -> {
                // scrollbar-style(track, thumb[, width[, minThumb]])
                ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
                if (!(v instanceof ComponentValue.Function fn)) {
                    ctx.warn("scrollbar-style expects a function value");
                    return null;
                }
                List<ComponentValue> args = fn.args().stream()
                        .filter(c -> c != ComponentValue.Whitespace.instance
                                && !(c instanceof ComponentValue.Delim d && d.value() == ','))
                        .toList();
                VisualTexture track =
                        args.size() > 0 ? dev.vfyjxf.cloudlib.internal.ui.style.CssTextures.texture(args.get(0)) : null;
                VisualTexture thumb =
                        args.size() > 1 ? dev.vfyjxf.cloudlib.internal.ui.style.CssTextures.texture(args.get(1)) : null;
                int w = args.size() > 2 && args.get(2) instanceof ComponentValue.NumericValue n ? (int) n.value() : -1;
                int min = args.size() > 3 && args.get(3) instanceof ComponentValue.NumericValue n2
                        ? (int) n2.value()
                        : -1;
                return new ScrollbarStyleData(track, thumb, w, min);
            },
            dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.customProp(ScrollbarStyleData.propertyKey));

    /** Border texture — stored as a visual custom property for renderers. */
    public static final StyleKey<VisualTexture> borderTexture = textureKey(
            "border-texture", dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies.customProp("border-texture"));

    // endregion

    // region shorthands

    static {
        var registry = StyleRegistry.get();

        // edge boxes — css 1/2/3/4-value expansion
        edgeBox(
                registry,
                "padding",
                paddingTop,
                paddingRight,
                paddingBottom,
                paddingLeft,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthPercentage);
        edgeBox(
                registry,
                "margin",
                marginTop,
                marginRight,
                marginBottom,
                marginLeft,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthAuto);
        edgeBox(
                registry,
                "inset",
                insetTop,
                insetRight,
                insetBottom,
                insetLeft,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthAuto);
        edgeBox(
                registry,
                "border-width",
                borderTopWidth,
                borderRightWidth,
                borderBottomWidth,
                borderLeftWidth,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthPercentage);
        edgeBox(
                registry,
                "border",
                borderTopWidth,
                borderRightWidth,
                borderBottomWidth,
                borderLeftWidth,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthPercentage);

        // logical halves
        edgePair(
                registry,
                "padding-horizontal",
                paddingLeft,
                paddingRight,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthPercentage);
        edgePair(
                registry,
                "padding-vertical",
                paddingTop,
                paddingBottom,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthPercentage);
        edgePair(
                registry,
                "margin-horizontal",
                marginLeft,
                marginRight,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthAuto);
        edgePair(
                registry,
                "margin-vertical",
                marginTop,
                marginBottom,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthAuto);
        edgePair(
                registry,
                "inset-horizontal",
                insetLeft,
                insetRight,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthAuto);
        edgePair(
                registry,
                "inset-vertical",
                insetTop,
                insetBottom,
                dev.vfyjxf.cloudlib.internal.ui.style.CssValues::lengthAuto);

        // size
        sizePair(registry, "size", width, height, dev.vfyjxf.cloudlib.internal.ui.style.CssValues::dimension);
        sizePair(registry, "min-size", minWidth, minHeight, dev.vfyjxf.cloudlib.internal.ui.style.CssValues::dimension);
        sizePair(registry, "max-size", maxWidth, maxHeight, dev.vfyjxf.cloudlib.internal.ui.style.CssValues::dimension);

        // gap — css order is <row> <column>
        registry.registerShorthand("gap", (values, ctx) -> {
            List<ComponentValue> flat = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flat(values);
            if (flat.isEmpty() || flat.size() > 2) return null;
            LengthPercentage row = dev.vfyjxf.cloudlib.internal.ui.style.CssValues.lengthPercentage(flat.get(0));
            if (row == null) return null;
            LengthPercentage col = flat.size() > 1
                    ? dev.vfyjxf.cloudlib.internal.ui.style.CssValues.lengthPercentage(flat.get(1))
                    : row;
            return col == null ? null : List.of(rowGap.of(row), columnGap.of(col));
        });

        // overflow: <x> [y]
        registry.registerShorthand("overflow", (values, ctx) -> {
            List<ComponentValue> flat = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flat(values);
            if (flat.isEmpty() || flat.size() > 2) return null;
            Overflow x = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.enumValue(
                    flat.get(0), Overflow.class, dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.overflow);
            if (x == null) return null;
            Overflow y = flat.size() > 1
                    ? dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.enumValue(
                            flat.get(1), Overflow.class, dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.overflow)
                    : x;
            return y == null ? null : List.of(overflowX.of(x), overflowY.of(y));
        });

        // flex: none | initial | auto | <grow> [<shrink> [<basis>]]
        registry.registerShorthand("flex", (values, ctx) -> {
            List<ComponentValue> flat = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flat(values);
            if (flat.isEmpty()) return null;
            if (flat.size() == 1 && flat.get(0) instanceof ComponentValue.Ident id) {
                return switch (id.value().toLowerCase(java.util.Locale.ROOT)) {
                    case "none", "initial" -> List.of(flexGrow.of(0f), flexShrink.of(0f));
                    case "auto" -> List.of(flexGrow.of(1f), flexShrink.of(1f));
                    default -> null;
                };
            }
            List<StyleValue<?>> out = new ArrayList<>(3);
            Float grow = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.number(flat.subList(0, 1));
            if (grow == null) return null;
            out.add(flexGrow.of(grow));
            if (flat.size() > 1) {
                Float shrink = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.number(flat.subList(1, 2));
                if (shrink == null) return null;
                out.add(flexShrink.of(shrink));
            }
            if (flat.size() > 2) {
                TaffyDimension basis = dev.vfyjxf.cloudlib.internal.ui.style.CssValues.dimension(flat.get(2));
                if (basis == null) return null;
                out.add(flexBasis.of(basis));
            }
            return out;
        });

        // flex-flow: <direction> [wrap]
        registry.registerShorthand("flex-flow", (values, ctx) -> {
            List<ComponentValue> flat = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flat(values);
            FlexDirection d = null;
            FlexWrap w = null;
            for (ComponentValue c : flat) {
                if (d == null)
                    d = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.enumValue(
                            c, FlexDirection.class, dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flexDirection);
                if (w == null)
                    w = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.enumValue(
                            c, FlexWrap.class, dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flexWrap);
            }
            if (d == null && w == null) return null;
            List<StyleValue<?>> out = new ArrayList<>(2);
            if (d != null) out.add(flexDirection.of(d));
            if (w != null) out.add(flexWrap.of(w));
            return out;
        });

        // grid-row / grid-column: <start> [/ <end>]
        registry.registerShorthand("grid-row", gridLine(gridRowStart, gridRowEnd));
        registry.registerShorthand("grid-column", gridLine(gridColumnStart, gridColumnEnd));

        // aliases
        registry.registerAlias("text-color", "color");
        registry.registerAlias("textcolor", "color");
        registry.registerAlias("zindex", "z-index");
        registry.registerAlias("shadow", "box-shadow");
    }

    /** css 1/2/3/4-value edge-box expansion → four longhand keys. */
    private static <T> void edgeBox(
            StyleRegistry registry,
            String name,
            StyleKey<T> top,
            StyleKey<T> right,
            StyleKey<T> bottom,
            StyleKey<T> left,
            Function<ComponentValue, @Nullable T> convert) {
        registry.registerShorthand(name, (values, ctx) -> {
            List<ComponentValue> flat = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flat(values);
            if (flat.isEmpty() || flat.size() > 4) return null;
            List<T> edges = new ArrayList<>(flat.size());
            for (ComponentValue v : flat) {
                T t = convert.apply(v);
                if (t == null) {
                    ctx.warn("invalid " + name + " edge", v);
                    return null;
                }
                edges.add(t);
            }
            T t = edges.get(0);
            T r = edges.size() > 1 ? edges.get(1) : t;
            T b = edges.size() > 2 ? edges.get(2) : t;
            T l = edges.size() > 3 ? edges.get(3) : r;
            return List.of(top.of(t), right.of(r), bottom.of(b), left.of(l));
        });
    }

    /** two-value logical-half shorthand → two longhand keys. */
    private static <T> void edgePair(
            StyleRegistry registry,
            String name,
            StyleKey<T> a,
            StyleKey<T> b,
            Function<ComponentValue, @Nullable T> convert) {
        registry.registerShorthand(name, (values, ctx) -> {
            ComponentValue v = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.single(values);
            if (v == null) return null;
            T t = convert.apply(v);
            if (t == null) {
                ctx.warn("invalid " + name, v);
                return null;
            }
            return List.of(a.of(t), b.of(t));
        });
    }

    /** two-value width/height shorthand. */
    private static <T> void sizePair(
            StyleRegistry registry,
            String name,
            StyleKey<T> width,
            StyleKey<T> height,
            Function<ComponentValue, @Nullable T> convert) {
        registry.registerShorthand(name, (values, ctx) -> {
            List<ComponentValue> flat = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flat(values);
            if (flat.isEmpty() || flat.size() > 2) return null;
            T w = convert.apply(flat.get(0));
            if (w == null) return null;
            T h = flat.size() > 1 ? convert.apply(flat.get(1)) : w;
            return h == null ? null : List.of(width.of(w), height.of(h));
        });
    }

    /** {@code grid-row|grid-column: <start> [/ <end>]} → start/end placements. */
    private static dev.vfyjxf.cloudlib.api.ui.style.key.Shorthand gridLine(
            StyleKey<GridPlacement> start, StyleKey<GridPlacement> end) {
        return (values, ctx) -> {
            List<ComponentValue> flat = dev.vfyjxf.cloudlib.internal.ui.style.CssEnums.flat(values);
            int slash = -1;
            for (int i = 0; i < flat.size(); i++) {
                if (flat.get(i) instanceof ComponentValue.Delim d && d.value() == '/') {
                    slash = i;
                    break;
                }
            }
            List<ComponentValue> startToks = slash < 0 ? flat : flat.subList(0, slash);
            List<ComponentValue> endToks = slash < 0 ? List.of() : flat.subList(slash + 1, flat.size());
            GridPlacement s = dev.vfyjxf.cloudlib.internal.ui.style.CssValues.placement(startToks);
            if (s == null) return null;
            if (endToks.isEmpty()) {
                return List.of(start.of(s));
            }
            GridPlacement e = dev.vfyjxf.cloudlib.internal.ui.style.CssValues.placement(endToks);
            return e == null ? null : List.of(start.of(s), end.of(e));
        };
    }

    // endregion
}
