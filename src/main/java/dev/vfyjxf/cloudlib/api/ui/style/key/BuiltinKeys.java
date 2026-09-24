package dev.vfyjxf.cloudlib.api.ui.style.key;

import dev.vfyjxf.cloudlib.api.css.ComponentValue;
import dev.vfyjxf.cloudlib.api.css.Tokens;
import dev.vfyjxf.cloudlib.api.ui.base.SceneLayer;
import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import dev.vfyjxf.cloudlib.api.ui.style.ScrollbarStyleData;
import dev.vfyjxf.cloudlib.api.ui.style.Shadow;
import dev.vfyjxf.cloudlib.api.ui.style.StyleVar;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import dev.vfyjxf.cloudlib.internal.ui.style.CssEnums;
import dev.vfyjxf.cloudlib.internal.ui.style.CssTextures;
import dev.vfyjxf.cloudlib.internal.ui.style.CssValues;
import dev.vfyjxf.cloudlib.internal.ui.style.StyleApplies;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.JustifyContent;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TextAlign;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The builtin style vocabulary — every supported css property as a
 * {@link StyleKey} constant, plus the fixed shorthand/alias tables.
 * <p>
 * The vocabulary is <b>closed</b>: {@code StyleKey}'s constructor is
 * package-private, so this class is the only place keys can be built. Custom
 * styling goes through css custom properties ({@code --*}) read via
 * {@link StyleVar} — there is no registration.
 * <p>
 * Longhands only: box shorthands ({@code padding}, {@code margin}, {@code
 * inset}, {@code border-width}, …) live in the shorthand table and expand into
 * per-edge keys at declaration-collection time, exactly like css.
 */
public class BuiltinKeys {

    /** Subclassing exists only for the {@code Styles} facade. */
    protected BuiltinKeys() {}

    private static final Map<String, StyleKey<?>> keys = new LinkedHashMap<>();
    private static final Map<String, Shorthand> shorthands = new LinkedHashMap<>();
    private static final Map<String, String> aliases = new LinkedHashMap<>();

    /** Forces {@code <clinit>} — every constant and table entry is built. */
    public static void init() {}

    /** Resolves a css property name (aliases applied) to its key. */
    public static @Nullable StyleKey<?> byId(String cssName) {
        return keys.get(aliases.getOrDefault(cssName, cssName));
    }

    /** The shorthand expander for a css name, or {@code null}. */
    public static @Nullable Shorthand shorthand(String name) {
        return shorthands.get(name);
    }

    /** All builtin keys. */
    public static Collection<StyleKey<?>> all() {
        return Collections.unmodifiableCollection(keys.values());
    }

    /** All builtin longhand ids + shorthand names, sorted. */
    public static List<String> names() {
        var out = new ArrayList<String>(keys.keySet());
        out.addAll(shorthands.keySet());
        Collections.sort(out);
        return out;
    }

    // region construction helpers

    private static <T> StyleKey<T> track(StyleKey<T> key) {
        if (keys.putIfAbsent(key.id(), key) != null) {
            throw new IllegalStateException("duplicate style key: " + key.id());
        }
        return key;
    }

    private static void shorthand(String name, Shorthand expander) {
        shorthands.put(name, expander);
    }

    private static void alias(String alias, String canonicalId) {
        aliases.put(alias, canonicalId);
    }

    private static <T> StyleKey<T> key(
        String id,
        Class<T> type,
        StyleScope scope,
        StyleParser<T> parser,
        StyleApply<T> applier
    ) {
        return track(new StyleKey<>(id, type, scope, parser, applier));
    }

    private static <T> StyleKey<T> key(
        String id,
        Class<T> type,
        StyleScope scope,
        boolean inherited,
        StyleParser<T> parser,
        StyleApply<T> applier
    ) {
        return track(new StyleKey<>(id, type, scope, inherited, null, parser, applier, Objects::toString));
    }

    private static <E extends Enum<E>> StyleKey<E> enumKey(
        String id,
        Class<E> type,
        Map<String, E> aliases,
        StyleApply<E> applier
    ) {
        return key(id, type, StyleScope.layout, CssEnums.parser(type, aliases), applier);
    }

    private static <E extends Enum<E>> StyleKey<E> enumKey(
        String id,
        Class<E> type,
        Map<String, E> aliases,
        boolean inherited,
        StyleApply<E> applier
    ) {
        return key(id, type, StyleScope.layout, inherited, CssEnums.parser(type, aliases), applier);
    }

    private static StyleKey<LengthPercentage> lpKey(String id, StyleApply<LengthPercentage> applier) {
        return key(id, LengthPercentage.class, StyleScope.layout, (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
            LengthPercentage r = v == null ? null : CssValues.lengthPercentage(v);
            if (r == null) ctx.warn("invalid <length-percentage> for " + id);
            return r;
        }, applier);
    }

    private static StyleKey<LengthPercentageAuto> lpaKey(String id, StyleApply<LengthPercentageAuto> applier) {
        return key(id, LengthPercentageAuto.class, StyleScope.layout, (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
            LengthPercentageAuto r = v == null ? null : CssValues.lengthAuto(v);
            if (r == null) ctx.warn("invalid <length-percentage>|auto for " + id);
            return r;
        }, applier);
    }

    private static StyleKey<TaffyDimension> dimKey(String id, StyleApply<TaffyDimension> applier) {
        return key(id, TaffyDimension.class, StyleScope.layout, (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
            TaffyDimension r = v == null ? null : CssValues.dimension(v);
            if (r == null) ctx.warn("invalid <dimension> for " + id);
            return r;
        }, applier);
    }

    private static StyleKey<Float> floatKey(String id, StyleScope scope, StyleApply<Float> applier) {
        return key(id, Float.class, scope, CssEnums.floatParser(id), applier);
    }

    private static StyleKey<Integer> intKey(String id, StyleScope scope, StyleApply<Integer> applier) {
        return key(id, Integer.class, scope, CssEnums.intParser(id), applier);
    }

    private static StyleKey<Boolean> boolKey(String id, StyleApply<Boolean> applier) {
        return key(id, Boolean.class, StyleScope.layout, CssEnums.boolParser(id), applier);
    }

    private static StyleKey<Integer> colorKey(String id, boolean inherited, StyleApply<Integer> applier) {
        return key(id, Integer.class, StyleScope.visual, inherited, (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
            Integer c = v == null ? null : CssValues.color(v);
            if (c == null) ctx.warn("invalid <color> for " + id);
            return c;
        }, applier);
    }

    private static StyleKey<VisualTexture> textureKey(String id, StyleApply<VisualTexture> applier) {
        return key(id, VisualTexture.class, StyleScope.visual, (values, ctx) -> {
            VisualTexture t = CssTextures.parse(values);
            if (t == null) ctx.warn("invalid texture for " + id);
            return t;
        }, applier);
    }

    /**
     * A property whose value shape is not modelled yet — the declaration keeps
     * its raw token stream so it parses, inherits nothing and is inspectable
     * until a consumer claims it.
     */
    private static StyleKey<Tokens> rawKey(String id, StyleApply<Tokens> applier) {
        return key(
            id,
            Tokens.class,
            StyleScope.visual,
            (values, ctx) -> values.isEmpty() ? null : Tokens.of(values),
            applier
        );
    }

    private static StyleKey<ResourceLocation> locationKey(
        String id,
        boolean inherited,
        StyleApply<ResourceLocation> applier
    ) {
        return key(id, ResourceLocation.class, StyleScope.visual, inherited, (values, ctx) -> {
            ResourceLocation loc = location(values);
            if (loc == null) ctx.warn("invalid resource location for " + id);
            return loc;
        }, applier);
    }

    /**
     * A widget metric — a number in ui units, written either as a px length
     * ({@code 8px}) or bare ({@code 8}). Not a taffy dimension: the value never
     * reaches the layout style, so the unitless form stays legal here.
     */
    private static StyleKey<Float> metricKey(String id, boolean inherited, StyleApply<Float> applier) {
        return key(id, Float.class, StyleScope.visual, inherited, (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
            Float n = v == null ? null : CssValues.lengthPx(v);
            if (n == null) n = CssEnums.number(values);
            if (n == null) ctx.warn("invalid number for " + id);
            return n;
        }, applier);
    }

    /**
     * A resource location from a declaration — {@code ns:path} arrives as three
     * tokens ({@code ns}, {@code :}, {@code path}), so a multi-token value is
     * re-serialized before parsing.
     */
    private static @Nullable ResourceLocation location(List<ComponentValue> values) {
        List<ComponentValue> flat = CssEnums.flat(values);
        if (flat.size() == 1) {
            ResourceLocation single = CssTextures.location(flat.get(0));
            if (single != null) return single;
        }
        return flat.isEmpty() ? null : ResourceLocation.tryParse(Tokens.serialize(flat));
    }

    // endregion

    // region layout — box

    public static final StyleKey<TaffyDisplay> display = enumKey(
        "display",
        TaffyDisplay.class,
        CssEnums.display,
        (ctx, v) -> ctx.layoutStyle().display = v
    );

    public static final StyleKey<TaffyPosition> position = enumKey(
        "position",
        TaffyPosition.class,
        CssEnums.position,
        (ctx, v) -> ctx.layoutStyle().position = v
    );

    public static final StyleKey<Overflow> overflowX = enumKey(
        "overflow-x",
        Overflow.class,
        CssEnums.overflow,
        StyleApplies.point(s -> s.overflow, true)
    );

    public static final StyleKey<Overflow> overflowY = enumKey(
        "overflow-y",
        Overflow.class,
        CssEnums.overflow,
        StyleApplies.point(s -> s.overflow, false)
    );

    public static final StyleKey<Float> scrollbarWidth = floatKey(
        "scrollbar-width",
        StyleScope.layout,
        (ctx, v) -> ctx.layoutStyle().scrollbarWidth = v
    );

    public static final StyleKey<BoxSizing> boxSizing = enumKey(
        "box-sizing",
        BoxSizing.class,
        CssEnums.boxSizing,
        (ctx, v) -> ctx.layoutStyle().boxSizing = v
    );

    public static final StyleKey<TaffyDirection> direction = enumKey(
        "direction",
        TaffyDirection.class,
        CssEnums.direction,
        true,
        (ctx, v) -> ctx.layoutStyle().direction = v
    );

    public static final StyleKey<Boolean> itemIsTable = boolKey(
        "item-is-table",
        (ctx, v) -> ctx.layoutStyle().itemIsTable = v
    );

    public static final StyleKey<Boolean> itemIsReplaced = boolKey(
        "item-is-replaced",
        (ctx, v) -> ctx.layoutStyle().itemIsReplaced = v
    );

    // endregion

    // region layout — sizing

    public static final StyleKey<TaffyDimension> width = dimKey("width", StyleApplies.size(s -> s.size, true));

    public static final StyleKey<TaffyDimension> height = dimKey("height", StyleApplies.size(s -> s.size, false));

    public static final StyleKey<TaffyDimension> minWidth = dimKey(
        "min-width",
        StyleApplies.size(s -> s.minSize, true)
    );

    public static final StyleKey<TaffyDimension> minHeight = dimKey(
        "min-height",
        StyleApplies.size(s -> s.minSize, false)
    );

    public static final StyleKey<TaffyDimension> maxWidth = dimKey(
        "max-width",
        StyleApplies.size(s -> s.maxSize, true)
    );

    public static final StyleKey<TaffyDimension> maxHeight = dimKey(
        "max-height",
        StyleApplies.size(s -> s.maxSize, false)
    );

    public static final StyleKey<Float> aspectRatio = key(
        "aspect-ratio",
        Float.class,
        StyleScope.layout,
        (values, ctx) -> {
            Float r = CssValues.aspectRatio(values);
            if (r == null) ctx.warn("invalid aspect-ratio");
            return r;
        },
        (ctx, v) -> ctx.layoutStyle().aspectRatio = v
    );

    // endregion

    // region layout — edge boxes (longhand only)

    public static final StyleKey<LengthPercentageAuto> insetTop = lpaKey(
        "inset-top",
        StyleApplies.edge(s -> s.inset, Edge.top)
    );
    public static final StyleKey<LengthPercentageAuto> insetRight = lpaKey(
        "inset-right",
        StyleApplies.edge(s -> s.inset, Edge.right)
    );
    public static final StyleKey<LengthPercentageAuto> insetBottom = lpaKey(
        "inset-bottom",
        StyleApplies.edge(s -> s.inset, Edge.bottom)
    );
    public static final StyleKey<LengthPercentageAuto> insetLeft = lpaKey(
        "inset-left",
        StyleApplies.edge(s -> s.inset, Edge.left)
    );

    public static final StyleKey<LengthPercentageAuto> marginTop = lpaKey(
        "margin-top",
        StyleApplies.edge(s -> s.margin, Edge.top)
    );
    public static final StyleKey<LengthPercentageAuto> marginRight = lpaKey(
        "margin-right",
        StyleApplies.edge(s -> s.margin, Edge.right)
    );
    public static final StyleKey<LengthPercentageAuto> marginBottom = lpaKey(
        "margin-bottom",
        StyleApplies.edge(s -> s.margin, Edge.bottom)
    );
    public static final StyleKey<LengthPercentageAuto> marginLeft = lpaKey(
        "margin-left",
        StyleApplies.edge(s -> s.margin, Edge.left)
    );

    public static final StyleKey<LengthPercentage> paddingTop = lpKey(
        "padding-top",
        StyleApplies.edge(s -> s.padding, Edge.top)
    );
    public static final StyleKey<LengthPercentage> paddingRight = lpKey(
        "padding-right",
        StyleApplies.edge(s -> s.padding, Edge.right)
    );
    public static final StyleKey<LengthPercentage> paddingBottom = lpKey(
        "padding-bottom",
        StyleApplies.edge(s -> s.padding, Edge.bottom)
    );
    public static final StyleKey<LengthPercentage> paddingLeft = lpKey(
        "padding-left",
        StyleApplies.edge(s -> s.padding, Edge.left)
    );

    public static final StyleKey<LengthPercentage> borderTopWidth = lpKey(
        "border-top-width",
        StyleApplies.edge(s -> s.border, Edge.top)
    );
    public static final StyleKey<LengthPercentage> borderRightWidth = lpKey(
        "border-right-width",
        StyleApplies.edge(s -> s.border, Edge.right)
    );
    public static final StyleKey<LengthPercentage> borderBottomWidth = lpKey(
        "border-bottom-width",
        StyleApplies.edge(s -> s.border, Edge.bottom)
    );
    public static final StyleKey<LengthPercentage> borderLeftWidth = lpKey(
        "border-left-width",
        StyleApplies.edge(s -> s.border, Edge.left)
    );

    // endregion

    // region layout — flex

    public static final StyleKey<FlexDirection> flexDirection = enumKey(
        "flex-direction",
        FlexDirection.class,
        CssEnums.flexDirection,
        (ctx, v) -> ctx.layoutStyle().flexDirection = v
    );

    public static final StyleKey<FlexWrap> flexWrap = enumKey(
        "flex-wrap",
        FlexWrap.class,
        CssEnums.flexWrap,
        (ctx, v) -> ctx.layoutStyle().flexWrap = v
    );

    public static final StyleKey<Float> flexGrow = floatKey(
        "flex-grow",
        StyleScope.layout,
        (ctx, v) -> ctx.layoutStyle().flexGrow = v
    );

    public static final StyleKey<Float> flexShrink = floatKey(
        "flex-shrink",
        StyleScope.layout,
        (ctx, v) -> ctx.layoutStyle().flexShrink = v
    );

    public static final StyleKey<TaffyDimension> flexBasis = dimKey(
        "flex-basis",
        (ctx, v) -> ctx.layoutStyle().flexBasis = v
    );

    // endregion

    // region layout — alignment

    public static final StyleKey<AlignItems> alignItems = enumKey(
        "align-items",
        AlignItems.class,
        CssEnums.alignItems,
        (ctx, v) -> ctx.layoutStyle().alignItems = v
    );

    public static final StyleKey<AlignItems> alignSelf = enumKey(
        "align-self",
        AlignItems.class,
        CssEnums.alignItems,
        (ctx, v) -> ctx.layoutStyle().alignSelf = v
    );

    public static final StyleKey<AlignItems> justifyItems = enumKey(
        "justify-items",
        AlignItems.class,
        CssEnums.alignItems,
        (ctx, v) -> ctx.layoutStyle().justifyItems = v
    );

    public static final StyleKey<AlignItems> justifySelf = enumKey(
        "justify-self",
        AlignItems.class,
        CssEnums.alignItems,
        (ctx, v) -> ctx.layoutStyle().justifySelf = v
    );

    public static final StyleKey<AlignContent> alignContent = enumKey(
        "align-content",
        AlignContent.class,
        CssEnums.alignContent,
        (ctx, v) -> ctx.layoutStyle().alignContent = v
    );

    public static final StyleKey<JustifyContent> justifyContent = enumKey(
        "justify-content",
        JustifyContent.class,
        CssEnums.justifyContent,
        (ctx, v) -> ctx.layoutStyle().justifyContent = CssEnums.toAlignContent(v)
    );

    public static final StyleKey<TextAlign> textAlign = enumKey(
        "text-align",
        TextAlign.class,
        CssEnums.textAlign,
        true,
        (ctx, v) -> ctx.layoutStyle().textAlign = v
    );

    // endregion

    // region layout — gap

    /** css {@code row-gap} → the vertical spacing (taffy {@code gap.height}). */
    public static final StyleKey<LengthPercentage> rowGap = lpKey("row-gap", StyleApplies.size(s -> s.gap, false));

    /** css {@code column-gap} → the horizontal spacing (taffy {@code gap.width}). */
    public static final StyleKey<LengthPercentage> columnGap = lpKey("column-gap", StyleApplies.size(s -> s.gap, true));

    // endregion

    // region layout — grid

    public static final StyleKey<GridAutoFlow> gridAutoFlow = key(
        "grid-auto-flow",
        GridAutoFlow.class,
        StyleScope.layout,
        (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
            if (v instanceof ComponentValue.Ident id) {
                GridAutoFlow flow = switch (id.value().toLowerCase(Locale.ROOT)) {
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
        (ctx, v) -> ctx.layoutStyle().gridAutoFlow = v
    );

    public static final StyleKey<GridPlacement> gridRowStart = key(
        "grid-row-start",
        GridPlacement.class,
        StyleScope.layout,
        (values, ctx) -> CssValues.placement(values),
        StyleApplies.line(s -> s.gridRow, true)
    );

    public static final StyleKey<GridPlacement> gridRowEnd = key(
        "grid-row-end",
        GridPlacement.class,
        StyleScope.layout,
        (values, ctx) -> CssValues.placement(values),
        StyleApplies.line(s -> s.gridRow, false)
    );

    public static final StyleKey<GridPlacement> gridColumnStart = key(
        "grid-column-start",
        GridPlacement.class,
        StyleScope.layout,
        (values, ctx) -> CssValues.placement(values),
        StyleApplies.line(s -> s.gridColumn, true)
    );

    public static final StyleKey<GridPlacement> gridColumnEnd = key(
        "grid-column-end",
        GridPlacement.class,
        StyleScope.layout,
        (values, ctx) -> CssValues.placement(values),
        StyleApplies.line(s -> s.gridColumn, false)
    );

    // java-dsl-only grid keys — no css parser yet (track grammar lands with grid themes)

    @SuppressWarnings("unchecked")
    private static <T> StyleKey<List<T>> listKey(String id, StyleApply<List<T>> applier) {
        return key(id, (Class<List<T>>) (Class<?>) List.class, StyleScope.layout, (values, ctx) -> {
            ctx.warn(id + " is not css-parseable yet");
            return null;
        }, applier);
    }

    public static final StyleKey<List<TrackSizingFunction>> gridTemplateRows = listKey(
        "grid-template-rows",
        (ctx, v) -> ctx.layoutStyle().gridTemplateRows = v
    );

    public static final StyleKey<List<TrackSizingFunction>> gridTemplateColumns = listKey(
        "grid-template-columns",
        (ctx, v) -> ctx.layoutStyle().gridTemplateColumns = v
    );

    public static final StyleKey<List<GridTemplateComponent>> gridTemplateRowsWithRepeat = listKey(
        "grid-template-rows-with-repeat",
        (ctx, v) -> ctx.layoutStyle().gridTemplateRowsWithRepeat = v
    );

    public static final StyleKey<List<GridTemplateComponent>> gridTemplateColumnsWithRepeat = listKey(
        "grid-template-columns-with-repeat",
        (ctx, v) -> ctx.layoutStyle().gridTemplateColumnsWithRepeat = v
    );

    public static final StyleKey<List<GridTemplateArea>> gridTemplateAreas = listKey(
        "grid-template-areas",
        (ctx, v) -> ctx.layoutStyle().gridTemplateAreas = v
    );

    public static final StyleKey<List<NamedGridLine>> gridTemplateColumnNames = listKey(
        "grid-template-column-names",
        (ctx, v) -> ctx.layoutStyle().gridTemplateColumnNames = v
    );

    public static final StyleKey<List<NamedGridLine>> gridTemplateRowNames = listKey(
        "grid-template-row-names",
        (ctx, v) -> ctx.layoutStyle().gridTemplateRowNames = v
    );

    public static final StyleKey<List<TrackSizingFunction>> gridAutoRows = listKey(
        "grid-auto-rows",
        (ctx, v) -> ctx.layoutStyle().gridAutoRows = v
    );

    public static final StyleKey<List<TrackSizingFunction>> gridAutoColumns = listKey(
        "grid-auto-columns",
        (ctx, v) -> ctx.layoutStyle().gridAutoColumns = v
    );

    // endregion

    // region visual

    /**
     * {@code background} — a texture function ({@code nine-slice(...)},
     * {@code sprite(...)}, …) with its optional modifier chain, or a bare
     * {@code <color>} wrapped in {@link ColorTexture}.
     */
    public static final StyleKey<VisualTexture> background = key(
        "background",
        VisualTexture.class,
        StyleScope.visual,
        (values, ctx) -> {
            VisualTexture t = CssTextures.parse(values);
            if (t != null) return t;
            ComponentValue v = CssEnums.single(values);
            Integer c = v == null ? null : CssValues.color(v);
            return c != null ? new ColorTexture(c) : null;
        },
        (ctx, v) -> ctx.visualContext().setBackground(v)
    );

    /** {@code background-color} — always a color texture. */
    public static final StyleKey<Integer> backgroundColor = colorKey(
        "background-color",
        false,
        (ctx, v) -> ctx.visualContext().setBackground(new ColorTexture(v))
    );

    public static final StyleKey<VisualTexture> icon = textureKey("icon", (ctx, v) -> ctx.visualContext().setIcon(v));

    /**
     * {@code color} — the inherited text color. Aliases: {@code text-color},
     * {@code textcolor}.
     */
    public static final StyleKey<Integer> color = colorKey("color", true, (ctx, v) -> ctx.visualContext().textColor(v));

    public static final StyleKey<Float> opacity = floatKey(
        "opacity",
        StyleScope.custom,
        StyleApplies.customProp("opacity")
    );

    public static final StyleKey<Integer> zIndex = intKey(
        "z-index",
        StyleScope.visual,
        (ctx, v) -> ctx.visualContext().setZIndex(v)
    );

    public static final StyleKey<Shadow> boxShadow = key(
        "box-shadow",
        Shadow.class,
        StyleScope.visual,
        (values, ctx) -> {
            Shadow s = CssValues.shadow(values);
            if (s == null) ctx.warn("invalid box-shadow");
            return s;
        },
        StyleApplies.shadow
    );

    /** Visual border color — pairs with {@link #borderThickness}. */
    public static final StyleKey<Integer> borderColor = colorKey("border-color", false, StyleApplies.borderColor);

    /**
     * Visual border thickness (the drawn outline width) — distinct from the
     * taffy {@code border} layout rect.
     */
    public static final StyleKey<Float> borderThickness = floatKey(
        "border-thickness",
        StyleScope.visual,
        StyleApplies.borderWidth
    );

    /**
     * Whether text drawn by the widget renders with a drop shadow — inherited so
     * a themed panel silences shadows on every label inside it. The value lives
     * in the context map; widgets read it via {@code style().get(textShadow)}.
     */
    public static final StyleKey<Boolean> textShadow = track(
        new StyleKey<>(
            "text-shadow",
            Boolean.class,
            StyleScope.visual,
            true,
            null,
            CssEnums.boolParser("text-shadow"),
            (ctx, v) -> {},
            Object::toString
        )
    );

    /**
     * Theme accent color — the "interactive" ink (focused borders, chips,
     * affordances). Inherited so a themed panel tints every descendant;
     * stored as a visual custom property, read via {@code style().get(accent)}.
     */
    public static final StyleKey<Integer> accent = colorKey("accent", true, StyleApplies.customProp("accent"));

    /**
     * Secondary/dimmed text ink — hints, counters, "···" markers. Inherited
     * like {@link #accent}; falls back to the widget's own default when the
     * theme doesn't set it.
     */
    public static final StyleKey<Integer> textDim = colorKey("text-dim", true, StyleApplies.customProp("text-dim"));

    /**
     * Slot-cell fill — the inset color behind item icons in grids. Inherited
     * so a themed panel re-tints every descendant grid; read via
     * {@code style().get(slot)}.
     */
    public static final StyleKey<Integer> slot = colorKey("slot", true, StyleApplies.customProp("slot"));

    /** Hover/active slot fill — the focused variant of {@link #slot}. */
    public static final StyleKey<Integer> slotHot = colorKey("slot-hot", true, StyleApplies.customProp("slot-hot"));

    /** The scene compositing layer the widget renders into. */
    public static final StyleKey<SceneLayer> sceneLayer = key(
        "scene-layer",
        SceneLayer.class,
        StyleScope.custom,
        (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
            if (v instanceof ComponentValue.Ident id) {
                try {
                    return SceneLayer.valueOf(id.value().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    ctx.warn("unknown scene-layer " + id.value());
                    return null;
                }
            }
            return null;
        },
        StyleApplies.sceneLayer
    );

    /** Scrollbar texture/size data — stored as a visual custom property. */
    public static final StyleKey<ScrollbarStyleData> scrollbarStyle = key(
        "scrollbar-style",
        ScrollbarStyleData.class,
        StyleScope.custom,
        (values, ctx) -> {
            // scrollbar-style(track, thumb[, width[, minThumb]])
            ComponentValue v = CssEnums.single(values);
            if (!(v instanceof ComponentValue.Function fn)) {
                ctx.warn("scrollbar-style expects a function value");
                return null;
            }
            List<List<ComponentValue>> args = CssTextures.segments(fn.args());
            VisualTexture track = args.size() > 0 ? CssTextures.parse(args.get(0)) : null;
            VisualTexture thumb = args.size() > 1 ? CssTextures.parse(args.get(1)) : null;
            Float w = args.size() > 2 ? CssEnums.number(args.get(2)) : null;
            Float min = args.size() > 3 ? CssEnums.number(args.get(3)) : null;
            return new ScrollbarStyleData(
                track,
                thumb,
                w == null ? -1 : w.intValue(),
                min == null ? -1 : min.intValue()
            );
        },
        StyleApplies.customProp(ScrollbarStyleData.propertyKey)
    );

    /** Border texture — stored as a visual custom property for renderers. */
    public static final StyleKey<VisualTexture> borderTexture = textureKey(
        "border-texture",
        StyleApplies.customProp("border-texture")
    );

    // endregion

    // region stateful surfaces

    /**
     * {@code base-background} — the resting surface of a stateful widget
     * (button, switch, tab); the state variants below override it.
     */
    public static final StyleKey<VisualTexture> baseBackground = textureKey(
        "base-background",
        StyleApplies.customProp("base-background")
    );

    /** {@code hover-background} — the surface while hovered. */
    public static final StyleKey<VisualTexture> hoverBackground = textureKey(
        "hover-background",
        StyleApplies.customProp("hover-background")
    );

    /** {@code pressed-background} — the surface while pressed. */
    public static final StyleKey<VisualTexture> pressedBackground = textureKey(
        "pressed-background",
        StyleApplies.customProp("pressed-background")
    );

    /** {@code mark-background} — the marked/on half of a two-part control. */
    public static final StyleKey<VisualTexture> markBackground = textureKey(
        "mark-background",
        StyleApplies.customProp("mark-background")
    );

    /** {@code unmark-background} — the unmarked/off half of a two-part control. */
    public static final StyleKey<VisualTexture> unmarkBackground = textureKey(
        "unmark-background",
        StyleApplies.customProp("unmark-background")
    );

    // endregion

    // region overlays & glyphs

    /** {@code hover-overlay} — drawn over the surface while hovered. */
    public static final StyleKey<VisualTexture> hoverOverlay = textureKey(
        "hover-overlay",
        StyleApplies.customProp("hover-overlay")
    );

    /** {@code focus-overlay} — drawn over the surface while focused. */
    public static final StyleKey<VisualTexture> focusOverlay = textureKey(
        "focus-overlay",
        StyleApplies.customProp("focus-overlay")
    );

    /** {@code slot-overlay} — drawn over an item slot's icon. */
    public static final StyleKey<VisualTexture> slotOverlay = textureKey(
        "slot-overlay",
        StyleApplies.customProp("slot-overlay")
    );

    /** {@code arrow} — the disclosure chevron (pagers, dropdowns, tree rows). */
    public static final StyleKey<VisualTexture> arrow = textureKey("arrow", StyleApplies.customProp("arrow"));

    /** {@code collapse-icon} — the marker shown while a collapsible section is open. */
    public static final StyleKey<VisualTexture> collapseIcon = textureKey(
        "collapse-icon",
        StyleApplies.customProp("collapse-icon")
    );

    /** {@code expand-icon} — the marker shown while a collapsible section is closed. */
    public static final StyleKey<VisualTexture> expandIcon = textureKey(
        "expand-icon",
        StyleApplies.customProp("expand-icon")
    );

    // endregion

    // region text metrics

    /**
     * {@code font} — the font resource id every text below this node is drawn
     * with ({@code cloudlib:jetbrains_mono_bold}). Inherited like css
     * {@code font} so a themed panel re-fonts every label inside it.
     */
    public static final StyleKey<ResourceLocation> font = locationKey("font", true, StyleApplies.customProp("font"));

    /** {@code font-size} — inherited text size in ui units. */
    public static final StyleKey<Float> fontSize = metricKey("font-size", true, StyleApplies.customProp("font-size"));

    /** {@code cursor-color} — the text-field caret ink. */
    public static final StyleKey<Integer> cursorColor = colorKey(
        "cursor-color",
        false,
        StyleApplies.customProp("cursor-color")
    );

    // endregion

    // region widget metrics

    /** {@code slider-track-size} — the track thickness of a slider. */
    public static final StyleKey<Float> sliderTrackSize = metricKey(
        "slider-track-size",
        false,
        StyleApplies.customProp("slider-track-size")
    );

    /** {@code slider-handle-size} — the handle size of a slider. */
    public static final StyleKey<Float> sliderHandleSize = metricKey(
        "slider-handle-size",
        false,
        StyleApplies.customProp("slider-handle-size")
    );

    /** {@code scroller-view-margin} — the inset a scroller keeps off its viewport edge. */
    public static final StyleKey<Float> scrollerViewMargin = metricKey(
        "scroller-view-margin",
        false,
        StyleApplies.customProp("scroller-view-margin")
    );

    // endregion

    // region motion — raw until a consumer claims the property

    /**
     * {@code transition} — the animation declaration kept as its raw token
     * stream ({@code transition: all 200ms ease}). Read it with
     * {@code Tokens tokens = style().get(transition)}, which is null when no
     * rule sets the property; a non-null value's {@code text()} is the
     * declaration.
     */
    public static final StyleKey<Tokens> transition = rawKey("transition", StyleApplies.customProp("transition"));

    /** {@code transform} — the 2d transform declaration ({@code translateY(1px)}), kept raw. */
    public static final StyleKey<Tokens> transform = rawKey("transform", StyleApplies.customProp("transform"));

    // endregion

    // region shorthands

    static {

        // edge boxes — css 1/2/3/4-value expansion
        edgeBox("padding", paddingTop, paddingRight, paddingBottom, paddingLeft, CssValues::lengthPercentage);
        edgeBox("margin", marginTop, marginRight, marginBottom, marginLeft, CssValues::lengthAuto);
        edgeBox("inset", insetTop, insetRight, insetBottom, insetLeft, CssValues::lengthAuto);
        edgeBox(
            "border-width",
            borderTopWidth,
            borderRightWidth,
            borderBottomWidth,
            borderLeftWidth,
            CssValues::lengthPercentage
        );
        edgeBox(
            "border",
            borderTopWidth,
            borderRightWidth,
            borderBottomWidth,
            borderLeftWidth,
            CssValues::lengthPercentage
        );

        // all-sides boxes — the LDLib2 spelling of "one value, every edge"
        edgeBox("padding-all", paddingTop, paddingRight, paddingBottom, paddingLeft, CssValues::lengthPercentage);
        edgeBox("margin-all", marginTop, marginRight, marginBottom, marginLeft, CssValues::lengthAuto);

        // logical halves
        edgePair("padding-horizontal", paddingLeft, paddingRight, CssValues::lengthPercentage);
        edgePair("padding-vertical", paddingTop, paddingBottom, CssValues::lengthPercentage);
        edgePair("margin-horizontal", marginLeft, marginRight, CssValues::lengthAuto);
        edgePair("margin-vertical", marginTop, marginBottom, CssValues::lengthAuto);
        edgePair("inset-horizontal", insetLeft, insetRight, CssValues::lengthAuto);
        edgePair("inset-vertical", insetTop, insetBottom, CssValues::lengthAuto);

        // size
        sizePair("size", width, height, CssValues::dimension);
        sizePair("min-size", minWidth, minHeight, CssValues::dimension);
        sizePair("max-size", maxWidth, maxHeight, CssValues::dimension);

        // gap — css order is <row> <column>
        shorthand("gap", (values, ctx) -> {
            List<ComponentValue> flat = CssEnums.flat(values);
            if (flat.isEmpty() || flat.size() > 2) return null;
            LengthPercentage row = CssValues.lengthPercentage(flat.get(0));
            if (row == null) return null;
            LengthPercentage col = flat.size() > 1 ? CssValues.lengthPercentage(flat.get(1)) : row;
            return col == null ? null : List.of(rowGap.of(row), columnGap.of(col));
        });

        // gap-all: <length> — the LDLib2 spelling: one value, both axes
        shorthand("gap-all", (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
            LengthPercentage all = v == null ? null : CssValues.lengthPercentage(v);
            if (all == null) {
                ctx.warn("invalid gap-all", v);
                return null;
            }
            return List.of(rowGap.of(all), columnGap.of(all));
        });

        // overflow: <x> [y]
        shorthand("overflow", (values, ctx) -> {
            List<ComponentValue> flat = CssEnums.flat(values);
            if (flat.isEmpty() || flat.size() > 2) return null;
            Overflow x = CssEnums.enumValue(flat.get(0), Overflow.class, CssEnums.overflow);
            if (x == null) return null;
            Overflow y = flat.size() > 1 ? CssEnums.enumValue(flat.get(1), Overflow.class, CssEnums.overflow) : x;
            return y == null ? null : List.of(overflowX.of(x), overflowY.of(y));
        });

        // flex: none | initial | auto | <grow> [<shrink> [<basis>]]
        shorthand("flex", (values, ctx) -> {
            List<ComponentValue> flat = CssEnums.flat(values);
            if (flat.isEmpty()) return null;
            if (flat.size() == 1 && flat.get(0) instanceof ComponentValue.Ident id) {
                return switch (id.value().toLowerCase(Locale.ROOT)) {
                    case "none", "initial" -> List.of(flexGrow.of(0f), flexShrink.of(0f));
                    case "auto" -> List.of(flexGrow.of(1f), flexShrink.of(1f));
                    default -> null;
                };
            }
            List<StyleValue<?>> out = new ArrayList<>(3);
            Float grow = CssEnums.number(flat.subList(0, 1));
            if (grow == null) return null;
            out.add(flexGrow.of(grow));
            if (flat.size() > 1) {
                Float shrink = CssEnums.number(flat.subList(1, 2));
                if (shrink == null) return null;
                out.add(flexShrink.of(shrink));
            }
            if (flat.size() > 2) {
                TaffyDimension basis = CssValues.dimension(flat.get(2));
                if (basis == null) return null;
                out.add(flexBasis.of(basis));
            }
            return out;
        });

        // flex-flow: <direction> [wrap]
        shorthand("flex-flow", (values, ctx) -> {
            List<ComponentValue> flat = CssEnums.flat(values);
            FlexDirection d = null;
            FlexWrap w = null;
            for (ComponentValue c : flat) {
                if (d == null) d = CssEnums.enumValue(c, FlexDirection.class, CssEnums.flexDirection);
                if (w == null) w = CssEnums.enumValue(c, FlexWrap.class, CssEnums.flexWrap);
            }
            if (d == null && w == null) return null;
            List<StyleValue<?>> out = new ArrayList<>(2);
            if (d != null) out.add(flexDirection.of(d));
            if (w != null) out.add(flexWrap.of(w));
            return out;
        });

        // grid-row / grid-column: <start> [/ <end>]
        shorthand("grid-row", gridLine(gridRowStart, gridRowEnd));
        shorthand("grid-column", gridLine(gridColumnStart, gridColumnEnd));

        // aliases
        alias("text-color", "color");
        alias("textcolor", "color");
        alias("zindex", "z-index");
        alias("shadow", "box-shadow");

        // physical position names — the longhands are the logical inset-*
        alias("top", "inset-top");
        alias("left", "inset-left");
    }

    /** css 1/2/3/4-value edge-box expansion → four longhand keys. */
    private static <T> void edgeBox(
        String name,
        StyleKey<T> top,
        StyleKey<T> right,
        StyleKey<T> bottom,
        StyleKey<T> left,
        Function<ComponentValue, @Nullable T> convert
    ) {
        shorthand(name, (values, ctx) -> {
            List<ComponentValue> flat = CssEnums.flat(values);
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
        String name,
        StyleKey<T> a,
        StyleKey<T> b,
        Function<ComponentValue, @Nullable T> convert
    ) {
        shorthand(name, (values, ctx) -> {
            ComponentValue v = CssEnums.single(values);
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
        String name,
        StyleKey<T> width,
        StyleKey<T> height,
        Function<ComponentValue, @Nullable T> convert
    ) {
        shorthand(name, (values, ctx) -> {
            List<ComponentValue> flat = CssEnums.flat(values);
            if (flat.isEmpty() || flat.size() > 2) return null;
            T w = convert.apply(flat.get(0));
            if (w == null) return null;
            T h = flat.size() > 1 ? convert.apply(flat.get(1)) : w;
            return h == null ? null : List.of(width.of(w), height.of(h));
        });
    }

    /** {@code grid-row|grid-column: <start> [/ <end>]} → start/end placements. */
    private static Shorthand gridLine(StyleKey<GridPlacement> start, StyleKey<GridPlacement> end) {
        return (values, ctx) -> {
            List<ComponentValue> flat = CssEnums.flat(values);
            int slash = -1;
            for (int i = 0; i < flat.size(); i++) {
                if (flat.get(i) instanceof ComponentValue.Delim d && d.value() == '/') {
                    slash = i;
                    break;
                }
            }
            List<ComponentValue> startToks = slash < 0 ? flat : flat.subList(0, slash);
            List<ComponentValue> endToks = slash < 0 ? List.of() : flat.subList(slash + 1, flat.size());
            GridPlacement s = CssValues.placement(startToks);
            if (s == null) return null;
            if (endToks.isEmpty()) {
                return List.of(start.of(s));
            }
            GridPlacement e = CssValues.placement(endToks);
            return e == null ? null : List.of(start.of(s), end.of(e));
        };
    }

    // endregion
}
